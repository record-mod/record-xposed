package dev.tralwdwd.record.xposed.modules

import de.robv.android.xposed.callbacks.XC_LoadPackage
import dev.tralwdwd.record.xposed.Module
import dev.tralwdwd.record.xposed.Utils.Companion.JSON
import dev.tralwdwd.record.xposed.modules.bridge.BridgeModule
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
data class LoaderConfig(
    val isCustomBundle: Boolean? = false,
    val customBundleUrl: String? = null
) {
    companion object {
        fun fromMap(map: Map<String, Any>): LoaderConfig {
            val jsonElement = mapToJsonElement(map)

            return JSON.decodeFromJsonElement(
                serializer(),
                jsonElement
            )
        }

        private fun mapToJsonElement(map: Map<String, Any?>): JsonElement {
            val content = map.mapValues { (_, value) ->
                @Suppress("UNCHECKED_CAST")
                when (value) {
                    null -> JsonNull
                    is String -> JsonPrimitive(value)
                    is Boolean -> JsonPrimitive(value)
                    is Number -> JsonPrimitive(value)
                    is Map<*, *> -> mapToJsonElement(value as Map<String, Any?>)
                    is Collection<*> -> JsonArray(value.map { item ->
                        when (item) {
                            is Map<*, *> -> mapToJsonElement(item as Map<String, Any?>)
                            is String -> JsonPrimitive(item)
                            is Number -> JsonPrimitive(item)
                            is Boolean -> JsonPrimitive(item)
                            null -> JsonNull
                            else -> JsonPrimitive(item.toString())
                        }
                    })
                    else -> JsonPrimitive(value.toString())
                }
            }

            return JsonObject(content)
        }

    }
}

fun LoaderConfig.toMap(): Map<String, Any?> {
    val jsonElement = JSON.encodeToJsonElement(LoaderConfig.serializer(), this)

    @Suppress("UNCHECKED_CAST")
    return jsonElementToMap(jsonElement) as? Map<String, Any?> ?: emptyMap()
}

private fun jsonElementToMap(element: JsonElement): Any? = when (element) {
    is JsonObject -> element.mapValues { jsonElementToMap(it.value) }
    is JsonArray -> element.map { jsonElementToMap(it) }
    is JsonPrimitive -> when {
        element.booleanOrNull != null -> element.boolean
        element.intOrNull != null -> element.int
        element.longOrNull != null -> element.long
        element.floatOrNull != null -> element.float
        element.doubleOrNull != null -> element.double
        element.contentOrNull != null -> element.content
        else -> null
    }
}

object LoaderConfigModule : Module() {
    private var cachedConfig: LoaderConfig? = null

    override fun onLoad(packageParam: XC_LoadPackage.LoadPackageParam) {
        cachedConfig = loadConfig()

        BridgeModule.registerMethod("record.loader.getConfig") {
            loaderConfig.toMap()
        }

        BridgeModule.registerMethod("record.loader.configureLoader") {
            val (rawConfig) = it

            @Suppress("UNCHECKED_CAST")
            val config = LoaderConfig.fromMap(rawConfig as Map<String, Any>)
            configureLoader(config)
        }

        BridgeModule.registerMethod("record.loader.resetConfig") {
            resetConfig()
        }
    }

    fun loadConfig(): LoaderConfig {
        cachedConfig?.let { return it }

        val loaderJson = ConfigModule.getDeep("loader")

        val config = loaderJson?.let {
            runCatching {
                JSON.decodeFromJsonElement(
                    LoaderConfig.serializer(),
                    it
                )
            }.getOrNull()
        } ?: defaultConfig()

        cachedConfig = config

        return config
    }

    fun saveConfig(config: LoaderConfig) {
        cachedConfig = config
        ConfigModule.set("loader", JSON.encodeToJsonElement(LoaderConfig.serializer(), config))
    }

    fun resetConfig() {
        saveConfig(defaultConfig())
    }

    fun configureLoader(config: LoaderConfig) {
        saveConfig(config)
    }

    val loaderConfig: LoaderConfig
        get() = cachedConfig ?: loadConfig()

    private fun defaultConfig() = LoaderConfig(
        isCustomBundle = false,
        customBundleUrl = null
    )
}