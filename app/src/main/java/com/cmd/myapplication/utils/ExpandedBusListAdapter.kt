package com.cmd.myapplication.utils

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.children
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.cmd.myapplication.R
import com.cmd.myapplication.toDp
import com.google.android.material.card.MaterialCardView
import com.google.android.material.shape.ShapeAppearanceModel

data class BusStopData(
    val id: String,
    val name: CharSequence,
    val lines: List<String>,
)

class ExpandedBusListAdapter(
    val busList: MutableList<BusStopData> = mutableListOf(),
) : RecyclerView.Adapter<ExpandedBusListAdapter.ViewHolder>() {

    private var expandCallback: (view: View, stopId: String) -> Unit = {view, stopId ->  }

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
        if (position == 0) {
            return ViewHolder.START
        } else if (position == 2) {
            return ViewHolder.END
        }

        return ViewHolder.MIDDLE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.expanded_bus_list_item, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val context = holder.context

        Log.e("OBVH", busList[position].name.toString())
        holder.stopNameView.text = busList[position].name

        val linesListView = holder.linesListView

        for (line in busList[position].lines) {
            val lineView = BusLineTag.createStyled(
                context,
                line,
                com.google.android.material.R.attr.colorPrimary,
                com.google.android.material.R.attr.colorOnPrimary
            )

            lineView.updateLayoutParams<MarginLayoutParams> {
                leftMargin = 2.toDp(context)
                rightMargin = 2.toDp(context)
            }

            linesListView.addView(lineView)
        }

        val (ellipsizedTag, id) = BusLineTag.createStyledEllipsized(
            context,
            com.google.android.material.R.attr.colorSecondary,
            com.google.android.material.R.attr.colorOnSecondary
        )

        linesListView.addView(ellipsizedTag)

        linesListView.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                linesListView.viewTreeObserver.removeOnGlobalLayoutListener(this)

                val width = linesListView.width
                val ellipsizedTagWidth = ellipsizedTag.width

                val toRemove = mutableListOf<View>()

                val lineViews = linesListView.children.toList().filterNot { it.id == id }

                for (i in 0..lineViews.lastIndex) {
                    val lineView = lineViews[i]

                    if (i != lineViews.lastIndex) {
                        if (lineView.right + ellipsizedTagWidth > width) {
                            toRemove.add(lineView)
                        }
                    } else {
                        if (lineView.right > width) {
                            toRemove.add(lineView)
                        }
                    }
                }

                if (toRemove.isEmpty()) {
                    toRemove.add(ellipsizedTag)
                }

                toRemove.forEach { linesListView.removeView(it) }

                lineViews.filterNot { toRemove.contains(it) }
                    .map { it as MaterialCardView }
                    .apply {
                        forEachIndexed { i, view ->
                            if (i == 0) {
                                view.makeStart(
                                    BusLineTag.TAG_GROUP_INNER_CORNER_SIZE_DP.toDp(context).toFloat()
                                )
                            } else if (i == lastIndex) {
                                view.makeEnd(
                                    BusLineTag.TAG_GROUP_INNER_CORNER_SIZE_DP.toDp(context).toFloat()
                                )
                            } else {
                                view.makeIntermediate(
                                    BusLineTag.TAG_GROUP_INNER_CORNER_SIZE_DP.toDp(context).toFloat()
                                )
                            }
                        }
                    }

                ellipsizedTag.apply {
                    layoutParams = LinearLayout.LayoutParams(
                        layoutParams.width,
                        lineViews.first().height
                    )
                }
            }
        })

        holder.view.transitionName = busList[position].id
        holder.expandButton.setOnClickListener { expandCallback(holder.view, busList[position].id) }
    }

    override fun getItemCount(): Int {
        return busList.size
    }

    fun onExpand(callback: (view: View, stopId: String) -> Unit) {
        expandCallback = callback
    }

    private fun MaterialCardView.makeStart(groupCornerSize: Float = 0f) {
        shapeAppearanceModel = ShapeAppearanceModel().toBuilder().apply {
            setTopLeftCornerSize(shapeAppearanceModel.topLeftCornerSize)
            setBottomLeftCornerSize(shapeAppearanceModel.bottomLeftCornerSize)
            setTopRightCornerSize(groupCornerSize)
            setBottomRightCornerSize(groupCornerSize)
        }.build()
    }

    private fun MaterialCardView.makeIntermediate(groupCornerSize: Float = 0f) {
        shapeAppearanceModel = ShapeAppearanceModel().toBuilder().apply {
            setTopLeftCornerSize(groupCornerSize)
            setBottomLeftCornerSize(groupCornerSize)
            setTopRightCornerSize(groupCornerSize)
            setBottomRightCornerSize(groupCornerSize)
        }.build()
    }

    private fun MaterialCardView.makeEnd(groupCornerSize: Float = 0f) {
        shapeAppearanceModel = ShapeAppearanceModel().toBuilder().apply {
            setTopLeftCornerSize(groupCornerSize)
            setBottomLeftCornerSize(groupCornerSize)
            setTopRightCornerSize(shapeAppearanceModel.topRightCornerSize)
            setBottomRightCornerSize(shapeAppearanceModel.bottomRightCornerSize)
        }.build()
    }
}