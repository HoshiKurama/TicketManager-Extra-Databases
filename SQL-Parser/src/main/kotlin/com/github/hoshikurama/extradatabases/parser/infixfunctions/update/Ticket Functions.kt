package com.github.hoshikurama.extradatabases.parser.infixfunctions.update

import com.github.hoshikurama.extradatabases.parser.TerminalStage
import com.github.hoshikurama.extradatabases.parser.column.InSupport
import com.github.hoshikurama.extradatabases.parser.column.TicketColumnField
import com.github.hoshikurama.extradatabases.parser.components.Update

@Suppress("Unused", "FunctionName")
abstract class UpdateExposeTicketFunctions(ids: List<Long>) : Update(ids) {

    infix fun <Column, Input, Output> Column.`=`(input: Input)
        where Column : TicketColumnField,
              Column : InSupport<Input, Output>
    {
        TerminalStage("$sqlColumnName = ?", listOf(typeToInCompare(input)))
            .run(stages::add)
    }
}