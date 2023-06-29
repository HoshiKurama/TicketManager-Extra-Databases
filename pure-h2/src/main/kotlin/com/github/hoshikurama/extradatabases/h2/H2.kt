package com.github.hoshikurama.extradatabases.h2

import com.github.hoshikurama.extradatabases.h2.extensions.*
import com.github.hoshikurama.extradatabases.h2.extensions.ActionAsEnum
import com.github.hoshikurama.extradatabases.h2.parser.*
import com.github.hoshikurama.ticketmanager.api.common.database.AsyncDatabase
import com.github.hoshikurama.ticketmanager.api.common.database.DBResult
import com.github.hoshikurama.ticketmanager.api.common.database.SearchConstraints
import com.github.hoshikurama.ticketmanager.api.common.ticket.*
import com.google.common.collect.ImmutableList
import kotliquery.*
import org.h2.jdbcx.JdbcConnectionPool
import java.sql.Connection
import java.sql.Statement
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.CompletableFuture as CF

class H2(absoluteDataFolderPath: String) : AsyncDatabase {
    private val connectionPool: JdbcConnectionPool
    init {
        val fixedURL = "jdbc:h2:file:$absoluteDataFolderPath/TicketManager-H2-V8.db"
            .replace("C:", "")
            .replace("\\", "/")

        connectionPool = JdbcConnectionPool.create(fixedURL,"","")
        connectionPool.maxConnections = 3
    }

    private inline fun <T> usingSession(crossinline f: Session.() -> T): T {
        return using(sessionOf(connectionPool)) { f(it) }
    }

    private fun updateAsync(ticketID: Long, builder: Insert.FromTicketColumn.() -> Unit) = CF.runAsync {
        val (statement, args) = SQL.update(ticketID, builder).addEnding()
        usingSession { update(queryOf(statement, *args.toTypedArray())) }
    }

    private fun selectFullTickets(whereBuilder: Where.TicketCol.() -> Unit) = CF.supplyAsync {
        val (statement, args) = SQL.selectPartialTicket(whereBuilder)
        statement.append("ORDER BY ID ASC;")

        usingSession {
            val partialTickets = run(queryOf(statement.toString(), *args.toTypedArray())
                .map(Row::toTicket)
                .asList
            )

            if (partialTickets.isEmpty())
                return@usingSession emptyList()

            val ids = partialTickets
                .asSequence()
                .map(Ticket::id)
                .map(Long::toString)
                .joinToString(",")

            val actionsWithIDs = run(queryOf("SELECT * FROM \"TicketManager_V8_Actions\" WHERE TICKET_ID IN ($ids) ORDER BY TICKET_ID ASC, EPOCH_TIME ASC;")
                .map { it.long(2) to it.toAction() }
                .asList
            ).groupBy({ it.first }, { it.second })

            // Note: Already sorted
            partialTickets.map { it + actionsWithIDs[it.id]!! }
        }
    }

    private fun selectPartialTickets(whereBuilder: Where.TicketCol.() -> Unit) = CF.supplyAsync {
        val (statement, args) = SQL.selectPartialTicket(whereBuilder).addEnding()

        usingSession {
            run(queryOf(statement, *args.toTypedArray())
                .map(Row::toTicket)
                .asList
            )
        }
    }


    override fun closeDatabase() {
        connectionPool.connection.createStatement().execute("SHUTDOWN")
    }

    override fun countOpenTicketsAssignedToAsync(assignments: List<Assignment>): CF<Long> = CF.supplyAsync {
        val assignedSQL = assignments
            .map(Assignment::asString)
            .joinToString(" OR ") { "ASSIGNED_TO = ?" }

        usingSession {
            run(queryOf("SELECT COUNT(*) FROM \"TicketManager_V8_Tickets\" WHERE STATUS = ? AND ($assignedSQL);", *assignments.toTypedArray())
                .map { it.long(1) }
                .asSingle
            )
        }
    }

