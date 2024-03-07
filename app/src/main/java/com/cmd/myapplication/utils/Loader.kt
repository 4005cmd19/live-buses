package com.cmd.myapplication.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.LayoutRes

class Loader(
    context: Context,
    @LayoutRes id: Int,
) {
    val view: View

    init {
        view = LayoutInflater.from(context)
            .inflate(id, null)
    }
}