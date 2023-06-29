package com.github.hoshikurama.extradatabases.h2.parser2

fun StringBuilder.appendApply(other: String) = apply { append(other) }
fun StringBuilder.appendApply(other: StringBuilder) = apply { append(other) }
fun StringBuilder.appendAtFront(before: String) = StringBuilder(before).appendApply(this)

/**
 * Collection of nice formatters
 */
object SQLFormat {

    fun joinTogether(
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
     * Creates (stage,stage,...stage) format
     */
    fun list(stages: List<TerminalStage>) = joinTogether(stages, separator = ",", prefix = "(", postfix = ")")
    /**
     * Adds spaces between statements
     */
    fun spaces(stages: List<TerminalStage>) = joinTogether(stages, separator = " ")
    /**
     * Adds " AND " between statements
     */
    fun and(stages: List<TerminalStage>) = joinTogether(stages, separator = " AND ")
}

val emptyStage = TerminalStage("", emptyList())
fun stringOnlyStage(str: String) = TerminalStage(str, emptyList())