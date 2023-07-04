package com.github.hoshikurama.extradatabases.h2.parser.infixfunctions.update

import com.github.hoshikurama.extradatabases.h2.extensions.asString
import com.github.hoshikurama.extradatabases.h2.parser.TerminalStage
import com.github.hoshikurama.extradatabases.h2.parser.column.NamedColumn
import com.github.hoshikurama.extradatabases.h2.parser.components.Update
import com.github.hoshikurama.ticketmanager.api.common.ticket.Assignment
import com.github.hoshikurama.ticketmanager.api.common.ticket.Creator
import com.github.hoshikurama.extradatabases.h2.parser.column.Ticket as TicketColumn
import com.github.hoshikurama.ticketmanager.api.common.ticket.Ticket as ActualTicket

@Suppress("Unused", "FunctionName")
abstract class UpdateExposeTicketFunctions(ids: List<Long>) : Update(ids) {

    private fun <T : NamedColumn> T.stdAdd(value: Any) {
        TerminalStage("$sqlColumnName = ?", listOf(value))
            .run(stages::add)
    }

    infix fun TicketColumn.Creator.`=`(creator: Creator) = stdAdd(creator.asString())
    infix fun TicketColumn.Status.`=`(status: ActualTicket.Status) = stdAdd(status.name)
    infix fun TicketColumn.Priority.`=`(priority: ActualTicket.Priority) = stdAdd(priority.name)
    infix fun TicketColumn.Assignment.`=`(assignment: Assignment) = stdAdd(assignment.asString())
    infix fun TicketColumn.StatusUpdate.`=`(statusUpdate: Boolean) = stdAdd(statusUpdate)
}