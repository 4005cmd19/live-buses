package com.cmd.myapplication

import android.app.Application
import android.util.Log
import com.cmd.myapplication.data.Locality
import com.cmd.myapplication.data.repositories.BusLinesRepository
import com.cmd.myapplication.data.repositories.BusRoutesRepository
import com.cmd.myapplication.data.repositories.BusStopsRepository
import com.cmd.myapplication.data.repositories.DeviceLocationRepository
import com.cmd.myapplication.data.repositories.PlacesDataSource
import com.cmd.myapplication.data.repositories.RemoteDataSource
import com.cmd.myapplication.data.repositories.SearchRepository
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.SearchByTextRequest

class App : Application() {
    lateinit var deviceLocationRepository: DeviceLocationRepository

    lateinit var remoteDataSource: RemoteDataSource
    lateinit var placesDataSource: PlacesDataSource

    lateinit var busLinesRepository: BusLinesRepository
    lateinit var busStopsRepository: BusStopsRepository
    lateinit var busRoutesRepository: BusRoutesRepository

    lateinit var searchRepository: SearchRepository

    override fun onCreate() {
        super.onCreate()

        deviceLocationRepository = DeviceLocationRepository(this)

        remoteDataSource = RemoteDataSource()
        placesDataSource = PlacesDataSource(this)

        busLinesRepository = BusLinesRepository(remoteDataSource)
        busStopsRepository = BusStopsRepository(remoteDataSource)
        busRoutesRepository = BusRoutesRepository(remoteDataSource)

        searchRepository = SearchRepository(placesDataSource)

        Places.initializeWithNewPlacesApiEnabled(
            applicationContext,
            "AIzaSyBVnEB4WLSxuZ-DLPzB42OBz0heU5J7OHo"
        )
        val client = Places.createClient(this)
        val fields = listOf(Place.Field.ID, Place.Field.NAME)
        val sw = Locality.COVENTRY.location.southwest.let { LatLng(it.lat, it.lng) }
        val ne = Locality.COVENTRY.location.northeast.let { LatLng(it.lat, it.lng) }

        val searchRequest = SearchByTextRequest.builder("coventry university", fields).apply {
            maxResultCount = 10
            locationRestriction = RectangularBounds.newInstance(sw, ne)
        }.build()

        client.searchByText(searchRequest).apply {
            addOnSuccessListener {
                val places = it.places
                places.joinToString {
                    "(name=${it.name} location=${it.latLng})"
                }.also { Log.e("PLACES", it) }
            }
        }

        //supplyTestData()
    }

    override fun onTerminate() {
        super.onTerminate()
    }
}