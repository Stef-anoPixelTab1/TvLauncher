package com.mytv.launcher

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs

    private val pickImage = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        Thread {
            val ok = try {
                contentResolver.openInputStream(uri)?.use { Wallpapers.save(this, it) } ?: false
            } catch (e: Exception) { false }
            runOnUiThread {
                if (ok) { prefs.wallpaperMode = "file"; toast("Sfondo impostato") }
                else toast("Immagine non valida")
            }
        }.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(64), dp(32), dp(64), dp(32))
        }
        col.addView(TextView(this).apply {
            text = "Impostazioni launcher"
            textSize = 28f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, dp(20))
        })

        col.addView(row("Sfondi predefiniti", "Scegli uno degli sfondi integrati") { choosePreset() })
        col.addView(row("Sfondo casuale da internet", "Scarica una nuova foto ogni volta") {
            download("https://picsum.photos/seed/${System.currentTimeMillis()}/1920/1080")
        })
        col.addView(row("Sfondo da link (URL)", "Incolla l'indirizzo di un'immagine") { askUrl() })
        col.addView(row("Sfondo dal dispositivo", "Scegli un'immagine dalla memoria / USB") {
            try { pickImage.launch(arrayOf("image/*")) }
            catch (e: ActivityNotFoundException) { toast("Nessun gestore file: usa l'opzione URL") }
        })
        col.addView(row("Gestisci app visibili", "Scegli quali app mostrare nella home") { manageApps() })
        col.addView(row("Solo app principali", "Nascondi tutto tranne streaming e app note") { onlyMain() })
        col.addView(row("Mostra tutte le app", "Annulla ogni app nascosta") {
            prefs.hidden = emptySet(); toast("Tutte le app sono visibili")
        })
        col.addView(row("Launcher predefinito", "Apri le impostazioni Home della TV") { defaultLauncher() })

        setContentView(ScrollView(this).apply { addView(col) })
    }

    private fun row(title: String, sub: String, action: () -> Unit): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(14), dp(24), dp(14))
            background = ContextCompat.getDrawable(this@SettingsActivity, R.drawable.btn_bg)
            isFocusable = true
            isClickable = true
            setOnClickListener { action() }
            addView(TextView(context).apply { text = title; textSize = 18f; setTextColor(Color.WHITE) })
            addView(TextView(context).apply { text = sub; textSize = 13f; setTextColor(Color.parseColor("#B3FFFFFF")) })
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { bottomMargin = dp(10) }
        }

    private fun choosePreset() {
        AlertDialog.Builder(this).setTitle("Sfondi predefiniti")
            .setItems(Wallpapers.presetNames.toTypedArray()) { _, i ->
                prefs.wallpaperMode = "preset"; prefs.wallpaperPreset = i; toast("Sfondo impostato")
            }.show()
    }

    private fun askUrl() {
        val et = EditText(this).apply {
            hint = "https://esempio.com/sfondo.jpg"
            inputType = InputType.TYPE_TEXT_VARIATION_URI
            setTextColor(Color.WHITE)
        }
        AlertDialog.Builder(this).setTitle("URL dell'immagine").setView(et)
            .setPositiveButton("Scarica") { _, _ ->
                val u = et.text.toString().trim()
                if (u.startsWith("http")) download(u) else toast("Indirizzo non valido")
            }.setNegativeButton("Annulla", null).show()
    }

    private fun download(url: String) {
        toast("Scarico lo sfondo…")
        Thread {
            val ok = Wallpapers.download(this, url)
            runOnUiThread {
                if (ok) { prefs.wallpaperMode = "file"; toast("Sfondo impostato") }
                else toast("Download non riuscito")
            }
        }.start()
    }

    private fun manageApps() {
        val all = Apps.loadAll(this)
        val hidden = prefs.hidden
        val checked = BooleanArray(all.size) { all[it].pkg !in hidden }
        AlertDialog.Builder(this).setTitle("App visibili nella home")
            .setMultiChoiceItems(all.map { it.label }.toTypedArray(), checked) { _, i, c -> checked[i] = c }
            .setPositiveButton("Salva") { _, _ ->
                prefs.hidden = all.filterIndexed { i, _ -> !checked[i] }.map { it.pkg }.toSet()
                toast("Salvato")
            }.setNegativeButton("Annulla", null).show()
    }

    private fun onlyMain() {
        prefs.hidden = Apps.loadAll(this).filter { !Apps.isPriority(it.pkg) }.map { it.pkg }.toSet()
        toast("Mostro solo le app principali")
    }

    private fun defaultLauncher() {
        try { startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
        catch (e: Exception) {
            AlertDialog.Builder(this).setTitle("Launcher predefinito")
                .setMessage("Premi il tasto Home del telecomando e scegli «Launcher TV» → «Sempre».\n\nSe non compare nulla, usa ADB:\nadb shell cmd package set-home-activity com.mytv.launcher/.MainActivity")
                .setPositiveButton("OK", null).show()
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
}
