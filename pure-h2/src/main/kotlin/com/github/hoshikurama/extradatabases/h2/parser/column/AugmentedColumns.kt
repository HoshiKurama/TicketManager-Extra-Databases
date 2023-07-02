package com.github.hoshikurama.extradatabases.h2.parser.column

import com.github.hoshikurama.extradatabases.h2.parser.CompositeStage
import com.github.hoshikurama.extradatabases.h2.parser.TerminalStage
import com.github.hoshikurama.extradatabases.h2.parser.emptyStage

sealed interface AugmentedColumn<T> : CompositeStage

class Distinct<T: NamedColumn>(private val column: T) : AugmentedColumn<T> {
    override fun parseStage() = TerminalStage("DISTINCT ${column.sqlColumnName}", emptyStage)
}

class Max<T: NamedColumn>(private val column: T) : AugmentedColumn<T> {
    override fun parseStage() = TerminalStage("MAX(${column.sqlColumnName})", emptyStage)
}