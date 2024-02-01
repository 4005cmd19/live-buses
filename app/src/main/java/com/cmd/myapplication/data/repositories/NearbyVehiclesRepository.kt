package com.cmd.myapplication.data.repositories

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import android.os.Looper
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.cmd.myapplication.data.repositories.DeviceLocationRepository.LocationData
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.OnTokenCanceledListener
import com.google.gson.Gson
import com.google.gson.GsonBuilder

@SuppressLint("MissingPermission")
class NearbyVehiclesRepository(
    private val remoteDataSource: RemoteDataSource,
) {
    private fun requiredNearbyStops(location: LocationData) {
        val request = Topic.buildStopsRequest(location)

        remoteDataSource.request(Topic.STOPS, request)
    }

    object Topic {
        const val STOPS = "nearby/stops"
        const val ROUTES = "nearby/routes"
        const val VEHICLES = "nearby/vehicles"

        const val REQUEST_TTL = 2 * 60 * 1000

        private val gson = GsonBuilder()
            .setPrettyPrinting()
            .create()

        fun buildStopsRequest(locationData: LocationData): ByteArray {
            val (latitude, longitude) = locationData

            val request = object {
                val state = "state:unsatisfied"
                val ttl = REQUEST_TTL
                val type = STOPS
                val location = arrayOf(latitude, longitude)
            }

            val json = gson.toJson(request)

            return json.toByteArray()
        }
    }
}