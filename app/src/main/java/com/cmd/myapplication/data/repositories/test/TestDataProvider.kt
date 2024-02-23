package com.cmd.myapplication.data.repositories.test

import android.util.Log
import com.cmd.myapplication.data.BusArrival
import com.cmd.myapplication.data.BusLine
import com.cmd.myapplication.data.BusLineOperator
import com.cmd.myapplication.data.BusLineRoute
import com.cmd.myapplication.data.BusLineRoutes
import com.cmd.myapplication.data.BusStop
import com.cmd.myapplication.data.LatLngPoint
import com.cmd.myapplication.data.repositories.RemoteDataSource
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt3.Mqtt3BlockingClient
import java.io.InputStreamReader
import java.net.URL
import java.util.UUID

class TestDataProvider {
    private val client: Mqtt3BlockingClient = MqttClient.builder()
        .useMqttVersion3()
        .identifier(UUID.randomUUID().toString())
        .serverHost(RemoteDataSource.HOST)
        .serverPort(RemoteDataSource.PORT)
        .useSslWithDefaultConfig()
        .buildBlocking()

    init {
        client.connectWith()
            .simpleAuth()
            .username(RemoteDataSource.APP_ID)
            .password(RemoteDataSource.APP_KEY)
            .applySimpleAuth()
            .send()
    }

    fun supplyData (lines: Set<BusLine>, stops: Set<BusStop>, routes: Set<BusLineRoutes>) {
        for (line in lines.take(1)) {
            val payload = Gson().toJson(line)

            Log.e("PAYLOAD", payload)

            client.publishWith()
                .topic("buses/lines/${line.id}")
                .retain(true)
                .qos(MqttQos.AT_MOST_ONCE)
                .payload(payload.toByteArray())
                .send()
        }

        for (stop in stops.take(1)) {
            val payload = Gson().toJson(stop)

            Log.e("PAYLOAD", payload)

            client.publishWith()
                .topic("buses/stops/${stop.id}")
                .retain(true)
                .qos(MqttQos.AT_MOST_ONCE)
                .payload(payload.toByteArray())
                .send()
        }

        for (route in routes.take(1)) {
            val payload = Gson().toJson(route)

            Log.e("PAYLOAD", payload)

            client.publishWith()
                .topic("buses/lines/${route.lineId}/routes")
                .retain(true)
                .qos(MqttQos.AT_MOST_ONCE)
                .payload(payload.toByteArray())
                .send()
        }
    }
}

object Api {
    fun httpGet(endpoint: String, params: Map<String, Any> = mutableMapOf()): String {
        val p = with(params as MutableMap) {
            putAll(AUTH_PARAMS)
            map { "${it.key}=${it.value}" }
        }.joinToString("&")


        val addr = "$BASE_URL$endpoint?$p"

        val url = URL(addr)
        val conn = url.openConnection()
        conn.setRequestProperty("accept", "application/json")
        val inputStream = InputStreamReader(conn.getInputStream())

        return inputStream.readText()
    }

