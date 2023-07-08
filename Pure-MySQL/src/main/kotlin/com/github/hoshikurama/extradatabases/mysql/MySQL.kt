package com.github.hoshikurama.extradatabases.mysql

import com.github.hoshikurama.extradatabases.common.extensions.*
import com.github.hoshikurama.extradatabases.parser.column.Count
import com.github.hoshikurama.extradatabases.parser.column.Distinct
import com.github.hoshikurama.extradatabases.parser.column.TicketColumnField
import com.github.hoshikurama.extradatabases.parser.column.TicketMeta
import com.github.hoshikurama.extradatabases.parser.components.SQL
import com.github.hoshikurama.extradatabases.parser.components.Update
import com.github.hoshikurama.extradatabases.parser.components.Where
import com.github.hoshikurama.extradatabases.parser.components.sql
import com.github.hoshikurama.ticketmanager.api.common.TMCoroutine
import com.github.hoshikurama.ticketmanager.api.common.database.AsyncDatabase
import com.github.hoshikurama.ticketmanager.api.common.database.DBResult
import com.github.hoshikurama.ticketmanager.api.common.database.SearchConstraints
import com.github.hoshikurama.ticketmanager.api.common.ticket.*
import com.github.jasync.sql.db.*
import com.github.jasync.sql.db.mysql.MySQLConnectionBuilder
import com.github.jasync.sql.db.mysql.MySQLQueryResult
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.*
import java.time.Instant
import com.github.hoshikurama.extradatabases.parser.column.Ticket as TicketCol
import com.github.hoshikurama.extradatabases.parser.column.Action as ActionCol

