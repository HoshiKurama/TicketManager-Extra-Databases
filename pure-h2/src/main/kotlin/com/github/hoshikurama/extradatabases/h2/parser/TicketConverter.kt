package com.github.hoshikurama.extradatabases.h2.parser

import com.github.hoshikurama.extradatabases.h2.extensions.ASSIGNMENT_NOBODY
import com.github.hoshikurama.extradatabases.h2.extensions.asByte
import com.github.hoshikurama.extradatabases.h2.extensions.asString
import com.github.hoshikurama.ticketmanager.api.common.ticket.Assignment
import com.github.hoshikurama.ticketmanager.api.common.ticket.Creator
import com.github.hoshikurama.ticketmanager.api.common.ticket.Ticket

/*
SELECT Ticket.ID/listOf(Ticket.ID, Ticket.CREATOR) FROM "TicketManager_V8_Tickets" WHERE
SQL.SelectFromTicket(Ticket.ID) WHERE {

}

// NOTE: SQL SELECT vs internal select
*/

object SQL {

    // Select Full Ticket
    fun selectPartialTicket(whereBuilder: Where.TicketCol.() -> Unit): CompleteSQLChunk {
        val statement = StringBuilder("SELECT * FROM \"TicketManager_V8_Tickets\" ")
        val arguments = mutableListOf<Any>()

        // Execute and parse where
        val whereParsed = Where.TicketCol().run {
            whereBuilder(this)
            parseSelect()
        }

        // Add data to current parsing
        statement.append(whereParsed.statement)
        arguments.addAll(whereParsed.arguments)

        return CompleteSQLChunk(statement, arguments)
    }

    // For Tickets
    fun select(vararg ticketArgs: TicketColumn, whereBuilder: Where.TicketCol.() -> Unit): CompleteSQLChunk {
        val statement = StringBuilder()
        val arguments = mutableListOf<Any>()

        val returns = if (ticketArgs.size == 1) ticketArgs[0].sqlColumnName
        else "(${ticketArgs.joinToString(",") { it.sqlColumnName }})"
        statement.append("SELECT $returns FROM \"TicketManager_V8_Tickets\" ")

        ticketArgs
            .map(TicketColumn::sqlColumnName)
            .run(arguments::addAll)

        // Execute and parse where
        val whereParsed = Where.TicketCol().run {
            whereBuilder(this)
            parseSelect()
        }

        // Add data to current parsing
        statement.append(whereParsed.statement)
        arguments.addAll(whereParsed.arguments)

        return CompleteSQLChunk(statement, arguments)
    }
    fun update(ticketID: Long, builder: Insert.FromTicketColumn.() -> Unit): CompleteSQLChunk {
        val changes = Insert.FromTicketColumn()
        builder(changes)
        return changes.parse(ticketID)
    }

    // For Actions
    /*
    fun select(vararg actionArgs: ActionColumn, whereBuilder: Where.FromActionColumn.() -> Unit): CompleteSQLChunk {
        val statement = StringBuilder()
        val arguments = mutableListOf<Any>()

        val returns = if (actionArgs.size == 1) "?"
        else "(${List(actionArgs.size) {"?"}.joinToString(",")})"
        statement.append("SELECT $returns FROM \"TicketManager_V8_Actions\" ")

        actionArgs
            .map(ActionColumn::sqlColumnName)
            .run(arguments::addAll)

        // Execute and parse where
        val whereParsed = Where.FromActionColumn().run {
            whereBuilder(this)
            parseSelect()
        }

        // Add data to current parsing
        statement.append(whereParsed.statement)
        arguments.addAll(whereParsed.arguments)

        return CompleteSQLChunk(statement, arguments)
    }
*/
}

abstract class Insert {
    protected val table = mutableListOf<Pair<String, Any>>()
    private fun TicketColumn.standardSet() = "$sqlColumnName = ?"

