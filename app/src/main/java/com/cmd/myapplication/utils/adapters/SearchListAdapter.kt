package com.cmd.myapplication.utils.adapters

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.helper.widget.Flow
import androidx.recyclerview.widget.RecyclerView
import com.cmd.myapplication.R
import com.cmd.myapplication.utils.Tag
import com.google.android.material.shape.ShapeAppearanceModel

class SearchListAdapter(
    val searchResults: MutableList<SearchResultData> = mutableListOf(),
) : RecyclerView.Adapter<SearchListAdapter.ViewHolder>() {

    open class ViewHolder(val view: View) : RecyclerView.ViewHolder(view)

    class StopViewHolder(view: View) : ViewHolder(view) {
        val stopNameView: TextView
        val linesListView: Flow

        init {
            stopNameView = view.findViewById(R.id.stop_name_view)
            linesListView = view.findViewById(R.id.bus_lines_list_view)
        }
    }

    class RouteViewHolder(view: View) : ViewHolder(view) {
        val lineNameView: Tag
        val lineRouteView: TextView
        val operatorNameView: TextView

        init {
            lineNameView = view.findViewById(R.id.line_name_view)
            lineRouteView = view.findViewById(R.id.line_route_name_view)
            operatorNameView = view.findViewById(R.id.line_operator_view)
        }
    }
    class PlaceViewHolder(view: View) : ViewHolder(view) {
        val shortNameView: TextView
        val nameView: TextView

        init {
            shortNameView = view.findViewById(R.id.place_short_name_view)
            nameView = view.findViewById(R.id.place_name_view)
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        if (viewType == Type.STOP) {
            val view =
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.search_list_stop_item, parent, false)

            return StopViewHolder(view)
        } else if (viewType == Type.LINE) {
            val view =
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.search_list_line_item, parent, false)

            return RouteViewHolder(view)
        }
        else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.search_list_place_item, parent, false)

            return PlaceViewHolder (view)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val data = searchResults[position]

        return if (data is BusStopData) {
            Type.STOP
        } else if (data is BusRouteData) {
            Type.LINE
        }
        else {
            Type.PLACE
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val result = searchResults[position]
        val context = holder.view.context

        if (holder is StopViewHolder) {
            val data = result as BusStopData

            holder.apply {
                stopNameView.text = data.name

                // required for navigation transition between BusListFragment and StopFragment
                linesListView.referencedIds.forEachIndexed { i, it ->
                    val v = view.findViewById<Tag>(it)
                    v.visibility = View.GONE

                    if (i == 0) {
                        v.shapeAppearanceModel = ShapeAppearanceModel.builder(context, R.style.Tag_Start_ShapeAppearance, 0)
                            .build()
                    }
                    else if (i == linesListView.referencedIds.lastIndex) {
                        v.shapeAppearanceModel = ShapeAppearanceModel.builder(context, R.style.Tag_End_ShapeAppearance, 0)
                            .build()
                    }
                    else {
                        v.shapeAppearanceModel = ShapeAppearanceModel.builder(context, R.style.Tag_Intermediate_ShapeAppearance, 0)
                            .build()
                    }
                }

                val linesCount = data.lines.size

                if (linesCount <= BusStopListAdapter.TAG_COUNT) {
                    // can show all lines
                    linesListView.referencedIds.take(BusStopListAdapter.TAG_COUNT - 1).forEachIndexed { i, it ->
                        val v = view.findViewById<Tag>(it)
                        if (i < linesCount - 1) {
                            v.text = data.lines[i]
                            v.visibility = View.VISIBLE
                        } else {
                            v.visibility = View.GONE
                        }
                    }

                    linesListView.referencedIds.last().let { view.findViewById<Tag>(it) }.apply {
                        text = data.lines.last()
                        visibility = View.VISIBLE

                        if (linesCount == 1) {
                            shapeAppearanceModel = ShapeAppearanceModel.builder(context, com.google.android.material.R.style.ShapeAppearance_Material3_LargeComponent, 0)
                                .build()
                        }
                    }
                } else {
                    // show TAG_COUNT - 1 lines
                    linesListView.referencedIds.forEachIndexed { i, it ->
                        val v = view.findViewById<Tag>(it)
                        if (i < BusStopListAdapter.TAG_COUNT) {
                            v.text = data.lines[i]
                            v.visibility = View.VISIBLE
                        } else {
                            v.visibility = View.GONE
                        }
                    }

                    // and use last tag view to show how many lines are not shown
                    linesListView.referencedIds[BusStopListAdapter.TAG_COUNT - 1].let { view.findViewById<Tag>(it) }
                        .apply {
                            text = "${linesCount - BusStopListAdapter.TAG_COUNT + 1} more"
                            visibility = View.VISIBLE

                            // use secondary color palette for 'more' tag
                            setBackgroundColor(with(TypedValue()) {
                                context.theme.resolveAttribute(
                                    com.google.android.material.R.attr.colorSecondary,
                                    this,
                                    true
                                )
                                this.data
                            })

                            foregroundColor = with(TypedValue()) {
                                context.theme.resolveAttribute(
                                    com.google.android.material.R.attr.colorOnSecondary,
                                    this,
                                    true
                                )
                                this.data
                            }
                        }
                }
            }
        }
        else if (holder is RouteViewHolder) {
            val routeResult = result as BusRouteData
            holder.lineNameView.text = routeResult.lineName
            holder.operatorNameView.text = routeResult.operatorName
        }
        else if (holder is PlaceViewHolder) {
            val placeResult = result as PlaceData
            holder.shortNameView.text = placeResult.shortName
            holder.nameView.text = placeResult.name
        }
    }

    override fun getItemCount(): Int {
        return searchResults.size
    }

    companion object {
        object Type {
            const val STOP = 0
            const val LINE = 1
            const val PLACE = 2
        }
    }
}