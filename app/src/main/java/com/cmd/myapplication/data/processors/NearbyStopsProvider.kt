package com.cmd.myapplication.data.processors

import com.cmd.myapplication.data.BusStop
import com.cmd.myapplication.data.LatLngPoint
import com.cmd.myapplication.data.LatLngRect
import kotlin.math.abs
import kotlin.math.sqrt

fun Collection<BusStop>.inArea(
    area: LatLngRect,
    max: Int = -1,
): List<BusStop> {
    val s = mutableSetOf<BusStop>()

    for (stop in this) {
        val lat = stop.location.lat
        val lng = stop.location.lng

        if (
            lat >= area.southwest.lat
            && lat <= area.northeast.lat
            && lng >= area.southwest.lng
            && lng <= area.northeast.lng
        ) {
            s.add(stop)
        }

        if (s.size == max) {
            break
        }
    }

    return s.toList()
}

fun Collection<BusStop>.closest(
    location: LatLngPoint,
    max: Int = -1,
): List<BusStop> {
    val ds = this.associateWith {
        val dx = abs(
            it.location.lat - location.lat
        )

        val dy = abs(
            it.location.lng - location.lng
        )

        sqrt(dx * dx + dy * dy)
    }

    val sorted = ds.toList().sortedBy { it.second }.toMap()

    if (max > -1) {
        return sorted.toList().take(max).map { it.first }
    }

    return sorted.toList().map { it.first }
}

fun Array<BusStop>.inArea(area: LatLngRect, max: Int = -1) =
    this.toList().inArea(area, max)

fun Array<BusStop>.closest(location: LatLngPoint, max: Int = -1) =
    this.toList().closest(location, max)