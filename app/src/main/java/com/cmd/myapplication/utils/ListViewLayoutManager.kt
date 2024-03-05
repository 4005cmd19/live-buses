package com.cmd.myapplication.utils

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ListViewLayoutManager(
    context: Context,
    orientation: Int,
    reverseLayout: Boolean,
) : LinearLayoutManager(
    context, orientation, reverseLayout
) {
    var canScrollVertically: Boolean = true
    var canScrollHorizontally: Boolean = true

    constructor(context: Context) : this(context, RecyclerView.VERTICAL, false)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int): this(context) {
        val properties = getProperties(context, attrs, defStyleAttr, defStyleRes)

        orientation = properties.orientation
        reverseLayout = properties.reverseLayout
        stackFromEnd = properties.stackFromEnd
    }

    override fun canScrollVertically(): Boolean {
        return canScrollVertically && super.canScrollVertically()
    }

    override fun canScrollHorizontally(): Boolean {
        return canScrollHorizontally && super.canScrollHorizontally()
    }
}