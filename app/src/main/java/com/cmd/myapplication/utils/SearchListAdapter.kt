package com.cmd.myapplication.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cmd.myapplication.R
import com.cmd.myapplication.data.viewModels.LineSearchResult
import com.cmd.myapplication.data.viewModels.SearchResult
import com.cmd.myapplication.data.viewModels.StopSearchResult

class SearchListAdapter(
    val searchResults: MutableList<SearchResult> = mutableListOf(),
) : RecyclerView.Adapter<SearchListAdapter.ViewHolder>() {

    open class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    class StopViewHolder(view: View) : ViewHolder(view) {
        val stopName: TextView
        val lines: TextView

        init {
            stopName = view.findViewById(R.id.stop_name_view)
            lines = view.findViewById(R.id.lines_view)
        }
    }

    class LineViewHolder(view: View) : ViewHolder(view) {
        val lineName: TextView
        val operatorName: TextView

        init {
            lineName = view.findViewById(R.id.line_name_view)
            operatorName = view.findViewById(R.id.line_operator_view)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        if (viewType == Type.STOP) {
            val view =
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.search_list_stop_item, parent, false)

            return StopViewHolder(view)
        } else {
            val view =
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.search_list_line_item, parent, false)

            return LineViewHolder(view)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (searchResults[position] is StopSearchResult) {
            Type.STOP
        } else {
            Type.LINE
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val result = searchResults[position]

        if (holder is StopViewHolder) {
            val stopResult = result as StopSearchResult
            holder.stopName.text = stopResult.name
        }
        else if (holder is LineViewHolder) {
            val lineResult = result as LineSearchResult
            holder.lineName.text = lineResult.name
            holder.operatorName.text = lineResult.operatorName
        }
    }

    override fun getItemCount(): Int {
        return searchResults.size
    }

    companion object {
        object Type {
            const val STOP = 0
            const val LINE = 1
        }
    }
}