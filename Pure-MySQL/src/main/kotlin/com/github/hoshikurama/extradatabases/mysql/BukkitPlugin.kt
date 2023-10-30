package com.github.hoshikurama.extradatabases.mysql

import com.github.hoshikurama.ticketmanager.api.impl.TicketManager
import org.bukkit.plugin.java.JavaPlugin

class BukkitPlugin : JavaPlugin() {
    override fun onEnable() {
        TicketManager.DatabaseRegistry.register(MySQLExtension::class)
    }
}