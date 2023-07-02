package com.github.hoshikurama.extradatabases.h2.parser


sealed interface Stage {
    fun parseStage(): TerminalStage
}

/**
 * Stage that can only contain its own data, i.e. it does not allow for further SQL nesting.
 */
data class TerminalStage(val statement: StringBuilder, val arguments: List<Any?>): Stage {
    constructor(str: String, arguments: List<Any?>): this(StringBuilder(str), arguments)
    constructor(str: String, argument: Any?): this(StringBuilder(str), listOf(argument))
    override fun parseStage(): TerminalStage = this
}

/**
 * Stage that contains one or more stages within.
 */
interface CompositeStage : Stage