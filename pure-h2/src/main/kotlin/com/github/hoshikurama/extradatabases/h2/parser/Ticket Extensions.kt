package com.github.hoshikurama.extradatabases.h2.parser

import com.github.hoshikurama.ticketmanager.api.common.ticket.Assignment
import com.github.hoshikurama.ticketmanager.api.common.ticket.Creator
import com.github.hoshikurama.ticketmanager.api.common.ticket.Ticket

private const val ASSIGNMENT_CONSOLE = "CONSOLE"
internal const val ASSIGNMENT_NOBODY = "NOBODY"
private const val ASSIGNMENT_PLAYER = "PLAYER"
private const val ASSIGNMENT_GROUP = "GROUP"
private const val ASSIGNMENT_PHRASE = "PHRASE"
private const val CREATOR_USER = "USER"
private const val CREATOR_CONSOLE = "CONSOLE"
private const val CREATOR_INVALID_UUID = "INVALID_UUID"
private const val CREATOR_DUMMY = "DUMMY"

fun Assignment.asString() = when (this) {
    is Assignment.Console -> ASSIGNMENT_CONSOLE
    is Assignment.Nobody -> ASSIGNMENT_NOBODY
    is Assignment.Player -> "$ASSIGNMENT_PLAYER.$username"
    is Assignment.PermissionGroup -> "$ASSIGNMENT_GROUP.$permissionGroup"
    is Assignment.Phrase -> "$ASSIGNMENT_PHRASE.$phrase"
}

fun Creator.asString(): String = when (this) {
    is Creator.Console -> CREATOR_CONSOLE
    is Creator.UUIDNoMatch -> CREATOR_INVALID_UUID
    is Creator.User -> "$CREATOR_USER.$uuid"
    is Creator.DummyCreator -> CREATOR_DUMMY
}

fun Ticket.Priority.asByte(): Byte = when (this) {
    Ticket.Priority.LOWEST -> 1
    Ticket.Priority.LOW -> 2
    Ticket.Priority.NORMAL -> 3
    Ticket.Priority.HIGH -> 4
    Ticket.Priority.HIGHEST -> 5
}