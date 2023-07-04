package com.github.hoshikurama.extradatabases.h2.parser

fun StringBuilder.appendApply(other: String) = apply { append(other) }
fun StringBuilder.appendApply(other: StringBuilder) = apply { append(other) }
fun StringBuilder.appendAtFront(before: String) = apply { insert(0, before) }

/**
 * Collection of nice formatters
 */
object SQLFormat {

    private fun joinTogether(
        stages: List<TerminalStage>,
        separator: String = "",
        prefix: String = "",
        postfix: String = "",
    ): TerminalStage {
        val usefulStages = stages
            .filter { it.statement.isNotEmpty() }

        val newStatement = usefulStages.joinTo(
            buffer = StringBuilder(),
            separator = separator,
            prefix = prefix,
            postfix = postfix,
            transform = TerminalStage::statement
        )

        val arguments = usefulStages
            .map(TerminalStage::arguments)
            .flatten()

        return TerminalStage(newStatement, arguments)
    }

    /**
     * Adds spaces between statements
     */
    fun spaces(stages: List<TerminalStage>) = joinTogether(stages, separator = " ")
    /**
     * Adds " AND " between statements
     */
    fun spacedAND(stages: List<TerminalStage>) = joinTogether(stages, separator = " AND ")
    fun spacedOR(stages: List<TerminalStage>) = joinTogether(stages, separator = " OR ")
    fun spacedCommas(stages: List<TerminalStage>) = joinTogether(stages, separator = ", ")
}

fun stringOnlyStage(str: String) = TerminalStage(str, emptyList())
fun sbOnlyStage(sb: StringBuilder) = TerminalStage(sb, emptyList())

fun TerminalStage.applyParenthesis() = apply {
    statement.appendAtFront("(")
    statement.append(")")
}