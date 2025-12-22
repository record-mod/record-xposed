package dev.tralwdwd.record.xposed.modules.bridge

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import dev.tralwdwd.record.xposed.Module
import dev.tralwdwd.record.xposed.Utils
import dev.tralwdwd.record.xposed.modules.ConfigModule
import org.json.JSONObject
import java.io.File
import androidx.core.content.edit

object AdditionalBridgeMethodsModule : Module() {
    // TODO: Separate into different modules
    override fun onContext(context: Context) = with(context) {
        BridgeModule.registerMethod("record.fs.getConstants") {
            mapOf(
                "data" to dataDir.absolutePath,
                "files" to filesDir.absolutePath,
                "cache" to cacheDir.absolutePath,
            )
        }

        BridgeModule.registerMethod("record.fs.delete") {
            val (path) = it
            File(path as String).run {
                if (this.isDirectory) this.deleteRecursively()
                else this.delete()
            }
        }

        BridgeModule.registerMethod("record.fs.exists") {
            val (path) = it
            File(path as String).exists()
        }

        BridgeModule.registerMethod("record.fs.read") { it ->
            val (path) = it
            val file = File(path as String).apply { openFileGuarded() }

            file.bufferedReader().use { it.readText() }
        }

        BridgeModule.registerMethod("record.fs.write") {
            val (path, contents) = it
            val file = File(path as String).apply { openFileGuarded() }

            file.writeText(contents as String)
        }

        BridgeModule.registerMethod("record.kvStore.set") {
            val (namespace, key) = it
            val value: Any? = it[2] as? Any

            val prefs = context.getSharedPreferences(
                "record_kv",
                Context.MODE_PRIVATE
            )

            val fullKey = "$namespace.$key"

            prefs.edit {
                when (value) {
                    null -> remove(fullKey)

                    is String -> putString(fullKey, value)
                    is Boolean -> putBoolean(fullKey, value)

                    is Int -> putInt(fullKey, value)
                    is Long -> putLong(fullKey, value)
                    is Double -> putFloat(fullKey, value.toFloat())

                    is Map<*, *>, is List<*> -> {
                        val json = JSONObject(value as Map<*, *>).toString()
                        putString(fullKey, json)
                    }

                    else -> throw IllegalArgumentException(
                        "Unsupported KV type: ${value::class.java}"
                    )
                }

            }
        }

        BridgeModule.registerMethod("record.kvStore.get") {
            val (namespace, key, default) = it

            val prefs = context.getSharedPreferences(
                "record_kv",
                Context.MODE_PRIVATE
            )
            val fullKey = "$namespace.$key"

            if(!prefs.contains(fullKey)) return@registerMethod default

            return@registerMethod when (default) {
                is Boolean -> prefs.getBoolean(fullKey, default)
                is Int -> prefs.getInt(fullKey, default)
                is Long -> prefs.getLong(fullKey, default)
                is Double -> prefs.getFloat(fullKey, default.toFloat()).toDouble()
                // Whatever just parse JSON in JS
                else -> prefs.getString(fullKey, null)
            }
        }
    }

    override fun onActivity(activity: Activity) = with(activity) {
        BridgeModule.registerMethod("record.alertError") {
            val (error, version) = it
            val app = getAppInfo()
            val errorString = "$error"

            val clipboard = applicationContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Stack Trace", errorString)

            AlertDialog.Builder(this)
                .setTitle("ReCord Error")
                .setMessage(
                    """
                    ReCord: $version
                    ${app.name}: ${app.version} (${app.versionCode})
                    Device: ${Build.MANUFACTURER} ${Build.MODEL}
                    
                    
                """.trimIndent() + errorString
                )
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                }
                .setNeutralButton("Copy") { dialog, _ ->
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(applicationContext, "Copied stack trace", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .show()

            null
        }

        BridgeModule.registerMethod("record.showRecoveryAlert") {
            Utils.showRecoveryAlert(this)
        }
    }

    private fun File.openFileGuarded() {
        if (!this.exists()) throw Error("Path does not exist: $path")
        if (!this.isFile) throw Error("Path is not a file: $path")
    }
}