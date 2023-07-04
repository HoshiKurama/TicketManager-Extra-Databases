package com.github.hoshikurama.extradatabases.h2.parser.infixfunctions.where

import com.github.hoshikurama.extradatabases.h2.extensions.asByte
import com.github.hoshikurama.extradatabases.h2.extensions.asString
import com.github.hoshikurama.extradatabases.h2.parser.column.InSupport
import com.github.hoshikurama.extradatabases.h2.parser.TerminalStage
import com.github.hoshikurama.extradatabases.h2.parser.column.TicketColumnField
import com.github.hoshikurama.extradatabases.h2.parser.components.Where
import com.github.hoshikurama.ticketmanager.api.common.ticket.Assignment
import com.github.hoshikurama.ticketmanager.api.common.ticket.Creator
import com.github.hoshikurama.extradatabases.h2.parser.column.Ticket as TicketColumn
import com.github.hoshikurama.ticketmanager.api.common.ticket.Ticket as ActualTicket

@Suppress("FunctionName")
abstract class WhereExposeTicketFunctions : Where() {

    infix fun TicketColumn.ID.`==`(id: Long) = stdStage("=", id)
    infix fun TicketColumn.ID.`!=`(id: Long) = stdStage("!=", id)
    infix fun TicketColumn.Creator.`==`(creator: Creator) = stdStage("=", creator.asString())
    infix fun TicketColumn.Creator.`!=`(creator: Creator) = stdStage("!=", creator.asString())
    infix fun TicketColumn.Status.`==`(status: ActualTicket.Status) = stdStage("=", status.name)
    infix fun TicketColumn.Status.`!=`(status: ActualTicket.Status) = stdStage("!=", status.name)
    infix fun TicketColumn.StatusUpdate.`==`(statusUpdate: Boolean) = stdStage("=", statusUpdate)
    infix fun TicketColumn.StatusUpdate.`!=`(statusUpdate: Boolean) = stdStage("!=", statusUpdate)
    infix fun TicketColumn.Assignment.`==`(assignment: Assignment) = stdStage("=", assignment.asString())
    infix fun TicketColumn.Assignment.`!=`(assignment: Assignment) = stdStage("!=", assignment.asString())
    infix fun TicketColumn.Priority.`==`(priority: ActualTicket.Priority) = stdStage("=", priority.asByte())
    infix fun TicketColumn.Priority.`!=`(priority: ActualTicket.Priority) = stdStage("!=", priority.asByte())

    @JvmName("lessThanPriority")
    @Suppress("NonAsciiCharacters")
    infix fun TicketColumn.Priority.`＜`(priority: ActualTicket.Priority) = stdStage("<", priority.asByte())
    @JvmName("greaterThanPriority")
    @Suppress("NonAsciiCharacters")
    infix fun TicketColumn.Priority.`＞`(priority: ActualTicket.Priority) = stdStage(">", priority.asByte())

    infix fun <T, U> T.`in`(list: List<U>) where T : InSupport<U, *>, T : TicketColumnField {
        TerminalStage(
            str = "$sqlColumnName IN (${list.joinToString(",") { "?" }})",
            arguments = list.map(typeToInCompare)
        ).run(stages::add)
    }

    infix fun <T, U> T.`!in`(list: List<U>) where T : InSupport<U, *>, T : TicketColumnField {
        TerminalStage(
            str = "$sqlColumnName NOT IN (${list.joinToString(",") { "?" }})",
            arguments = list.map(typeToInCompare)
        ).run(stages::add)
    }

    infix fun TicketColumn.ID.inRange(range: LongRange) {
        TerminalStage("$sqlColumnName BETWEEN ${range.first} AND ${range.last}", emptyList())

    }
}