    fun getServiceData(): Triple<Set<BusLine>, Set<BusStop>, Set<BusLineRoutes>> {
        val successfulResponses = mutableListOf<String>()

        // val get all bus lines

        var finalBusLines = mutableSetOf<BusLine>()
        var finalBusStops = mutableSetOf<BusStop>()
        var finalBusRoutes = mutableSetOf<BusLineRoutes>()
        var finalBusArrivals = mutableSetOf<BusArrival>()

        try {
            val startTime = System.currentTimeMillis()
            val r = httpGet("/Line/Mode/bus%2Ccoach%2Cbus_or_coach/Route")
            val duration = System.currentTimeMillis() - startTime

            Log.e("TEST", "GET lines - duration=${duration}ms")

            val json = JsonParser.parseString(r).asJsonObject

            val _lines = json.get("ArrayOfLine").asJsonObject
                .get("Line").asJsonArray
                .forEach {
                    val lineId = it.asJsonObject
                        .get("Id").asString

                    val name = it.asJsonObject
                        .get("Name").asString

                    val operators = mutableListOf<BusLineOperator>()

                    val _operators = it.asJsonObject
                        .get("Operators").asJsonObject
                        .get("Operator").asJsonArray

                    _operators.forEach {
                        val name = it.asJsonObject
                            .get("Name").asString

                        val code = it.asJsonObject
                            .get("Code").asString

                        val id = it.asJsonObject
                            .get("Id").asString

                        operators.add(BusLineOperator(id, code, name))
                    }

                    val routes = mutableListOf<BusLineRoute>()

                    val _routes = it.asJsonObject
                        .get("RouteSections").asJsonObject
                        .get("MatchedRoute").asJsonArray

                    _routes.forEach {
                        val startId = it.asJsonObject
                            .get("Originator").asString

                        val startName = it.asJsonObject
                            .get("OriginationName").asString

                        val destinationId = it.asJsonObject
                            .get("Destination").asString

                        val destinationName = it.asJsonObject
                            .get("DestinationName").asString

                        val name = it.asJsonObject
                            .get("Name").asString

                        val id = it.asJsonObject
                            .get("RouteCode").asString

                        val direction = it.asJsonObject
                            .get("Direction").asString
                            .let { if (it == "inbound") BusLineRoute.Direction.INBOUND else BusLineRoute.Direction.OUTBOUND }

                        routes.add(
                            BusLineRoute(
                                id,
                                name,
                                startId,
                                startName,
                                destinationId,
                                destinationName,
                                direction,
                                emptyArray()
                            )
                        )
                    }

                    finalBusRoutes.add(BusLineRoutes(lineId, routes.toSet()))

                    finalBusLines.add(
                        BusLine(
                            lineId,
                            name,
                            operators.toSet(),
                            emptySet(),
                            routes.map { it.id }.toSet()
                        )
                    )
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        Log.e("TEST", "GET lines - size=${finalBusLines.size}")

        // get stops for each line

        var unpopulatedBusStops: MutableList<BusStop>


        for (i in 0..4) {
            val line = finalBusLines.toList()[i]

            // get stop points
            try {
                Log.e("TEST", "GET StopPoints - line=${line.id}")
                val r = httpGet("/Line/${line.id}/StopPoints")
                val json = JsonParser.parseString(r).asJsonObject

                val arr = json.get("ArrayOfStopPoint").asJsonObject
                    .get("StopPoint").asJsonArray
                    .map {
                        val id = it.asJsonObject
                            .get("Id").asString

                        val lat = it.asJsonObject
                            .get("Lat").asDouble

                        val lng = it.asJsonObject
                            .get("Lon").asDouble

                        val name = it.asJsonObject
                            .get("CommonName").asString

                        val code = it.asJsonObject
                            .get("HubNaptanCode").asString

                        val locality = it.asJsonObject
                            .get("Locality").asString

                        BusStop(id, code, name, LatLngPoint(lat, lng), emptySet())
                    }

                unpopulatedBusStops = arr.toMutableList()

                unpopulatedBusStops.forEach {
                    val l = it.lines.toMutableSet()
                    l.add(line.id)
                    it.lines = l
                }

                finalBusStops = unpopulatedBusStops.toMutableSet()

                line.stops = unpopulatedBusStops.map { it.id }.toSet()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // get path
            try {
                val r1 = httpGet("/Line/${line.id}/Route/Sequence/inbound")
                val r2 = httpGet("/Line/${line.id}/Route/Sequence/outbound")

                val json1 = JsonParser.parseString(r1).asJsonObject
                val json2 = JsonParser.parseString(r2).asJsonObject

                json1.get("RouteSequence").asJsonObject
                    .get("OrderedLineRoutes").asJsonObject
                    .get("OrderedRoute").asJsonArray
                    .forEach {
                        val id = it.asJsonObject
                            .get("RouteId").asString

                        val name = it.asJsonObject
                            .get("Name").asString

                        val codes = it.asJsonObject
                            .get("NaptanIds").asJsonObject
                            .get("string").asJsonArray.map { it.asString }
                            .toTypedArray()

                        val routesObject = finalBusRoutes.find {
                            it.lineId == line.id
                        }

                        if (routesObject != null) {
                            val route = routesObject.routes.find { id.contains(it.id) && it.name == name }

                            if (route != null) {
                                val path = route.path.toMutableList()
                                path.addAll(codes)
                                route.path = path.toTypedArray()
                            }
                        }
                    }

                json2.get("RouteSequence").asJsonObject
                    .get("OrderedLineRoutes").asJsonObject
                    .get("OrderedRoute").asJsonArray
                    .forEach {
                        val id = it.asJsonObject
                            .get("RouteId").asString

                        val name = it.asJsonObject
                            .get("Name").asString

                        val codes = it.asJsonObject
                            .get("NaptanIds").asJsonObject
                            .get("string").asJsonArray.map { it.asString }
                            .toTypedArray()

                        val routesObject = finalBusRoutes.find {
                            it.lineId == line.id
                        }

                        if (routesObject != null) {
                            val route = routesObject.routes.find { id.contains(it.id) && it.name == name }

                            if (route != null) {
                                val path = route.path.toMutableList()
                                path.addAll(codes)
                                route.path = path.toTypedArray()
                            }
                        }
                    }
            } catch (e: Exception) {

            }
        }

        Log.e("CHECK", "lines - ${finalBusLines.size}")

        val line = finalBusLines.first()

        with(line) {
            Log.e("TEST", "line - $id $displayName ${operators.size} ${stops.size}")
        }

        val l = finalBusLines.joinToString("\n") { it.displayName }
        Log.e("TEST", l)

        Log.e("TEST", finalBusLines.first().toString())
        Log.e("TEST", finalBusStops.first().toString())
        Log.e("TEST", finalBusRoutes.first().toString())

        // 1 + (34) + (2 * )

        return Triple(
            finalBusLines,
            finalBusStops,
            finalBusRoutes
        )
    }

    const val BASE_URL = "http://api.tfwm.org.uk"
    const val APP_ID = "bdb89e2f"
    const val APP_KEY = "ab6050821a1b294ce61fd8a010815456"
    val AUTH_PARAMS = mapOf(
        "app_id" to APP_ID,
        "app_key" to APP_KEY
    )
}