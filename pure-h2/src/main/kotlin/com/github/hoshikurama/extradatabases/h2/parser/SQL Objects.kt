package com.github.hoshikurama.extradatabases.h2.parser

object SQLTicket {
    val id = TicketID("ID")
    val creator = TicketCreator("CREATOR")
    val priority = TicketPriority("PRIORITY")
    val status = TicketStatus("STATUS")
    val assignment = TicketAssignment("ASSIGNED_TO")
    val creatorStatusUpdate = TicketStatusUpdate("STATUS_UPDATE_FOR_CREATOR")
}

object SQLAction {
    //val ticketID = ActionTicketID("TICKET_ID")
    //val type = ActionType("ACTION_TYPE")
    val creator = ActionCreator("CREATOR")
    val message = ActionMessage("MESSAGE")
    val epochTime = ActionEpochTime("EPOCH_TIME")
    val world = ActionWorld("WORLD")
    val keyword = ActionKeyword
}

internal enum class ActionAsEnum {
    ASSIGN, CLOSE, CLOSE_WITH_COMMENT, COMMENT, OPEN, REOPEN, SET_PRIORITY, MASS_CLOSE
}