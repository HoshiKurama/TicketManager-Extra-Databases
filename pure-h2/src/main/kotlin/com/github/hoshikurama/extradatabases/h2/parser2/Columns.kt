package com.github.hoshikurama.extradatabases.h2.parser2

typealias `Ticket.*` = Ticket.STAR
typealias `Action.*` = Action.STAR

sealed interface NamedColumn {
    val sqlColumnName: String
}

sealed class ColumnName(override val sqlColumnName: String): NamedColumn

object Ticket {
    sealed interface ColumnField: NamedColumn
    object ID: ColumnName("ID"), ColumnField
    object Creator: ColumnName("CREATOR"), ColumnField
    object Priority: ColumnName("PRIORITY"), ColumnField
    object Status: ColumnName("STATUS"), ColumnField
    object Assignment: ColumnName("ASSIGNED_TO"), ColumnField
    object StatusUpdate: ColumnName("STATUS_UPDATE_FOR_CREATOR"), ColumnField

    // SPECIAL USE
    object STAR: ColumnName("*"), ColumnField
}

object Action {
    sealed interface ColumnField: NamedColumn
    object TicketID: ColumnName("TICKET_ID"), ColumnField
    object ActionType: ColumnName("ACTION_TYPE"), ColumnField
    object Creator: ColumnName("CREATOR"), ColumnField
    //object Message: ColumnName("MESSAGE"), ColumnField //TODO WHAT IS THIS USED FOR?
    object EpochTime: ColumnName("EPOCH_TIME"), ColumnField
    object World: ColumnName("WORLD")

    // SPECIAL USE
    object STAR: ColumnName("*"), Ticket.ColumnField
}