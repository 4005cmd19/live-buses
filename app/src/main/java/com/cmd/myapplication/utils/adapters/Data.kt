package com.cmd.myapplication.utils.adapters

data class BusData(
    val stopId: String,
    val lineId: String,

    val line: CharSequence,
    val stop: CharSequence,
    val destination: CharSequence,
    val arrivalTime: String,
)

interface SearchResultData

data class BusStopData(
    val id: String,
    val name: CharSequence,
    val lines: List<String>,
) : SearchResultData

data class BusRouteData(
    val lineId: String,
    val routeId: String,

    val lineName: String,
    val routeName: String,

    val operatorName: String,
) : SearchResultData

data class PlaceData(
    val id: String,
    val shortName: String,
    val name: String,
) : SearchResultData