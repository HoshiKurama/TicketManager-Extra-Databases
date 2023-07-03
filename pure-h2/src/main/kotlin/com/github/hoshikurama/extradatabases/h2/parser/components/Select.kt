package com.github.hoshikurama.extradatabases.h2.parser.components

import com.github.hoshikurama.extradatabases.h2.parser.*
import com.github.hoshikurama.extradatabases.h2.parser.column.ActionColumnField
import com.github.hoshikurama.extradatabases.h2.parser.column.AugmentedColumn
import com.github.hoshikurama.extradatabases.h2.parser.column.TicketColumnField

//@SelectMarker
abstract class Select(private val tableName: String): CompositeStage {
    protected val columns = mutableListOf<TerminalStage>()
    private val stages = mutableListOf<Stage>()
    private val rawEndings = mutableListOf<String>()

    override fun parseStage(): TerminalStage {
        return listOf(
            listOf(
                stringOnlyStage("SELECT"),
                SQLFormat.possibleList(columns),
                stringOnlyStage("FROM \"$tableName\""),
                stages
                    .map(Stage::parseStage)
                    .run(SQLFormat::spacedAND),
            ).run(SQLFormat::spaces),
            rawEndings
                .map(::stringOnlyStage)
                .run(SQLFormat::spaces)
        ).run(SQLFormat::spaces)
    }

    fun raw(sql: String) = rawEndings.add(sql)

    protected fun <T : Where> where(startingValue: T, init: T.() -> Unit) {
        startingValue
            .apply(init)
            .parseStage()
            .run(stages::add)
    }

    class Ticket : Select("TicketManager_V8_Tickets") {
        operator fun TicketColumnField.unaryPlus() {
            stringOnlyStage(sqlColumnName).run(columns::add)
        }
        operator fun AugmentedColumn<out TicketColumnField>.unaryPlus() {
            parseStage().run(columns::add)
        }


        fun where(init: Where.Ticket.() -> Unit) {
            super.where(Where.Ticket(), init)
        }
    }

    class Action : Select("TicketManager_V8_Actions") {
        operator fun ActionColumnField.unaryPlus() {
            stringOnlyStage(sqlColumnName).run(columns::add)
        }
        operator fun AugmentedColumn<out ActionColumnField>.unaryPlus() {
            parseStage().run(columns::add)
        }

        fun where(init: Where.Action.() -> Unit) {
            super.where(Where.Action(), init)
        }
    }
}