package com.mytv.launcher

import android.content.Context
import android.content.Intent
import android.media.tv.TvContract
import android.media.tv.TvInputManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var adapter: AppAdapter
    private lateinit var recycler: RecyclerView
    private lateinit var titleView: TextView
    private lateinit var greeting: TextView
    private lateinit var wallpaper: ImageView
    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() { updateGreeting(); handler.postDelayed(this, 30_000) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = Prefs(this)
        wallpaper = findViewById(R.id.wallpaper)
        titleView = findViewById(R.id.title)
        greeting = findViewById(R.id.greeting)
        recycler = findViewById(R.id.recycler)

        adapter = AppAdapter(packageManager,
            onClick = { app ->
                try { startActivity(app.launchIntent()) }
                catch (e: Exception) { toast("Impossibile aprire ${app.label}") }
            },
            onFocus = { app -> titleView.text = app.label })
        recycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recycler.adapter = adapter

        findViewById<View>(R.id.btnInputs).setOnClickListener { showInputs() }
        findViewById<View>(R.id.btnWifi).setOnClickListener {
            try { startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) }
            catch (e: Exception) {
                try { startActivity(Intent(Settings.ACTION_SETTINGS)) } catch (e2: Exception) { toast("Impostazioni non disponibili") }
            }
        }
        findViewById<View>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Su un launcher il tasto Indietro non deve chiudere la home.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { goFirst() }
        })
    }

    override fun onResume() {
        super.onResume()
        wallpaper.setImageDrawable(Wallpapers.load(this, prefs))
        val hidden = prefs.hidden
        adapter.submit(Apps.loadAll(this).filter { it.pkg !in hidden })
        if (currentFocus == null) goFirst()
        handler.post(tick)
    }

    override fun onPause() {
        handler.removeCallbacks(tick)
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        goFirst() // tasto Home premuto mentre il launcher è già aperto
    }

    private fun goFirst() {
        recycler.scrollToPosition(0)
        recycler.post { recycler.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus() }
    }

    private fun updateGreeting() {
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        greeting.text = when (h) {
            in 5..11 -> "BUONGIORNO"
            in 12..17 -> "BUON POMERIGGIO"
            in 18..22 -> "BUONASERA"
            else -> "BUONANOTTE"
        }
    }

    private fun showInputs() {
        try {
            val tim = getSystemService(Context.TV_INPUT_SERVICE) as TvInputManager
            val list = tim.tvInputList.filter { it.isPassthroughInput }
            if (list.isEmpty()) { toast("Nessun ingresso trovato"); return }
            val labels = list.map { it.loadLabel(this).toString() }.toTypedArray()
            AlertDialog.Builder(this).setTitle("Ingressi")
                .setItems(labels) { _, i ->
                    try {
                        startActivity(
                            Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(list[i].id))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    } catch (e: Exception) { toast("Impossibile aprire l'ingresso") }
                }.show()
        } catch (e: Exception) {
            toast("Ingressi non disponibili su questa TV")
        }
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
}
