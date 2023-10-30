package com.github.hoshikurama.extradatabases.h2

import com.github.hoshikurama.ticketmanager.api.impl.TicketManager
import org.bukkit.plugin.java.JavaPlugin

@Suppress("UNUSED")
class BukkitPlugin : JavaPlugin() {
    override fun onEnable() {
        TicketManager.DatabaseRegistry.register(PureH2DBExtension::class)
    }
}