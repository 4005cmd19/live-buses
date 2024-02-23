package com.cmd.myapplication.data.viewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.cmd.myapplication.App
import com.cmd.myapplication.data.BusLineRoutes
import com.cmd.myapplication.data.repositories.BusRoutesRepository

class BusRoutesViewModel(
    private val repository: BusRoutesRepository,
) : ViewModel() {

    val busRoutes: LiveData<Set<BusLineRoutes>> by lazy { MutableLiveData(emptySet()) }

    fun requestBusRoutes(lineIds: Array<String> = emptyArray()) {
        val ids = lineIds.copyOf().toMutableSet()

        if (lineIds.isEmpty()) {
            ids.add("+")
        }

        val routes = mutableSetOf<BusLineRoutes>()

        repository.requestOnce(ids.toTypedArray()) { id, busRoutes ->
            routes.add(busRoutes)

            ids.remove(id)
            if (ids.isEmpty()) {
                update(routes)
            }
        }
    }

    fun debugSet(routes: Set<BusLineRoutes>) {
        update(routes)
    }

    private fun update(routes: Set<BusLineRoutes>) {
        (busRoutes as MutableLiveData).value = routes
    }

    companion object {
        const val TAG = "BusLinesViewModel"
        const val PERIOD = 5000L

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                Log.e(TAG, "create")

                val application =
                    checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as App
                return BusRoutesViewModel(application.busRoutesRepository) as T
            }
        }
    }
}