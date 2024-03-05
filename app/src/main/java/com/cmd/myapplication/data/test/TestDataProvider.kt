package com.cmd.myapplication.data.test

import android.util.Log
import com.cmd.myapplication.data.BusLine
import com.cmd.myapplication.data.BusLineRoute
import com.cmd.myapplication.data.BusLineRoutes
import com.cmd.myapplication.data.BusStop
import com.cmd.myapplication.data.LatLngPoint
import com.cmd.myapplication.data.Locality
import com.cmd.myapplication.data.viewModels.BusLinesViewModel
import com.cmd.myapplication.data.viewModels.BusRoutesViewModel
import com.cmd.myapplication.data.viewModels.BusStopsViewModel
import kotlin.random.Random
import kotlin.random.nextInt

class TestDataProvider(
    private val busStopsViewModel: BusStopsViewModel,
    private val busLinesViewModel: BusLinesViewModel,
    private val busRoutesViewModel: BusRoutesViewModel,
) {
    companion object {
        const val TAG = "TestDataProvider"
    }

    fun publishTestData(stops: Set<BusStop>, lines: Set<BusLine>, routes: Set<BusLineRoutes>) {
        busStopsViewModel.debugSet(stops)
        busLinesViewModel.debugSet(lines)
        busRoutesViewModel.debugSet(routes)
    }

    fun generateTestData(): Triple<Set<BusStop>, Set<BusLine>, Set<BusLineRoutes>> {
        val stops = generateBusStops(20)
        val lines = generateBusLines(20)
        val routes = generateBusRoutes(lines.map { it.id }.toSet())

        stops.forEach { stop ->
            val selectedLines = mutableSetOf<BusLine>()
            val linesCount = Random.nextInt(1..7)

            while (selectedLines.size < linesCount) {
                val l = lines.random()

                if (!selectedLines.contains(l)) {
                    selectedLines.add(l)

                    l.stops = l.stops.toMutableSet().apply { add(stop.id) }
                    l.routes = routes.find { it.lineId == l.id }?.routes?.map { it.id }?.toSet()
                        ?: emptySet()
                }
            }

            stop.lines = selectedLines.map { it.id }.toSet()
        }

        return Triple(
            stops.toSet(),
            lines.toSet(),
            routes.toSet()
        )
    }

    private fun generateBusStops(count: Int = 10, prefix: String = "Stop "): MutableSet<BusStop> {
        val generated = mutableSetOf<BusStop>()

        for (i in 0..<count) {
            val name = "${prefix}${i}"

            val stop = BusStop(
                "stopId$i",
                "stopCode$i",
                name,
                randomLocation(),
                emptySet()
            )

            generated.add(stop)
        }

        return generated
    }

    private fun generateBusLines(count: Int = 10): MutableSet<BusLine> {
        val generated = mutableSetOf<BusLine>()

        generated.addAll(
            arrayOf(
                BusLine("lineId0", "X10", setOf(), setOf("stopId1", "stopId3"), setOf()),
                BusLine("lineId1", "2A", setOf(), setOf("stopId2", "stopId4"), setOf()),
                BusLine("lineId2", "2", setOf(), setOf("stopId0"), setOf()),
                BusLine("lineId3", "9X", setOf(), setOf("stopId6", "stopId7"), setOf()),
                BusLine("lineId4", "4W", setOf(), setOf("stopId1"), setOf()),
                BusLine("lineId5", "Y2", setOf(), setOf("stopId5"), setOf()),
                BusLine("lineId6", "91", setOf(), setOf("stopId2", "stopId3"), setOf()),
                BusLine("lineId7", "134", setOf(), setOf("stopId1", "stopId7"), setOf()),
                BusLine("lineId8", "43", setOf(), setOf("stopId4"), setOf()),
                BusLine("lineId9", "W10", setOf(), setOf("stopId5", "stopId8"), setOf()),
                BusLine("lineId10", "69", setOf(), setOf("stopId6"), setOf()),
                BusLine("lineId11", "261", setOf(), setOf("stopId5", "stopId8"), setOf()),
            )
        )

        var s = ""
        val r = ('A'..'Z').toMutableList()

        Log.e(TAG, "r - $r")

        for (i in 0..<count - generated.size) {
            val hasLetter = Random.nextBoolean()
            val number = Random.nextInt(1..99)

            val letter = if (hasLetter) ('A'..'Z').random() else ""

            val name = "$letter$number"

            val stop = BusLine(
                "lineId$i",
                name,
                emptySet(),
                emptySet(),
                emptySet()
            )

            generated.add(stop)
        }

        return generated
    }

    private fun generateBusRoutes(lines: Set<String>, count: Int = 10): MutableSet<BusLineRoutes> {
        val generated = mutableSetOf<BusLineRoutes>()

        generated.addAll(lines.map {
            BusLineRoutes(
                it, setOf(
                    BusLineRoute(
                        "routeId0",
                        "Muswell Hill to Archway",
                        "0",
                        "Muswell Hill Broadway",
                        "0",
                        "Archway Station",
                        BusLineRoute.Direction.OUTBOUND,
                        arrayOf()
                    ),
                    BusLineRoute(
                        "routeId1",
                        "Archway to Muswell Hill",
                        "0",
                        "Archway Station",
                        "0",
                        "Muswell Hill Broadway",
                        BusLineRoute.Direction.INBOUND,
                        arrayOf()
                    ),
                    BusLineRoute(
                        "routeId2",
                        "Muswell Hill to North Finchley",
                        "0",
                        "Muswell Hill Broadway",
                        "0",
                        "Woodhouse College",
                        BusLineRoute.Direction.OUTBOUND,
                        arrayOf()
                    ),
                    BusLineRoute(
                        "routeId3",
                        "North Finchely to Muswell Hill",
                        "0",
                        "Woodhouse College",
                        "0",
                        "Muswell Hill Broadway",
                        BusLineRoute.Direction.INBOUND,
                        arrayOf()
                    ),
                )
            )
        })

        return generated
    }

    private fun randomLocation(): LatLngPoint {
        val bounds = Locality.COVENTRY.location

        val rLat = Random.nextDouble(bounds.southwest.lat, bounds.northeast.lat)
        val rLng = Random.nextDouble(bounds.southwest.lng, bounds.northeast.lng)

        return LatLngPoint(rLat, rLng)
    }
}