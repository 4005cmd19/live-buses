package com.cmd.myapplication.data.adapters

import android.app.Application
import com.cmd.myapplication.data.LatLngPoint
import com.cmd.myapplication.data.LatLngRect
import com.cmd.myapplication.data.Locality
import com.cmd.myapplication.data.LocalityBounds
import com.cmd.myapplication.data.PlaceSearchResult
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.PlaceTypes
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.SearchByTextRequest

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
        val bias = LocalityBounds.forLocation(location)

        val request = FindAutocompletePredictionsRequest.builder().apply {
            // set to receive distance from origin from the api
            // allows sorting by closest
            origin = location.toLatLng()

            // bias the api to give results for locations closest to the user first
            if (bias != Locality.NOT_FOUND) {
                locationBias = bias.bounds.toRectangularBounds()
            }

            // ensure results are in the uk
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

    fun search(query: String, location: LatLngPoint): List<PlaceSearchResult> {
        val bias = LocalityBounds.forLocation(location)

        val request = SearchByTextRequest.builder(
            query,
            listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG,
                Place.Field.TYPES
            )
        ).apply {
            maxResultCount = MAX_RESULT_COUNT

            if (bias != Locality.NOT_FOUND) {
                locationBias = bias.bounds.toRectangularBounds()
            }
        }.build()

        return Tasks.await(client.searchByText(request))?.places
            ?.filterNot {
                it.placeTypes?.contains(PlaceTypes.BUS_STATION) == true || it.placeTypes?.contains("bus_stop") == true || it.placeTypes?.contains(
                    PlaceTypes.TRANSIT_STATION
                ) == true
            }
            ?.map {
                PlaceSearchResult(
                    it.id!!,
                    it.name!!,
                    it.address!!,
                    -1
                )
            } ?: emptyList()
    }

    companion object {
        const val TAG = "PlacesApiAdapter"

        const val API_KEY = "AIzaSyBVnEB4WLSxuZ-DLPzB42OBz0heU5J7OHo"

        const val MAX_RESULT_COUNT = 20

        // convenience function - converts to google maps api LatLng
        private fun LatLngPoint.toLatLng() = LatLng(lat, lng)

        // convenience function - converts to google maps api RectangularBounds
        private fun LatLngRect.toRectangularBounds() = RectangularBounds.newInstance(
            southwest.toLatLng(),
            northeast.toLatLng()
        )
    }
}