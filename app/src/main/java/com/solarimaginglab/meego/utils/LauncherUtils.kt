package com.solarimaginglab.meego.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.runtime.mutableStateListOf
import com.solarimaginglab.meego.model.AppInfo

class LauncherUtils(private val context: Context) {
    private val prefs = context.getSharedPreferences("meego_prefs", Context.MODE_PRIVATE)
    private val iconManager = IconPackManager(context)

    companion object {
        val customRecentHistory = mutableStateListOf<AppInfo>()
        val dockApps = mutableStateListOf<AppInfo?>()
    }

    init {
        if (customRecentHistory.isEmpty()) loadHistory()
        refreshDock()
    }

    fun fetchInstalledApps(selectedIconPack: String? = null): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        return pm.queryIntentActivities(intent, PackageManager.MATCH_ALL).map { info ->
            val pkg = info.activityInfo.packageName
            val icon = if (selectedIconPack != null) {
                iconManager.getIconFromPack(pkg, selectedIconPack) ?: info.loadIcon(pm)
            } else info.loadIcon(pm)
            AppInfo(info.loadLabel(pm).toString(), pkg, icon)
        }.filter { it.packageName != context.packageName }.sortedBy { it.name.lowercase() }.distinctBy { it.packageName }
    }

    fun launchApp(app: AppInfo) {
        customRecentHistory.removeAll { it.packageName == app.packageName }
        customRecentHistory.add(0, app)
        if (customRecentHistory.size > 12) customRecentHistory.removeLast()
        saveHistory()
        context.packageManager.getLaunchIntentForPackage(app.packageName)?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(it)
        }
    }

    fun uninstallApp(packageName: String) {
        try {
            val uri = android.net.Uri.fromParts("package", packageName, null)
            val intent = Intent(Intent.ACTION_DELETE, uri)
            // Eliminamos el NEW_TASK si da problemas, o nos aseguramos de que el sistema lo vea como una acción de usuario
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveIconPack(pkg: String?) = prefs.edit().putString("selected_icon_pack", pkg).apply()
    fun getSelectedIconPack(): String? = prefs.getString("selected_icon_pack", null)
    fun saveGridColumns(cols: Int) = prefs.edit().putInt("grid_columns", cols).apply()
    fun getGridColumns(): Int = prefs.getInt("grid_columns", 4)
    fun saveDockSize(size: Int) {
        prefs.edit().putInt("dock_size", size).apply()
        refreshDock()
    }
    fun getDockSize(): Int = prefs.getInt("dock_size", 4)

    fun setDockApp(index: Int, app: AppInfo) {
        if (index in dockApps.indices) { dockApps[index] = app; saveDock() }
    }

    fun removeDockApp(index: Int) {
        if (index in dockApps.indices) { dockApps[index] = null; saveDock() }
    }

    fun refreshDock() {
        val size = getDockSize()
        val pkgs = prefs.getString("dock_pkgs", "") ?: ""
        val savedPkgs = pkgs.split(",").filter { it.isNotEmpty() }
        val pm = context.packageManager
        dockApps.clear()
        repeat(size) { i ->
            val pkg = savedPkgs.getOrNull(i) ?: "null"
            if (pkg == "null") dockApps.add(null)
            else try {
                val ai = pm.getApplicationInfo(pkg, 0)
                dockApps.add(AppInfo(pm.getApplicationLabel(ai).toString(), pkg, pm.getApplicationIcon(ai)))
            } catch (e: Exception) { dockApps.add(null) }
        }
    }

    private fun saveHistory() = prefs.edit().putString("recent_pkgs", customRecentHistory.joinToString(",") { it.packageName }).apply()
    private fun saveDock() = prefs.edit().putString("dock_pkgs", dockApps.joinToString(",") { it?.packageName ?: "null" }).apply()
    private fun loadHistory() {
        val pkgs = prefs.getString("recent_pkgs", "") ?: ""
        val pm = context.packageManager
        pkgs.split(",").filter { it.isNotEmpty() }.forEach { pkg ->
            try {
                val ai = pm.getApplicationInfo(pkg, 0)
                customRecentHistory.add(AppInfo(pm.getApplicationLabel(ai).toString(), pkg, pm.getApplicationIcon(ai)))
            } catch (e: Exception) {}
        }
    }
}