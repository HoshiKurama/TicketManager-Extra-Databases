package com.github.hoshikurama.extradatabases.mysql

import com.github.hoshikurama.ticketmanager.api.paper.TicketManagerDatabaseRegister
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class BukkitPlugin : JavaPlugin() {
    private var closeDBIfOpen: (() -> Unit)? = null

    override fun onEnable() {
        val directory = dataFolder.toPath()
            .resolveSibling("TicketManager")
            .resolve("addons")
            .resolve("ExtraDatabases")
            .resolve("MySQL")

        closeDBIfOpen = Bukkit.getServicesManager()
            .getRegistration(TicketManagerDatabaseRegister::class.java)
            ?.provider
            ?.register1("MYSQL", MySQLBuilder(directory).createBuilder())
    }

    override fun onDisable() {
        closeDBIfOpen?.invoke()
    }
}