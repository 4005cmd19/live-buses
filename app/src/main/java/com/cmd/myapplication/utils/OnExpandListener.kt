package com.cmd.myapplication.utils

import android.view.View

fun interface OnExpandListener<T> {
    fun onExpand(view: View, position: Int, data: T)
}