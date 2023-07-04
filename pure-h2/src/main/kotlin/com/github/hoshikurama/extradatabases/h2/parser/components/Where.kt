package com.github.hoshikurama.extradatabases.h2.parser.components

import com.github.hoshikurama.extradatabases.h2.parser.*
import com.github.hoshikurama.extradatabases.h2.parser.column.Distinct
import com.github.hoshikurama.extradatabases.h2.parser.column.NamedColumn
import com.github.hoshikurama.extradatabases.h2.parser.infixfunctions.where.WhereExposeActionFunctions
import com.github.hoshikurama.extradatabases.h2.parser.infixfunctions.where.WhereExposeTicketFunctions
import java.lang.StringBuilder
import com.github.hoshikurama.extradatabases.h2.parser.column.Action as ActionColumn
import com.github.hoshikurama.extradatabases.h2.parser.column.Ticket as TicketColumn

//@WhereTicketMarker
abstract class Where(useWhereClause: Boolean = true) : CompositeStage {
    val initialClause = if (useWhereClause) "WHERE " else ""
    val stages = mutableListOf<Stage>()
    private val raws = mutableListOf<String>()

    override fun parseStage(): TerminalStage {
        return stages
            .map(Stage::parseStage)
            .run(SQLFormat::spacedAND)
            .apply {
                statement.appendAtFront("WHERE ")

                raws.takeIf { it.isNotEmpty() }
                    ?.joinTo(StringBuilder(), " ")
                    ?.run(::sbOnlyStage)
                    ?.run(stages::add)
            }
    }

    protected fun NamedColumn.stdStage(symbol: String, value: Any?) {
        TerminalStage("$sqlColumnName $symbol ?", value)
            .run(stages::add)
    }

    inline fun selectTicket(init: Select.Ticket.() -> Unit) = selectT(Select.Ticket(), init)
    inline fun selectAction(init: Select.Action.() -> Unit) = selectT(Select.Action(), init)
    fun raw(sql: String) = raws.add(sql)

    inline fun <T : Select> selectT(t: T, init: T.() -> Unit): TerminalStage {
        return t.apply(init)
            .parseStage()
    }


    class Ticket : WhereExposeTicketFunctions() {
        inline fun whereAction(init: Action.() -> Unit) {
            selectAction { // SELECT DISTINCT
                +Distinct(ActionColumn.TicketID)
                where(init)
            }
                .parseStage()
                .apply {
                    statement.appendAtFront("${TicketColumn.ID.sqlColumnName} IN (")
                    statement.append(")")
                }
                .run(stages::add)
        }
    }

    class Action(useWhereClause: Boolean = true) : WhereExposeActionFunctions(useWhereClause) {
        @Suppress("Unused")
        inline fun whereTicket(init: Ticket.() -> Unit) {
            selectTicket { // SELECT DISTINCT
                +Distinct(TicketColumn.ID)
                where(init)
            }
                .parseStage()
                .apply {
                    statement.appendAtFront("${TicketColumn.ID.sqlColumnName} IN (")
                    statement.append(")")
                }
                .run(stages::add)
        }
    }
}