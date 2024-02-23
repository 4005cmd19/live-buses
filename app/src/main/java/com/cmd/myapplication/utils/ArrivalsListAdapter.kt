package com.cmd.myapplication.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cmd.myapplication.R

data class ArrivalData(
    val route: CharSequence,
    val destination: CharSequence,
    val arrivalTime: CharSequence,
)

class ArrivalsListAdapter(
    val arrivalsList: MutableList<ArrivalData> = mutableListOf(),
) : RecyclerView.Adapter<ArrivalsListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val destinationNameView: TextView
        val routeNameView: TextView
        val arrivalTimeView: TextView

        init {
            destinationNameView = view.findViewById(R.id.destination_name_view)
            routeNameView = view.findViewById(R.id.route_name_view)
            arrivalTimeView = view.findViewById(R.id.arrival_time_view)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.arrivals_list_item, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = arrivalsList[position]

        holder.apply {
            destinationNameView.text = data.destination
            routeNameView.text = data.route
            arrivalTimeView.text = data.arrivalTime
        }
    }

    override fun getItemCount(): Int {
        return arrivalsList.size
    }
}