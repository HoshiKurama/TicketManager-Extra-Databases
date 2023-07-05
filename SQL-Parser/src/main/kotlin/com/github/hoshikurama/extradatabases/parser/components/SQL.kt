package com.github.hoshikurama.extradatabases.parser.components

import com.github.hoshikurama.extradatabases.parser.SQLMarker
import com.github.hoshikurama.extradatabases.parser.Stage
import com.github.hoshikurama.extradatabases.parser.TerminalStage
import com.github.hoshikurama.extradatabases.parser.appendApply

inline fun sql(init: SQL.() -> Unit): SQL.Completed = SQL().apply(init).complete()

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

    inline fun <T : Stage> T.initParseAndAdd(init: T.() -> Unit) {
        apply(init).parseStage().addToSQL()
    }

    inline fun selectTicket(init: Select.Ticket.() -> Unit) = Select.Ticket().initParseAndAdd(init)
    inline fun selectAction(init: Select.Action.() -> Unit) = Select.Action().initParseAndAdd(init)
    inline fun update(ticketID: Long, init: Update.Ticket.() -> Unit) = Update.Ticket(listOf(ticketID)).initParseAndAdd(init)
    inline fun update(ticketIDs: List<Long>, init: Update.Ticket.() -> Unit) = Update.Ticket(ticketIDs).initParseAndAdd(init)

    fun TerminalStage.addToSQL() {
        this@SQL.statement.append(statement)
        this@SQL.arguments.addAll(arguments)
    }

    data class Completed(val statement: String, val args: List<Any?>)
}