    // Ticket Table
    //infix fun TicketCreator.setTo(creator: Creator) = table.add(standardSet() to creator.asString())
    infix fun TicketPriority.setTo(priority: Ticket.Priority) = table.add(standardSet() to priority.asByte())
    infix fun TicketStatus.setTo(status: Ticket.Status) = table.add(standardSet() to status.name)
    infix fun TicketAssignment.setTo(assignment: Assignment) = table.add(standardSet() to assignment.asString())
    infix fun TicketStatusUpdate.setTo(update: Boolean) = table.add(standardSet() to update)

    class FromTicketColumn : Insert() {
        fun parse(id: Long): CompleteSQLChunk {
            val (statements, arguments) = table.unzip()
            val stmt = statements.joinToString(" AND ")
            return CompleteSQLChunk("UPDATE TicketManager_V8_Tickets SET $stmt WHERE ID = ?".asStringBuilder(), arguments.toMutableList().apply { add(id) })
        }
    }
}

abstract class Where {
    protected val ticketTable = mutableListOf<Pair<StringBuilder, List<Any>>>()
    protected val actionTable = mutableListOf<Pair<StringBuilder, List<Any>>>()

    // Note: These place the messages in the correct location
    private fun fromTicketCol(statement: String, argument: Any) {
        ticketTable.add(StringBuilder(statement) to listOf(argument))
    }
    private fun fromActionCol(statement: String, vararg args: Any) {
        actionTable.add(StringBuilder(statement) to args.toList())
    }

    // Ticket Table
    infix fun TicketID.equalTo(id: Long) = fromTicketCol("$sqlColumnName = ?", id)
    //infix fun TicketID.notEqualTo(id: Long) = fromTicketCol("$sqlColumnName != ?", id)
    infix fun TicketCreator.equalTo(creator: Creator) = fromTicketCol("$sqlColumnName = ?", creator.asString())
    infix fun TicketCreator.notEqualTo(creator: Creator) = fromTicketCol("$sqlColumnName != ?", creator.asString())
    infix fun TicketPriority.equalTo(priority: Ticket.Priority) = fromTicketCol("$sqlColumnName = ?", priority.asByte())
    infix fun TicketPriority.notEqualTo(priority: Ticket.Priority) = fromTicketCol("$sqlColumnName != ?", priority.asByte())
    infix fun TicketPriority.lessThan(priority: Ticket.Priority) = fromTicketCol("$sqlColumnName < ?", priority.asByte())
    infix fun TicketPriority.greaterThan(priority: Ticket.Priority) = fromTicketCol("$sqlColumnName > ?", priority.asByte())
    infix fun TicketStatus.equalTo(status: Ticket.Status) = fromTicketCol("$sqlColumnName = ?", status.name)
    infix fun TicketStatus.notEqualTo(status: Ticket.Status) = fromTicketCol("$sqlColumnName != ?", status.name)
    infix fun TicketStatusUpdate.equalTo(statusUpdate: Boolean) = fromTicketCol("$sqlColumnName = ?", statusUpdate)
    //infix fun TicketStatusUpdate.notEqualTo(statusUpdate: Boolean) = fromTicketCol("$sqlColumnName != ?", statusUpdate)
    infix fun TicketAssignment.equalTo(assignment: Assignment) {
        if (assignment is Assignment.Nobody)
            ticketTable.add(StringBuilder("($sqlColumnName = $ASSIGNMENT_NOBODY OR $sqlColumnName IS NULL)") to emptyList())
        else fromTicketCol("ASSIGNED_TO = ?", assignment.asString())
    }
    infix fun TicketAssignment.notEqualTo(assignment: Assignment) {
        if (assignment is Assignment.Nobody)
            ticketTable.add(StringBuilder("($sqlColumnName != $ASSIGNMENT_NOBODY AND $sqlColumnName IS NOT NULL)") to emptyList())
        else fromTicketCol("ASSIGNED_TO != ?", assignment.asString())
    }
    infix fun TicketID.inside(ids: List<Ticket>) {
        ticketTable.add("ID IN (${ids.joinToString(", ")})".asStringBuilder() to ids)
    }

