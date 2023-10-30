package com.github.hoshikurama.extradatabases.parser.column

import com.github.hoshikurama.extradatabases.common.extensions.ActionAsEnum
import com.github.hoshikurama.extradatabases.common.extensions.asByte
import com.github.hoshikurama.extradatabases.common.extensions.asString
import com.github.hoshikurama.ticketmanager.api.ticket.Assignment as ActualAssignment
import com.github.hoshikurama.ticketmanager.api.ticket.Creator as ActualCreator
import com.github.hoshikurama.ticketmanager.api.ticket.Ticket as ActualTicket

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

// Combines interfaces above for objects. Useful for update dsl
sealed interface TicketColumnObject<KotlinInput, SQLCompare> : TicketColumnField, InSupport<KotlinInput, SQLCompare>
sealed interface ActionColumnObject<KotlinInput, SQLCompare> : ActionColumnField, InSupport<KotlinInput, SQLCompare>

/**
 * Represents columns in the Ticket table
 */
object Ticket {
    object ID : TicketColumnObject<Long, Long> {
        override val typeToInCompare: (Long) -> Long = { it }
        override val sqlColumnName = "ID"
    }
    object Creator : TicketColumnObject<ActualCreator, String> {
        override val typeToInCompare = ActualCreator::asString
        override val sqlColumnName = "CREATOR"
    }
    object Priority : TicketColumnObject<ActualTicket.Priority, Byte> {
        override val typeToInCompare = ActualTicket.Priority::asByte
        override val sqlColumnName = "PRIORITY"
    }
    object Assignment : TicketColumnObject<ActualAssignment, String> {
        override val typeToInCompare = ActualAssignment::asString
        override val sqlColumnName = "ASSIGNED_TO"
    }
    object Status : TicketColumnObject<ActualTicket.Status, String> {
        override val typeToInCompare = ActualTicket.Status::name
        override val sqlColumnName = "STATUS"
    }
    object StatusUpdate : TicketColumnObject<Boolean, Boolean> {
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
    object  TicketID: ActionColumnObject<Long, Long> {
        override val typeToInCompare: (Long) -> Long = { it }
        override val sqlColumnName = "TICKET_ID"
    }
    object ActionType : ActionColumnObject<ActionAsEnum, String> {
        override val typeToInCompare = ActionAsEnum::name
        override val sqlColumnName = "ACTION_TYPE"
    }
    object Creator : ActionColumnObject<ActualCreator, String> {
        override val typeToInCompare = ActualCreator::asString
        override val sqlColumnName = "CREATOR"
    }
    object World : ActionColumnObject<String, String> {
        override val typeToInCompare: (String) -> String = { it }
        override val sqlColumnName = "WORLD"
    }
    object EpochTime : ActionColumnObject<Long, Long> {
        override val typeToInCompare: (Long) -> Long = { it }
        override val sqlColumnName = "EPOCH_TIME"
    }
    object Message : ActionColumnObject<String, String> {
        override val typeToInCompare: (String) -> String = { it }
        override val sqlColumnName = "MESSAGE"
    }
    object ID : ActionColumnObject<Long, Long> {
        override val typeToInCompare: (Long) -> Long = { it }
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