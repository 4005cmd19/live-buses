package com.cmd.myapplication.utils

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.core.widget.NestedScrollView

class ScrollableHost(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int
) : NestedScrollView(
    context, attrs, defStyleAttr
) {
    var isScrollable = true
    private var onOverScrolledListener: OnOverScrolledListener? = null

    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, androidx.core.R.attr.nestedScrollViewStyle)

    constructor(context: Context): this(context, null)

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        if (isScrollable) {
            super.onScrollChanged(l, t, oldl, oldt)
        }
    }

    override fun onOverScrolled(scrollX: Int, scrollY: Int, clampedX: Boolean, clampedY: Boolean) {
        onOverScrolledListener?.onOverScrolled(scrollX, scrollY, clampedX, clampedY)

        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY)
    }

    override fun onTouchEvent(motionEvent: MotionEvent): Boolean {
        return super.onTouchEvent(motionEvent)
    }

    override fun onInterceptTouchEvent(e: MotionEvent): Boolean {
        return isScrollable && super.onInterceptTouchEvent(e)//false //isScrollable && super.onInterceptTouchEvent(e)
    }

    fun setOnOverScrolledListener (listener: OnOverScrolledListener?) {
        this.onOverScrolledListener = listener
    }

    companion object {
        fun interface OnOverScrolledListener {
            fun onOverScrolled (scrollX: Int, scrollY: Int, clampedX: Boolean, clampedY: Boolean)
        }
    }
}