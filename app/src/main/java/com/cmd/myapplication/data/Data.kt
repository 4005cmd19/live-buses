package com.cmd.myapplication.data

import kotlin.math.abs
import kotlin.math.sqrt

// NW 52.46307 -1.58341
// SE 52.36034 -1.40351

object LocalityLocation {
    fun forLocation(location: LatLngPoint): Locality {
        val filtered = Locality.entries.filter { isInBounds(location, it.location) }

        return if (filtered.isNotEmpty()) filtered.first() else Locality.NOT_FOUND
    }

    private fun isInBounds(what: LatLngPoint, bounds: LatLngRect): Boolean {
        return (what.lat >= bounds.southwest.lat && what.lat <= bounds.northeast.lat)
                && (what.lng >= bounds.southwest.lng && what.lng <= bounds.northeast.lng)
    }
}

enum class Locality(val location: LatLngRect) {
    COVENTRY(
        LatLngRect(
            LatLngPoint(52.36034, -1.58341),
            LatLngPoint(52.46307, -1.40351)
        )
    ),

    NOT_FOUND(LatLngRect(LatLngPoint(0.0, 0.0), LatLngPoint(0.0, 0.0)))
}

data class LatLngPoint(
    var lat: Double,
    var lng: Double,
) {
    override operator fun equals(other: Any?): Boolean {
        if (other !is LatLngPoint) {
            return false
        }

        return this.lat.toFloat() == other.lat.toFloat() && this.lng.toFloat() == other.lng.toFloat()
    }

    fun approxEquals(other: LatLngPoint?, epsilon: Double = 1e-2): Boolean {
        if (other == null) {
            return false
        }

        return abs(other - this) <= epsilon
    }

    operator fun minus(latLngPoint: LatLngPoint): Double {
        val dx = abs(this.lat - latLngPoint.lat)
        val dy = abs(this.lng - latLngPoint.lng)

        return sqrt(dx * dx + dy * dy)
    }
}

data class LatLngRect(
    val southwest: LatLngPoint,
    val northeast: LatLngPoint,
) {
    operator fun contains(latLngPoint: LatLngPoint): Boolean =
        this.southwest.lat >= latLngPoint.lat
                && this.northeast.lat <= latLngPoint.lat
                && this.southwest.lng >= latLngPoint.lng
                && this.northeast.lng <= latLngPoint.lng

    override operator fun equals(other: Any?): Boolean {
        if (other !is LatLngRect) {
            return false
        }

        return southwest == other.southwest && northeast == other.northeast
    }

    fun approxEquals(other: LatLngRect?) =
        southwest.approxEquals(other?.southwest) && northeast.approxEquals(other?.northeast)

    val center: LatLngPoint
        get() = LatLngPoint(
            northeast.lat - southwest.lat,
            northeast.lng - southwest.lng
        )
}

/**
 * Implemented by data classes that can be returned by a search in [SearchViewModel]
 */
interface SearchResult

interface Meta {
    val size: Int
}

data class BusStop(
    var id: String,
    var code: String, // hub code
    var displayName: String,
    var location: LatLngPoint,
    var lines: Set<String>,
): SearchResult

data class BusLine(
    var id: String,
    var displayName: String,
    var operators: Set<BusLineOperator>,
    var stops: Set<String>,
    var routes: Set<String>,
)

data class BusLineRoutes(
    var lineId: String,
    var routes: Set<BusLineRoute>,
): SearchResult

data class BusLineRoute(
    var id: String,
    var name: String,

    var startId: String,
    var startName: String,

    var destinationId: String,
    var destinationName: String,

    var direction: Direction,
    var path: Array<String>,
) {
    enum class Direction {
        INBOUND,
        OUTBOUND
    }
}

data class BusLineOperator(
    var id: String,
    var code: String,
    var name: String,
)

data class BusArrival(
    var destinationId: String,
    var destinationName: String,
    var stopId: String,
    var stopName: String,
    var lineId: String,
    var lineName: String,
    var direction: BusLineRoute.Direction,

    var scheduledTime: Long,
    var expectedTime: Long,
)

data class StopSearchResult(
    var stopId: String,
    var lineIds: List<String>,
) : SearchResult

data class BusLineRouteSearchResult(
    var line: BusLine,
    var route: BusLineRoute,
) : SearchResult

data class PlaceSearchResult(
    val id: String,
    val name: String,
    val address: String,
    val distance: Int,
) : SearchResult

data class Place (
    val id: String,
    val name: String,
    val address: String,
    val location: LatLngPoint,
): SearchResult

data class BusStopMetadata (
    override val size: Int
): Meta

data class BusLineMetadata (
    override val size: Int
): Meta

data class BusLineRoutesMetadata (
    override val size: Int
): Meta

data class BusArrivalMetadata(
    override val size: Int
): Meta

/*

/buses/lines/{line_id} -> BusLine
/buses/lines/{line_id}/routes -> BusLineRoutes

/buses/stops/{stop_id} -> BusStop
/buses/stops/{stop_id}/arrivals -> BusArrival

 */