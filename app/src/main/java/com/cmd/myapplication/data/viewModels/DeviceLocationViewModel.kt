package com.cmd.myapplication.data.viewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.cmd.myapplication.App
import com.cmd.myapplication.data.LatLngOrientation
import com.cmd.myapplication.data.LatLngPoint
import com.cmd.myapplication.data.Orientation
import com.cmd.myapplication.data.repositories.DeviceLocationRepository
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.abs

class DeviceLocationViewModel(
    private val repository: DeviceLocationRepository,
) : ViewModel() {

    val currentLocation: LiveData<LatLngPoint> by lazy { MutableLiveData(null) }
    val currentOrientation: LiveData<Orientation> by lazy { MutableLiveData(null) }
    val currentLocationData: LiveData<LatLngOrientation> by lazy { MutableLiveData(null) }

    private val orientationUpdateHandler = OrientationUpdateHandler(200)

    init {
    }

    fun requestCurrentLocation() = viewModelScope.launch {
        val location = repository.requestCurrentLocation()

        currentLocation.let { it as MutableLiveData }.value = location.split().first
        currentOrientation.let { it as MutableLiveData }.value = location.split().second
        currentLocationData.let { it as MutableLiveData }.value = location
    }

    fun requestUpdates() {
        orientationUpdateHandler.start()

        repository.listen(PERIOD) { location, orientation ->
            currentLocation.let { it as MutableLiveData }.apply {
                if (!location.approxEquals(value)) {
                    value = location
                }
            }

            currentOrientation.let { it as MutableLiveData }.apply {
                orientationUpdateHandler.post(orientation) {
                    value = it

                    if (currentLocation.value != null) {
                        currentLocationData.let { it as MutableLiveData }.apply {
                            value = LatLngOrientation.from(location, orientation)
                        }
                    }
                }
            }
        }
    }

    fun stopUpdates() {
        repository.stopListening()
        orientationUpdateHandler.stop()
    }

    private inner class OrientationUpdateHandler(
        private val rateMillis: Long,
        private val size: Int = 10,
        private val theta: Double = PI / 16,
    ) {
        private val updates = ArrayDeque<Orientation>()
        private var posted: Orientation? = null

        private val scheduler = Executors.newSingleThreadScheduledExecutor()
        private var callback: ((Orientation) -> Unit)? = null

        init {
            if (size < 1) {
                throw IllegalArgumentException("N must be >= 1")
            }
        }

        fun post(orientation: Orientation, callback: (orientation: Orientation) -> Unit) {
            posted = orientation

            if (updates.size >= size) {
                updates.removeFirst()
            }

            if (updates.isNotEmpty()) {
                val lastOrientation = updates.last()
                val old = (lastOrientation.minusZ + lastOrientation.y) / 2
                val new = (orientation.minusZ + orientation.y) / 2

                if (
                    abs(new - old) > theta
                ) {
                    updates.clear()
                }
            }

            updates.addLast(orientation)

            if (this.callback == null) {
                this.callback = callback
            }
        }

        private fun calculateNPointAverage(): Orientation {
            val sumZ = updates.sumOf { it.minusZ.toDouble() }
            val sumX = updates.sumOf { it.x.toDouble() }
            val sumY = updates.sumOf { it.y.toDouble() }

            val mZ = (sumZ / updates.size).toFloat()
            val mX = (sumX / updates.size).toFloat()
            val mY = (sumY / updates.size).toFloat()

            return Orientation(mZ, mX, mY)
        }

        fun start() {
            scheduler.scheduleAtFixedRate(
                {
                    try {
//                        if (posted != null) {
//                            viewModelScope.launch {
//                                callback?.let { it(posted!!) }
//                            }
//                        }

                        if (updates.isNotEmpty()) {
                            val newOrientation = calculateNPointAverage()

                            viewModelScope.launch { callback?.let { it(newOrientation) } }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, 0, rateMillis, TimeUnit.MILLISECONDS
            )
        }

        fun stop() {
            scheduler.shutdownNow()
        }
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