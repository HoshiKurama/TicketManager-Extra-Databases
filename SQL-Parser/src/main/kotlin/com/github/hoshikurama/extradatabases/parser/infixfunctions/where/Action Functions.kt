package com.github.hoshikurama.extradatabases.parser.infixfunctions.where

import com.github.hoshikurama.extradatabases.common.extensions.ActionAsEnum
import com.github.hoshikurama.extradatabases.parser.*
import com.github.hoshikurama.extradatabases.parser.column.*
import com.github.hoshikurama.extradatabases.parser.components.Where
import com.github.hoshikurama.ticketmanager.api.ticket.Creator
import com.github.hoshikurama.extradatabases.parser.column.ActionColumnField
import com.github.hoshikurama.extradatabases.parser.column.InSupport
import com.github.hoshikurama.extradatabases.parser.column.Max
import com.github.hoshikurama.extradatabases.parser.column.TicketMeta
import com.github.hoshikurama.extradatabases.parser.column.Action as ActionColumn

@Suppress("FunctionName")
abstract class WhereExposeActionFunctions(useWhereClause: Boolean = true) : Where(useWhereClause) {

    infix fun <Column, Input, Output> Column.`==`(input: Input)
        where Column : ActionColumnField,
              Column : InSupport<Input, Output> {
        TerminalStage("$sqlColumnName = ?", typeToInCompare(input))
            .run(stages::add)
    }

    infix fun <Column, Input, Output> Column.`!=`(input: Input)
            where Column : ActionColumnField,
                  Column : InSupport<Input, Output> {
        TerminalStage("$sqlColumnName != ?", typeToInCompare(input))
            .run(stages::add)
    }

    @JvmName("lessThanPriority")
    @Suppress("NonAsciiCharacters")
    infix fun ActionColumn.EpochTime.`＜`(time: Long) = stdStage("<", time)
    @JvmName("greaterThanPriority")
    @Suppress("NonAsciiCharacters")
    infix fun ActionColumn.EpochTime.`＞`(time: Long) = stdStage(">", time)

    // IN statements

    // _ IN (SELECT
    infix fun <T : ActionColumnField> T.`in`(otherStatement: Stage) {
        otherStatement.parseStage()
            .apply {
                statement.appendAtFront("$sqlColumnName IN (")
                statement.append(")")
            }
            .run(stages::add)
    }

    infix fun <T : ActionColumnField> T.`!in`(otherStatement: Stage) {
        otherStatement.parseStage()
            .apply {
                statement.appendAtFront("$sqlColumnName NOT IN (")
                statement.append(")")
            }
            .run(stages::add)
    }

    // Check in list
    infix fun <T, U> T.`in`(list: List<U>) where T : InSupport<U, *>, T : ActionColumnField {
        TerminalStage(
            str = "$sqlColumnName IN (${list.joinToString(",") { "?" }})",
            arguments = list.map(typeToInCompare)
        ).run(stages::add)
    }

    // Check not in list
    infix fun <T, U> T.`!in`(list: List<U>) where T : InSupport<U, *>, T : ActionColumnField {
        TerminalStage(
            str = "$sqlColumnName NOT IN (${list.joinToString(",") { "?" }})",
            arguments = list.map(typeToInCompare)
        ).run(stages::add)
    }

    // Composite Meta Functions

    // Last Closed By
    private fun lastClosedBySearch(creator: Creator) = selectAction {
        +Max(ActionColumn.ID)

        where {
            ActionColumn.ActionType `in` listOf(ActionAsEnum.CLOSE, ActionAsEnum.CLOSE_WITH_COMMENT, ActionAsEnum.MASS_CLOSE)
            ActionColumn.Creator `==` creator
        }
        raw("GROUP BY TICKET_ID")
    }
    infix fun TicketMeta.LastClosedBy.`==`(creator: Creator) {
        this@WhereExposeActionFunctions.run {
            ActionColumn.ID `in` lastClosedBySearch(creator)
        }
    }
    infix fun TicketMeta.LastClosedBy.`!=`(creator: Creator) {
        this@WhereExposeActionFunctions.run {
            ActionColumn.ID `!in` lastClosedBySearch(creator)
        }
    }

    // ClosedBy
    infix fun TicketMeta.ClosedBy.`==`(creator: Creator) {
        this@WhereExposeActionFunctions.run {
            ActionColumn.ActionType `in` listOf(ActionAsEnum.CLOSE, ActionAsEnum.CLOSE_WITH_COMMENT, ActionAsEnum.MASS_CLOSE)
            ActionColumn.Creator `==` creator
        }
    }
    infix fun TicketMeta.ClosedBy.`!=`(creator: Creator) {
        this@WhereExposeActionFunctions.run {
            ActionColumn.ActionType `in` listOf(ActionAsEnum.CLOSE, ActionAsEnum.CLOSE_WITH_COMMENT, ActionAsEnum.MASS_CLOSE)
            ActionColumn.Creator `!=` creator
        }
    }

    // Ticket Made Time
    infix fun TicketMeta.TimeCreated.before(epochTime: Long) {
        this@WhereExposeActionFunctions.run {
            ActionColumn.ActionType `==` ActionAsEnum.OPEN
            ActionColumn.EpochTime `＞` epochTime
        }
    }
    infix fun TicketMeta.TimeCreated.after(epochTime: Long) {
        this@WhereExposeActionFunctions.run {
            ActionColumn.ActionType `==` ActionAsEnum.OPEN
            ActionColumn.EpochTime `＜` epochTime
        }
    }

    // World
    infix fun TicketMeta.CreationWorld.`==`(world: String) {
        this@WhereExposeActionFunctions.run {
            ActionColumn.ActionType `==` ActionAsEnum.OPEN
            ActionColumn.World `==` world
        }
    }
    infix fun TicketMeta.CreationWorld.`!=`(world: String) {
        this@WhereExposeActionFunctions.run {
            ActionColumn.ActionType `==` ActionAsEnum.OPEN
            ActionColumn.World `!=` world
        }
    }

    // Contains Keyword
    infix fun TicketMeta.Keywords.`in`(list: List<String>) {
        this@WhereExposeActionFunctions.run {
            ActionColumn.ActionType `in` listOf(ActionAsEnum.OPEN, ActionAsEnum.CLOSE_WITH_COMMENT, ActionAsEnum.COMMENT)

            list.map { TerminalStage("${ActionColumn.Message.sqlColumnName} LIKE CONCAT( '%',?,'%')", it) }
                .run(SQLFormat::spacedOR)
                .applyParenthesis()
                .run(stages::add)
        }
    }
    infix fun TicketMeta.Keywords.`!in`(list: List<String>) {
        this@WhereExposeActionFunctions.run {
            ActionColumn.ActionType `in` listOf(ActionAsEnum.OPEN, ActionAsEnum.CLOSE_WITH_COMMENT, ActionAsEnum.COMMENT)

            list.map { TerminalStage("${ActionColumn.Message.sqlColumnName} NOT LIKE CONCAT( '%',?,'%')", it) }
                .run(SQLFormat::spacedOR)
                .applyParenthesis()
                .run(stages::add)
        }
    }
}