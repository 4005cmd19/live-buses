package com.cmd.myapplication

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout

class SearchView: LinearLayout {
    constructor(context: Context?) : super(context) {
        init()
    }
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    private fun init () {
        inflate(context, R.layout.search_view, this)
    }

    override fun setAlpha(alpha: Float) {
        super.setAlpha(alpha)
    }
}