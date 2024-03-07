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
import com.cmd.myapplication.data.BusLineRoutes
import com.cmd.myapplication.data.BusStop
import com.cmd.myapplication.data.repositories.BusLinesRepository
import com.cmd.myapplication.data.repositories.BusRoutesRepository
import com.cmd.myapplication.data.repositories.BusStopsRepository
import kotlinx.coroutines.launch

class BusDataViewModel(
    private val busStopsRepository: BusStopsRepository,
    private val busLinesRepository: BusLinesRepository,
    private val busRoutesRepository: BusRoutesRepository,
) : ViewModel() {
    val busStops: LiveData<List<BusStop>> by lazy { MutableLiveData(emptyList()) }
    val busLines: LiveData<List<BusLine>> by lazy { MutableLiveData(emptyList()) }
    val busLineRoutes: LiveData<List<BusLineRoutes>> by lazy { MutableLiveData(emptyList()) }

    val data: LiveData<Triple<List<BusStop>, List<BusLine>, List<BusLineRoutes>>> by lazy {
        MutableLiveData(
            Triple(
                emptyList(),
                emptyList(),
                emptyList()
            )
        )
    }

    /**
     * Request the ViewModel to refresh its data
     */
    fun requestData() = viewModelScope.launch {
        val startTime = System.currentTimeMillis()

        // request all synchronously
        Log.e(TAG, "started bus data requests @$startTime")
        val busStops = busStopsRepository.requestAll()
        Log.e(TAG, "received bus stop data @${System.currentTimeMillis() - startTime} ms")
        val busLines = busLinesRepository.requestAll()
        Log.e(TAG, "received bus line data @${System.currentTimeMillis() - startTime} ms")
        val busLineRoutes = busRoutesRepository.requestAll()
        Log.e(TAG, "received bus line routes data @${System.currentTimeMillis() - startTime} ms")

        // update ViewModel data
        updateLiveData(busStops, busLines, busLineRoutes)
    }

    private fun updateLiveData(
        busStops: List<BusStop>,
        busLines: List<BusLine>,
        busLineRoutes: List<BusLineRoutes>,
    ) {
        Log.e(TAG, "[BEFORE_UPDATE] busStops=[${this.busStops.value?.joinToString { it.id }}]")

        this.busStops.let { it as MutableLiveData }.value = busStops
        this.busLines.let { it as MutableLiveData }.value = busLines
        this.busLineRoutes.let { it as MutableLiveData }.value = busLineRoutes

        Log.e(TAG, "[AFTER_UPDATE] busStops=[${this.busStops.value?.joinToString { it.id }}]")

        this.data.let { it as MutableLiveData }.value = Triple(busStops, busLines, busLineRoutes)
    }

    companion object {
        const val TAG = "BusDataViewModel"

        /**
         * [ViewModel] factory used to instantiate this ViewModel in
         * [Activities][androidx.appcompat.app.AppCompatActivity]
         * and [Fragments][androidx.fragment.app.Fragment]
         */
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                // get application instance
                val application =
                    checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as App

                // provide required repositories
                return BusDataViewModel(
                    application.busStopsRepository,
                    application.busLinesRepository,
                    application.busRoutesRepository
                ) as T
            }
        }

        enum class DataType {
            ALL,
            BUS_STOPS,
            BUS_LINES,
            BUS_LINE_ROUTES
        }
    }
}