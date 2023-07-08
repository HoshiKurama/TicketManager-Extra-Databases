package com.github.hoshikurama.extradatabases.h2

import com.github.hoshikurama.ticketmanager.api.paper.TicketManagerDatabaseRegister
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

@Suppress("UNUSED")
class BukkitPlugin : JavaPlugin() {
    private var closeDBIfOpen: (() -> Unit)? = null

    override fun onEnable() {
        closeDBIfOpen = Bukkit.getServicesManager()
            .getRegistration(TicketManagerDatabaseRegister::class.java)
            ?.provider
            ?.register2("H2", H2Builder(dataFolder.toPath()).createBuilder())
    }

    override fun onDisable() {
        closeDBIfOpen?.invoke()
    }
}