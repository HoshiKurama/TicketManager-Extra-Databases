package com.github.hoshikurama.extradatabases.h2

import com.github.hoshikurama.ticketmanager.api.common.database.AsyncDatabase
import com.github.hoshikurama.ticketmanager.api.common.database.DBResult
import com.github.hoshikurama.ticketmanager.api.common.database.SearchConstraints
import com.github.hoshikurama.ticketmanager.api.common.ticket.*
import com.google.common.collect.ImmutableList
import java.util.concurrent.CompletableFuture

class H2 : AsyncDatabase {
    /*
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

     */

    override fun closeDatabase() {
        TODO("Not yet implemented")
    }

    override fun countOpenTicketsAssignedToAsync(assignments: List<Assignment>): CompletableFuture<Long> {
        TODO("Not yet implemented")
    }

    override fun countOpenTicketsAsync(): CompletableFuture<Long> {
        TODO("Not yet implemented")
    }

    override fun getOpenTicketIDsAsync(): CompletableFuture<ImmutableList<Long>> {
        TODO("Not yet implemented")
    }

    override fun getOpenTicketIDsForUser(creator: Creator): CompletableFuture<ImmutableList<Long>> {
        TODO("Not yet implemented")
    }

    override fun getOpenTicketsAssignedToAsync(
        page: Int,
        pageSize: Int,
        assignments: List<Assignment>
    ): CompletableFuture<DBResult> {
        TODO("Not yet implemented")
    }

    override fun getOpenTicketsAsync(page: Int, pageSize: Int): CompletableFuture<DBResult> {
        TODO("Not yet implemented")
    }

    override fun getOpenTicketsNotAssignedAsync(page: Int, pageSize: Int): CompletableFuture<DBResult> {
        TODO("Not yet implemented")
    }

    override fun getOwnedTicketIDsAsync(creator: Creator): CompletableFuture<ImmutableList<Long>> {
        TODO("Not yet implemented")
    }

    override fun getTicketIDsWithUpdatesAsync(): CompletableFuture<ImmutableList<Long>> {
        TODO("Not yet implemented")
    }

    override fun getTicketIDsWithUpdatesForAsync(creator: Creator): CompletableFuture<ImmutableList<Long>> {
        TODO("Not yet implemented")
    }

    override fun getTicketOrNullAsync(id: Long): CompletableFuture<Ticket?> {
        TODO("Not yet implemented")
    }

    override fun initializeDatabase() {
        TODO("Not yet implemented")
    }

    override fun insertActionAsync(id: Long, action: Action): CompletableFuture<Void> {
        TODO("Not yet implemented")
    }

    override fun insertNewTicketAsync(ticket: Ticket): CompletableFuture<Long> {
        TODO("Not yet implemented")
    }

    override fun massCloseTicketsAsync(
        lowerBound: Long,
        upperBound: Long,
        actor: Creator,
        ticketLoc: ActionLocation
    ): CompletableFuture<Void> {
        TODO("Not yet implemented")
    }

    override fun searchDatabaseAsync(constraints: SearchConstraints, pageSize: Int): CompletableFuture<DBResult> {
        TODO("Not yet implemented")
    }

    override fun setAssignmentAsync(ticketID: Long, assignment: Assignment): CompletableFuture<Void> {
        TODO("Not yet implemented")
    }

    override fun setCreatorStatusUpdateAsync(ticketID: Long, status: Boolean): CompletableFuture<Void> {
        TODO("Not yet implemented")
    }

    override fun setPriorityAsync(ticketID: Long, priority: Ticket.Priority): CompletableFuture<Void> {
        TODO("Not yet implemented")
    }

