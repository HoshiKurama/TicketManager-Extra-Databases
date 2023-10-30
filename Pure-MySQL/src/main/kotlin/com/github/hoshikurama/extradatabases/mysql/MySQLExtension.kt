package com.github.hoshikurama.extradatabases.mysql

import com.github.hoshikurama.extradatabases.common.abstractplugin.ConfigParameters
import com.github.hoshikurama.extradatabases.common.abstractplugin.DatabaseInitializer
import com.github.hoshikurama.ticketmanager.api.registry.database.AsyncDatabase
import org.bukkit.Bukkit
import java.nio.file.Path
import java.util.logging.Level

class MySQLExtension : DatabaseInitializer<MySQLConfigParameters>() {
    override fun buildDB(config: MySQLConfigParameters, dataFolder: Path): AsyncDatabase {
        return MySQL(config.host, config.port, config.dbName, config.username, config.password)
    }

    override fun getDirectoryPath(tmAddonsPath: Path): Path {
        return tmAddonsPath.resolve("ExtraDatabases").resolve("MySQL")
    }

    override fun pushInfoToConsole(msg: String) {
        Bukkit.getLogger().log(Level.INFO, msg)
    }

    override fun buildConfig(
        playerConfigMap: Map<String, String>,
        internalConfigMap: Map<String, String>
    ): MySQLConfigParameters {
        return MySQLConfigParameters(
            autoUpdateConfig = playerConfigMap["Auto_Update_Config"]?.toBooleanStrictOrNull()
                ?: internalConfigMap["Auto_Update_Config"]!!.toBooleanStrict(),
            port = playerConfigMap["MySQL_Port"]!!,
            host = playerConfigMap["MySQL_Host"]!!,
            dbName = playerConfigMap["MySQL_DBName"]!!,
            username = playerConfigMap["MySQL_Username"]!!,
            password = playerConfigMap["MySQL_Password"]!!,
        )
    }
}

class MySQLConfigParameters(
    override val autoUpdateConfig: Boolean,
    val port: String,
    val host: String,
    val dbName: String,
    val username: String,
    val password: String,
) : ConfigParameters