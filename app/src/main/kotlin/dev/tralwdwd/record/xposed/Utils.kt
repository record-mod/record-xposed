package dev.tralwdwd.record.xposed

import android.app.AlertDialog
import android.app.AndroidAppHelper
import android.content.Context
import android.content.Intent
import dev.tralwdwd.record.xposed.modules.UpdaterModule
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.*
import java.io.File
import kotlin.system.exitProcess

class Utils {
    companion object {
        val JSON = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            explicitNulls = true
        }

        fun reloadApp() {
            val application = AndroidAppHelper.currentApplication()
            val intent = application.packageManager.getLaunchIntentForPackage(application.packageName)
            application.startActivity(Intent.makeRestartActivityTask(intent!!.component))
            exitProcess(0)
        }

        fun showRecoveryAlert(context: Context) {
            AlertDialog.Builder(context).setTitle("ReCord Recovery Options")
                .setItems(arrayOf("Reload", "Delete Script", "Reset Loader Config")) { _, which ->
                    when (which) {
                        0 -> {
                            reloadApp()
                        }

                        1 -> {
                            val bundleFile = File(
                                context.dataDir, "${Constants.CACHE_DIR}/${Constants.MAIN_SCRIPT_FILE}"
                            )

                            if (bundleFile.exists()) bundleFile.delete()

                            reloadApp()
                        }

                        2 -> {
                            UpdaterModule.resetLoaderConfig()
                            reloadApp()
                        }
                    }
                }.show()
        }

        fun mergeJson(base: JsonObject, override: JsonObject): JsonObject {
            val merged = base.toMutableMap()

            for ((key, value) in override) {
                val baseValue = base[key]
                merged[key] = if (baseValue is JsonObject && value is JsonObject) {
                    mergeJson(baseValue, value)
                } else {
                    value
                }
            }

            return JsonObject(merged)
        }

        fun <T> mergeWithOverrides(
            defaults: T,
            overrides: T,
            serializer: KSerializer<T>
        ): T {
            val mergedJson = mergeJson(
                JSON.encodeToJsonElement(serializer, defaults).jsonObject,
                JSON.encodeToJsonElement(serializer, overrides).jsonObject
            )

            return JSON.decodeFromJsonElement(serializer, mergedJson)
        }

        fun <T> dataClassToMap(
            data: T,
            serializer: KSerializer<T>
        ): Map<String, Any?> {
            val json = JSON.encodeToJsonElement(serializer, data)

            val map = JSON.decodeFromJsonElement(
                MapSerializer(
                    String.serializer(),
                    JsonElement.serializer()
                ),
                json)
                .toMap()

            return map.mapValues { entry ->
                val e = entry.value
                when {
                    e is JsonPrimitive && e.booleanOrNull != null -> e.boolean
                    e is JsonPrimitive && e.intOrNull != null -> e.int
                    e is JsonPrimitive && e.contentOrNull != null -> e.content
                    else -> null
                } as Any?
            }
        }
    }

    object Log {
        fun e(msg: String) = android.util.Log.e(Constants.LOG_TAG, msg)
        fun e(msg: String, throwable: Throwable) = android.util.Log.e(Constants.LOG_TAG, msg, throwable)
        fun i(msg: String) = android.util.Log.i(Constants.LOG_TAG, msg)
        fun i(msg: String, throwable: Throwable) = android.util.Log.i(Constants.LOG_TAG, msg, throwable)
        fun w(msg: String) = android.util.Log.w(Constants.LOG_TAG, msg)
        fun w(msg: String, throwable: Throwable) = android.util.Log.w(Constants.LOG_TAG, msg, throwable)
    }
}