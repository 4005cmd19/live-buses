package com.cmd.myapplication.data.viewModels

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras

class SearchViewModel : ViewModel() {
    val searchText: MutableLiveData<String> by lazy { MutableLiveData("") }

    val searchResults: MutableLiveData<Set<SearchResult>> by lazy {
        MutableLiveData(
            emptySet()
        )
    }

    companion object {
        const val TAG = "BusLinesViewModel"
        const val PERIOD = 5000L

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                Log.e(TAG, "create")

                return SearchViewModel() as T
            }
        }
    }
}

interface SearchResult

data class StopSearchResult(
    var name: String,
    var lineNames: Set<String>,
) : SearchResult

data class LineSearchResult(
    var name: String,
    var operatorName: String
) : SearchResult