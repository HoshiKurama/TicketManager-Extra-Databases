package com.github.hoshikurama.extradatabases.mysql

import com.github.hoshikurama.ticketmanager.api.paper.TicketManagerDatabaseRegister
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class BukkitPlugin : JavaPlugin() {

    override fun onEnable() {
        Bukkit.getServicesManager()
            .getRegistration(TicketManagerDatabaseRegister::class.java)
            ?.provider
            ?.register1("MYSQL", MySQLBuilder(dataFolder.toPath()).createBuilder())
    }

    override fun onDisable() {
        // Not Needed! TicketManager calls closeDatabase()
    }
}