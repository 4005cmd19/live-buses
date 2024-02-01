package com.cmd.myapplication.data.viewModels

import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.cmd.myapplication.App
import com.cmd.myapplication.data.repositories.DeviceLocationRepository

class DeviceLocationViewModel(
    private val repository: DeviceLocationRepository,
) : ViewModel() {

    private val _currentLocation: MutableLiveData<DeviceLocationRepository.LocationData?> =
        MutableLiveData(null)
    private lateinit var observer: Observer<DeviceLocationRepository.LocationData?>

    var currentLocation: DeviceLocationRepository.LocationData? = _currentLocation.value
        private set

    init {
        Log.e(TAG, "init")

        repository.requestCurrentLocation {
            if (it != null) {
                _currentLocation.value = it
                Log.e(TAG, "current loc - ${it.latitude} ${it.longitude}")
            } else {
                repository.listenSilent(PERIOD) {
                    if (it != null) {
                        _currentLocation.value = it

                        repository.stopListeningSilent()
                    }
                }
            }
        }
    }

    fun requestLocation (callback: (location: DeviceLocationRepository.LocationData?) -> Unit) {
        val location = repository.requestCurrentLocation(callback)
    }

    fun observe(
        lifecycleOwner: LifecycleOwner,
        callback: (location: DeviceLocationRepository.LocationData?) -> Unit,
    ) {
        Log.e(TAG, "observe")
        if (!::observer.isInitialized) {
            observer = Observer {
                callback(it)
            }
        }

        Log.e(TAG, "observer - $_currentLocation")

        _currentLocation.observe(lifecycleOwner, observer)

        Log.e(TAG, "calling listen")
        repository.listen(PERIOD) { _currentLocation.value = it }
    }

    fun stopObserving() {
        repository.stopListening()
        _currentLocation.removeObserver(observer)
    }

    private fun updateLocation(location: DeviceLocationRepository.LocationData) {
        _currentLocation.value = location
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