package com.github.hoshikurama.extradatabases.mysql

import com.github.hoshikurama.extradatabases.common.abstractplugin.ConfigParameters
import com.github.hoshikurama.extradatabases.common.abstractplugin.DatabaseInitializer
import com.github.hoshikurama.ticketmanager.api.common.database.AsyncDatabase
import org.bukkit.Bukkit
import java.nio.file.Path
import java.util.logging.Level

class MySQLBuilder(dataFolder: Path) : DatabaseInitializer<MySQLConfigParameters, AsyncDatabase>
    (dataFolder, BukkitPlugin::class.java.classLoader) {

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
            port = playerConfigMap["MySQL_Port"] ?: internalConfigMap["MySQL_Port"]!!,
            host = playerConfigMap["MySQL_Host"] ?: internalConfigMap["MySQL_Host"]!!,
            dbName = playerConfigMap["MySQL_DBName"] ?: internalConfigMap["MySQL_DBName"]!!,
            username = playerConfigMap["MySQL_Username"] ?: internalConfigMap["MySQL_Username"]!!,
            password = playerConfigMap["MySQL_Password"] ?: internalConfigMap["MySQL_Password"]!!,
        )
    }

    override fun buildDBFunction(config: MySQLConfigParameters): AsyncDatabase {
        return MySQL(config.host, config.port, config.dbName, config.username, config.password)
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