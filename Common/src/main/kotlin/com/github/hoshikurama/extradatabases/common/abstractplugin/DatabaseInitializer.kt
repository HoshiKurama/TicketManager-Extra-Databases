package com.github.hoshikurama.extradatabases.common.abstractplugin

import com.github.hoshikurama.ticketmanager.api.registry.config.Config
import com.github.hoshikurama.ticketmanager.api.registry.database.AsyncDatabase
import com.github.hoshikurama.ticketmanager.api.registry.database.DatabaseExtension
import com.github.hoshikurama.ticketmanager.api.registry.locale.Locale
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createFile
import kotlin.io.path.notExists

interface ConfigParameters {
    val autoUpdateConfig: Boolean
}

abstract class DatabaseInitializer<TConfig : ConfigParameters>: DatabaseExtension {

    final override suspend fun load(tmDirectory: Path, config: Config, locale: Locale): AsyncDatabase {
        val dataFolder = getDirectoryPath(tmDirectory)

        // Generate Data Folder
        if (dataFolder.notExists())
            dataFolder.toFile().mkdirs()

        // Generate Config File
        val configPath = dataFolder.resolve("config.yml")
        if (configPath.notExists()) {
            pushInfoToConsole("Config file not found. Generating new one.")
            configPath.createFile()
            updateConfig(dataFolder, ::loadInternalConfig) { listOf() }
        }

        // Read Config

        val (playerConfigMap, internalConfigMap) = listOf(
            Files.readAllLines(configPath, Charsets.UTF_8),
            loadInternalConfig()
        ).map { c ->
            c.asSequence()
                .filterNot { it.startsWith("#") }
                .map { it.split(": ", limit = 2) }
                .map { it[0] to StringBuilder(it[1]) }
                .onEach { (_, sb) ->
                    listOf(sb.lastIndex, 0)
                        .filter { sb[it] == '\"' || sb[it] == '\'' }
                        .forEach(sb::deleteCharAt)
                }
                .map { it.first to it.second.toString() }
                .toMap()
        }
        val tConfig = buildConfig(
            playerConfigMap = playerConfigMap,
            internalConfigMap = internalConfigMap,
        )

        // Auto Update Config if requested
        if (tConfig.autoUpdateConfig)
            updateConfig(dataFolder, ::loadInternalConfig) { Files.readAllLines(configPath, Charsets.UTF_8) }

        return buildDB(tConfig, dataFolder)
    }

    protected abstract fun buildDB(config: TConfig, dataFolder: Path): AsyncDatabase
    protected abstract fun getDirectoryPath(tmDirectory: Path): Path
    protected abstract fun pushInfoToConsole(msg: String)
    protected abstract fun buildConfig(
        playerConfigMap: Map<String, String>,
        internalConfigMap: Map<String, String>
    ): TConfig

    private fun loadInternalConfig() = this::class.java.classLoader
        .getResourceAsStream("config.yml")
        ?.let(InputStream::reader)
        ?.let(InputStreamReader::readLines) ?: emptyList()

    private fun updateConfig(
        dataFolder: Path,
        loadInternalConfig: () -> List<String>,
        loadPlayerConfig: () -> List<String>,
    ) {
        val isComment: (String) -> Boolean = { it.startsWith("#") }
        val getKey: (String) -> String = { it.split(":")[0] }

        val externalConfig = loadPlayerConfig() //NOTE: This will not work with future Sponge support
        val externalIdentifiers = externalConfig
            .filterNot(isComment)
            .map(getKey)

        val newValues = loadInternalConfig().map { str ->
            if (!isComment(str) && getKey(str) in externalIdentifiers)
                externalConfig.first { it.startsWith(getKey(str))}
            else str
        }

        // Write Config file
        val writer = dataFolder
            .resolve("config.yml")
            .apply { if (notExists()) createFile() }
            .toFile()
            .bufferedWriter()

        newValues.forEachIndexed { index, str ->
            writer.write(str)

            if (index != newValues.lastIndex)
                writer.newLine()
        }
        writer.close()
    }
}