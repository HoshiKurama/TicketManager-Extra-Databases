package com.github.hoshikurama.extradatabases.h2.extensions

import com.github.hoshikurama.ticketmanager.api.common.ticket.Action
import com.github.hoshikurama.ticketmanager.api.common.ticket.ActionInfo

fun Action.getMessage(): String? = when (this) {
    is ActionInfo.Assign -> assignment.asString()
    is ActionInfo.CloseWithComment -> comment
    is ActionInfo.Open -> message
    is ActionInfo.Comment -> comment
    is ActionInfo.SetPriority -> priority.asByte().toString()
    else -> null
}

fun Action.getEnumForDB(): ActionAsEnum = when (this) {
    is ActionInfo.Assign -> ActionAsEnum.ASSIGN
    is ActionInfo.CloseWithComment -> ActionAsEnum.CLOSE_WITH_COMMENT
    is ActionInfo.CloseWithoutComment -> ActionAsEnum.CLOSE
    is ActionInfo.Comment -> ActionAsEnum.COMMENT
    is ActionInfo.MassClose -> ActionAsEnum.MASS_CLOSE
    is ActionInfo.Open -> ActionAsEnum.OPEN
    is ActionInfo.Reopen -> ActionAsEnum.REOPEN
    is ActionInfo.SetPriority -> ActionAsEnum.SET_PRIORITY
}

enum class ActionAsEnum {
    ASSIGN, CLOSE, CLOSE_WITH_COMMENT, COMMENT, OPEN, REOPEN, SET_PRIORITY, MASS_CLOSE
}