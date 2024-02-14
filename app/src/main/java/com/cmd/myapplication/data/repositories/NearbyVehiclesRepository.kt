package com.cmd.myapplication.data.repositories

import android.annotation.SuppressLint
import com.google.gson.GsonBuilder

@SuppressLint("MissingPermission")
class NearbyVehiclesRepository(
    private val remoteDataSource: RemoteDataSource,
) {
    private fun requiredNearbyStops(location: LocationData) {
        val request = Topic.buildStopsRequest(location)

        remoteDataSource.request(Topic.STOPS, request)
    }

    private fun buildTopic () {

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

