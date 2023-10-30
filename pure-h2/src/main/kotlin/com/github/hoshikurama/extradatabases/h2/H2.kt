package com.github.hoshikurama.extradatabases.h2

import com.github.hoshikurama.extradatabases.common.extensions.*
import com.github.hoshikurama.extradatabases.parser.components.SQL
import com.github.hoshikurama.extradatabases.parser.components.Update
import com.github.hoshikurama.extradatabases.parser.components.Where
import com.github.hoshikurama.extradatabases.parser.components.sql
import com.github.hoshikurama.extradatabases.parser.column.Count
import com.github.hoshikurama.extradatabases.parser.column.Distinct
import com.github.hoshikurama.extradatabases.parser.column.TicketColumnField
import com.github.hoshikurama.extradatabases.parser.column.TicketMeta
import com.github.hoshikurama.ticketmanager.api.registry.database.AsyncDatabase
import com.github.hoshikurama.ticketmanager.api.registry.database.utils.DBResult
import com.github.hoshikurama.ticketmanager.api.registry.database.utils.SearchConstraints
import com.github.hoshikurama.ticketmanager.api.ticket.*
import com.github.hoshikurama.tmcoroutine.TMCoroutine
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotliquery.*
import org.h2.jdbcx.JdbcConnectionPool
import java.sql.Connection
import java.sql.Statement
import java.time.Instant
import com.github.hoshikurama.extradatabases.parser.column.Action as ActionCol
import com.github.hoshikurama.extradatabases.parser.column.Ticket as TicketCol

class H2(absoluteDataFolderPath: String, maxConnections: Int) : AsyncDatabase {
    private val connectionPool: JdbcConnectionPool

    init {
        val fixedURL = "jdbc:h2:file:$absoluteDataFolderPath/TicketManager-H2-v10.db"
            .replace("C:", "")
            .replace("\\", "/")

        connectionPool = JdbcConnectionPool.create(fixedURL,"","")
        connectionPool.maxConnections = maxConnections
    }

    private inline fun <T> usingSession(crossinline f: Session.() -> T): T {
        return using(sessionOf(connectionPool)) { f(it) }
    }

    override fun closeDatabase() {
        connectionPool.dispose()
    }

    override suspend fun countOpenTicketsAssignedToAsync(assignments: List<Assignment>): Long {
        return countTicketsWhere {
            TicketCol.Status `==` Ticket.Status.OPEN
            TicketCol.Assignment `in` assignments
        }
    }

