package com.cmd.myapplication

import android.content.Context
import android.util.TypedValue

fun Float.toDp (context: Context?): Float {
    if (context == null) {
        return 0f
    }

    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this,
        context.resources.displayMetrics
    )
}

fun Double.toDp (context: Context?): Double {
    return this.toFloat().toDp(context).toDouble()
}

fun Int.toDp (context: Context?): Int {
    return this.toFloat().toDp(context).toInt()
}

fun Long.toDp (context: Context?): Long {
    return this.toFloat().toDp(context).toLong()
}