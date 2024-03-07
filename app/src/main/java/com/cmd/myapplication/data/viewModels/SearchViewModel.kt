package com.cmd.myapplication.data.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.cmd.myapplication.App
import com.cmd.myapplication.data.BusLine
import com.cmd.myapplication.data.BusLineRouteSearchResult
import com.cmd.myapplication.data.BusLineRoutes
import com.cmd.myapplication.data.BusStop
import com.cmd.myapplication.data.LatLngPoint
import com.cmd.myapplication.data.Place
import com.cmd.myapplication.data.PlaceSearchResult
import com.cmd.myapplication.data.SearchResult
import com.cmd.myapplication.data.repositories.SearchRepository
import com.cmd.myapplication.data.utils.SearchUtils
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

class SearchViewModel(
    private val searchRepository: SearchRepository,
) : ViewModel() {
    private val busStops: LiveData<List<BusStop>> by lazy { MutableLiveData(emptyList()) }
    private val busLines: LiveData<List<BusLine>> by lazy { MutableLiveData(emptyList()) }
    private val busLineRoutes: LiveData<List<BusLineRoutes>> by lazy { MutableLiveData(emptyList()) }

    private val autocompleteHandler = AutocompleteHandler(1000, ::processAutocompleteQuery)

    val searchText: MutableLiveData<String> by lazy { MutableLiveData(null) }

    val searchResults: LiveData<List<SearchResult>> by lazy {
        MutableLiveData(
            emptyList()
        )
    }

    init {
        autocompleteHandler.start()
    }

    fun search(query: String, autocomplete: Boolean = true) {
        if (autocomplete) {
            autocompleteHandler.post(query)
        } else {
            processSearchQuery(query)
        }
    }

    fun resolveSearchResult(searchResult: PlaceSearchResult, callback: (place: Place?) -> Unit) = viewModelScope.launch {
        val place = searchRepository.processPlaceSearchResult(searchResult)
        callback(place)
    }

    fun supplyBusData(
        busStops: List<BusStop>,
        busLines: List<BusLine>,
        busLineRoutes: List<BusLineRoutes>,
    ) {
        this.busStops.let { it as MutableLiveData }.value = busStops
        this.busLines.let { it as MutableLiveData }.value = busLines
        this.busLineRoutes.let { it as MutableLiveData }.value = busLineRoutes
    }

    fun supplyLocation(location: LatLngPoint) {
        autocompleteHandler.setLocationForAutocomplete(location)
    }

    // delegate this function to the autocomplete handler
    private fun processAutocompleteQuery(
        query: String,
        location: LatLngPoint?,
    ) =
        viewModelScope.launch {
            if (location == null) {
                return@launch
            }

            searchText.value = query

            val places = searchRepository.autocompleteSearch(query, location).toTypedArray()
            val busData = searchBusData(query).toTypedArray()

            val results = listOf(
                *places,
                *busData
            )

            updateSearchResults(results)
        }

    private fun processSearchQuery(query: String) = viewModelScope.launch {
        val places = searchRepository.search(query).toTypedArray()
        val busData = searchBusData(query).toTypedArray()

        val results = listOf(
            *places,
            *busData
        )

        updateSearchResults(results)
    }

    private fun updateSearchResults(results: List<SearchResult>) {
        searchResults.let { it as MutableLiveData }.value = results
    }

    private fun searchBusData (query: String): List<SearchResult> {
        val busStops = searchBusStops(query).toTypedArray()
        val busLineRoutes = searchBusRoutes(query).toTypedArray()

        return listOf(
            *busStops,
            *busLineRoutes
        )
    }

    private fun searchBusStops(query: String): List<BusStop> {
        val busStops = busStops.value ?: emptyList()

        return busStops.filter { SearchUtils.isMatch(query, it.displayName) }
    }

    private fun searchBusRoutes(query: String): List<BusLineRouteSearchResult> {
        val busLines = busLines.value ?: emptyList()
        val busLineRoutes = busLineRoutes.value ?: emptyList()

        return busLineRoutes.flatMap {
            val lineId = it.lineId

            it.routes.filter {
                SearchUtils.isMatch(query, it.name)
                        || SearchUtils.isMatch(query, it.startName)
                        || SearchUtils.isMatch(query, it.destinationName)

            }.map {
                val line = busLines.find { it.id == lineId }

                if (line != null) BusLineRouteSearchResult(line, it) else null
            }
        }.filterNotNull()
    }

    override fun onCleared() {
        super.onCleared()

        autocompleteHandler.stop()
    }

    private inner class AutocompleteHandler(
        val intervalMs: Long,
        autocompleteFn: (query: String, location: LatLngPoint?) -> Unit,
    ) {
        var next: QueryParams = QueryParams()
            private set
        var last: QueryParams = QueryParams()
            private set

        private var autocompleteLocation: LatLngPoint? = null

        private var isPending = false

        private val scheduledExecutor = Executors.newSingleThreadScheduledExecutor()
        private val queryExecutor = Executors.newSingleThreadExecutor()

        private val autocompleteRunnable: Runnable

        private var runnableFuture: Future<*>? = null

        init {
            this.autocompleteRunnable = wrapRunnable(autocompleteFn)
        }

        // post a new query
        fun post(query: String, forceDispatch: Boolean = false) {
            this.next = QueryParams(
                query,
                autocompleteLocation
            )

            dispatch(forceDispatch)
        }

        fun setLocationForAutocomplete(location: LatLngPoint) {
            autocompleteLocation = location
        }

        private fun dispatch(force: Boolean = false) {
            val canDispatch = with(next) { !(query == null || location == null) }

            if (canDispatch) {
                // check if new query is different from last
                isPending =
                    (next.query != last.query && !next.location!!.approxEquals(last.location)) || force
            } else {
                isPending = false
            }
        }

        private fun dispatchQuery() {
            isPending = false
            last = next
            next = QueryParams()
        }

        private fun wrapRunnable(block: (query: String, location: LatLngPoint?) -> Unit) =
            Runnable {
                block(next.query!!, next.location)

                dispatchQuery()
            }

        fun start() {
            scheduledExecutor.scheduleAtFixedRate(
                {
                    if (runnableFuture?.isDone == true) {
                        runnableFuture = queryExecutor.submit(autocompleteRunnable)
                    }
                }, 0, intervalMs, TimeUnit.MILLISECONDS
            )
        }

        fun stop() {
            scheduledExecutor.shutdownNow()
        }
    }

    private data class QueryParams(
        val query: String? = null,
        val location: LatLngPoint? = null,
        val isAutocomplete: Boolean = true,
    )

    companion object {
        const val TAG = "SearchViewModel"

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application =
                    checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as App

                return SearchViewModel(
                    application.searchRepository,
                ) as T
            }
        }
    }
}
