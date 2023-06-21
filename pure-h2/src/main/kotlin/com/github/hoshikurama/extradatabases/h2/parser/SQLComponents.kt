package com.github.hoshikurama.extradatabases.h2.parser

sealed interface SQLComponent

sealed interface Column: SQLComponent {
    val sqlColumnName: String
}

sealed interface TicketColumn : Column
class TicketID(override val sqlColumnName: String): TicketColumn
class TicketCreator(override val sqlColumnName: String): TicketColumn
class TicketPriority(override val sqlColumnName: String): TicketColumn
class TicketStatus(override val sqlColumnName: String): TicketColumn
class TicketAssignment(override val sqlColumnName: String): TicketColumn
class TicketStatusUpdate(override val sqlColumnName: String): TicketColumn

sealed interface ActionColumn : Column
class ActionTicketID(override val sqlColumnName: String): ActionColumn
class ActionType(override val sqlColumnName: String): ActionColumn
class ActionCreator(override val sqlColumnName: String): ActionColumn
class ActionMessage(override val sqlColumnName: String): ActionColumn
class ActionEpochTime(override val sqlColumnName: String): ActionColumn
class ActionServer(override val sqlColumnName: String): ActionColumn
class ActionWorld(override val sqlColumnName: String): ActionColumn
object ActionKeyword : SQLComponent