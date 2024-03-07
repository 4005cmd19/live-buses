package com.cmd.myapplication.data.viewModels

import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.cmd.myapplication.data.BusStop
import com.cmd.myapplication.data.LatLngPoint
import com.cmd.myapplication.data.Locality
import com.cmd.myapplication.data.LocalityLocation
import com.cmd.myapplication.data.processors.closest
import com.cmd.myapplication.data.processors.inArea

class NearbyBusesViewModel : ViewModel() {
    private val _location: MutableLiveData<LatLngPoint> by lazy { MutableLiveData(null) }
    private val _busStops: MutableLiveData<List<BusStop>> by lazy { MutableLiveData(emptyList()) }

    val nearbyBusStops: LiveData<List<BusStop>> by lazy { MutableLiveData(emptyList()) }
    val selectedBusStop: MutableLiveData<Pair<String, View>> by lazy { MutableLiveData(null) }

    var location: LatLngPoint?
        get() = _location.value
        set(value) {
            _location.value = value

            recompute()
        }

    var busStops: List<BusStop>?
        get() = _busStops.value
        set(value) {
            _busStops.value = value

            recompute()
        }

    private fun recompute() {
        if (
            location == null
            || busStops == null
        ) {
            return
        }

        val locality = LocalityLocation.forLocation(location!!)

        if (locality == Locality.NOT_FOUND) {

        } else {
            val bounds = locality.location

            val nearbyStops = busStops!!.inArea(bounds)
                .closest(location!!)
                .toList()

            nearbyBusStops.let { it as MutableLiveData }.value = nearbyStops
        }
    }

    companion object {
        const val TAG = "NearbyBusesViewModel"
        const val PERIOD = 5000L

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return NearbyBusesViewModel() as T
            }
        }
    }
}