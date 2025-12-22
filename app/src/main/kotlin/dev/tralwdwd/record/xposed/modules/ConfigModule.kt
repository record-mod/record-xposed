package dev.tralwdwd.record.xposed.modules

import de.robv.android.xposed.callbacks.XC_LoadPackage
import dev.tralwdwd.record.xposed.Constants
import dev.tralwdwd.record.xposed.Module
import dev.tralwdwd.record.xposed.Utils.Companion.JSON
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.*
import java.io.File
import java.nio.file.Paths

object ConfigModule : Module() {

    private const val CONFIG_DIR = "config"
    private lateinit var configFile: File
    private val currentConfig: MutableMap<String, JsonElement> = mutableMapOf()

    override fun onLoad(packageParam: XC_LoadPackage.LoadPackageParam) {
        val dir = Paths.get(packageParam.appInfo.dataDir, Constants.FILES_DIR, CONFIG_DIR)
            .toFile()
            .apply { mkdirs() }

        configFile = File(dir, "record-config.json")
        load()
    }

    private fun load() {
        if (!configFile.exists()) return
        runCatching {
            val json = configFile.readText()
            val map = JSON.decodeFromString(
                MapSerializer(String.serializer(), JsonElement.serializer()),
                json
            )
            currentConfig.clear()
            currentConfig.putAll(map)
        }
    }

    private fun save() {
        val json = JSON.encodeToString(
            MapSerializer(String.serializer(), JsonElement.serializer()),
            currentConfig
        )
        configFile.writeText(json)
    }

    fun getString(key: String, default: String? = null): String? =
        currentConfig[key]?.jsonPrimitive?.contentOrNull ?: default

    fun getBoolean(key: String, default: Boolean = false): Boolean =
        currentConfig[key]?.jsonPrimitive?.booleanOrNull ?: default

    fun getInt(key: String, default: Int = 0): Int =
        currentConfig[key]?.jsonPrimitive?.intOrNull ?: default

    fun getLong(key: String, default: Long = 0L): Long =
        currentConfig[key]?.jsonPrimitive?.longOrNull ?: default

    fun getDouble(key: String, default: Double = 0.0): Double =
        currentConfig[key]?.jsonPrimitive?.doubleOrNull ?: default

    fun getJsonElement(key: String, default: JsonElement = JsonNull): JsonElement =
        currentConfig[key] ?: default

    fun getOrNull(key: String): JsonElement? = currentConfig[key]

    fun set(key: String, value: Any?) {
        currentConfig[key] = anyToJson(value)
        save()
    }

    fun remove(key: String) {
        currentConfig.remove(key)
        save()
    }

    fun getDeep(path: String): JsonElement? {
        val keys = path.split(".")
        var current: JsonElement = JsonObject(currentConfig)

        for (key in keys) {
            current = current.jsonObject[key] ?: return null
        }
        return current
    }

    fun anyToJson(value: Any?): JsonElement = when (value) {
        null -> JsonNull
        is JsonElement -> value
        is String -> JsonPrimitive(value)
        is Boolean -> JsonPrimitive(value)
        is Int -> JsonPrimitive(value)
        is Long -> JsonPrimitive(value)
        is Float -> JsonPrimitive(value)
        is Double -> JsonPrimitive(value)
        is Map<*, *> -> JsonObject(
            value.mapNotNull { (k, v) -> (k as? String)?.let { it to anyToJson(v) } }.toMap()
        )
        is Collection<*> -> JsonArray(value.map { anyToJson(it) })
        else -> JsonPrimitive(value.toString())
    }
}
