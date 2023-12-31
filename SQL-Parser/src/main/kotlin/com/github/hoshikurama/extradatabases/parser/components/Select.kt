package com.github.hoshikurama.extradatabases.parser.components


import com.github.hoshikurama.extradatabases.parser.column.ActionColumnField
import com.github.hoshikurama.extradatabases.parser.column.AugmentedColumn
import com.github.hoshikurama.extradatabases.parser.*
import com.github.hoshikurama.extradatabases.parser.column.Ticket
import com.github.hoshikurama.extradatabases.parser.column.TicketColumnField
import com.github.hoshikurama.ticketmanager.api.ticket.Creator

fun main() {
    sql {
        selectTicket {
            +Ticket.ID

            where {
                Ticket.Creator `==` Creator.Console
            }
        }
    }.statement.run(::println)
}

//@SelectMarker
abstract class Select(private val tableName: String): CompositeStage {
    val columns = mutableListOf<TerminalStage>()
    val stages = mutableListOf<Stage>()
    private val rawEndings = mutableListOf<String>()

    override fun parseStage(): TerminalStage {
        return listOf(
            listOf(
                stringOnlyStage("SELECT"),
                SQLFormat.spacedCommas(columns),
                stringOnlyStage("FROM $tableName"),
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

    inline fun <T : Where> whereSuper(startingValue: T, init: T.() -> Unit) {
        startingValue
            .apply(init)
            .parseStage()
            .run(stages::add)
    }

    class Ticket : Select("TicketManager_V10_Tickets") {
        operator fun TicketColumnField.unaryPlus() {
            stringOnlyStage(sqlColumnName).run(columns::add)
        }
        operator fun AugmentedColumn<out TicketColumnField>.unaryPlus() {
            parseStage().run(columns::add)
        }

        inline fun where(init: Where.Ticket.() -> Unit) {
            whereSuper(Where.Ticket(), init)
        }
    }

    class Action : Select("TicketManager_V10_Actions") {
        operator fun ActionColumnField.unaryPlus() {
            stringOnlyStage(sqlColumnName).run(columns::add)
        }
        operator fun AugmentedColumn<out ActionColumnField>.unaryPlus() {
            parseStage().run(columns::add)
        }

        inline fun where(init: Where.Action.() -> Unit) {
            whereSuper(Where.Action(), init)
        }
    }
}