class MySQL(
    host: String,
    port: String,
    dbName: String,
    username: String,
    password: String,
) : AsyncDatabase {

    private val connectionPool = MySQLConnectionBuilder.createConnectionPool(
        ConnectionPoolConfiguration(
            host = host,
            port = port.toInt(),
            database = dbName,
            username = username,
            password = password,
        )
    )
    private val suspendingConnection: SuspendingConnection = connectionPool.asSuspending

    private suspend fun SQL.Completed.sendPreparedStatement() =
        suspendingConnection.sendPreparedStatement(statement, args)

    private inline fun <T> QueryResult.mapRowData(crossinline mapper: (RowData) -> T) = rows.map(mapper)

    private suspend inline fun getFullTicketsAsDBResult(
        requestedPage: Int,
        pageSize: Int,
        orderBySQL: String,
        vararg columnsDueToOrderBy: TicketColumnField,
        crossinline init: Where.Ticket.() -> Unit
    ): DBResult = coroutineScope {

        // Select only applicable Ticket ID's
        val relevantIDs = sql {
            selectTicket {
                +TicketCol.ID
                columnsDueToOrderBy.forEach { +it }

                where(init)
                raw(orderBySQL)
            }
        }.sendPreparedStatement()
            .mapRowData { it.getLong(0)!! }

        // Handles empty result
        if (relevantIDs.isEmpty()) {
            return@coroutineScope DBResult(emptyList<Ticket>().toImmutableList(), 0, 0, 0)
        }

        // Get relevant tickets
        val chunkedIDs = if (pageSize == 0) listOf(relevantIDs) else relevantIDs.chunked(pageSize)

        // Store information related to search
        val totalSize = relevantIDs.size
        val totalPages = chunkedIDs.size
        val fixedPage = when {
            totalPages == 0 || requestedPage < 1 -> 1
            requestedPage in 1..totalPages -> requestedPage
            else -> totalPages
        }

        // Acquire Relevant Tickets
        val ticketsDef = async {
            sql {
                selectTicket {
                    +TicketCol.STAR

                    where {
                        TicketCol.ID `in` chunkedIDs[fixedPage-1]
                    }
                    raw(orderBySQL)
                }
            }.sendPreparedStatement()
                .mapRowData(RowData::toTicket)
        }

        // Acquire Relevant Actions
        val actionsDef = async {
            sql {
                selectAction {
                    +ActionCol.STAR

                    where {
                        ActionCol.TicketID `in` chunkedIDs[fixedPage-1]
                    }
                    raw("ORDER BY TICKET_ID ASC, EPOCH_TIME ASC")
                }
            }.sendPreparedStatement()
                .mapRowData { it.getLong(0)!! to it.toAction() }
                .groupBy({ it.first }, { it.second })
        }

        // Note: Already sorted
        val fullTickets = ticketsDef.await()
            .map { it + actionsDef.await()[it.id]!! }
            .toImmutableList()

        return@coroutineScope DBResult(
            filteredResults = fullTickets,
            totalPages = totalPages,
            totalResults = totalSize,
            returnedPage = fixedPage
        )
    }

    // Individual property setters

    private suspend inline fun setAsAsync(ticketID: Long, crossinline init: Update.Ticket.() -> Unit) = coroutineScope {
        sql {
            update(ticketID, init)
        }.sendPreparedStatement()

        CompletableDeferred(Unit)
    }

    override suspend fun setAssignmentAsync(ticketID: Long, assignment: Assignment): Deferred<Unit> =
        setAsAsync(ticketID) { TicketCol.Assignment `=` assignment }
    override suspend fun setCreatorStatusUpdateAsync(ticketID: Long, status: Boolean): Deferred<Unit> =
        setAsAsync(ticketID) { TicketCol.StatusUpdate `=` status }
    override suspend fun setPriorityAsync(ticketID: Long, priority: Ticket.Priority): Deferred<Unit> =
        setAsAsync(ticketID) { TicketCol.Priority `=` priority }
    override suspend fun setStatusAsync(ticketID: Long, status: Ticket.Status): Deferred<Unit> =
        setAsAsync(ticketID) { TicketCol.Status `=` status }

    // Database Additions

    override suspend fun insertActionAsync(id: Long, action: Action): Deferred<Unit> {
        suspendingConnection.sendPreparedStatement(
            query = "INSERT INTO TicketManager_V8_Actions (TICKET_ID, ACTION_TYPE, CREATOR, MESSAGE, EPOCH_TIME, SERVER, WORLD, WORLD_X, WORLD_Y, WORLD_Z) VALUES (?,?,?,?,?,?,?,?,?,?);",
            values = listOf(
                id,
                action.getEnumForDB().name,
                action.user.asString(),
                action.getMessage(),
                action.timestamp,
                action.location.server,
                action.location.let { if (it is ActionLocation.FromPlayer) it.world else null },
                action.location.let { if (it is ActionLocation.FromPlayer) it.x else null },
                action.location.let { if (it is ActionLocation.FromPlayer) it.y else null },
                action.location.let { if (it is ActionLocation.FromPlayer) it.z else null }
            )
        )
        return CompletableDeferred(Unit)
    }

    override suspend fun insertNewTicketAsync(ticket: Ticket): Long {
        val id = suspendingConnection.sendPreparedStatement(
            query = "INSERT INTO TicketManager_V8_Tickets (CREATOR, PRIORITY, STATUS, ASSIGNED_TO, STATUS_UPDATE_FOR_CREATOR) VALUES (?,?,?,?,?);",
            values = listOf(
                ticket.creator.asString(),
                ticket.priority.asByte(),
                ticket.status.name,
                ticket.assignedTo.asString(),
                ticket.creatorStatusUpdate
            )
        )
            .let { it as MySQLQueryResult }
            .lastInsertId

        TMCoroutine.launchSupervised { ticket.actions.map { insertActionAsync(id, it) } }
        return id
    }

    // Get Ticket

    override suspend fun getTicketOrNullAsync(id: Long): Ticket? = coroutineScope {
        val ticketDef = async {
            sql {
                selectTicket {
                    +TicketCol.STAR

                    where {
                        TicketCol.ID `==` id
                    }
                }
            }
                .sendPreparedStatement()
                .mapRowData(RowData::toTicket)
                .firstOrNull()
        }

        val actionDef = async {
            sql {
                selectAction {
                    +ActionCol.STAR

                    where {
                        ActionCol.TicketID `==` id
                    }
                    raw("ORDER BY EPOCH_TIME ASC")
                }
            }
                .sendPreparedStatement()
                .mapRowData(RowData::toAction)
        }

        // Combine Ticket with its Actions
        ticketDef.await()?.let { it + actionDef.await() }
    }

    // Aggregate Operations

    override suspend fun getOpenTicketsAsync(page: Int, pageSize: Int): DBResult  {
        return getFullTicketsAsDBResult(page, pageSize, "ORDER BY PRIORITY DESC, ID DESC", TicketCol.Priority) {
            TicketCol.Status `==` Ticket.Status.OPEN
        }
    }

    override suspend fun getOpenTicketsAssignedToAsync(page: Int, pageSize: Int, assignments: List<Assignment>): DBResult {
        return getFullTicketsAsDBResult(page, pageSize, "ORDER BY PRIORITY DESC, ID DESC", TicketCol.Priority) {
            TicketCol.Status `==` Ticket.Status.OPEN
            TicketCol.Assignment `in` assignments
        }
    }

    override suspend fun getOpenTicketsNotAssignedAsync(page: Int, pageSize: Int): DBResult {
        return getFullTicketsAsDBResult(page, pageSize, "ORDER BY PRIORITY DESC, ID DESC", TicketCol.Priority) {
            TicketCol.Status `==` Ticket.Status.OPEN
            TicketCol.Assignment `==` Assignment.Nobody
        }
    }

    override suspend fun massCloseTicketsAsync(
        lowerBound: Long,
        upperBound: Long,
        actor: Creator,
        ticketLoc: ActionLocation
    ): Deferred<Unit> = coroutineScope {
        val curTime = Instant.now().epochSecond
        val action = ActionInfo(
            user = actor,
            location = ticketLoc,
            timestamp = curTime,
        ).MassClose()

        // Generate applicable IDs or return if empty
        val applicableIDs = sql {
            selectTicket {
                +Distinct(TicketCol.ID)

                where {
                    TicketCol.Status `==` Ticket.Status.OPEN
                    TicketCol.ID inRange lowerBound..upperBound
                }
            }
        }
            .sendPreparedStatement()
            .mapRowData { it.getLong(0)!! }
            .takeIf { it.isNotEmpty() }
            ?: return@coroutineScope CompletableDeferred(Unit)

        // Close Tickets
        val ticketWrite = launch {
            sql {
                update(applicableIDs) {
                    TicketCol.Status `=` Ticket.Status.CLOSED
                }
            }.sendPreparedStatement()
        }

        // Write Actions
        val actionWrites = launch {
            applicableIDs
                .map { insertActionAsync(it, action) }
                .awaitAll()
        }

        listOf(ticketWrite, actionWrites).joinAll()
        CompletableDeferred(Unit)
    }

    // Counting

    private suspend inline fun countTicketsWhere(init: Where.Ticket.() -> Unit): Long {
        return sql {
            selectTicket {
                +Count(TicketCol.ID)

                where(init)
            }
        }
            .sendPreparedStatement()
            .mapRowData { it.getLong(0)!! }
            .first()
    }

    override suspend fun countOpenTicketsAsync(): Long {
        return countTicketsWhere { TicketCol.Status `==` Ticket.Status.OPEN }
    }

    override suspend fun countOpenTicketsAssignedToAsync(assignments: List<Assignment>): Long {
        return countTicketsWhere {
            TicketCol.Status `==` Ticket.Status.OPEN
            TicketCol.Assignment `in` assignments
        }
    }

    // Searching

    override suspend fun searchDatabaseAsync(constraints: SearchConstraints, pageSize: Int): DBResult {
        return getFullTicketsAsDBResult(constraints.requestedPage, pageSize, "ORDER BY ID DESC") {
            constraints.creator?.let {
                when (it.symbol) {
                    SearchConstraints.Symbol.EQUALS -> TicketCol.Creator `==` it.value
                    SearchConstraints.Symbol.NOT_EQUALS -> TicketCol.Creator `!=` it.value
                    else -> throw Exception("Impossible to reach here!")
                }
            }
            constraints.assigned?.let {
                when (it.symbol) {
                    SearchConstraints.Symbol.EQUALS -> TicketCol.Assignment `==` it.value
                    SearchConstraints.Symbol.NOT_EQUALS -> TicketCol.Assignment `!=` it.value
                    else -> throw Exception("Impossible to reach here!")
                }
            }
            constraints.priority?.let {
                when (it.symbol) {
                    SearchConstraints.Symbol.EQUALS -> TicketCol.Priority `==` it.value
                    SearchConstraints.Symbol.NOT_EQUALS -> TicketCol.Priority `!=` it.value
                    SearchConstraints.Symbol.LESS_THAN -> TicketCol.Priority `＜` it.value
                    SearchConstraints.Symbol.GREATER_THAN -> TicketCol.Priority `＞` it.value
                }
            }
            constraints.status?.let {
                when (it.symbol) {
                    SearchConstraints.Symbol.EQUALS -> TicketCol.Status `==` it.value
                    SearchConstraints.Symbol.NOT_EQUALS -> TicketCol.Status `!=` it.value
                    else -> throw Exception("Impossible to reach here!")
                }
            }


            val whereActionNeeded = constraints.run { listOf(closedBy, lastClosedBy, world, creationTime, keywords) }
                .any { it != null }

            if (whereActionNeeded) {
                whereAction {
                    constraints.closedBy?.let {
                        when (it.symbol) {
                            SearchConstraints.Symbol.EQUALS -> TicketMeta.ClosedBy `==` it.value
                            SearchConstraints.Symbol.NOT_EQUALS -> TicketMeta.ClosedBy `!=` it.value
                            else -> throw Exception("Impossible to reach here!")
                        }
                    }
                    constraints.lastClosedBy?.let {
                        when (it.symbol) {
                            SearchConstraints.Symbol.EQUALS -> TicketMeta.LastClosedBy `==` it.value
                            SearchConstraints.Symbol.NOT_EQUALS -> TicketMeta.LastClosedBy `!=` it.value
                            else -> throw Exception("Impossible to reach here!")
                        }
                    }
                    constraints.world?.let {
                        when (it.symbol) {
                            SearchConstraints.Symbol.EQUALS -> TicketMeta.CreationWorld `==` it.value
                            SearchConstraints.Symbol.NOT_EQUALS -> TicketMeta.CreationWorld `!=` it.value
                            else -> throw Exception("Impossible to reach here!")
                        }
                    }
                    constraints.creationTime?.let {
                        when (it.symbol) {
                            SearchConstraints.Symbol.LESS_THAN -> TicketMeta.TimeCreated before it.value
                            SearchConstraints.Symbol.GREATER_THAN -> TicketMeta.TimeCreated after it.value
                            else -> throw Exception("Impossible to reach here!")
                        }
                    }
                    constraints.keywords?.let {
                        when (it.symbol) {
                            SearchConstraints.Symbol.EQUALS -> TicketMeta.Keywords `in` it.value
                            SearchConstraints.Symbol.NOT_EQUALS -> TicketMeta.Keywords `!in` it.value
                            else -> throw Exception("Impossible to reach here!")
                        }
                    }
                }
            }
        }
    }

    // ID Acquisition

    private suspend inline fun getTicketIDsWhere(init: Where.Ticket.() -> Unit): List<Long> {
        return sql {
            selectTicket {
                +Distinct(TicketCol.ID)

                where(init)
            }
        }
            .sendPreparedStatement()
            .mapRowData { it.getLong(0)!! }
            .takeIf { it.isNotEmpty() }
            ?.toImmutableList()
            ?: emptyList<Long>().toImmutableList() // This prevents bug I remember happening in the past with one of the db libraries
    }

    override suspend fun getTicketIDsWithUpdatesAsync(): List<Long> {
        return getTicketIDsWhere { TicketCol.StatusUpdate `==` true }
    }

    override suspend fun getTicketIDsWithUpdatesForAsync(creator: Creator):List<Long> {
        return getTicketIDsWhere {
            TicketCol.StatusUpdate `==` true
            TicketCol.Creator `==` creator
        }
    }

    override suspend fun getOwnedTicketIDsAsync(creator: Creator): List<Long> {
        return getTicketIDsWhere { TicketCol.Creator `==` creator }
    }

    override suspend fun getOpenTicketIDsAsync(): List<Long> {
        return getTicketIDsWhere { TicketCol.Status `==` Ticket.Status.OPEN }
    }

    override suspend fun getOpenTicketIDsForUser(creator: Creator): List<Long> {
        return getTicketIDsWhere {
            TicketCol.Status `==` Ticket.Status.OPEN
            TicketCol.Creator `==` creator
        }
    }

    // Internal Database Functions

    override fun closeDatabase() {
        connectionPool.disconnect()
    }

    override fun initializeDatabase(): Unit = runBlocking {
        suspendingConnection.connect()

        suspend fun tableNotExists(table: String) = suspendingConnection
            .sendQuery("SHOW TABLES;")
            .mapRowData { it.getString(0)!! }
            .none { it.lowercase() == table.lowercase() }

        if (tableNotExists("TicketManager_V8_Tickets")) {
            suspendingConnection.sendQuery(
                """
                        CREATE TABLE TicketManager_V8_Tickets (
                            ID BIGINT NOT NULL AUTO_INCREMENT,
                            CREATOR VARCHAR(70) CHARACTER SET latin1 COLLATE latin1_general_ci NOT NULL,
                            PRIORITY TINYINT NOT NULL,
                            STATUS VARCHAR(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
                            ASSIGNED_TO VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
                            STATUS_UPDATE_FOR_CREATOR BOOLEAN NOT NULL,
                            KEY STATUS_V (STATUS) USING BTREE,
                            KEY STATUS_UPDATE_FOR_CREATOR_V (STATUS_UPDATE_FOR_CREATOR) USING BTREE,
                            PRIMARY KEY (ID)
                    ) ENGINE=InnoDB;
                """.replace("\n", "").trimIndent()
            )
        }

        if (tableNotExists("TicketManager_V8_Actions")) {
            suspendingConnection.sendQuery(
                """
                        CREATE TABLE TicketManager_V8_Actions (
                            ACTION_ID BIGINT NOT NULL AUTO_INCREMENT,
                            TICKET_ID BIGINT NOT NULL,
                            ACTION_TYPE VARCHAR(20) CHARACTER SET latin1 COLLATE latin1_general_ci NOT NULL,
                            CREATOR VARCHAR(64) CHARACTER SET latin1 COLLATE latin1_general_ci NOT NULL,
                            MESSAGE TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
                            EPOCH_TIME BIGINT NOT NULL,
                            SERVER VARCHAR(100) CHARACTER SET utf8mb4,
                            WORLD VARCHAR(100) CHARACTER SET utf8mb4,
                            WORLD_X INT,
                            WORLD_Y INT,
                            WORLD_Z INT,
                            KEY TICKET_ID_V (TICKET_ID) USING BTREE,
                            PRIMARY KEY (ACTION_ID)
                        ) ENGINE=InnoDB;
                """.replace("\n", "").trimIndent()
            )
        }
    }
}

