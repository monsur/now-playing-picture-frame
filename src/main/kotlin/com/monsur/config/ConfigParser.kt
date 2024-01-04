package com.monsur.config

import io.ktor.util.logging.*
import kotlinx.serialization.json.Json
import java.io.File

internal val LOGGER = KtorSimpleLogger("com.monsur.config.ConfigParser")

class ConfigParser {
    private val configFile = "config.json"

    fun read(): Config {
        val file = File(configFile)
        val bodyText = file.readText()
        val bodyJson = Json.decodeFromString<Config>(bodyText)
        LOGGER.info("Loaded config from $configFile")
        return bodyJson
    }
}