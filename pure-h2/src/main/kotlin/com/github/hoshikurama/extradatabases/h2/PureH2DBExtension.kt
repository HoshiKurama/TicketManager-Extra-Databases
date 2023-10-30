package com.github.hoshikurama.extradatabases.h2

import com.github.hoshikurama.extradatabases.common.abstractplugin.ConfigParameters
import com.github.hoshikurama.extradatabases.common.abstractplugin.DatabaseInitializer
import com.github.hoshikurama.ticketmanager.api.registry.database.AsyncDatabase
import org.bukkit.Bukkit
import java.nio.file.Path
import java.util.logging.Level
import kotlin.io.path.absolutePathString

class PureH2DBExtension : DatabaseInitializer<H2ConfigParameters>() {

    override fun pushInfoToConsole(msg: String) {
        Bukkit.getLogger().log(Level.INFO, msg)
    }

    override fun buildDB(config: H2ConfigParameters, dataFolder: Path): AsyncDatabase {
        return H2(dataFolder.absolutePathString(), config.maxConnections)
    }

    override fun getDirectoryPath(tmAddonsPath: Path): Path {
        return tmAddonsPath.resolve("ExtraDatabases").resolve("H2")
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
}

class H2ConfigParameters(
    override val autoUpdateConfig: Boolean,
    val maxConnections: Int,
) : ConfigParameters