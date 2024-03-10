package com.cmd.myapplication.utils.adapters

import androidx.recyclerview.widget.RecyclerView

abstract class SearchListAdapter<VH: RecyclerView.ViewHolder> (
    open val searchResults: MutableList<SearchResultData> = mutableListOf()
): RecyclerView.Adapter<VH>()