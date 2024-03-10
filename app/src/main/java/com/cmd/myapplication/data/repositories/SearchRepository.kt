package com.cmd.myapplication.data.repositories

import android.content.Context
import com.cmd.myapplication.data.LatLngPoint
import com.cmd.myapplication.data.Place
import com.cmd.myapplication.data.PlaceSearchResult
import com.cmd.myapplication.data.adapters.PlacesApiAdapter
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

class SearchRepository(
    private val context: Context,
    private val placesDataSource: PlacesApiAdapter,
) {
    private var placesCacheHandler = PlacesCacheHandler(TimeUnit.DAYS.toMillis(30))

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

    suspend fun search(query: String, location: LatLngPoint) = withContext(Dispatchers.IO) {
        return@withContext placesDataSource.search(query, location)
    }

    suspend fun processPlaceSearchResult(searchResult: PlaceSearchResult): Place? =
        withContext(Dispatchers.IO) {
            val cached = placesCacheHandler.tryGetPlace(searchResult.id)

            if (cached != null) {
                return@withContext cached
            }

            val location = placesDataSource.requestPlaceLocation(searchResult.id)

            return@withContext if (location != null) Place(
                searchResult.id,
                searchResult.address,
                searchResult.name,
                location
            ) else null
        }

    fun loadCache() {
        val cacheFile = File(context.cacheDir, CACHE_FILE)
        if (!cacheFile.exists()) {
            return
        }

        val s = cacheFile.readText()

        this.placesCacheHandler = PlacesCacheHandler.fromSearializable(s)
    }

    fun saveCache() {
        val cacheFile = File(context.cacheDir, CACHE_FILE)
        cacheFile.delete()

        File.createTempFile(CACHE_FILE, null, context.cacheDir)
    }

    private class PlacesCacheHandler(
        private val maxRetainMillis: Long,
    ) {
        private val cachedList = mutableListOf<CachedPlace>()

        fun cache(place: Place) {
            val timestamp = System.currentTimeMillis()

            cachedList.add(CachedPlace(place, timestamp))
        }

        fun tryGetPlace(id: String): Place? {
            val accessTime = System.currentTimeMillis()

            val placeCache = cachedList.firstOrNull { it.place.id == id }

            if (placeCache == null || accessTime - placeCache.timestamp > maxRetainMillis) {
                return null
            }

            return placeCache.place
        }

        fun toSerializable(): String {
            return Gson().toJson(object {
                val maxRetainMillis = this@PlacesCacheHandler.maxRetainMillis
                val cachedList = this@PlacesCacheHandler.cachedList
            })
        }

        data class CachedPlace(
            val place: Place,
            val timestamp: Long,
        )

        companion object {
            fun fromSearializable(s: String): PlacesCacheHandler {
                // gson doesn't work with kotlin data classes
                // use jsonparser

                val serializedObject = JsonParser.parseString(s).asJsonObject
                val maxRetainMillis = serializedObject.get("maxRetainMillis").asLong

                val deserializedCacheList = serializedObject.get("cachedList").asJsonArray.map {
                    val serializedCachedPlace = it.asJsonObject
                    val serializedPlace = serializedCachedPlace.get("place").asJsonObject
                    val timestamp = serializedCachedPlace.get("timestamp").asLong

                    val serializedLocation = serializedPlace.get("location").asJsonObject

                    val place = Place(
                        serializedPlace.get("id").asString,
                        serializedPlace.get("name").asString,
                        serializedPlace.get("address").asString,
                        LatLngPoint(
                            serializedLocation.get("lat").asDouble,
                            serializedLocation.get("lng").asDouble
                        )
                    )

                    CachedPlace(
                        place,
                        timestamp
                    )
                }

                return PlacesCacheHandler(maxRetainMillis).apply {
                    cachedList.addAll(deserializedCacheList)
                }
            }
        }
    }

    companion object {
        const val CACHE_FILE = "search/places.json"
    }
}