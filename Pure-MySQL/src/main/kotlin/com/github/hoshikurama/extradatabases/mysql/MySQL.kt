package com.github.hoshikurama.extradatabases.mysql

import com.github.hoshikurama.extradatabases.common.extensions.*
import com.github.hoshikurama.extradatabases.parser.column.TicketColumnField
import com.github.hoshikurama.extradatabases.parser.components.SQL
import com.github.hoshikurama.extradatabases.parser.components.Where
import com.github.hoshikurama.extradatabases.parser.components.sql
import com.github.hoshikurama.ticketmanager.api.common.database.AsyncDatabase
import com.github.hoshikurama.ticketmanager.api.common.database.DBResult
import com.github.hoshikurama.ticketmanager.api.common.ticket.*
import com.github.jasync.sql.db.*
import com.github.jasync.sql.db.mysql.MySQLConnectionBuilder
import com.google.common.collect.ImmutableList
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.CompletableFuture
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

    private suspend fun SQL.Completed.sendPreparedStatement() = suspendingConnection.sendPreparedStatement(statement, args)

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
            return@coroutineScope DBResult(ImmutableList.of(), 0, 0, 0)
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
            .let { ImmutableList.copyOf(it) }

        return@coroutineScope DBResult(
            filteredResults = fullTickets,
            totalPages = totalPages,
            totalResults = totalSize,
            returnedPage = fixedPage
        )
    }

    override fun setAssignmentAsync(ticketID: Long, assignment: Assignment): CompletableFuture<Void> {
        coroutineScope {  }
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
        actions = ImmutableList.of()
    )
}