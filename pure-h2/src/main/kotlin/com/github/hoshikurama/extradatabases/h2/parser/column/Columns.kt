package com.github.hoshikurama.extradatabases.h2.parser.column

import com.github.hoshikurama.extradatabases.h2.extensions.ActionAsEnum
import com.github.hoshikurama.extradatabases.h2.extensions.asString
import com.github.hoshikurama.ticketmanager.api.common.ticket.Assignment as ActualAssignment
import com.github.hoshikurama.ticketmanager.api.common.ticket.Creator as ActualCreator
import com.github.hoshikurama.ticketmanager.api.common.ticket.Ticket as ActualTicket

sealed interface NamedColumn {
    val sqlColumnName: String
}
sealed interface InSupport<KotlinInput, SQLCompare> : NamedColumn {
    val typeToInCompare: (KotlinInput) -> SQLCompare
}


// These target whether a search relies on a particular table
sealed interface TicketColumnField : NamedColumn
sealed interface ActionField
sealed interface ActionColumnField : ActionField, NamedColumn

/**
 * Represents columns in the Ticket table
 */
object Ticket {
    object ID : TicketColumnField, InSupport<Long, Long> {
        override val typeToInCompare: (Long) -> Long = { it }
        override val sqlColumnName = "ID"
    }
    object Creator : TicketColumnField, InSupport<ActualCreator, String> {
        override val typeToInCompare = ActualCreator::asString
        override val sqlColumnName = "CREATOR"
    }
    object Priority : TicketColumnField, InSupport<ActualTicket.Priority, String> {
        override val typeToInCompare = ActualTicket.Priority::name
        override val sqlColumnName = "PRIORITY"
    }
    object Assignment : TicketColumnField, InSupport<ActualAssignment, String> {
        override val typeToInCompare = ActualAssignment::asString
        override val sqlColumnName = "ASSIGNED_TO"
    }
    object Status : TicketColumnField, InSupport<ActualTicket.Status, String> {
        override val typeToInCompare = ActualTicket.Status::name
        override val sqlColumnName = "STATUS"
    }
    object StatusUpdate : TicketColumnField, InSupport<Boolean, Boolean> {
        override val typeToInCompare: (Boolean) -> Boolean = { it }
        override val sqlColumnName = "STATUS_UPDATE_FOR_CREATOR"
    }

    // SPECIAL USE
    object STAR : TicketColumnField {
        override val sqlColumnName = "*"
    }
}

/**
 * Represents columns in the Action table
 */
object Action {
    object  TicketID: ActionColumnField, InSupport<Long, Long> {
        override val typeToInCompare: (Long) -> Long = { it }
        override val sqlColumnName = "TICKET_ID"
    }
    object ActionType : ActionColumnField, InSupport<ActionAsEnum, String> {
        override val typeToInCompare = ActionAsEnum::name
        override val sqlColumnName = "ACTION_TYPE"
    }
    object Creator : ActionColumnField, InSupport<ActualCreator, String> {
        override val typeToInCompare = ActualCreator::asString
        override val sqlColumnName = "CREATOR"
    }
    object World : ActionColumnField, InSupport<String, String> {
        override val typeToInCompare: (String) -> String = { it }
        override val sqlColumnName = "WORLD"
    }
    object EpochTime : ActionColumnField {
        override val sqlColumnName = "EPOCH_TIME"
    }
    object Message : ActionColumnField {
        override val sqlColumnName = "MESSAGE"
    }
    object ID : ActionColumnField {
        override val sqlColumnName = "ACTION_ID"
    }

    // SPECIAL USE
    object STAR : ActionColumnField {
        override val sqlColumnName = "*"
    }
}

/**
 * Represents an abstract ticket concept that is stored in the Actions table. For example, the creator a ticket
 * was last closed by.
 */
object TicketMeta {
    object LastClosedBy : ActionField
    object ClosedBy : ActionField
    object TimeCreated : ActionField
    object CreationWorld : ActionField
    object Keywords : ActionField
}