package com.cmd.myapplication.data.repositories

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import android.os.Looper
import android.util.Log
import com.cmd.myapplication.data.LatLngPoint
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LastLocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

@SuppressLint("MissingPermission")
class DeviceLocationRepository(application: Application) {
    private val locationClient: FusedLocationProviderClient

    var isListening = false
        private set

    private lateinit var locationUpdateCallback: LocationCallback

    init {
        Log.e(TAG, "app - ${application.applicationContext}")
        locationClient = LocationServices.getFusedLocationProviderClient(application)
    }

    fun requestCurrentLocation (callback: (location: LatLngPoint?) -> Unit) {
        val locationRequest = LastLocationRequest.Builder()
            .setGranularity(Granularity.GRANULARITY_FINE)
            .build()

        locationClient.getLastLocation(locationRequest).addOnSuccessListener {
            callback(it.toData())
        }
    }

    fun listenSilent (period: Long, callback: (location: LatLngPoint?) -> Unit) {
        val request = buildLocationRequest(period)
        locationUpdateCallback = buildCallback(callback)

        Log.e(TAG, "request")
        locationClient.requestLocationUpdates(
            request,
            locationUpdateCallback,
            Looper.getMainLooper()
        )
    }

    fun listen (period: Long, callback: (location: LatLngPoint?) -> Unit = {}) {
        if (!isListening) {
            isListening = true

            listenSilent (period, callback)
        }
    }

    fun stopListeningSilent () {
        locationClient.removeLocationUpdates(locationUpdateCallback)
    }

    fun stopListening () {
        stopListeningSilent()
        isListening = false
    }

    private fun buildLocationRequest (period: Long, priority: Int = Priority.PRIORITY_HIGH_ACCURACY): LocationRequest {
        val locationRequest = LocationRequest.Builder(period)
            .setMinUpdateIntervalMillis(period)
            .setPriority(priority)
            .build()

        return locationRequest
    }

    private fun buildCallback (callback: (location: LatLngPoint?) -> Unit): LocationCallback {
        return object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val l = locationResult.lastLocation

                callback(l?.toData())
            }
        }
    }

    companion object {
        const val TAG = "LocationRepository"
    }
}

fun Location.toData(): LatLngPoint {
    return LatLngPoint(
        this.latitude,
        this.longitude
    )
}