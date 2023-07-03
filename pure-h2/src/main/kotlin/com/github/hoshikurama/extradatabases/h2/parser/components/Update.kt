package com.github.hoshikurama.extradatabases.h2.parser.components

import com.github.hoshikurama.extradatabases.h2.parser.CompositeStage
import com.github.hoshikurama.extradatabases.h2.parser.SQLFormat
import com.github.hoshikurama.extradatabases.h2.parser.TerminalStage
import com.github.hoshikurama.extradatabases.h2.parser.appendAtFront
import com.github.hoshikurama.extradatabases.h2.parser.column.InSupport
import com.github.hoshikurama.extradatabases.h2.parser.column.NamedColumn
import com.github.hoshikurama.extradatabases.h2.parser.column.TicketColumnObject

class UpdateTicket() :  {



    override fun parseStage(): TerminalStage {
        valuesToUpdate.asSequence()
            .map { (columnObject, rawValue) ->
                val column = columnObject.sqlColumnName
                val value = columnObject.typeToInCompare(rawValue)
                val symbol = if (rawValue != null) "=" else "IS"
                TerminalStage(
                    str = "$column $symbol ?",
                    arguments = listOf(value),
                )
            }
            .toList()
            .run(SQLFormat::spacedAND)
            .apply {
                statement.appendAtFront("UPDATE \"TicketManager_V8_Tickets\" SET ")
                statement.append(" WHERE ID")
            }
    }
}



abstract class Update<in T>(private val table: String): CompositeStage
    where T : NamedColumn,
          T : InSupport<in Any?,*>
{
    // Format: UPDATE <table> SET <sqlColumnName> = ? WHERE ID = ?

    private val valuesToUpdate = mutableMapOf<T, Any?>()

    override fun parseStage(): TerminalStage {
        valuesToUpdate.asSequence()
            .map { (columnObject, rawValue) ->
                val column = columnObject.sqlColumnName
                val value = columnObject.typeToInCompare(rawValue)
                val symbol = if (rawValue != null) "=" else "IS"
                TerminalStage(
                    str = "$column $symbol ?",
                    arguments = listOf(value),
                )
            }
            .toList()
            .run(SQLFormat::spacedAND)
            .apply {
                statement.appendAtFront("UPDATE \"TicketManager_V8_Tickets\" SET ")
                statement.append(" WHERE ID")
            }
    }
}