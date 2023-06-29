package com.github.hoshikurama.extradatabases.h2.parser2

typealias `Ticket.*` = Ticket.STAR
typealias `Action.*` = Action.STAR

sealed interface NamedColumn {
    val sqlColumnName: String
}

sealed class ColumnName(override val sqlColumnName: String): NamedColumn

sealed interface TicketColumnField : NamedColumn
sealed interface ActionColumnField : NamedColumn

object Ticket {
    object ID: ColumnName("ID"), TicketColumnField
    object Creator: ColumnName("CREATOR"), TicketColumnField
    object Priority: ColumnName("PRIORITY"), TicketColumnField
    object Status: ColumnName("STATUS"), TicketColumnField
    object Assignment: ColumnName("ASSIGNED_TO"), TicketColumnField
    object StatusUpdate: ColumnName("STATUS_UPDATE_FOR_CREATOR"), TicketColumnField

    // SPECIAL USE
    object STAR: ColumnName("*"), TicketColumnField
}

object Action {
    object TicketID: ColumnName("TICKET_ID"), ActionColumnField
    object ActionType: ColumnName("ACTION_TYPE"), ActionColumnField
    object Creator: ColumnName("CREATOR"), ActionColumnField
    //object Message: ColumnName("MESSAGE"), ActionColumnField //TODO WHAT IS THIS USED FOR?
    object EpochTime: ColumnName("EPOCH_TIME"), ActionColumnField
    object World: ColumnName("WORLD"), ActionColumnField

    // SPECIAL USE
    object STAR: ColumnName("*"), ActionColumnField
}