package com.mytv.launcher

import android.content.pm.PackageManager
import android.graphics.Outline
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppAdapter(
    private val pm: PackageManager,
    private val onClick: (AppEntry) -> Unit,
    private val onFocus: (AppEntry) -> Unit
) : RecyclerView.Adapter<AppAdapter.VH>() {

    private var items: List<AppEntry> = emptyList()

    fun submit(list: List<AppEntry>) { items = list; notifyDataSetChanged() }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val banner: ImageView = v.findViewById(R.id.banner)
        val icon: ImageView = v.findViewById(R.id.icon)
        val label: TextView = v.findViewById(R.id.label)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        val r = 12f * parent.resources.displayMetrics.density
        v.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, r)
            }
        }
        v.clipToOutline = true
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, position: Int) {
        val e = items[position]
        h.label.text = e.label
        val b = try { e.info.loadBanner(pm) } catch (ex: Exception) { null }
        if (b != null) {
            h.banner.setImageDrawable(b); h.banner.visibility = View.VISIBLE
            h.icon.visibility = View.GONE; h.label.visibility = View.GONE
        } else {
            h.banner.visibility = View.GONE
            h.icon.setImageDrawable(try { e.info.loadIcon(pm) } catch (ex: Exception) { null })
            h.icon.visibility = View.VISIBLE; h.label.visibility = View.VISIBLE
        }
        h.itemView.setOnClickListener { onClick(e) }
        h.itemView.scaleX = if (h.itemView.hasFocus()) 1.12f else 1f
        h.itemView.scaleY = h.itemView.scaleX
        h.itemView.setOnFocusChangeListener { v, f ->
            val s = if (f) 1.12f else 1f
            v.animate().scaleX(s).scaleY(s).setDuration(120).start()
            v.translationZ = if (f) 12f else 0f
            if (f) onFocus(e)
        }
    }

    override fun getItemCount() = items.size
}
