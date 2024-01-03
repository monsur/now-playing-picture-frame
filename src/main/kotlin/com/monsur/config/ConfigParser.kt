package com.monsur.config

import kotlinx.serialization.json.Json
import java.io.File

class ConfigParser {
    private val configPath = "src/main/resources/"
    private val configFile = "config.json"

    fun read(): Config {
        val file = File("$configPath$configFile")
        val bodyText = file.readText()
        return Json.decodeFromString<Config>(bodyText)
    }
}