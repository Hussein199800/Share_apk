package com.example.installer

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

enum class RemoveMode { UNINSTALL, UNINSTALL_FOR_USER, DISABLE }

data class RemovedApp(val packageName: String, val label: String, val mode: RemoveMode, val at: Long)

/** System apps this app removed or disabled, so the user can bring them back. */
class RemovedAppsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("removed_apps", Context.MODE_PRIVATE)

    fun list(): List<RemovedApp> = runCatching {
        val arr = JSONArray(prefs.getString(KEY, "[]"))
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            RemovedApp(o.getString("p"), o.optString("l"), RemoveMode.valueOf(o.getString("m")), o.optLong("t"))
        }
    }.getOrDefault(emptyList())

    fun add(app: RemovedApp) = save(list().filterNot { it.packageName == app.packageName } + app)
    fun remove(pkg: String) = save(list().filterNot { it.packageName == pkg })

    private fun save(items: List<RemovedApp>) {
        val arr = JSONArray()
        items.forEach { arr.put(JSONObject().put("p", it.packageName).put("l", it.label).put("m", it.mode.name).put("t", it.at)) }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    companion object { private const val KEY = "items" }
}

object AppRemoval {
    /** Removes or disables through Shizuku. Returns null on success, else the reason. */
    suspend fun remove(pkg: String, mode: RemoveMode): String? {
        if (ShellCommands.isProtected(pkg)) return "protected"
        val cmd = when (mode) {
            RemoveMode.UNINSTALL -> ShellCommands.uninstall(pkg)
            RemoveMode.UNINSTALL_FOR_USER -> ShellCommands.uninstallForUser(pkg)
            RemoveMode.DISABLE -> ShellCommands.disable(pkg)
        }
        val r = ShizukuBridge.run(*cmd)
        return if (r.ok) null else r.reason
    }

    /** Undoes [RemoveMode.UNINSTALL_FOR_USER] or [RemoveMode.DISABLE]. */
    suspend fun restore(app: RemovedApp): String? {
        val r = ShizukuBridge.run(*if (app.mode == RemoveMode.DISABLE) ShellCommands.enable(app.packageName) else ShellCommands.restoreForUser(app.packageName))
        return if (r.ok) null else r.reason
    }
}
