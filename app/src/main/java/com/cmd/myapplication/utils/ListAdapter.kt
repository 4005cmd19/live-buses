package com.cmd.myapplication.utils

import androidx.recyclerview.widget.RecyclerView

abstract class ListAdapter <T, VH: RecyclerView.ViewHolder> : RecyclerView.Adapter<VH> () {
    protected var onExpandListener: OnExpandListener<T>? = null
        private set

    fun setOnExpandListener (listener: OnExpandListener<T>?) {
        this.onExpandListener = listener
    }
}