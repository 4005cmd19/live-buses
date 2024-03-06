package com.cmd.myapplication.data.repositories

import com.cmd.myapplication.data.LatLngPoint
import com.cmd.myapplication.data.LatLngRect
import com.cmd.myapplication.data.PlaceSearchResult
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SearchRepository(
    private val placesDataSource: PlacesDataSource,
) {
    private val searchToken: AutocompleteSessionToken = AutocompleteSessionToken.newInstance()

    suspend fun findNearbyPlaces(
        query: String,
        location: LatLngPoint,
        bounds: LatLngRect,
    ): Set<PlaceSearchResult> {
        return withContext(Dispatchers.IO) {
            placesDataSource.findNearbyPlaces(
                query,
                location,
                bounds,
                searchToken
            )
        }
    }

    suspend fun test() {
        withContext(Dispatchers.IO) {

        }
    }
}