package com.cmd.myapplication.data.viewModels

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.cmd.myapplication.App
import com.cmd.myapplication.data.LatLngPoint
import com.cmd.myapplication.data.LatLngRect
import com.cmd.myapplication.data.SearchResult
import com.cmd.myapplication.data.repositories.BusLinesRepository
import com.cmd.myapplication.data.repositories.BusRoutesRepository
import com.cmd.myapplication.data.repositories.BusStopsRepository
import com.cmd.myapplication.data.repositories.SearchRepository
import com.cmd.myapplication.data.utils.SearchUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SearchViewModel(
    private val searchRepository: SearchRepository,
    private val busStopsRepository: BusStopsRepository,
    private val busLinesRepository: BusLinesRepository,
    private val busRoutesRepository: BusRoutesRepository,
) : ViewModel() {
    fun test() {
        viewModelScope.launch(Dispatchers.IO) {
            searchRepository.test()
        }
    }

    val searchText: MutableLiveData<String> by lazy { MutableLiveData(null) }

    val searchResults: MutableLiveData<Set<SearchResult>> by lazy {
        MutableLiveData(
            emptySet()
        )
    }

    fun search(query: String, location: LatLngPoint, bounds: LatLngRect) = viewModelScope.launch {
        searchText.value = query

        val nearbyPlaces = searchRepository.findNearbyPlaces(query, location, bounds)
//        searchBusStops(query)
//        searchBusRoutes(query)

        searchResults.value = setOf(
            *nearbyPlaces.toTypedArray()
        )
    }

    private fun searchBusStops(query: String) {
        busStopsRepository.requestAll { id, busStop ->
            if (SearchUtils.isMatch(query, busStop.displayName)) {

            }
        }
    }

    private fun searchBusRoutes(query: String) {
        busRoutesRepository.requestAll { id, busLineRoutes ->
            busLineRoutes.routes.forEach {
                if (
                    SearchUtils.isMatch(query, it.name)
                    || SearchUtils.isMatch(query, it.startName)
                    || SearchUtils.isMatch(query, it.destinationName)
                ) {

                }
            }
        }
    }

    companion object {
        const val TAG = "SearchViewModel"

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                Log.e(TAG, "create")

                val application =
                    checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as App

                return SearchViewModel(
                    application.searchRepository,
                    application.busStopsRepository,
                    application.busLinesRepository,
                    application.busRoutesRepository
                ) as T
            }
        }
    }
}
