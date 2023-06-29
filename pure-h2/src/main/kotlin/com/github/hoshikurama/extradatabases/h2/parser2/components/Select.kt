package com.github.hoshikurama.extradatabases.h2.parser2.components

import com.github.hoshikurama.extradatabases.h2.parser2.*
import com.github.hoshikurama.extradatabases.h2.parser2.Action as ActionColumn
import com.github.hoshikurama.extradatabases.h2.parser2.Ticket as TicketColumn

const val TICKET_TABLE_NAME = "TicketManager_V8_Tickets"
const val ACTION_TABLE_NAME = "TicketManager_V8_Actions"

@SelectMarker
abstract class Select(private val tableName: String): CompositeStage {
    protected val columns = mutableListOf<TerminalStage>()
    protected val stages = mutableListOf<Stage>()

    override fun parseStage(): TerminalStage {
        return listOf(
            stringOnlyStage("SELECT"),
            SQLFormat.list(columns),
            stringOnlyStage("FROM \"$tableName\""),
            stages
                .map(Stage::parseStage)
                .run(SQLFormat::and),
        ).run(SQLFormat::spaces)
    }

    object Ticket : Select(TICKET_TABLE_NAME) {
        operator fun TicketColumn.ColumnField.unaryPlus() {
            stringOnlyStage(sqlColumnName).run(columns::add)
        }

        fun where(init: Where.Ticket.() -> Unit) {
            Where.Ticket()
                .apply(init)
                .parseStage()
                .run(stages::add)
        }
    }

    object Action : Select(ACTION_TABLE_NAME) {
        operator fun ActionColumn.ColumnField.unaryPlus() {
            stringOnlyStage(sqlColumnName).run(columns::add)
        }
    }
}