    // Action Table
    infix fun ActionEpochTime.madeBefore(epochTime: Long) = fromActionCol("$sqlColumnName < ? AND ACTION_TYPE = ?", epochTime, ActionAsEnum.OPEN.name)
    infix fun ActionEpochTime.madeAfter(epochTime: Long) = fromActionCol("$sqlColumnName > ? AND ACTION_TYPE = ?", epochTime, ActionAsEnum.OPEN.name)
    infix fun ActionWorld.equalTo(world: String) = fromActionCol("$sqlColumnName = ? AND ACTION_TYPE = ?", world, ActionAsEnum.OPEN.name)
    infix fun ActionWorld.notEqualTo(world: String) = fromActionCol("$sqlColumnName != ? AND ACTION_TYPE = ?", world, ActionAsEnum.OPEN.name)
    infix fun ActionCreator.closedBy(creator: Creator) = fromActionCol("($sqlColumnName = ? OR ACTION_TYPE = ? OR ACTION_TYPE = ?) AND $sqlColumnName = ?",
        ActionAsEnum.CLOSE.name, ActionAsEnum.CLOSE_WITH_COMMENT.name, ActionAsEnum.MASS_CLOSE.name, creator.asString())
    infix fun ActionCreator.notClosedBy(creator: Creator) = fromActionCol("(ACTION_TYPE = ? OR ACTION_TYPE = ? OR ACTION_TYPE = ?) AND $sqlColumnName != ?",
        ActionAsEnum.CLOSE.name, ActionAsEnum.CLOSE_WITH_COMMENT.name, ActionAsEnum.MASS_CLOSE.name, creator.asString())
    infix fun ActionCreator.lastClosedBy(creator: Creator) = fromActionCol("""ACTION_ID IN (SELECT MAX(ACTION_ID) FROM "TicketManager_V8_Actions" 
        WHERE (ACTION_TYPE = ? OR ACTION_TYPE = ? OR ACTION_TYPE = ?) AND CREATOR = ? GROUP BY TICKET_ID)""".trimIndent(),
        ActionAsEnum.CLOSE.name, ActionAsEnum.CLOSE_WITH_COMMENT.name, ActionAsEnum.MASS_CLOSE.name, creator.asString())
    infix fun ActionCreator.notLastClosedBy(creator: Creator) = fromActionCol("""ACTION_ID IN (SELECT MAX(ACTION_ID) FROM "TicketManager_V8_Actions" 
        WHERE (ACTION_TYPE = ? OR ACTION_TYPE = ? OR ACTION_TYPE = ?) AND CREATOR != ? GROUP BY TICKET_ID)""".trimIndent(),
        ActionAsEnum.CLOSE.name, ActionAsEnum.CLOSE_WITH_COMMENT.name, ActionAsEnum.MASS_CLOSE.name, creator.asString())
    infix fun ActionKeyword.containing(keywords: List<String>) {
        val firstPart = StringBuilder("(ACTION_TYPE = ? OR ACTION_TYPE = ?, ACTION_TYPE = ?)")
        val secondPart = keywords.joinToString(" OR ") { "MESSAGE LIKE CONCAT( '%',?,'%')" }
        return fromActionCol("$firstPart AND ($secondPart)", ActionAsEnum.OPEN.name,
            ActionAsEnum.CLOSE_WITH_COMMENT.name, ActionAsEnum.COMMENT.name, *keywords.toTypedArray()
        )
    }
    infix fun ActionKeyword.notContaining(keywords: List<String>) {
        val firstPart = StringBuilder("(ACTION_TYPE = ? OR ACTION_TYPE = ?, ACTION_TYPE = ?)")
        val secondPart = keywords.joinToString(" AND ") { "MESSAGE NOT LIKE CONCAT( '%',?,'%')" }
        return fromActionCol("$firstPart AND ($secondPart)", ActionAsEnum.OPEN.name,
            ActionAsEnum.CLOSE_WITH_COMMENT.name, ActionAsEnum.COMMENT.name, *keywords.toTypedArray()
        )
    }

