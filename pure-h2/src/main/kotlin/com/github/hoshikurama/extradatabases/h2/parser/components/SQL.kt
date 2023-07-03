package com.github.hoshikurama.extradatabases.h2.parser.components

import com.github.hoshikurama.extradatabases.h2.parser.column.Action
import com.github.hoshikurama.extradatabases.h2.parser.*
import com.github.hoshikurama.extradatabases.h2.parser.column.Ticket
import com.github.hoshikurama.extradatabases.h2.parser.column.TicketMeta
import com.github.hoshikurama.ticketmanager.api.common.ticket.Creator

typealias `Ticket.*` = Ticket.STAR
typealias `Action.*` = Action.STAR

/*
sql {
    updateTicket {
        Ticket.Column `=` 35
    }
}
 */
fun main() {

    val (sql, args) = sql {
        selectAction {
            +`Action.*`

            where {
                TicketMeta.LastClosedBy `==` Creator.Console
            }
        }
    }
    println(sql)
}

fun sql(init: SQL.() -> Unit): SQL.Completed = SQL().apply(init).complete()

@SQLMarker
class SQL {
    private val statement = StringBuilder()
    private val arguments = mutableListOf<Any?>()
    private val stages = mutableListOf<Stage>()


    fun complete(): Completed {
        stages.forEach { stage -> stage.parseStage()
            .also { statement.append(it.statement) }
            .also { arguments.addAll(it.arguments) }
        }
        statement
            .apply(StringBuilder::trimEnd)
            .appendApply(";")

        return Completed(statement.toString(), arguments)
    }

    fun selectTicket(init: Select.Ticket.() -> Unit) {
        Select.Ticket()
            .apply(init)
            .parseStage()
            .addToSQL()
    }

    fun selectAction(init: Select.Action.() -> Unit) {
        Select.Action()
            .apply(init)
            .parseStage()
            .addToSQL()
    }

    private fun TerminalStage.addToSQL() {
        this@SQL.statement.append(statement)
        this@SQL.arguments.addAll(arguments)
    }

    data class Completed(val statement: String, val args: List<Any?>)
}