    override fun countOpenTicketsAsync(): CF<Long> {
        return selectFullTickets {
            SQLTicket.status equalTo Ticket.Status.OPEN
        }.thenApplyAsync {
            it.count().toLong()
        }
    }

    override fun getOpenTicketIDsAsync(): CF<ImmutableList<Long>> = CF.supplyAsync {
        val (statement, args) = SQL.select(SQLTicket.id) {
            SQLTicket.status equalTo Ticket.Status.OPEN
        }.addEnding()

        usingSession {
            run(queryOf(statement, *args.toTypedArray())
                .map { it.long(1) }
                .asList
            )
        }.let { ImmutableList.copyOf(it) }
    }

    override fun getOpenTicketIDsForUser(creator: Creator): CF<ImmutableList<Long>> = CF.supplyAsync {
        val (statement, args) = SQL.select(SQLTicket.id) {
            SQLTicket.status equalTo Ticket.Status.OPEN
            SQLTicket.creator equalTo creator
        }.addEnding()

        usingSession {
            run(queryOf(statement, *args.toTypedArray())
                .map { it.long(1) }
                .asList
            )
        }.let { ImmutableList.copyOf(it) }
    }

    override fun getOpenTicketsAssignedToAsync(
        page: Int,
        pageSize: Int,
        assignments: List<Assignment>
    ): CF<DBResult> {
        return selectFullTickets {
            SQLTicket.status equalTo Ticket.Status.OPEN
            andAny {
                assignments.forEach {
                    SQLTicket.assignment equalTo it
                }
            }
        }.thenComposeAsync {
            ticketsFilteredByAsync(page, pageSize, it)
        }
    }

    override fun getOpenTicketsAsync(page: Int, pageSize: Int): CF<DBResult> {
        return selectFullTickets {
            SQLTicket.status equalTo Ticket.Status.OPEN
        }.thenComposeAsync {
            ticketsFilteredByAsync(page, pageSize, it)
        }
    }

    override fun getOpenTicketsNotAssignedAsync(page: Int, pageSize: Int): CF<DBResult> {
        return selectFullTickets {
            SQLTicket.status equalTo Ticket.Status.OPEN
            SQLTicket.assignment equalTo Assignment.Nobody
        }.thenComposeAsync {
            ticketsFilteredByAsync(page, pageSize, it)
        }
    }

    override fun getOwnedTicketIDsAsync(creator: Creator): CF<ImmutableList<Long>> = CF.supplyAsync {
        val (stmt, args) = SQL.select(SQLTicket.id) {
            SQLTicket.creator equalTo creator
        }.addEnding()

        usingSession {
            run(queryOf(stmt, *args.toTypedArray())
                .map { it.long(1) }
                .asList
            )
        }.let { ImmutableList.copyOf(it) }
    }

    override fun getTicketIDsWithUpdatesAsync(): CF<ImmutableList<Long>> = CF.supplyAsync {
        val (stmt, args) = SQL.select(SQLTicket.id) {
            SQLTicket.creatorStatusUpdate equalTo true
        }.addEnding()

        usingSession {
            run(queryOf(stmt, *args.toTypedArray())
                .map { it.long(1) }
                .asList
            )
        }.let { ImmutableList.copyOf(it) }
    }

    override fun getTicketIDsWithUpdatesForAsync(creator: Creator): CF<ImmutableList<Long>> = CF.supplyAsync {
        val (stmt, args) = SQL.select(SQLTicket.id) {
            SQLTicket.creatorStatusUpdate equalTo true
            SQLTicket.creator equalTo creator
        }.addEnding()

        usingSession {
            run(queryOf(stmt, *args.toTypedArray())
                .map { it.long(1) }
                .asList
            )
        }.let { ImmutableList.copyOf(it) }
    }

