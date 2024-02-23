package com.cmd.myapplication.data.viewModels

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.cmd.myapplication.App
import com.cmd.myapplication.data.LatLngPoint
import com.cmd.myapplication.data.LatLngRect
import com.cmd.myapplication.data.repositories.DeviceLocationRepository

class DeviceLocationViewModel(
    private val repository: DeviceLocationRepository,
) : ViewModel() {

    val currentLocation: MutableLiveData<LatLngPoint?> by lazy { MutableLiveData(null) }

    private val mapBounds: MutableLiveData<LatLngRect> by lazy { MutableLiveData() }

    init {
        Log.e(TAG, "init")

        repository.requestCurrentLocation {
            if (it != null) {
                currentLocation.value = it
                Log.e(TAG, "current loc - ${it.lat} ${it.lng}")
            } else {
                repository.listenSilent(PERIOD) {
                    if (it != null) {
                        currentLocation.value = it

                        repository.stopListeningSilent()
                    }
                }
            }
        }
    }

    fun requestLocation(callback: (location: LatLngPoint?) -> Unit) {
        repository.requestCurrentLocation(callback)
    }

    fun start() {
        repository.listen(PERIOD) {
            currentLocation.value = it
        }
    }

    fun stop () {
        repository.stopListening()
    }

    companion object {
        const val TAG = "LocationViewModel"
        const val PERIOD = 5000L

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                Log.e(TAG, "create")

                val application =
                    checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as App
                return DeviceLocationViewModel(application.deviceLocationRepository) as T
            }
        }
    }
}