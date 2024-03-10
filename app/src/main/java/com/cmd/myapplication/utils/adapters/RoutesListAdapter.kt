package com.cmd.myapplication.utils.adapters

import android.text.SpannableString
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

class RoutesListAdapter(
    override val searchResults: MutableList<SearchResultData> = mutableListOf(),
) : SearchListAdapter<RoutesListAdapter.ViewHolder>() {
    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val lineNameView: Tag = view.findViewById(R.id.line_name_view)
        val destinationNameView: TextView = view.findViewById(R.id.destination_name_view)
        val routeView: TextView = view.findViewById(R.id.route_name_view)
        val operatorNameView: TextView = view.findViewById(R.id.line_operator_view)

        object Type {
            const val START = 0
            const val INTERMEDIATE = 1
            const val END = 2
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.search_list_line_item, parent, false)

        view.layoutParams =
            MarginLayoutParams(MarginLayoutParams.WRAP_CONTENT, MarginLayoutParams.WRAP_CONTENT)

        if (viewType == ViewHolder.Type.START) {
            view.updateLayoutParams<MarginLayoutParams> {
                leftMargin = 16.toDp(view.context)
            }
        } else if (viewType == ViewHolder.Type.END) {
            view.updateLayoutParams<MarginLayoutParams> {
                rightMargin = 16.toDp(view.context)
            }
        } else {
            view.updateLayoutParams<MarginLayoutParams> {
                leftMargin = 8.toDp(view.context)
                rightMargin = 8.toDp(view.context)
            }
        }

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val result = searchResults[position]
        val context = holder.view.context

        val routeResult = result as BusRouteData
        holder.lineNameView.text = routeResult.lineName
        holder.destinationNameView.text = routeResult.routeDestinationName
        holder.routeView.text = routeResult.routeName

        val operatorText = SpannableString(
            context.getString(
                R.string.bus_list_operator_name_view_prefix,
                result.operatorName
            )
        )

        holder.operatorNameView.text = operatorText
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> {
                ViewHolder.Type.START
            }

            searchResults.lastIndex -> {
                ViewHolder.Type.END
            }

            else -> {
                ViewHolder.Type.INTERMEDIATE
            }
        }
    }

    override fun getItemCount(): Int {
        return searchResults.size
    }
}