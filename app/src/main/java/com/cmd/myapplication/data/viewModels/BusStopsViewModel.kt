package com.cmd.myapplication.data.viewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.cmd.myapplication.App
import com.cmd.myapplication.data.BusStop
import com.cmd.myapplication.data.repositories.BusStopsRepository

class BusStopsViewModel(
    private val repository: BusStopsRepository,
) : ViewModel() {

    val busStops: LiveData<Set<BusStop>> by lazy { MutableLiveData(emptySet()) }

    fun requestBusStops(stopIds: Array<String> = emptyArray()) {
        val ids = stopIds.copyOf().toMutableSet()

        if (stopIds.isEmpty()) {
            ids.add("+")
        }

        val stops = mutableSetOf<BusStop>()

        repository.requestOnce(ids.toTypedArray()) { id, busLine ->
            stops.add(busLine)

            ids.remove(id)
            if (ids.isEmpty()) {
                update(stops)
            }
        }
    }

    fun debugSet(stops: Set<BusStop>) {
        update(stops)
    }

    private fun update(stops: Set<BusStop>) {
        (busStops as MutableLiveData).value = stops
    }

    companion object {
        const val TAG = "BusLinesViewModel"
        const val PERIOD = 5000L

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                Log.e(TAG, "create")

                val application =
                    checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as App
                return BusStopsViewModel(application.busStopsRepository) as T
            }
        }
    }
}