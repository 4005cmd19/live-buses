package com.cmd.myapplication.utils.adapters

import androidx.recyclerview.widget.RecyclerView
import com.cmd.myapplication.utils.OnExpandListener

abstract class ListAdapter <T, VH: RecyclerView.ViewHolder> : RecyclerView.Adapter<VH> () {
    protected var onExpandListener: OnExpandListener<T>? = null
        private set

    fun setOnExpandListener (listener: OnExpandListener<T>?) {
        this.onExpandListener = listener
    }
}