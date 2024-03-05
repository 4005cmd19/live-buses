package com.cmd.myapplication.utils

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class TagAdapter(
    val lines: MutableList<String> = mutableListOf(),
) :
    RecyclerView.Adapter<TagAdapter.ViewHolder>() {
    class ViewHolder(val view: Tag) : RecyclerView.ViewHolder(view) {
        companion object {
            const val START = 0
            const val MIDDLE = 1
            const val END = 2
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(Tag(parent.context))
    }

    override fun getItemCount(): Int {
        return lines.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.view.text = lines[position]
    }
}