    class TicketCol : Where() {

        fun andAny(builder: TicketCol.() -> Any) {
            val partialStatement = StringBuilder("(")

            val newWhere = TicketCol()
            builder(newWhere)

            val (statements, args) = newWhere.ticketTable.unzip()

            statements.joinToString(" OR ")
                .run(partialStatement::append)
            partialStatement.append(")")

            ticketTable.add(partialStatement to args)
        }

        fun parseSelect(): CompleteSQLChunk {
            val finalSQLStatement = StringBuilder()
            val finalArguments = mutableListOf<Any>()

            // Parse Ticket Column items
            val (ticketStatements, ticketArguments) = ticketTable.unzip()
            finalSQLStatement.append("WHERE ${ticketStatements.joinToString(" AND ")}")
            finalArguments.addAll(ticketArguments.flatten())

            // Parse Action Column items
            if (actionTable.isNotEmpty()) {
                val (actionStatements, actionArguments) = actionTable.unzip()
                finalSQLStatement.append("AND ID IN (SELECT DISTINCT TICKET_ID FROM \"TicketManager_V8_Actions\" WHERE ${actionStatements.joinToString(" AND ")})")
                finalArguments.addAll(actionArguments.flatten())
            }

            // Combine and finish parse
            return CompleteSQLChunk(
                statement = finalSQLStatement,
                arguments = finalArguments
            )
        }
    }

    class FromActionColumn : Where() {
        fun parseSelect(): CompleteSQLChunk {
            val finalSQLStatement = StringBuilder()
            val finalArguments = mutableListOf<Any>()

            // Parse Ticket Column items
            val (actionStatements, actionArguments) = actionTable.unzip()
            finalSQLStatement.append("WHERE ${actionStatements.joinToString(" AND ")}")
            finalArguments.addAll(actionArguments.flatten())

            // Parse Ticket Column items
            if (ticketTable.isNotEmpty()) {
                val (ticketStatements, ticketArguments) = ticketTable.unzip()
                finalSQLStatement.append("AND TICKET_ID IN (SELECT DISTINCT ID FROM \"TicketManager_V8_Tickets\" WHERE ${ticketStatements.joinToString(" AND ")})")
                finalArguments.addAll(ticketArguments.flatten())
            }

            // Combine and finish parse
            return CompleteSQLChunk(
                statement = finalSQLStatement,
                arguments = finalArguments
            )
        }
    }
}

data class CompleteSQLChunk(val statement: StringBuilder, val arguments: MutableList<Any>)
data class CompleteSQL(val statement: String, val arguments: List<Any>)

fun String.asStringBuilder() = StringBuilder(this)
fun CompleteSQLChunk.addEnding(): CompleteSQL {
    statement.run {
        trimEnd()
        append(";")
    }
    return CompleteSQL(statement.toString(), arguments)
}

/*
CREATE TABLE IF NOT EXISTS "TicketManager_V8_Actions"
                (
                    ACTION_ID       NUMBER GENERATED ALWAYS AS IDENTITY (START WITH 1 INCREMENT BY 1) not null,
                    TICKET_ID       BIGINT                  NOT NULL,
                    ACTION_TYPE     VARCHAR_IGNORECASE(20)  NOT NULL,
                    CREATOR         VARCHAR_IGNORECASE(70)  NOT NULL,
                    MESSAGE         LONGVARCHAR,
                    SERVER          VARCHAR(100),
                    WORLD           VARCHAR(100),
                    WORLD_X         INT,
                    WORLD_Y         INT,
                    WORLD_Z         INT,
 */
