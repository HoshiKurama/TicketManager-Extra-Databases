package com.github.hoshikurama.extradatabases.h2

import com.github.hoshikurama.ticketmanager.api.paper.TicketManagerDatabaseRegister
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

@Suppress("UNUSED")
class BukkitPlugin : JavaPlugin() {

    override fun onEnable() {
        Bukkit.getServicesManager()
            .getRegistration(TicketManagerDatabaseRegister::class.java)
            ?.provider
            ?.register2("H2", H2Builder(dataFolder.toPath()).createBuilder())
    }

    override fun onDisable() {
        // Not needed! TicketManager calls closeDatabase()
    }
}