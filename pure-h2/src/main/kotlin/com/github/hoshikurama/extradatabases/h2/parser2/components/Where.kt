package com.github.hoshikurama.extradatabases.h2.parser2.components

import com.github.hoshikurama.extradatabases.h2.extensions.asByte
import com.github.hoshikurama.extradatabases.h2.extensions.asString
import com.github.hoshikurama.extradatabases.h2.parser2.*
import com.github.hoshikurama.ticketmanager.api.common.ticket.Assignment
import com.github.hoshikurama.ticketmanager.api.common.ticket.Creator
import com.github.hoshikurama.ticketmanager.api.common.ticket.Ticket as ActualTicket
import com.github.hoshikurama.extradatabases.h2.parser2.Ticket as TicketColumn

abstract class Where : CompositeStage {
    protected val stages = mutableListOf<Stage>()

    override fun parseStage(): TerminalStage {
        val (stmt, args) = stages
            .map(Stage::parseStage)
            .run(SQLFormat::and)
        return TerminalStage(
            statement = stmt.appendAtFront("WHERE "),
            arguments = args
        )
    }

    @WhereTicketMarker
    @Suppress("FunctionName")
    class Ticket : Where() {

        private fun TicketColumnField.stdStage(symbol: String, value: Any?) {
            TerminalStage("$sqlColumnName $symbol ?", value)
                .run(stages::add)
        }
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


        /*
        NOTE: MAYBE GENERIC_COLUMN IN SELECT....

        infix fun TicketID.inside(ids: List<Ticket>) {
            ticketTable.add("ID IN (${ids.joinToString(", ")})".asStringBuilder() to ids)
        }
        */
    }

    class Action : Where() {

    }
    /*
 TODO WRITE OR {} THING
     */
}