    override fun getTicketOrNullAsync(id: Long): CF<Ticket?> {
        val ticketDef = selectPartialTickets {
            SQLTicket.id equalTo id
        }.thenApplyAsync(List<Ticket>::firstOrNull)

        val actionsDef = CF.supplyAsync {
            usingSession {
                run(queryOf("SELECT * FROM \"TicketManager_V8_Actions\" WHERE TICKET_ID = ? ORDER BY EPOCH_TIME ASC", id)
                    .map(Row::toAction)
                    .asList
                )
            }
        }

        return CF.allOf(actionsDef, ticketDef).thenApplyAsync {
            ticketDef.join()?.let { it + actionsDef.join() }
        }
    }

    private fun ticketsFilteredByAsync(page: Int, pageSize: Int, unchunkedTickets: List<Ticket>): CF<DBResult> {
        val totalSize = AtomicInteger(0)
        val totalPages = AtomicInteger(0)

        val sortedTickets = unchunkedTickets
            .sortedWith(compareByDescending<Ticket> { it.priority.asByte() }
            .thenByDescending(Ticket::id))

        totalSize.set(sortedTickets.count())

        val chunkedTickets = sortedTickets.let {
            if (pageSize == 0 || it.isEmpty())
                listOf(it)
            else it.chunked(pageSize)
        }

        totalPages.set(chunkedTickets.count())

        val fixedPage = when {
            totalPages.get() == 0 || page < 1 -> 1
            page in 1..totalPages.get()-> page
            else -> totalPages.get()
        }

        return CF.completedFuture(DBResult(
            filteredResults = chunkedTickets.getOrElse(fixedPage-1) { listOf() }.let { ImmutableList.copyOf(it) },
            totalPages = totalPages.get(),
            totalResults = totalSize.get(),
            returnedPage = fixedPage
        ))
    }

    override fun initializeDatabase() {
        usingSession {

            // Ticket Table
            execute(queryOf("""
                create table if not exists "TicketManager_V8_Tickets"
                (
                    ID                        NUMBER GENERATED BY DEFAULT AS IDENTITY (START WITH 1 INCREMENT BY 1) not null,
                    CREATOR                   VARCHAR_IGNORECASE(70)    not null,
                    PRIORITY                  TINYINT                   not null,
                    STATUS                    VARCHAR_IGNORECASE(10)    not null,
                    ASSIGNED_TO               VARCHAR_IGNORECASE(255)   not null,
                    STATUS_UPDATE_FOR_CREATOR BOOLEAN                   not null,
                    constraint "Ticket_ID"
                        primary key (ID)
                );""".replace("\n", "").trimIndent()))

            execute(queryOf("""create unique index if not exists INDEX_ID on "TicketManager_V8_Tickets" (ID);"""))
            execute(queryOf("""create index if not exists INDEX_STATUS_UPDATE_FOR_CREATOR on "TicketManager_V8_Tickets" (STATUS_UPDATE_FOR_CREATOR);"""))
            execute(queryOf("""create index if not exists INDEX_STATUS on "TicketManager_V8_Tickets" (STATUS);"""))


            // Actions Table
            execute(queryOf("""
                CREATE TABLE IF NOT EXISTS "TicketManager_V8_Actions"
                (
                    ACTION_ID       NUMBER GENERATED ALWAYS AS IDENTITY (START WITH 1 INCREMENT BY 1) not null,
                    TICKET_ID       BIGINT                  NOT NULL,
                    ACTION_TYPE     VARCHAR_IGNORECASE(20)  NOT NULL,
                    CREATOR         VARCHAR_IGNORECASE(70)  NOT NULL,
                    MESSAGE         LONGVARCHAR,
                    EPOCH_TIME      BIGINT                  NOT NULL,
                    SERVER          VARCHAR(100),
                    WORLD           VARCHAR(100),
                    WORLD_X         INT,
                    WORLD_Y         INT,
                    WORLD_Z         INT,
                    constraint "Actions_Action_ID"
                        primary key (ACTION_ID)
                );""".replace("\n", "").trimIndent()))

            execute(queryOf("""CREATE INDEX IF NOT EXISTS INDEX_TICKET_ID ON "TicketManager_V8_Actions" (TICKET_ID);"""))
        }
    }

