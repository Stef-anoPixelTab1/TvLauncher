package com.mytv.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo

data class AppEntry(val pkg: String, val label: String, val info: ActivityInfo, val category: String) {
    fun launchIntent(): Intent = Intent(Intent.ACTION_MAIN)
        .addCategory(category)
        .setClassName(pkg, info.name)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
}

object Apps {
    /** App "utili": compaiono per prime e sono quelle mantenute da "Solo app principali". */
    private val priority = listOf(
        "netflix", "youtube", "disney", "hbo", "wbd", "primevideo", "amazon.avod",
        "raiplay", "rai.", "mediaset", "infinity", "dazn", "spotify", "sky", "discovery",
        "apple.atve", "plex", "kodi", "tvnow", "pluto", "twitch", "chrome", "settings"
    )

    fun rank(pkg: String): Int {
        val p = pkg.lowercase()
        val i = priority.indexOfFirst { p.contains(it) }
        return if (i < 0) 1000 else i
    }

    fun isPriority(pkg: String) = rank(pkg) < 1000

    fun loadAll(ctx: Context): List<AppEntry> {
        val pm = ctx.packageManager
        val seen = LinkedHashMap<String, AppEntry>()
        for (cat in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            val q = Intent(Intent.ACTION_MAIN).addCategory(cat)
            for (ri in pm.queryIntentActivities(q, 0)) {
                val pkg = ri.activityInfo.packageName
                if (pkg == ctx.packageName || seen.containsKey(pkg)) continue
                seen[pkg] = AppEntry(pkg, ri.loadLabel(pm).toString(), ri.activityInfo, cat)
            }
        }
        return seen.values.sortedWith(compareBy({ rank(it.pkg) }, { it.label.lowercase() }))
    }
}
