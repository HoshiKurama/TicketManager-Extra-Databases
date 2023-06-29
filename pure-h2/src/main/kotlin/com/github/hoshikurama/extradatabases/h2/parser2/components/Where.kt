package com.github.hoshikurama.extradatabases.h2.parser2.components

import com.github.hoshikurama.extradatabases.h2.extensions.asByte
import com.github.hoshikurama.extradatabases.h2.extensions.asString
import com.github.hoshikurama.extradatabases.h2.parser2.*
import com.github.hoshikurama.ticketmanager.api.common.ticket.Assignment
import com.github.hoshikurama.ticketmanager.api.common.ticket.Creator
import com.github.hoshikurama.ticketmanager.api.common.ticket.Ticket as ActualTicket
import com.github.hoshikurama.extradatabases.h2.parser2.Ticket as TicketField

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

        private fun TicketField.ColumnField.stdStage(symbol: String, value: Any?) {
            TerminalStage("$sqlColumnName $symbol ?", value)
                .run(stages::add)
        }
        infix fun TicketField.ID.`==`(id: Long) = stdStage("=", id)
        infix fun TicketField.ID.`!=`(id: Long) = stdStage("!=", id)
        infix fun TicketField.Creator.`==`(creator: Creator) = stdStage("=", creator.asString())
        infix fun TicketField.Creator.`!=`(creator: Creator) = stdStage("!=", creator.asString())
        infix fun TicketField.Status.`==`(status: ActualTicket.Status) = stdStage("=", status.name)
        infix fun TicketField.Status.`!=`(status: ActualTicket.Status) = stdStage("!=", status.name)
        infix fun TicketField.StatusUpdate.`==`(statusUpdate: Boolean) = stdStage("=", statusUpdate)
        infix fun TicketField.StatusUpdate.`!=`(statusUpdate: Boolean) = stdStage("!=", statusUpdate)
        infix fun TicketField.Assignment.`==`(assignment: Assignment) = stdStage("=", assignment.asString())
        infix fun TicketField.Assignment.`!=`(assignment: Assignment) = stdStage("!=", assignment.asString())
        infix fun TicketField.Priority.`==`(priority: ActualTicket.Priority) = stdStage("=", priority.asByte())
        infix fun TicketField.Priority.`!=`(priority: ActualTicket.Priority) = stdStage("!=", priority.asByte())
        @JvmName("lessThanPriority")
        @Suppress("NonAsciiCharacters")
        infix fun TicketField.Priority.`＜`(priority: ActualTicket.Priority) = stdStage("<", priority.asByte())
        @JvmName("greaterThanPriority")
        @Suppress("NonAsciiCharacters")
        infix fun TicketField.Priority.`＞`(priority: ActualTicket.Priority) = stdStage(">", priority.asByte())


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