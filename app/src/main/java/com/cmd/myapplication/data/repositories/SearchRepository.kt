package com.cmd.myapplication.data.repositories

import com.cmd.myapplication.data.LatLngPoint
import com.cmd.myapplication.data.Place
import com.cmd.myapplication.data.PlaceSearchResult
import com.cmd.myapplication.data.adapters.PlacesApiAdapter
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SearchRepository(
    private val placesDataSource: PlacesApiAdapter,
) {
    private val searchToken: AutocompleteSessionToken = AutocompleteSessionToken.newInstance()

    suspend fun autocompleteSearch(
        query: String,
        location: LatLngPoint,
    ): List<PlaceSearchResult> {
        return withContext(Dispatchers.IO) {
            placesDataSource.autocompleteSearch(
                query,
                location,
                searchToken
            )
        }
    }

    suspend fun search (query: String) = withContext(Dispatchers.IO) {
        return@withContext placesDataSource.search(query)
    }

    suspend fun processPlaceSearchResult (searchResult: PlaceSearchResult): Place? = withContext(Dispatchers.IO) {
        val location = placesDataSource.requestPlaceLocation(searchResult.id)

        return@withContext if (location != null) Place(
            searchResult.id,
            searchResult.address,
            searchResult.name,
            location
        ) else null
    }

    suspend fun test() {
        withContext(Dispatchers.IO) {

        }
    }
}