    override fun insertActionAsync(id: Long, action: Action): CF<Void> = CF.runAsync {
        usingSession {
            update(
                queryOf("INSERT INTO \"TicketManager_V8_Actions\" (TICKET_ID, ACTION_TYPE, CREATOR, MESSAGE, EPOCH_TIME, SERVER, WORLD, WORLD_X, WORLD_Y, WORLD_Z) VALUES (?,?,?,?,?,?,?,?,?,?);",
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
    }

    override fun insertNewTicketAsync(ticket: Ticket): CF<Long> = CF.supplyAsync {
        var connection: Connection? = null

        val id = try {
            connection = connectionPool.connection
            val statement = connection.prepareStatement("INSERT INTO \"TicketManager_V8_Tickets\" (CREATOR, PRIORITY, STATUS, ASSIGNED_TO, STATUS_UPDATE_FOR_CREATOR) VALUES(?,?,?,?,?);", Statement.RETURN_GENERATED_KEYS)
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

        CompletableFuture.runAsync {
            ticket.actions.forEach {
                usingSession {
                    update(
                        queryOf("INSERT INTO \"TicketManager_V8_Actions\" (TICKET_ID, ACTION_TYPE, CREATOR, MESSAGE, EPOCH_TIME, SERVER, WORLD, WORLD_X, WORLD_Y, WORLD_Z) VALUES (?,?,?,?,?,?,?,?,?,?);",
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

        return@supplyAsync id
    }

    override fun massCloseTicketsAsync(
        lowerBound: Long,
        upperBound: Long,
        actor: Creator,
        ticketLoc: ActionLocation
    ): CF<Void> {
        val curTime = Instant.now().epochSecond
        val ticketIds = (lowerBound..upperBound).toList()
        val action = ActionInfo(
            user = actor,
            location = ticketLoc,
            timestamp = curTime,
        ).MassClose()

        usingSession { update(queryOf("UPDATE \"TicketManager_V8_Tickets\" SET STATUS = ? WHERE ID IN (${ticketIds.joinToString(", ")});", Ticket.Status.CLOSED.name)) }


        return usingSession {
            ticketIds.map { insertActionAsync(it, action)
            }.flatten()
        }.thenAcceptAsync {  }
    }

    override fun searchDatabaseAsync(constraints: SearchConstraints, pageSize: Int): CF<DBResult> {
        // Build Search string
        val relevantIDs = SQL.select(SQLTicket.id) {
            constraints.creator?.let {
                when (it.symbol) {
                    SearchConstraints.Symbol.EQUALS -> SQLTicket.creator equalTo it.value
                    SearchConstraints.Symbol.NOT_EQUALS -> SQLTicket.creator notEqualTo it.value
                    else -> throw Exception("Impossible to reach here!")
                }
            }
            constraints.assigned?.let {
                when (it.symbol) {
                    SearchConstraints.Symbol.EQUALS -> SQLTicket.assignment equalTo it.value
                    SearchConstraints.Symbol.NOT_EQUALS -> SQLTicket.assignment notEqualTo it.value
                    else -> throw Exception("Impossible to reach here!")
                }
            }
            constraints.priority?.let {
                when (it.symbol) {
                    SearchConstraints.Symbol.EQUALS -> SQLTicket.priority equalTo it.value
                    SearchConstraints.Symbol.NOT_EQUALS -> SQLTicket.priority notEqualTo it.value
                    SearchConstraints.Symbol.LESS_THAN -> SQLTicket.priority lessThan it.value
                    SearchConstraints.Symbol.GREATER_THAN -> SQLTicket.priority greaterThan it.value
                }
            }
            constraints.status?.let {
                when (it.symbol) {
                    SearchConstraints.Symbol.EQUALS -> SQLTicket.status equalTo it.value
                    SearchConstraints.Symbol.NOT_EQUALS -> SQLTicket.status notEqualTo it.value
                    else -> throw Exception("Impossible to reach here!")
                }
            }
            constraints.closedBy?.let {
                when (it.symbol) {
                    SearchConstraints.Symbol.EQUALS -> SQLAction.creator closedBy it.value
                    SearchConstraints.Symbol.NOT_EQUALS -> SQLAction.creator notClosedBy it.value
                    else -> throw Exception("Impossible to reach here!")
                }
            }
            constraints.lastClosedBy?.let {
                when (it.symbol) {
                    SearchConstraints.Symbol.EQUALS -> SQLAction.creator lastClosedBy it.value
                    SearchConstraints.Symbol.NOT_EQUALS -> SQLAction.creator notLastClosedBy it.value
                    else -> throw Exception("Impossible to reach here!")
                }
            }
            constraints.world?.let {
                when (it.symbol) {
                    SearchConstraints.Symbol.EQUALS -> SQLAction.world equalTo it.value
                    SearchConstraints.Symbol.NOT_EQUALS -> SQLAction.world notEqualTo it.value
                    else -> throw Exception("Impossible to reach here!")
                }
            }
            constraints.creationTime?.let {
                when (it.symbol) {
                    SearchConstraints.Symbol.LESS_THAN -> SQLAction.epochTime madeAfter it.value
                    SearchConstraints.Symbol.GREATER_THAN -> SQLAction.epochTime madeBefore it.value
                    else -> throw Exception("Impossible to reach here!")
                }
            }
            constraints.keywords?.let {
                when (it.symbol) {
                    SearchConstraints.Symbol.EQUALS -> SQLAction.keyword containing it.value
                    SearchConstraints.Symbol.NOT_EQUALS -> SQLAction.keyword notContaining it.value
                    else -> throw Exception("Impossible to reach here!")
                }
            }
        }.let { (statement, args) ->
            usingSession {
                run(queryOf("$statement ORDER BY ID DESC;", *args.toTypedArray())
                    .map { it.toTicket() }
                    .asList
                )
            }
        }

        // Handles empty result
        if (relevantIDs.isEmpty()) {
            return DBResult(ImmutableList.of(), 0, 0, 0)
                .let { CompletableFuture.completedFuture(it) }

        }

        // Get relevant tickets
        val chunkedIDs = if (pageSize == 0) listOf(relevantIDs) else relevantIDs.chunked(pageSize)

        // Store information related to search
        val totalSize = relevantIDs.size
        val totalPages = chunkedIDs.size
        val fixedPage = when {
            totalPages == 0 || constraints.requestedPage < 1 -> 1
            constraints.requestedPage in 1..totalPages -> constraints.requestedPage
            else -> totalPages
        }

        // Only get the correct number
        return selectFullTickets {
            SQLTicket.id inside chunkedIDs[fixedPage]
        }.thenApplyAsync {
            DBResult(
                filteredResults = ImmutableList.copyOf(it),
                totalPages = totalPages,
                totalResults = totalSize,
                returnedPage = fixedPage,
            )
        }
    }

    override fun setAssignmentAsync(ticketID: Long, assignment: Assignment): CF<Void> = updateAsync(ticketID) {
        SQLTicket.assignment setTo assignment
    }

    override fun setCreatorStatusUpdateAsync(ticketID: Long, status: Boolean): CF<Void> = updateAsync(ticketID) {
        SQLTicket.creatorStatusUpdate setTo status
    }

    override fun setPriorityAsync(ticketID: Long, priority: Ticket.Priority): CF<Void> = updateAsync(ticketID) {
        SQLTicket.priority setTo priority
    }

    override fun setStatusAsync(ticketID: Long, status: Ticket.Status): CF<Void> = updateAsync(ticketID) {
        SQLTicket.status setTo status
    }
}

private fun Row.toTicket(): Ticket {
    return Ticket(
        id = long(1),
        creator = CreatorString(string(2)).asTicketCreator(),
        priority = byte(3).toPriority(),
        status = Ticket.Status.valueOf(string(4)),
        assignedTo = stringOrNull(5)?.run(::AssignmentString)?.asAssignmentType() ?: Assignment.Nobody,
        creatorStatusUpdate = boolean(6),
        actions = ImmutableList.of()
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