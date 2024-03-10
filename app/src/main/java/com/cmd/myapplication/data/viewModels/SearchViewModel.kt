package com.cmd.myapplication.data.viewModels

import android.util.Log
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
import com.cmd.myapplication.data.processors.closest
import com.cmd.myapplication.data.repositories.SearchRepository
import com.cmd.myapplication.data.utils.SearchUtils
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class SearchViewModel(
    private val searchRepository: SearchRepository,
) : ViewModel() {
    private val busStops: LiveData<List<BusStop>> by lazy { MutableLiveData(emptyList()) }
    private val busLines: LiveData<List<BusLine>> by lazy { MutableLiveData(emptyList()) }
    private val busLineRoutes: LiveData<List<BusLineRoutes>> by lazy { MutableLiveData(emptyList()) }

    private val autocompleteHandler = AutocompleteHandler(
        1000,
        ::processAutocompleteQuery
    )

    private var locationBias: LatLngPoint? = null

    val searchText: MutableLiveData<String?> by lazy { MutableLiveData(null) }

    val searchResults: LiveData<List<SearchResult>> by lazy {
        MutableLiveData(
            emptyList()
        )
    }

    fun search(query: String, autocomplete: Boolean = true) {
        if (locationBias == null) {
            return
        }

        if (query.isNotEmpty()) {
            if (autocomplete) {
                autocompleteHandler.post(query, locationBias!!)
            } else {
                processSearchQuery(query, locationBias!!)
            }
        } else {
            clearSearchResults()
        }
    }

    fun resolveSearchResult(searchResult: PlaceSearchResult, callback: (place: Place?) -> Unit) =
        viewModelScope.launch {
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

    fun setLocationBias(location: LatLngPoint) {
        locationBias = location
    }

    // delegate this function to the autocomplete handler
    private fun processAutocompleteQuery(
        query: String,
        location: LatLngPoint,
    ) = viewModelScope.launch {
        val places = searchRepository.autocompleteSearch(query, location).toTypedArray()
        val busData = searchBusData(query, location).toTypedArray()

        val results = listOf(
            *places,
            *busData
        )

        updateSearchResults(results)
    }

    private fun processSearchQuery(query: String, location: LatLngPoint) = viewModelScope.launch {
        val places = searchRepository.search(query, location).toTypedArray()
        val busData = searchBusData(query, location).toTypedArray()

        val results = listOf(
            *places,
            *busData
        )

        updateSearchResults(results)
    }

    private fun updateSearchResults(results: List<SearchResult>) {
        searchResults.let { it as MutableLiveData }.value = results
    }

    private fun clearSearchResults() {
        autocompleteHandler.consumeQuery()
        searchResults.let { it as MutableLiveData }.value = emptyList()
    }

    private fun searchBusData(query: String, location: LatLngPoint): List<SearchResult> {
        val busStops = searchBusStops(query, location).toTypedArray()
        val busLineRoutes = searchBusRoutes(query).toTypedArray()

        return listOf(
            *busStops,
            *busLineRoutes
        )
    }

    private fun searchBusStops(query: String, location: LatLngPoint): List<BusStop> {
        val busStops = busStops.value ?: emptyList()

        return busStops.filter {
            SearchUtils.isMatch(query, it.displayName)
        }.closest(location, MAX_BUS_STOP_RESULT_COUNT)
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
        }.filterNotNull().take(MAX_BUS_LINE_ROUTE_RESULT_COUNT)
    }

    override fun onCleared() {
        super.onCleared()

        searchRepository.saveCache()
    }

    /**
     * Autocomplete request handler that executes autocomplete queries on a short interval schedule
     * to avoid calling the [PlacesApiAdapter] too often.
     * @param intervalMs The interval in milliseconds between each API call.
     * @param autocompleteFn The function that calls the API.
     */
    private class AutocompleteHandler(
        val intervalMs: Long,
        autocompleteFn: (query: String, location: LatLngPoint) -> Unit,
    ) {
        val TAG = "AutocompleteHandler"
        val SCHEDULER_TAG = "ACH::Ping"

        // next query
        var next: QueryParams? = null
            private set

        // last query, used to check if next is different
        var last: QueryParams? = null
            private set

        // true if the next query should be executed
        private var isPending = false

        // scheduler
        private var scheduledExecutor: ScheduledExecutorService? = null
        private val queryExecutor = Executors.newSingleThreadExecutor()

        // executed by queryExecutor
        private val autocompleteRunnable: Runnable

        // used to track if the queryExecutor is free or still handling the last request
        private var runnableFuture: Future<*>? = null

        init {
            this.autocompleteRunnable = wrapRunnable(autocompleteFn)
        }

        /**
         * Post a new query to be executed. This query will only be executed if it's different
         * from the last or if [forceDispatch] is true.
         * @param query Query to be posted.
         * @param location Used to bias the API to give results close to the user.
         * @param forceDispatch True if the query should be dispatched even it it's not different
         * from the last.
         */
        fun post(query: String, location: LatLngPoint, forceDispatch: Boolean = false) {
            this.next = QueryParams(
                query,
                location
            )

            dispatch(forceDispatch)
        }

        /**
         * Flag the next query as executable.
         * @param force True if the query should be dispatched even it it's not different from the
         * last.
         */
        private fun dispatch(force: Boolean = false) {
            // check that there is a query to dispatch
            val canDispatch = next != null

            if (canDispatch) {
                // check if new query is different from last
                isPending = next!!.query != last?.query
                        || !next!!.location.approxEquals(last?.location)
                        || force // or force dispatch
            } else {
                isPending = false
            }

            // restart the scheduler if needed
            startOrResume()
        }

        /**
         * Mark the posted query as executed.
         */
        fun consumeQuery() {
            isPending = false
            last = next
            next = null
        }

        /**
         * Utility function - wrap autocomplete function in a runnable.
         * @param block The autocomplete function
         */
        private fun wrapRunnable(block: (query: String, location: LatLngPoint) -> Unit) =
            Runnable {
                // redundancy not null check
                checkNotNull(next)

                block(next!!.query, next!!.location)

                consumeQuery()
            }

        private fun startOrResume () {
            if(scheduledExecutor == null) {
                start()
            }
        }

        /**
         * Start the scheduler.
         */
        private fun start() {
            Log.e(TAG, "starting")
            scheduledExecutor = Executors.newSingleThreadScheduledExecutor()
            scheduledExecutor!!.scheduleAtFixedRate(
                {
                    val isExecutorAvailable =
                        runnableFuture?.isDone == true || runnableFuture == null

                    if (isPending) {
                        if (isExecutorAvailable) {
                            // if can run next query, run it
                            runnableFuture = queryExecutor.submit(autocompleteRunnable)
                        }
                    } else if (isExecutorAvailable) {
                        // if none pending and executor is free -> shut it down
                        stop()
                    }
                }, 0, intervalMs, TimeUnit.MILLISECONDS
            )
        }

        /**
         * Stop the scheduler.
         */
        private fun stop() {
            scheduledExecutor?.shutdownNow()
            scheduledExecutor = null
        }
    }

    private data class QueryParams(
        val query: String,
        val location: LatLngPoint,
    )

    companion object {
        const val TAG = "SearchViewModel"

        const val MAX_BUS_STOP_RESULT_COUNT = 20
        const val MAX_BUS_LINE_ROUTE_RESULT_COUNT = 20

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
