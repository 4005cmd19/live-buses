package com.cmd.myapplication.utils.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.TextView
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.cmd.myapplication.R
import com.cmd.myapplication.toDp
import com.cmd.myapplication.utils.Tag

class BusListAdapter(
    val busList: MutableList<BusData> = mutableListOf(),
) : ListAdapter<BusData, BusListAdapter.ViewHolder>() {
    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        object Type {
            const val START = 0
            const val INTERMEDIATE = 1
            const val END = 2
        }

        val lineNameView: Tag = view.findViewById(R.id.line_name_view)
        val stopNameView: TextView = view.findViewById(R.id.stop_name_view)
        val destinationNameView: TextView = view.findViewById(R.id.destination_name_view)
        val routeNameView: TextView = view.findViewById(R.id.route_name_view)
        val arrivalTimeView: TextView = view.findViewById(R.id.arrival_time_view)
    }

    override fun getItemViewType(position: Int): Int {
        if (position == 0) {
            return ViewHolder.Type.START
        } else if (position == busList.lastIndex) {
            return ViewHolder.Type.END
        }

        return ViewHolder.Type.INTERMEDIATE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.compact_bus_list_item, parent, false)

        view.updateLayoutParams<MarginLayoutParams> {
            if (viewType == ViewHolder.Type.START) {
                leftMargin = 16.toDp(view.context)
            } else if (viewType == ViewHolder.Type.END) {
                rightMargin = 16.toDp(view.context)
            }
        }

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = busList[position]

        holder.apply {
            lineNameView.text = data.line
            stopNameView.text = data.stop
            destinationNameView.text = data.destination
            routeNameView.text = data.routeName
            arrivalTimeView.text = busList[position].arrivalTime
        }

        Log.e("TRANS", "name - ${holder.view.transitionName}")

        holder.view.transitionName =
            "expand_stop_transition_${data.stopId}_${data.lineId}_${data.destination}"
        holder.view.setOnClickListener {
            Log.e(TAG, "onClick - onExpandListener=${onExpandListener}")
            onExpandListener?.onExpand(holder.view, position, data)
        }
    }

    override fun getItemCount(): Int {
        return busList.size
    }

    companion object {
        const val TAG = "BusListAdapter"
    }
}

