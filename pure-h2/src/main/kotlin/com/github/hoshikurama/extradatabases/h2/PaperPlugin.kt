package com.github.hoshikurama.extradatabases.h2

import com.github.hoshikurama.ticketmanager.api.paper.TicketManagerDatabaseRegister
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import kotlin.io.path.absolutePathString

@Suppress("UNUSED")
class PaperPlugin : JavaPlugin() {

    override fun onEnable() {
        Bukkit.getServicesManager()
            .getRegistration(TicketManagerDatabaseRegister::class.java)
            ?.provider
            ?.register("H2") {
                H2(dataFolder.toPath().absolutePathString(), 3)
            }
    }

    override fun onDisable() {

    }
}