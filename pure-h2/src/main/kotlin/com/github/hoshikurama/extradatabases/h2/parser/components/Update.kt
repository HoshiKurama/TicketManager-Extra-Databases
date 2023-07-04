package com.github.hoshikurama.extradatabases.h2.parser.components

import com.github.hoshikurama.extradatabases.h2.parser.CompositeStage
import com.github.hoshikurama.extradatabases.h2.parser.SQLFormat
import com.github.hoshikurama.extradatabases.h2.parser.TerminalStage
import com.github.hoshikurama.extradatabases.h2.parser.appendAtFront
import com.github.hoshikurama.extradatabases.h2.parser.infixfunctions.update.UpdateExposeTicketFunctions

abstract class Update(protected val ids: List<Long>) : CompositeStage {
    protected val stages = mutableListOf<TerminalStage>()

    class Ticket(ids: List<Long>) : UpdateExposeTicketFunctions(ids) {

        override fun parseStage(): TerminalStage {
            return SQLFormat.spacedAND(stages)
                .apply {
                    statement.appendAtFront("UPDATE \"TicketManager_V8_Tickets\" SET ")

                    if (ids.size == 1) statement.append(" WHERE ID = ${ids.first()}")
                    else ids.joinToString(separator = ",", prefix = " WHERE ID IN (", postfix = ")")
                        .run(statement::append)
                }
        }
    }
}