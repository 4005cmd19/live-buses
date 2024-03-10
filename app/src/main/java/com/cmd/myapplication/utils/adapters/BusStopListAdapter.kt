package com.cmd.myapplication.utils.adapters

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.TextView
import androidx.constraintlayout.helper.widget.Flow
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.cmd.myapplication.R
import com.cmd.myapplication.utils.Tag
import com.google.android.material.card.MaterialCardView
import com.google.android.material.shape.ShapeAppearanceModel
import kotlin.random.Random
import kotlin.random.nextInt

class BusStopListAdapter(
    val busStops: MutableList<BusStopData> = mutableListOf(),
) : ListAdapter<BusStopData, BusStopListAdapter.ViewHolder>() {
    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        // used for apply margins correctly
        // returned by RecyclerView.getItemViewType(Int)
        object Type {
            /**
             * View holder holds the first item in the list
             */
            const val START = 0

            /**
             * View holder holds an item that is neither the first or last in the list
             */
            const val INTERMEDIATE = 1

            /**
             * View holder holds the last item in the list
             */
            const val END = 2
        }

        val context: Context = view.context
        val stopNameView: TextView = view.findViewById(R.id.stop_name_view)
        val linesListView: Flow = view.findViewById(R.id.bus_lines_list_view)
    }

    override fun getItemViewType(position: Int): Int {
        if (position == 0) {
            return ViewHolder.Type.START
        } else if (position == busStops.lastIndex) {
            return ViewHolder.Type.END
        }

        return ViewHolder.Type.INTERMEDIATE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.expanded_bus_list_item, parent, false)

        if (viewType == ViewHolder.Type.END) {
            ViewCompat.setOnApplyWindowInsetsListener(view) { view, windowInsets ->
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

                view.updateLayoutParams<MarginLayoutParams> {
                    bottomMargin = insets.bottom
                }

                return@setOnApplyWindowInsetsListener WindowInsetsCompat(windowInsets)
            }
        }

        return ViewHolder(view)
    }

    private var randomOne: Int? = null

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        var data = busStops[position]

        if (randomOne == null) {
            randomOne = Random.nextInt(0..busStops.lastIndex)
        }

        if (position == randomOne) {
            data = data.copy(
                lines = listOf(data.lines.first())
            )
        }

        holder.apply {
            stopNameView.text = busStops[position].name

            // required for navigation transition between BusListFragment and StopFragment
            view.transitionName = busStops[position].id

            view.setOnClickListener {
                onExpandListener?.onExpand(
                    holder.view,
                    position,
                    busStops[position]
                )
            }

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

            if (linesCount <= TAG_COUNT) {
                // can show all lines
                linesListView.referencedIds.take(TAG_COUNT - 1).forEachIndexed { i, it ->
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
                    if (i < TAG_COUNT) {
                        v.text = data.lines[i]
                        v.visibility = View.VISIBLE
                    } else {
                        v.visibility = View.GONE
                    }
                }

                // and use last tag view to show how many lines are not shown
                linesListView.referencedIds[TAG_COUNT - 1].let { view.findViewById<Tag>(it) }
                    .apply {
                        text = "${linesCount - TAG_COUNT + 1} more"
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

    override fun getItemCount(): Int {
        return busStops.size
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

    companion object {
        const val TAG = "ExpandedBusListAdapter"

        /**
         * Max number of [Tag]s for bus stop lines that each bus stop list item can display
         */
        const val TAG_COUNT = 5
    }
}