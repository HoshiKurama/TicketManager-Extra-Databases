package com.github.hoshikurama.extradatabases.parser.column

import com.github.hoshikurama.extradatabases.parser.CompositeStage
import com.github.hoshikurama.extradatabases.parser.TerminalStage

sealed interface AugmentedColumn<T> : CompositeStage

class Distinct<T: NamedColumn>(private val column: T) : AugmentedColumn<T> {
    override fun parseStage() = TerminalStage("DISTINCT ${column.sqlColumnName}", emptyList())
}

class Max<T: NamedColumn>(private val column: T) : AugmentedColumn<T> {
    override fun parseStage() = TerminalStage("MAX(${column.sqlColumnName})", emptyList())
}

class Count<T: NamedColumn>(private val column: T) : AugmentedColumn<T> {
    override fun parseStage() = TerminalStage("COUNT(${column.sqlColumnName})", emptyList())
}