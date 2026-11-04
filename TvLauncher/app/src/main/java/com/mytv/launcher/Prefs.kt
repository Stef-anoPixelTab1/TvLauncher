package com.mytv.launcher

import android.content.Context

class Prefs(ctx: Context) {
    private val sp = ctx.applicationContext.getSharedPreferences("launcher", Context.MODE_PRIVATE)

    /** Pacchetti nascosti dalla home. */
    var hidden: Set<String>
        get() = sp.getStringSet("hidden", emptySet())!!.toSet()
        set(v) { sp.edit().putStringSet("hidden", v.toSet()).apply() }

    /** "preset" (gradiente integrato) oppure "file" (immagine scaricata/scelta). */
    var wallpaperMode: String
        get() = sp.getString("wp_mode", "preset") ?: "preset"
        set(v) { sp.edit().putString("wp_mode", v).apply() }

    var wallpaperPreset: Int
        get() = sp.getInt("wp_preset", 0)
        set(v) { sp.edit().putInt("wp_preset", v).apply() }
}
