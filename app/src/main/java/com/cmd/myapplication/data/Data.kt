package com.cmd.myapplication.data

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

    NOT_FOUND (LatLngRect(LatLngPoint(0.0, 0.0), LatLngPoint(0.0, 0.0)))
}

data class LatLngPoint(
    var lat: Double,
    var lng: Double,
)

data class LatLngRect(
    val southwest: LatLngPoint,
    val northeast: LatLngPoint,
)

data class BusStop(
    var id: String,
    var code: String, // hub code
    var displayName: String,
    var location: LatLngPoint,
    var lines: Set<String>,
)

data class BusLine(
    var id: String,
    var displayName: String,
    var operators: Set<BusLineOperator>,
    var stops: Set<String>,
    var routes: Set<String>
)

data class BusLineRoutes (
    var lineId: String,
    var routes: Set<BusLineRoute>
)

data class BusLineRoute(
    var id: String,
    var name: String,
    
    var startId: String,
    var startName: String,

    var destinationId: String,
    var destinationName: String,

    var direction: Direction,
    var path: Array<String>
) {
    enum class Direction {
        INBOUND,
        OUTBOUND
    }
}

data class BusLineOperator(
    var id: String,
    var code: String,
    var name: String
)

data class BusArrival (
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

/*

/buses/lines/{line_id} -> BusLine
/buses/lines/{line_id}/routes -> BusLineRoutes

/buses/stops/{stop_id} -> BusStop
/buses/stops/{stop_id}/arrivals -> BusArrival

 */