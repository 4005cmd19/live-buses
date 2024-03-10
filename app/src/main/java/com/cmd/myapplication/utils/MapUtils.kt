package com.cmd.myapplication.utils

import com.cmd.myapplication.data.LatLngPoint
import com.cmd.myapplication.data.Orientation
import com.google.android.gms.maps.model.LatLng

object MapUtils {
    fun calculateBearing(orientation: Orientation) =
        Math.toDegrees(((orientation.minusZ + orientation.y) / 2).toDouble())
            .toFloat()

    fun LatLngPoint.toLatLng() = LatLng(lat, lng)

    fun LatLng.toLatLngPoint() = LatLngPoint(latitude, longitude)

    object ZoomLevel {
        const val CITY = 10f
        const val STREET = 15f
        const val BUILDINGS = 20f

        const val DEFAULT = STREET
        const val FOCUSED = BUILDINGS
    }
}