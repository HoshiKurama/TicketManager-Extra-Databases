package com.github.hoshikurama.extradatabases.common.abstractplugin

import java.io.InputStream
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createFile
import kotlin.io.path.notExists

interface ConfigParameters {
    val autoUpdateConfig: Boolean
}

abstract class DatabaseInitializer<Config : ConfigParameters, DBInterface>(
    protected val dataFolder: Path,
    private val classLoader4Config: ClassLoader
) {

    protected abstract fun pushInfoToConsole(msg: String)
    protected abstract fun buildConfig(
        playerConfigMap: Map<String, String>,
        internalConfigMap: Map<String, String>
    ): Config
    protected abstract fun buildDBFunction(config: Config): DBInterface

    fun createBuilder(): () -> DBInterface {

        // Generate Data Folder
        if (dataFolder.notExists())
            dataFolder.toFile().mkdirs()

        // Generate Config File
        val configPath = dataFolder.resolve("config.yml")
        if (configPath.notExists()) {
            pushInfoToConsole("Config file not found. Generating new one.")
            configPath.createFile()
            updateConfig(::loadInternalConfig) { listOf() }
        }

        // Read Config

        val (playerConfigMap, internalConfigMap) = listOf(
            Files.readAllLines(configPath, Charsets.UTF_8),
            loadInternalConfig(),
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
        val config = buildConfig(
            playerConfigMap = playerConfigMap,
            internalConfigMap = internalConfigMap,
        )

        // Auto Update Config if requested
        if (config.autoUpdateConfig)
            updateConfig(::loadInternalConfig) { Files.readAllLines(configPath, Charsets.UTF_8) }

        return { buildDBFunction(config) }
    }

    private fun loadInternalConfig() = classLoader4Config
        .getResourceAsStream("config.yml")
        ?.let(InputStream::reader)
        ?.let(InputStreamReader::readLines) ?: emptyList()

    private fun updateConfig(
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