    override suspend fun countOpenTicketsAsync(): Long {
        return countTicketsWhere { TicketCol.Status `==` Ticket.Status.OPEN }
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

    override suspend fun getOpenTicketsAssignedToAsync(
        page: Int,
        pageSize: Int,
        assignments: List<Assignment>
    ): DBResult {
        return getFullTicketsAsDBResult(page, pageSize, "ORDER BY PRIORITY DESC, ID DESC", TicketCol.Priority) {
            TicketCol.Status `==` Ticket.Status.OPEN
            TicketCol.Assignment `in` assignments
        }
    }

    override suspend fun getOpenTicketsAsync(page: Int, pageSize: Int): DBResult {
        return getFullTicketsAsDBResult(page, pageSize, "ORDER BY PRIORITY DESC, ID DESC", TicketCol.Priority) {
            TicketCol.Status `==` Ticket.Status.OPEN
        }
    }

    override suspend fun getOpenTicketsNotAssignedAsync(page: Int, pageSize: Int): DBResult {
        return getFullTicketsAsDBResult(page, pageSize, "ORDER BY PRIORITY DESC, ID DESC", TicketCol.Priority) {
            TicketCol.Status `==` Ticket.Status.OPEN
            TicketCol.Assignment `==` Assignment.Nobody
        }
    }

    override suspend fun getOwnedTicketIDsAsync(creator: Creator): List<Long> {
        return getTicketIDsWhere { TicketCol.Creator `==` creator }
    }

    override suspend fun getTicketIDsWithUpdatesAsync(): List<Long> {
        return getTicketIDsWhere { TicketCol.StatusUpdate `==` true }
    }

    override suspend fun getTicketIDsWithUpdatesForAsync(creator: Creator): List<Long> {
        return getTicketIDsWhere {
            TicketCol.StatusUpdate `==` true
            TicketCol.Creator `==` creator
        }
    }

    override suspend fun getTicketOrNullAsync(id: Long): Ticket? = coroutineScope {
        val ticket = async {
            val query = sql {
                selectTicket {
                    +TicketCol.STAR

                    where {
                        TicketCol.ID `==` id
                    }
                }
            }.asQueryOf()

            usingSession { run(query.map(Row::toTicket).asSingle) }
        }

        val actions = async {
            val query = sql {
                selectAction {
                    +ActionCol.STAR

                    where {
                        ActionCol.TicketID `==` id
                    }
                    raw("ORDER BY EPOCH_TIME ASC")
                }
            }.asQueryOf()

            usingSession { run(query.map(Row::toAction).asList) }
        }

        return@coroutineScope ticket.await()?.let { it + actions.await() }
    }

    override fun initializeDatabase() {
        usingSession {

            // Ticket Table
            execute(queryOf("""
                create table if not exists TicketManager_v10_Tickets
                (
                    ID                        NUMBER GENERATED BY DEFAULT AS IDENTITY (START WITH 1) PRIMARY KEY,
                    CREATOR                   VARCHAR_IGNORECASE(70)    not null,
                    PRIORITY                  TINYINT                   not null,
                    STATUS                    VARCHAR_IGNORECASE(10)    not null,
                    ASSIGNED_TO               VARCHAR_IGNORECASE(255)   not null,
                    STATUS_UPDATE_FOR_CREATOR BOOLEAN                   not null
                );""".replace("\n", "").trimIndent()))

            execute(queryOf("""create unique index if not exists INDEX_ID on TicketManager_v10_Tickets (ID);"""))
            execute(queryOf("""create index if not exists INDEX_STATUS_UPDATE_FOR_CREATOR on TicketManager_v10_Tickets (STATUS_UPDATE_FOR_CREATOR);"""))
            execute(queryOf("""create index if not exists INDEX_STATUS on TicketManager_v10_Tickets (STATUS);"""))


            // Actions Table
            execute(queryOf("""
                CREATE TABLE IF NOT EXISTS TicketManager_v10_Actions
                (
                    ACTION_ID       NUMBER GENERATED BY DEFAULT AS IDENTITY (START WITH 1) PRIMARY KEY,
                    TICKET_ID       BIGINT                  NOT NULL,
                    ACTION_TYPE     VARCHAR_IGNORECASE(20)  NOT NULL,
                    CREATOR         VARCHAR_IGNORECASE(70)  NOT NULL,
                    MESSAGE         LONGVARCHAR,
                    EPOCH_TIME      BIGINT                  NOT NULL,
                    SERVER          VARCHAR(100),
                    WORLD           VARCHAR(100),
                    WORLD_X         INT,
                    WORLD_Y         INT,
                    WORLD_Z         INT
                );""".replace("\n", "").trimIndent()))

            execute(queryOf("""CREATE INDEX IF NOT EXISTS INDEX_TICKET_ID ON TicketManager_v10_Actions (TICKET_ID);"""))
        }
    }

    override suspend fun insertActionAsync(id: Long, action: Action): Unit = usingSession {
        update(
            queryOf("INSERT INTO TicketManager_v10_Actions (TICKET_ID, ACTION_TYPE, CREATOR, MESSAGE, EPOCH_TIME, SERVER, WORLD, WORLD_X, WORLD_Y, WORLD_Z) VALUES (?,?,?,?,?,?,?,?,?,?);",
                id,
                action.getEnumForDB().name,
                action.user.asString(),
                action.getMessage(),
                action.timestamp,
                action.location.server,
                action.location.let { if (it is ActionLocation.FromPlayer) it.world else null },
                action.location.let { if (it is ActionLocation.FromPlayer) it.x else null },
                action.location.let { if (it is ActionLocation.FromPlayer) it.y else null },
                action.location.let { if (it is ActionLocation.FromPlayer) it.z else null },
            )
        )
    }

    override suspend fun insertNewTicketAsync(ticket: Ticket): Long {
        var connection: Connection? = null

        val id = try {
            connection = connectionPool.connection
            val statement = connection.prepareStatement("INSERT INTO TicketManager_v10_Tickets (CREATOR, PRIORITY, STATUS, ASSIGNED_TO, STATUS_UPDATE_FOR_CREATOR) VALUES (?,?,?,?,?);", Statement.RETURN_GENERATED_KEYS)
            statement.setString(1, ticket.creator.asString())
            statement.setByte(2, ticket.priority.asByte())
            statement.setString(3, ticket.status.name)
            statement.setString(4, ticket.assignedTo.asString())
            statement.setBoolean(5, ticket.creatorStatusUpdate)
            statement.executeUpdate()
            statement.generatedKeys.let {
                it.next()
                it.getLong(1)
            }
        } catch (e: Exception) {
            throw e
        } finally {
            connection?.close()
        }

        TMCoroutine.Supervised.launch {
            ticket.actions.forEach {
                usingSession {
                    update(
                        queryOf("INSERT INTO TicketManager_v10_Actions (TICKET_ID, ACTION_TYPE, CREATOR, MESSAGE, EPOCH_TIME, SERVER, WORLD, WORLD_X, WORLD_Y, WORLD_Z) VALUES (?,?,?,?,?,?,?,?,?,?);",
                            id,
                            it.getEnumForDB().name,
                            it.user.asString(),
                            it.getMessage(),
                            it.timestamp,
                            it.location.server,
                            it.location.let { if (it is ActionLocation.FromPlayer) it.world else null },
                            it.location.let { if (it is ActionLocation.FromPlayer) it.x else null },
                            it.location.let { if (it is ActionLocation.FromPlayer) it.y else null },
                            it.location.let { if (it is ActionLocation.FromPlayer) it.z else null },
                        )
                    )
                }
            }
        }

        return id
    }

    override suspend fun massCloseTicketsAsync(
        lowerBound: Long,
        upperBound: Long,
        actor: Creator,
        ticketLoc: ActionLocation
    ) {
        val curTime = Instant.now().epochSecond
        val action = ActionInfo(
            user = actor,
            location = ticketLoc,
            timestamp = curTime,
        ).MassClose()

        // Generate applicable IDs query
        val applicableIDsQuery = sql {
            selectTicket {
                +Distinct(TicketCol.ID)

                where {
                    TicketCol.Status `==` Ticket.Status.OPEN
                    TicketCol.ID inRange lowerBound..upperBound
                }
            }
        }.asQueryOf()

        // Get applicable IDs or return if empty
        val applicableIDs = usingSession { run(applicableIDsQuery.map { it.long(1) }.asList) }
            .takeIf { it.isNotEmpty() } ?: return

        // Close Tickets
        val query = sql {
            update(applicableIDs) {
                TicketCol.Status `=` Ticket.Status.CLOSED
            }
        }.asQueryOf()

        usingSession { update(query) }

        applicableIDs.map { insertActionAsync(it, action) }
    }

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

    override suspend fun setAssignmentAsync(ticketID: Long, assignment: Assignment) {
        setAsAsync(ticketID) { TicketCol.Assignment `=` assignment }
    }

    override suspend fun setCreatorStatusUpdateAsync(ticketID: Long, status: Boolean) {
        setAsAsync(ticketID) { TicketCol.StatusUpdate `=` status }
    }

    override suspend fun setPriorityAsync(ticketID: Long, priority: Ticket.Priority) {
        setAsAsync(ticketID) { TicketCol.Priority `=` priority }
    }

    override suspend fun setStatusAsync(ticketID: Long, status: Ticket.Status) {
        setAsAsync(ticketID) { TicketCol.Status `=` status }
    }


    private fun countTicketsWhere(init: Where.Ticket.() -> Unit): Long {
        val query = sql {
            selectTicket {
                +Count(TicketCol.ID)

                where(init)
            }
        }.asQueryOf()

        return usingSession { run(query.map { it.long(1) }.asSingle) }!!
    }

    private fun getTicketIDsWhere(init: Where.Ticket.() -> Unit): List<Long> {
        val query = sql {
            selectTicket {
                +Distinct(TicketCol.ID)

                where(init)
            }
        }.asQueryOf()

        return usingSession { run(query.map { it.long(1) }.asList) }
            .takeIf { it.isNotEmpty() }
            ?: emptyList() // This prevents bug I remember happening in the past with one of the db libraries
    }

    private suspend inline fun getFullTicketsAsDBResult(
        requestedPage: Int,
        pageSize: Int,
        orderBySQL: String,
        vararg columnsDueToOrderBy: TicketColumnField,
        crossinline init: Where.Ticket.() -> Unit
    ): DBResult = coroutineScope {

        // Select only applicable Ticket ID's
        val idQuery = sql {
            selectTicket {
                +TicketCol.ID
                columnsDueToOrderBy.forEach { +it }

                where(init)
                raw(orderBySQL)
            }
        }.asQueryOf()

        val relevantIDs = usingSession { run(idQuery.map { it.long(1) }.asList) }

        // Handles empty result
        if (relevantIDs.isEmpty()) {
            return@coroutineScope DBResult(emptyList(), 0, 0, 0)
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
        val ticketDeferred = async {
            val query = sql {
                selectTicket {
                    +TicketCol.STAR

                    where {
                        TicketCol.ID `in` chunkedIDs[fixedPage-1]
                    }
                    raw(orderBySQL)
                }
            }.asQueryOf()

            usingSession { run(query.map(Row::toTicket).asList) }
        }

        // Acquire Relevant Actions
        val actionsDeferred = async {
            val query = sql {
                selectAction {
                    +ActionCol.STAR

                    where {
                        ActionCol.TicketID `in` chunkedIDs[fixedPage-1]
                    }
                    raw("ORDER BY TICKET_ID ASC, EPOCH_TIME ASC")
                }
            }.asQueryOf()

            usingSession { run(query.map { it.long(2) to it.toAction() }.asList) }
                .groupBy({ it.first }, { it.second })
        }

        // Note: Already sorted
        val fullTickets = ticketDeferred.await()
            .map { it + actionsDeferred.await()[it.id]!! }

        return@coroutineScope DBResult(
            filteredResults = fullTickets,
            totalPages = totalPages,
            totalResults = totalSize,
            returnedPage = fixedPage
        )
    }

    private inline fun setAsAsync(ticketID: Long, crossinline init: Update.Ticket.() -> Unit) {
        val query = sql {
            update(ticketID, init)
        }.asQueryOf()

        usingSession { update(query) }
    }
}


private fun SQL.Completed.asQueryOf() = queryOf(statement, *args.toTypedArray())

private fun Row.toTicket(): Ticket {
    return Ticket(
        id = long(1),
        creator = CreatorString(string(2)).asTicketCreator(),
        priority = byte(3).toPriority(),
        status = Ticket.Status.valueOf(string(4)),
        assignedTo = stringOrNull(5)?.run(::AssignmentString)?.asAssignmentType() ?: Assignment.Nobody,
        creatorStatusUpdate = boolean(6),
        actions = emptyList()
    )
}

private fun Row.toAction(): Action {
    val actionInfo = ActionInfo(
        user = CreatorString(string(4)).asTicketCreator(),
        timestamp = long(6),
        location = kotlin.run {
            val x = intOrNull(9)

            if (x == null) ActionLocation.FromConsole(server = stringOrNull(7))
            else ActionLocation.FromPlayer(
                server = stringOrNull(7),
                world = string(8),
                x = int(9),
                y = int(10),
                z = int(11),
            )
        }
    )

    val msg = stringOrNull(5)
    return when (ActionAsEnum.valueOf(string(3))) {
        ActionAsEnum.OPEN -> actionInfo.Open(msg!!)
        ActionAsEnum.COMMENT -> actionInfo.Comment(msg!!)
        ActionAsEnum.CLOSE -> actionInfo.CloseWithoutComment()
        ActionAsEnum.CLOSE_WITH_COMMENT -> actionInfo.CloseWithComment(msg!!)
        ActionAsEnum.ASSIGN -> actionInfo.Assign(msg?.run(::AssignmentString)?.asAssignmentType() ?: Assignment.Nobody)
        ActionAsEnum.REOPEN -> actionInfo.Reopen()
        ActionAsEnum.SET_PRIORITY -> actionInfo.SetPriority(msg!!.toByte().toPriority())
        ActionAsEnum.MASS_CLOSE -> actionInfo.MassClose()
    }
}