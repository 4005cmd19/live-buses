package com.cmd.myapplication.utils

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cmd.myapplication.R

data class BusStopData(
    val id: String,
    val name: CharSequence,
    val lines: List<String>,
)

class ExpandedBusListAdapter(
    val busList: MutableList<BusStopData> = mutableListOf(),
) : RecyclerView.Adapter<ExpandedBusListAdapter.ViewHolder>() {

    private var expandCallback: (view: View, stopId: String) -> Unit = { view, stopId -> }

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        companion object {
            const val START = 0
            const val MIDDLE = 1
            const val END = 2
        }

        val context: Context
        val stopNameView: TextView
        val linesListView: LinearLayout
        val expandButton: Button

        init {
            context = view.context

            stopNameView = view.findViewById(R.id.stop_name_view)
            linesListView = view.findViewById(R.id.bus_lines_list_view)
            expandButton = view.findViewById(R.id.expand_button)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.expanded_bus_list_item, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val context = holder.context

        val data = busList[position]

        holder.stopNameView.text = data.name

        val linesListView = holder.linesListView

//        linesListView.children.map { it as Tag }
//            .filterIndexed { i, _ -> i != linesListView.children.toList().lastIndex}
//            .forEachIndexed { i, it -> it.text = data.lines[i] }

        holder.view.transitionName = busList[position].id
        holder.expandButton.setOnClickListener {
            expandCallback(holder.view, busList[position].id)
        }
    }

    override fun getItemCount(): Int {
        return busList.size
    }

    fun onExpand(callback: (view: View, stopId: String) -> Unit) {
        expandCallback = callback
    }

    private fun Tag.makeEllipsized(count: Int) {
        val backgroundColor = with(TypedValue()) {
            context.theme.resolveAttribute(
                com.google.android.material.R.attr.colorSecondary,
                this,
                true
            )
            this.data
        }

        val foregroundColor = with(TypedValue()) {
            context.theme.resolveAttribute(
                com.google.android.material.R.attr.colorOnSecondary,
                this,
                true
            )
            this.data
        }

//        val icon = ResourcesCompat.getDrawable(resources, R.drawable.ic_more_horizontal, context.theme)

        setBackgroundColor(backgroundColor)
        this.foregroundColor = foregroundColor
        this.text = "+$count"
    }
}