private fun RowData.toAction(): Action {
    val actionInfo = ActionInfo(
        user = CreatorString(getString(3)!!).asTicketCreator(),
        timestamp = getLong(5)!!,
        location = kotlin.run {
            val x = getInt(8)

            if (x == null) ActionLocation.FromConsole(server = getString(6))
            else ActionLocation.FromPlayer(
                server = getString(6),
                world = getString(7)!!,
                x = getInt(8)!!,
                y = getInt(9)!!,
                z = getInt(10)!!,
            )
        }
    )

    val msg = getString(4)
    return when (ActionAsEnum.valueOf(getString(2)!!)) {
        ActionAsEnum.ASSIGN -> actionInfo.Assign(msg?.run(::AssignmentString)?.asAssignmentType() ?: Assignment.Nobody)
        ActionAsEnum.CLOSE -> actionInfo.CloseWithoutComment()
        ActionAsEnum.CLOSE_WITH_COMMENT -> actionInfo.CloseWithComment(msg!!)
        ActionAsEnum.COMMENT -> actionInfo.Comment(msg!!)
        ActionAsEnum.OPEN -> actionInfo.Open(msg!!)
        ActionAsEnum.REOPEN -> actionInfo.Reopen()
        ActionAsEnum.SET_PRIORITY -> actionInfo.SetPriority(msg!!.toByte().toPriority())
        ActionAsEnum.MASS_CLOSE -> actionInfo.MassClose()
    }
}

private fun RowData.toTicket(): Ticket {
    return Ticket(
        id = getLong(0)!!,
        creator = CreatorString(getString(1)!!).asTicketCreator(),
        priority = getByte(2)!!.toPriority(),
        status = getString(3)!!.run(Ticket.Status::valueOf),
        assignedTo = getString(4)?.run(::AssignmentString)?.asAssignmentType() ?: Assignment.Nobody,
        creatorStatusUpdate = getBoolean(5)!!,
        actions = emptyList<Action>().toImmutableList()
    )
}