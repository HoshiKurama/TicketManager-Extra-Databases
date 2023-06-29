package com.github.hoshikurama.extradatabases.h2.parser2.components

import com.github.hoshikurama.extradatabases.h2.parser2.*

const val TICKET_TABLE_NAME = "TicketManager_V8_Tickets"
const val ACTION_TABLE_NAME = "TicketManager_V8_Actions"

@SelectMarker
abstract class Select(private val tableName: String): CompositeStage {
    protected val columns = mutableListOf<TerminalStage>()
    private val stages = mutableListOf<Stage>()

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

    protected fun <T : Where> where(startingValue: T, init: T.() -> Unit) {
        startingValue
            .apply(init)
            .parseStage()
            .run(stages::add)
    }

    object Ticket : Select(TICKET_TABLE_NAME) {
        operator fun TicketColumnField.unaryPlus() {
            stringOnlyStage(sqlColumnName).run(columns::add)
        }

        fun where(init: Where.Ticket.() -> Unit) {
            super.where(Where.Ticket(), init)
        }
    }

    object Action : Select(ACTION_TABLE_NAME) {
        operator fun ActionColumnField.unaryPlus() {
            stringOnlyStage(sqlColumnName).run(columns::add)
        }

        fun where(init: Where.Action.() -> Unit) {
            super.where(Where.Action(), init)
        }
    }
}