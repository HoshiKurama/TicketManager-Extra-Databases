package com.github.hoshikurama.extradatabases.h2

import com.github.hoshikurama.extradatabases.common.abstractplugin.ConfigParameters
import com.github.hoshikurama.extradatabases.common.abstractplugin.DatabaseInitializer
import com.github.hoshikurama.ticketmanager.api.common.database.CompletableFutureAsyncDatabase
import org.bukkit.Bukkit
import java.nio.file.Path
import java.util.logging.Level
import kotlin.io.path.absolutePathString

class H2Builder(dataFolder: Path) : DatabaseInitializer<H2ConfigParameters, CompletableFutureAsyncDatabase>
    (dataFolder, BukkitPlugin::class.java.classLoader) {

    override fun pushInfoToConsole(msg: String) {
        Bukkit.getLogger().log(Level.INFO, msg)
    }

    override fun buildConfig(
        playerConfigMap: Map<String, String>,
        internalConfigMap: Map<String, String>
    ): H2ConfigParameters {
        return H2ConfigParameters(
            autoUpdateConfig = playerConfigMap["Auto_Update_Config"]?.toBooleanStrictOrNull()
                ?: internalConfigMap["Auto_Update_Config"]!!.toBooleanStrict(),
            maxConnections = playerConfigMap["Max_Connections"]?.toIntOrNull()
                ?: internalConfigMap["Max_Connections"]!!.toInt()
        )
    }

    override fun buildDBFunction(config: H2ConfigParameters): CompletableFutureAsyncDatabase {
        return H2(dataFolder.absolutePathString(), config.maxConnections)
    }
}

class H2ConfigParameters(
    override val autoUpdateConfig: Boolean,
    val maxConnections: Int,
) : ConfigParameters