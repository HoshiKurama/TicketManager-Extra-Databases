package com.github.hoshikurama.extradatabases.h2.parser2.components

import com.github.hoshikurama.extradatabases.h2.parser2.*
import java.util.*

/*
sql {
    select {
        +Ticket.ID
        +Ticket.Creator

        where {
            Ticket.ID `==` 1L
            Ticket.Creator == CREATOR

            actionTable {
                ACTION.ID
            }

            raw {

            }
        }
    }
    raw { +"ORDER BY THING" }
}

// NOTE: SQL SELECT vs internal select
*/
fun main() {

    val (sql, args) = sql {
        selectAction {
            +Action.Creator
        }
/*

        selectTicket {
            +Ticket.ID
            +Ticket.Status
            +Ticket.Priority
            +Ticket.StatusUpdate
            +Ticket.Assignment
            +Ticket.Creator

            where {
                Ticket.ID `==` 3
                Ticket.Priority `＜` com.github.hoshikurama.ticketmanager.api.common.ticket.Ticket.Priority.HIGH
                Ticket.Creator `!=` Creator.User(UUID.randomUUID())
            }
        }
         */
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
        // Note: Up to stages to determine how they get merged. List here just for raw
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
        Select.Ticket
            .apply(init)
            .parseStage()
            .addToSQL()
    }

    fun selectAction(init: Select.Action.() -> Unit) {
        Select.Action
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