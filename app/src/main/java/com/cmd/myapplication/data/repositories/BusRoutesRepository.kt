package com.cmd.myapplication.data.repositories

import android.annotation.SuppressLint
import com.google.gson.Gson

/*
MAX_MQTT: 268,435,456 b

buses/routes/{id}
buses/nearby/

 */

@SuppressLint("MissingPermission")
class BusRoutesRepository(
    private val remoteDataSource: RemoteDataSource
) {
    fun requestBusRoutes (stopId: String, callback: (busRoutes: BusRoutesRequestObject) -> Unit) {
        val topic = buildTopic(stopId)

        remoteDataSource.listenTo(topic) { topic, payload ->
            val busRoutes = Gson().fromJson(
                payload.toString(),
                BusRoutesRequestObject::class.java
            )

            if (busRoutes.isSatisfied) {
                remoteDataSource.stopListeningTo(topic)
            }

            callback(busRoutes)
        }
    }

    fun buildTopic (stopId: String): String {
        return TOPIC + stopId
    }

    companion object {
        const val TOPIC = "api/routes/"
    }
}

data class BusRoutesRequestObject (
    val routes: Array<BusRouteObject>
): RequestObject()

data class BusRouteObject (
    val lineId: Int,
    val lineName: String,
    val id: String,
    val routePoints: Array<LocationData>
)



data class BusRoutePoints (
    val snappedPoints: Array<SnappedPoint>?,
    val warningMessage: String?
)

data class SnappedPoint (
    val location: LatitudeLongitudeLiteral,
    val placeId: String,
    val originalIndex: Int?
)

data class LatitudeLongitudeLiteral (
    val latitude: Double,
    val longitude: Double
)