package com.mytv.launcher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

object Wallpapers {
    val presetNames = listOf(
        "Turchese notte", "Viola profondo", "Tramonto", "Verde bosco", "Blu oceano", "Nero"
    )

    fun fileOf(ctx: Context) = File(ctx.filesDir, "wallpaper.img")

    private fun linear(o: GradientDrawable.Orientation, vararg hex: String) =
        GradientDrawable(o, hex.map { Color.parseColor(it) }.toIntArray())

    fun preset(ctx: Context, i: Int): Drawable {
        val w = ctx.resources.displayMetrics.widthPixels.toFloat()
        return when (i) {
            0 -> GradientDrawable().apply {
                gradientType = GradientDrawable.RADIAL_GRADIENT
                setGradientCenter(0.85f, 0.05f)
                gradientRadius = w * 0.9f
                colors = intArrayOf(Color.parseColor("#1BA7AA"), Color.parseColor("#0B4350"), Color.parseColor("#060D14"))
            }
            1 -> linear(GradientDrawable.Orientation.TL_BR, "#0D0A1F", "#3A1D5E", "#6B2F8F")
            2 -> linear(GradientDrawable.Orientation.TL_BR, "#1A0B1F", "#8A2E4A", "#E58A3A")
            3 -> linear(GradientDrawable.Orientation.TL_BR, "#04110C", "#0B4A33", "#2E8B57")
            4 -> linear(GradientDrawable.Orientation.TL_BR, "#030A1A", "#0A2A66", "#1E6FD9")
            else -> linear(GradientDrawable.Orientation.TL_BR, "#000000", "#050505", "#0B0B0B")
        }
    }

    fun load(ctx: Context, prefs: Prefs): Drawable {
        if (prefs.wallpaperMode == "file") {
            val f = fileOf(ctx)
            if (f.exists()) {
                decodeSampled(f)?.let { return BitmapDrawable(ctx.resources, it) }
            }
        }
        return preset(ctx, prefs.wallpaperPreset)
    }

    private fun decodeSampled(f: File, maxW: Int = 1920, maxH: Int = 1080): Bitmap? {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.path, o)
        if (o.outWidth <= 0) return null
        var s = 1
        while (o.outWidth / (s * 2) >= maxW && o.outHeight / (s * 2) >= maxH) s *= 2
        val o2 = BitmapFactory.Options().apply { inSampleSize = s; inPreferredConfig = Bitmap.Config.RGB_565 }
        return BitmapFactory.decodeFile(f.path, o2)
    }

    /** Salva uno stream come sfondo, verificando che sia un'immagine valida. */
    fun save(ctx: Context, input: InputStream): Boolean {
        val tmp = File(ctx.filesDir, "wp.tmp")
        tmp.outputStream().use { input.copyTo(it) }
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(tmp.path, o)
        if (o.outWidth <= 0) { tmp.delete(); return false }
        val dest = fileOf(ctx)
        dest.delete()
        return tmp.renameTo(dest)
    }

    /** Da chiamare su un thread in background. */
    fun download(ctx: Context, url: String): Boolean {
        var c: HttpURLConnection? = null
        return try {
            c = URL(url).openConnection() as HttpURLConnection
            c.connectTimeout = 15000
            c.readTimeout = 30000
            c.instanceFollowRedirects = true
            c.setRequestProperty("User-Agent", "Mozilla/5.0 TvLauncher")
            c.inputStream.use { save(ctx, it) }
        } catch (e: Exception) {
            false
        } finally {
            c?.disconnect()
        }
    }
}
