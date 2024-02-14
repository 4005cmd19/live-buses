package com.cmd.myapplication.data

data class LatLngPoint (
    var lat: Double,
    var lng: Double
)

data class LocationRect (
    val southwest: LatLngPoint,
    val northeast: LatLngPoint
)