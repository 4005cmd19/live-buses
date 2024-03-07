package com.cmd.myapplication.data.adapters

import android.app.Application
import android.util.Log
import com.cmd.myapplication.data.LatLngPoint
import com.cmd.myapplication.data.LatLngRect
import com.cmd.myapplication.data.PlaceSearchResult
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.SearchByTextRequest
import com.google.gson.Gson
import java.net.HttpURLConnection
import java.net.URL

import com.cmd.myapplication.data.Place as PlaceData

class PlacesApiAdapter(
    application: Application,
) {
    private val client: PlacesClient

    init {
        Places.initializeWithNewPlacesApiEnabled(application.applicationContext, API_KEY)
        client = Places.createClient(application.applicationContext)
    }

    fun autocompleteSearch(
        query: String,
        location: LatLngPoint,
        token: AutocompleteSessionToken,
    ): List<PlaceSearchResult> {
        val request = FindAutocompletePredictionsRequest.builder().apply {
            origin = location.toLatLng()
            countries = listOf("GB")
            sessionToken = token
            this.query = query
        }.build()

        return Tasks.await(client.findAutocompletePredictions(request)).autocompletePredictions.map {
            PlaceSearchResult(
                it.placeId,
                it.getPrimaryText(null).toString(),
                it.getSecondaryText(null).toString(),
                it.distanceMeters ?: 0
            )
        }
    }

    fun requestPlaceLocation(id: String): LatLngPoint? {
        val request = FetchPlaceRequest.builder(
            id,
            listOf(Place.Field.LAT_LNG),
        ).build()

        return Tasks.await(client.fetchPlace(request))?.place?.latLng?.let {
            LatLngPoint(it.latitude, it.longitude)
        }
    }

    fun search(query: String): List<PlaceData> {
        val request = SearchByTextRequest.builder(
            query,
            listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
        ).build()

        return Tasks.await(client.searchByText(request))?.places?.map {
            PlaceData(
                it.id!!,
                it.name!!,
                it.address!!,
                it.latLng!!.let { LatLngPoint(it.longitude, it.longitude) }
            )
        } ?: emptyList()
    }

    @Deprecated("Use search")
    fun doTextQuery(query: String) {
        val connection =
            URL(TEXT_QUERY_URL).openConnection().let { it as HttpURLConnection }.apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("content-type", "application/json")
                setRequestProperty("X-Goog-Api-Key", API_KEY)
                setRequestProperty(
                    "X-Goog-FieldMask",
                    "places.id,places.displayName,places.formattedAddress,places.location"
                )

                val requestData = createTextQuery(query)

                outputStream.write(requestData.toByteArray())
                outputStream.flush()
                outputStream.close()
            }

        if (connection.errorStream != null) {
            Log.e(TAG, "error - ${String(connection.errorStream.readBytes())}")
        }

        val result = connection.inputStream.readBytes()
        Log.e(TAG, "result - ${String(result, Charsets.UTF_8)}")
    }

    private fun createTextQuery(query: String) = Gson().toJson(object {
        val textQuery = query
    })

    companion object {
        const val TAG = "PlacesDataSource"

        const val TEXT_QUERY_URL = "https://places.googleapis.com/v1/places:searchText"
        const val API_KEY = "AIzaSyBVnEB4WLSxuZ-DLPzB42OBz0heU5J7OHo"

        private fun LatLngPoint.toLatLng() = LatLng(lat, lng)

        private fun LatLngRect.toRectangularBounds() = RectangularBounds.newInstance(
            southwest.toLatLng(),
            northeast.toLatLng()
        )
    }
}