package com.cmd.myapplication.data.test

import com.cmd.myapplication.data.BusLine
import com.cmd.myapplication.data.BusLineOperator
import com.cmd.myapplication.data.BusLineRoute
import com.cmd.myapplication.data.BusLineRoutes
import com.cmd.myapplication.data.BusStop
import com.cmd.myapplication.data.LatLngPoint
import com.cmd.myapplication.data.Locality
import com.google.gson.Gson
import kotlin.random.Random
import kotlin.random.nextInt

class TestDataProvider {
    companion object {
        const val TAG = "TestDataProvider"
    }

    fun publishToClient(
        client: TestClient,
        stops: Set<BusStop>,
        lines: Set<BusLine>,
        routes: Set<BusLineRoutes>,
    ) {
        for (line in lines) {
            val id = line.id
            val payload = Gson().toJson(line).toByteArray()

            client.publish("buses/lines/${id}", payload, true)
        }

        for (stop in stops) {
            val id = stop.id
            val payload = Gson().toJson(stop).toByteArray()

            client.publish("buses/stops/${id}", payload, true)
        }

        for (route in routes) {
            val lineId = route.lineId
            val payload = Gson().toJson(route).toByteArray()

            client.publish("buses/lines/${lineId}/routes", payload, true)
        }

        client.publish("meta/lines", Gson().toJson(object {
            val size = lines.size
        }).toByteArray(), true)

        client.publish("meta/stops", Gson().toJson(object {
            val size = stops.size
        }).toByteArray(), true)
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
                BusLine("lineId0", "X10", setOf(), setOf(), setOf()),
                BusLine("lineId1", "2A", setOf(), setOf(), setOf()),
                BusLine("lineId2", "2", setOf(), setOf(), setOf()),
                BusLine("lineId3", "9X", setOf(), setOf(), setOf()),
                BusLine("lineId4", "4W", setOf(), setOf(), setOf()),
                BusLine("lineId5", "Y2", setOf(), setOf(), setOf()),
                BusLine("lineId6", "91", setOf(), setOf(), setOf()),
                BusLine("lineId7", "134", setOf(), setOf(), setOf()),
                BusLine("lineId8", "43", setOf(), setOf(), setOf()),
                BusLine("lineId9", "W10", setOf(), setOf(), setOf()),
                BusLine("lineId10", "69", setOf(), setOf(), setOf()),
                BusLine("lineId11", "261", setOf(), setOf(), setOf()),
            )
        )

        for (i in generated.size..<count - generated.size) {
            val hasLetter = Random.nextBoolean()
            val number = Random.nextInt(1..99)

            val letter = if (hasLetter) ('A'..'Z').random().toString() else ""

            val name = "$letter$number"

            val line = BusLine(
                "lineId$i",
                name,
                setOf(BusLineOperator("idTfl", "codeTfl", "TfL")),
                emptySet(),
                emptySet()
            )

            generated.add(line)
        }

        return generated
    }

    private fun generateBusRoutes(lines: Set<String>): MutableSet<BusLineRoutes> {
        val generated = mutableSetOf<BusLineRoutes>()

        val possibleRoutes1 = listOf(
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
                "routeId4",
                "Muswell Hill to Highate",
                "0",
                "Muswell Hill",
                "0",
                "Highate",
                BusLineRoute.Direction.OUTBOUND,
                arrayOf()
            ),
            BusLineRoute(
                "routeId6",
                "Muswell Hill to Warren Street",
                "0",
                "Muswell Hill",
                "0",
                "Warren Street",
                BusLineRoute.Direction.OUTBOUND,
                arrayOf()
            )
        )

        val possibleRoutes2 = listOf(
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
                "routeId3",
                "North Finchely to Muswell Hill",
                "0",
                "Woodhouse College",
                "0",
                "Muswell Hill Broadway",
                BusLineRoute.Direction.INBOUND,
                arrayOf()
            ),
            BusLineRoute(
                "routeId5",
                "Highate to Muswell Hill",
                "0",
                "Highate",
                "0",
                "Muswell Hill",
                BusLineRoute.Direction.INBOUND,
                arrayOf()
            ),
            BusLineRoute(
                "routeId7",
                "Warren Street to Muswell Hill",
                "0",
                "Warren Street",
                "0",
                "Muswell Hill",
                BusLineRoute.Direction.INBOUND,
                arrayOf()
            )
        )

        generated.addAll(lines.map {
            val p1 = mutableListOf(*possibleRoutes1.toTypedArray())
            val i1 = Random.nextInt(0..p1.lastIndex)
            p1.removeAt(i1)
            val i2 = Random.nextInt(0..p1.lastIndex)

            BusLineRoutes(
                it, setOf(
                    possibleRoutes1[i1],
                    possibleRoutes2[i1],
                    possibleRoutes1[i2],
                    possibleRoutes2[i2]
                )
            )
        })

        return generated
    }

    private fun randomLocation(): LatLngPoint {
        val bounds = Locality.COVENTRY.bounds

        val rLat = Random.nextDouble(bounds.southwest.lat, bounds.northeast.lat)
        val rLng = Random.nextDouble(bounds.southwest.lng, bounds.northeast.lng)

        return LatLngPoint(rLat, rLng)
    }
}