    override fun setStatusAsync(ticketID: Long, status: Ticket.Status): CompletableFuture<Void> {
        TODO("Not yet implemented")
    }
}
/*
/*
class H2(
    absoluteDataFolderPath: String
) : AsyncDatabase {

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


    override fun setAssignmentAsync(ticketID: Long, assignment: Assignment): CompletableFuture<Void> = CompletableFuture.runAsync {
        usingSession {
            update(queryOf("UPDATE \"TicketManager_V8_Tickets\" SET ASSIGNED_TO = ? WHERE ID = ?;", assignment.asString(), ticketID))
        }
    }

    override fun setCreatorStatusUpdateAsync(ticketID: Long, status: Boolean): CompletableFuture<Void> = CompletableFuture.runAsync {
        usingSession {
            update(queryOf("UPDATE \"TicketManager_V8_Tickets\" SET STATUS_UPDATE_FOR_CREATOR = ? WHERE ID = ?;", status, ticketID))
        }
    }

    override fun setPriorityAsync(ticketID: Long, priority: Ticket.Priority): CompletableFuture<Void> = CompletableFuture.runAsync {
        usingSession {
            update(queryOf("UPDATE \"TicketManager_V8_Tickets\" SET PRIORITY = ? WHERE ID = ?;", priority.asByte(), ticketID))
        }
    }

    override fun setStatusAsync(ticketID: Long, status: Ticket.Status): CompletableFuture<Void> = CompletableFuture.runAsync {
        usingSession {
            update(queryOf("UPDATE \"TicketManager_V8_Tickets\" SET STATUS = ? WHERE ID = ?;", status.name, ticketID))
        }
    }

    override fun insertActionAsync(id: Long, action: Action): CompletableFuture<Void> = CompletableFuture.runAsync {
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

    override fun insertNewTicketAsync(ticket: Ticket): CompletableFuture<Long> = CompletableFuture.supplyAsync {
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

        TMCoroutine.launchGlobal {
            ticket.actions.forEach {
                usingSession {
                    update(
                        queryOf("INSERT INTO \"TicketManager_V8_Actions\" (TICKET_ID, ACTION_TYPE, CREATOR, MESSAGE, EPOCH_TIME, SERVER, WORLD, WORLD_X, WORLD_Y, WORLD_Z) VALUES (?,?,?,?,?,?,?,?,?,?);",
                            ticket.id,
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

    private fun getTickets(ids: List<Long>): ImmutableList<Ticket> {
        val idsSQL = ids.joinToString(", ") { "$it" }

        return usingSession {
            run(queryOf("SELECT * FROM \"TicketManager_V8_Tickets\" WHERE ID IN ($idsSQL);")
                .map { it.toTicket() }
                .asList
            )
        }.toImmutableList()
    }

    override fun getTicketOrNullAsync(id: Long): CompletableFuture<Ticket?> {
        val ticketDef = CompletableFuture.supplyAsync {
            usingSession {
                run(queryOf("SELECT * FROM \"TicketManager_V8_Tickets\" WHERE ID = ?", id)
                    .map { it.toTicket() }
                    .asSingle
                )
            }
        }

        val actionsDef = CompletableFuture.supplyAsync {
            usingSession {
                run(queryOf("SELECT * FROM \"TicketManager_V8_Actions\" WHERE TICKET_ID = ?", id)
                    .map { it.toAction() }
                    .asList
                )
            }
                .sortedBy(Action::timestamp)
                .toImmutableList()
        }

        return CompletableFuture.allOf(actionsDef, ticketDef).thenApplyAsync {
            ticketDef.join()?.let { it + actionsDef.join() }
        }
    }

    override fun getOpenTicketsAsync(page: Int, pageSize: Int): CompletableFuture<DBResult> {
        return ticketsFilteredByAsync(page, pageSize, "SELECT * FROM \"TicketManager_V8_Tickets\" WHERE STATUS = ?;", Ticket.Status.OPEN.name)
    }

    override fun getOpenTicketsAssignedToAsync(
        page: Int,
        pageSize: Int,
        assignments: List<Assignment>,
    ): CompletableFuture<DBResult> {

        val sql = assignments
            .map(Assignment::asString)
            .joinToString(" OR ") { "ASSIGNED_TO = ?" }

        return ticketsFilteredByAsync(page, pageSize,
            preparedQuery = "SELECT * FROM \"TicketManager_V8_Tickets\" WHERE STATUS = ? AND ($sql);",

        )

        /*
        val args = (listOf(Ticket.Status.OPEN.name, assignment) + groupsFixed).toTypedArray()

        return ticketsFilteredByAsync(page, pageSize, "SELECT * FROM \"TicketManager_V8_Tickets\" WHERE STATUS = ? AND ($assignedSQL);", *args)
    */
    }

    override fun getOpenTicketsNotAssignedAsync(page: Int, pageSize: Int): CompletableFuture<DBResult> {
        return ticketsFilteredByAsync(page, pageSize, "SELECT * FROM \"TicketManager_V8_Tickets\" WHERE STATUS = ? AND ASSIGNED_TO IS NULL", Ticket.Status.OPEN.name)
    }

    private fun ticketsFilteredByAsync(page: Int, pageSize: Int, preparedQuery: String, vararg params: Any?): CompletableFuture<DBResult> {
        val totalSize = AtomicInteger(0)
        val totalPages = AtomicInteger(0)

        val sortedTickets = usingSession {
            run(queryOf(preparedQuery, *params)
                .map { it.long(1) }
                .asList
            ) }
            .let(::getTickets)
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

        return CompletableFuture.completedFuture(DBResult(
                filteredResults = chunkedTickets.getOrElse(fixedPage-1) { listOf() }.toImmutableList(),
                totalPages = totalPages.get(),
                totalResults = totalSize.get(),
                returnedPage = fixedPage
        ))
    }

    override fun massCloseTicketsAsync(lowerBound: Long, upperBound: Long, actor: Creator, ticketLoc: ActionLocation): CompletableFuture<Void> {
        val curTime = Instant.now().epochSecond

        val ticketIds = (lowerBound..upperBound).toList()
        val action = ActionInfo(
            user = actor,
            location = ticketLoc,
            timestamp = curTime,
        ).MassClose()

        usingSession { update(queryOf("UPDATE \"TicketManager_V8_Tickets\" SET STATUS = ? WHERE ID IN (${ticketIds.joinToString(", ")});", Ticket.Status.CLOSED.name)) }
        return usingSession { ticketIds.map { insertActionAsync(it, action) }.flatten() }.thenAcceptAsync {  }
    }

    override fun countOpenTicketsAsync(): CompletableFuture<Long> {
        val returnedValue = usingSession {
            run(queryOf("SELECT COUNT(*) FROM \"TicketManager_V8_Tickets\" WHERE STATUS = ?;", Ticket.Status.OPEN.name)
                .map { it.long(1) }
                .asSingle
            )!!
        }
        return CompletableFuture.completedFuture(returnedValue)
    }

    override fun countOpenTicketsAssignedToAsync(assignments: List<Assignment>): CompletableFuture<Long> {
        val assignedSQL = assignments
            .map(Assignment::asString)
            .joinToString(" OR ") { "ASSIGNED_TO = ?" }

        usingSession {
            run(queryOf("SELECT COUNT(*) FROM \"TicketManager_V8_Tickets\" WHERE STATUS = ? AND ($assignedSQL);", *assignments)
                .map { it.long(1) }
                .asSingle
            )
        }!!
    /*

        val args = (listOf(Ticket.Status.OPEN.name, assignment) + groupsFixed).toTypedArray()

        return usingSession {
            run(queryOf("SELECT COUNT(*) FROM \"TicketManager_V8_Tickets\" WHERE STATUS = ? AND ($assignedSQL);", *args)
                .map { it.long(1) }
                .asSingle
            )
        }!!

         */
    }

    override suspend fun searchDatabaseAsync(
        constraints: SearchConstraint,
        page: Int,
        pageSize: Int
    ): Result {

        val args = mutableListOf<Any?>()
        val searches = mutableListOf<String>()
        val functions = mutableListOf<TicketPredicate>()

        fun addToCorrectLocations(value: String?, field: String) = value
            ?.apply(args::add)
            ?.let { "$field = ?" }
            ?.apply(searches::add)
            ?: "$field IS NULL".apply(searches::add)

        constraints.run {
            // Database Searches
            creator?.run { addToCorrectLocations(value.toString(), "CREATOR") }
            assigned?.run { addToCorrectLocations(value, "ASSIGNED_TO") }
            status?.run { addToCorrectLocations(value.name, "STATUS") }
            closedBy?.run {
                searches.add("ID IN (SELECT DISTINCT TICKET_ID FROM \"TicketManager_V8_Actions\" WHERE (ACTION_TYPE = ? OR ACTION_TYPE = ?) AND CREATOR = ?)")
                args.add(Ticket.Action.Type.CLOSE)
                args.add(Ticket.Action.Type.MASS_CLOSE)
                args.add(value.toString())
            }
            world?.run {
                searches.add("ID IN (SELECT DISTINCT TICKET_ID FROM \"TicketManager_V8_Actions\" WHERE WORLD = ?)")
                args.add(value)
            }

            // Functional Searches
            lastClosedBy?.run {
                { t: Ticket ->
                    t.actions.lastOrNull { it.type == Ticket.Action.Type.CLOSE || it.type == Ticket.Action.Type.MASS_CLOSE }
                        ?.run { user equal value } ?: false
                }
            }?.apply(functions::add)
            creationTime?.run { { t: Ticket -> t.actions[0].timestamp >= value } }?.apply(functions::add)
            constraints.run {
                keywords?.run {
                    { t: Ticket ->
                        val comments = t.actions
                            .filter { it.type == Ticket.Action.Type.OPEN || it.type == Ticket.Action.Type.COMMENT }
                            .map { it.message!! }
                        value.map { w -> comments.any { it.lowercase().contains(w.lowercase()) } }
                            .all { it }
                    }
                }?.apply(functions::add)
            }
        }

        // Builds composed function
        val combinedFunction = if (functions.isNotEmpty()) { t: Ticket -> functions.all { it(t) } }
        else { _: Ticket -> true }

        // Builds final search string
        var searchString = "SELECT * FROM \"TicketManager_V8_Tickets\""
        if (searches.isNotEmpty())
            searchString += " WHERE ${searches.joinToString(" AND ")}"

        // Searches
        val totalSize = AtomicInteger(0)
        val totalPages = AtomicInteger(0)

        // Query
        val baseTickets = usingSession {
            run(queryOf("$searchString;", *args.toTypedArray())
                .map { it.toTicket() }
                .asList
            )
        }

        val fullTickets =
            if (baseTickets.isEmpty()) emptyList()
            else {
                val constraintsStr = baseTickets.map { it.id }.joinToString(", ")
                val actionMap = usingSession {
                    run(queryOf("SELECT * FROM \"TicketManager_V8_Actions\" WHERE TICKET_ID IN ($constraintsStr);")
                        .map { it.long(2) to it.toAction() }
                        .asList
                    )
                }
                    .groupBy({ it.first }, { it.second })
                baseTickets.map { it + actionMap[it.id]!! }
            }

        val chunkedTargetTickets = fullTickets
            .asParallelStream()
            .filter(combinedFunction)
            .toList()
            .sortedWith(compareByDescending { it.id })
            .also { totalSize.set(it.count()) }
            .let { if (pageSize == 0 || it.isEmpty()) listOf(it) else it.chunked(pageSize) }
            .also { totalPages.set(it.count()) }

        val fixedPage = when {
            totalPages.get() == 0 || page < 1 -> 1
            page in 1..totalPages.get() -> page
            else -> totalPages.get()
        }

        return Result(
            filteredResults = chunkedTargetTickets.getOrElse(fixedPage-1) { listOf() },
            totalPages = totalPages.get(),
            totalResults = totalSize.get(),
            returnedPage = fixedPage,
        )
    }

    override suspend fun getTicketIDsWithUpdatesAsync(): List<Long> {
        return usingSession {
            run(queryOf("SELECT ID FROM \"TicketManager_V8_Tickets\" WHERE STATUS_UPDATE_FOR_CREATOR = ?;", true)
                .map { it.long(1) }
                .asList
            )
        }
    }

    override suspend fun getTicketIDsWithUpdatesForAsync(creator: Creator): List<Long> {
        return usingSession {
            run(queryOf( "SELECT ID FROM \"TicketManager_V8_Tickets\" WHERE STATUS_UPDATE_FOR_CREATOR = ? AND CREATOR = ?;", true, creator.toString())
                .map { it.long(1) }
                .asList
            )
        }
    }

    override suspend fun closeDatabase() {
        connectionPool.connection.createStatement().execute("SHUTDOWN")
    }

    override suspend fun insertTicketForMigration(other: AsyncDatabase) {
        usingSession {
            run(queryOf("SELECT ID FROM \"TicketManager_V8_Tickets\";")
                .map { it.long(1) }
                .asList
            )
        }
            .forEach { id ->
                val ticket = getTicketOrNullAsync(id)!!
                TMCoroutine.runAsync { other.insertNewTicketAsync(ticket) }
            }
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

    override fun initializeDatabase() {
        usingSession {

            // Ticket Table
            execute(queryOf("""
                create table if not exists "TicketManager_V8_Tickets"
                (
                    ID                        NUMBER GENERATED BY DEFAULT AS IDENTITY (START WITH 1 INCREMENT BY 1) not null,
                    CREATOR                   VARCHAR_IGNORECASE(70) not null,
                    PRIORITY                  TINYINT                not null,
                    STATUS                    VARCHAR_IGNORECASE(10) not null,
                    ASSIGNED_TO               VARCHAR_IGNORECASE(255),
                    STATUS_UPDATE_FOR_CREATOR BOOLEAN                not null,
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
                    TICKET_ID
                     BIGINT                  NOT NULL,
 */
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
}
 */