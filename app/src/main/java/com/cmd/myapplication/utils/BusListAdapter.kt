package com.cmd.myapplication.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.TextView
import androidx.core.view.marginLeft
import androidx.recyclerview.widget.RecyclerView
import com.cmd.myapplication.R
import com.cmd.myapplication.toDp

data class BusInfo(
    val routeName: String,
    val arrivalTime: String,
)

class BusListAdapter(
    private var busList: Array<BusInfo>,
    val isExpanded: Boolean = false,
) : RecyclerView.Adapter<BusListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        companion object {
            const val START = 0
            const val MIDDLE = 1
            const val END = 2
        }

        val routeNameView: TextView
        val arrivalTimeView: TextView

        init {
            routeNameView = view.findViewById(R.id.route_name_view)
            arrivalTimeView = view.findViewById(R.id.arrival_time_view)
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (position == 0) {
            return ViewHolder.START
        } else if (position == 2) {
            return ViewHolder.END
        }

        return ViewHolder.MIDDLE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = if (isExpanded) {
            LayoutInflater.from(parent.context)
                .inflate(R.layout.expanded_bus_list_item, parent, false)
        } else {
            LayoutInflater.from(parent.context)
                .inflate(R.layout.compact_bus_list_item, parent, false)
        }

        if (!isExpanded) {
            val lp = view.layoutParams as MarginLayoutParams
            if (viewType == ViewHolder.START) {
                lp.leftMargin = 16.toDp(view.context)
                view.layoutParams = lp
            } else if (viewType == ViewHolder.END) {
                lp.rightMargin = 16.toDp(view.context)
                view.layoutParams = lp
            }
        }

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.routeNameView.text = busList[position].routeName
        holder.arrivalTimeView.text = busList[position].arrivalTime
    }

    override fun getItemCount(): Int {
        return busList.size
    }
}