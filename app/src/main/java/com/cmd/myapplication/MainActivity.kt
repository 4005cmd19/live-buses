package com.cmd.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.cmd.myapplication.data.BusLine
import com.cmd.myapplication.data.BusLineRoute
import com.cmd.myapplication.data.BusLineRoutes
import com.cmd.myapplication.data.BusStop
import com.cmd.myapplication.data.LatLngPoint
import com.cmd.myapplication.data.Locality
import com.cmd.myapplication.data.viewModels.BusLinesViewModel
import com.cmd.myapplication.data.viewModels.BusRoutesViewModel
import com.cmd.myapplication.data.viewModels.BusStopsViewModel
import com.cmd.myapplication.data.viewModels.DeviceLocationViewModel
import com.cmd.myapplication.data.viewModels.NearbyBusesViewModel
import com.cmd.myapplication.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"

        const val PERMISSION_REQUEST_CODE = 1
    }

    val deviceLocationViewModel: DeviceLocationViewModel by viewModels { DeviceLocationViewModel.Factory }
    val busStopsViewModel: BusStopsViewModel by viewModels { BusStopsViewModel.Factory }
    val busLinesViewModel: BusLinesViewModel by viewModels { BusLinesViewModel.Factory }
    val nearbyBusesViewModel: NearbyBusesViewModel by viewModels { NearbyBusesViewModel.Factory }
    private val busRoutesViewModel: BusRoutesViewModel by viewModels { BusRoutesViewModel.Factory }

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    private lateinit var locationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        provideTestData()

        // get view
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        navController = NavHostFragment.findNavController(
            supportFragmentManager.findFragmentById(R.id.fragment_container)!!
        )

        if (hasPermissions()) {
            navigateToMainFragment()
        }

        val content: View = findViewById(android.R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    if (hasPermissions()) {
                        val currentFragmentId = navController.currentDestination?.id

                        if (currentFragmentId == R.id.mainFragment) {
                            content.viewTreeObserver.removeOnPreDrawListener(this)
                            return true
                        }
                    } else {
                        content.viewTreeObserver.removeOnPreDrawListener(this)
                        return true
                    }

                    return false
                }
            }
        )

        deviceLocationViewModel.currentLocation.observe(this) {
            nearbyBusesViewModel.location = it
        }

        busStopsViewModel.busStops.observe(this) {
            nearbyBusesViewModel.busStops = it
        }
    }

    private fun hasPermissions(): Boolean {
        return (ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PERMISSION_GRANTED)
                && (ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PERMISSION_GRANTED)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        Log.e(TAG, "permissions - $requestCode")

        permissions.forEachIndexed { i, it ->
            Log.e(TAG, "$it -> ${grantResults[i]}")
        }

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PERMISSION_GRANTED }) {
                // has all permissions, swap fragments
                navigateToMainFragment()
            }
        }
    }

    private fun navigateToMainFragment() {
        navController.navigate(NeedsPermissionsFragmentDirections.actionNeedsPermissionsFragmentToMainFragment())
    }

    @Deprecated("Use nav controller")
    private fun showMainFragment() {
        val mainFragment = MainFragment()

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, mainFragment)
            .commitNow()
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates(
        client: FusedLocationProviderClient,
        callback: () -> Unit = {},
    ) {
        val locationRequest = LocationRequest.Builder(10000)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                Log.w(TAG, "locations - ${result.locations.size}")

                for (location in result.locations) {
                    Log.w(TAG, "location - ${location.latitude} @${location.time}")
                }
            }
        }

        client.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
    }

    private var hasTestData = false

    private fun provideTestData() {
        if (hasTestData) return
        hasTestData = true

        val stops = setOf(
            BusStop(
                "stopId0",
                "stopCode0",
                "Stop 1",
                randomLocation(),
                setOf("lineId2", "lineId5")
            ),
            BusStop(
                "stopId1",
                "stopCode1",
                "Stop 2",
                randomLocation(),
                setOf("lineId0", "lineId4", "lineId7")
            ),
            BusStop(
                "stopId2",
                "stopCode2",
                "Stop 3",
                randomLocation(),
                setOf("lineId1", "lineId6")
            ),
            BusStop(
                "stopId3",
                "stopCode3",
                "Stop 4",
                randomLocation(),
                setOf("lineId0", "lineId6")
            ),
            BusStop(
                "stopId4",
                "stopCode4",
                "Stop 5",
                randomLocation(),
                setOf("lineId8", "lineId1")
            ),
            BusStop(
                "stopId5",
                "stopCode5",
                "Stop 6",
                randomLocation(),
                setOf("lineId11", "lineId9")
            ),
            BusStop(
                "stopId6",
                "stopCode6",
                "Stop 7",
                randomLocation(),
                setOf("lineId3", "lineId10")
            ),
            BusStop(
                "stopId7",
                "stopCode7",
                "Stop 8",
                randomLocation(),
                setOf("lineId7", "lineId3")
            ),
            BusStop(
                "stopId8",
                "stopCode8",
                "Stop 9",
                randomLocation(),
                setOf("lineId9", "lineId11")
            ),
        )

        val lines = setOf(
            BusLine("lineId0", "X10", setOf(), setOf(""), setOf("stopId1", "stopId3")),
            BusLine("lineId1", "2A", setOf(), setOf(""), setOf("stopId2", "stopId4")),
            BusLine("lineId2", "2", setOf(), setOf(""), setOf("stopId0")),
            BusLine("lineId3", "9X", setOf(), setOf(""), setOf("stopId6", "stopId7")),
            BusLine("lineId4", "4W", setOf(), setOf(""), setOf("stopId1")),
            BusLine("lineId5", "Y2", setOf(), setOf(""), setOf("stopId5")),
            BusLine("lineId6", "91", setOf(), setOf(""), setOf("stopId2", "stopId3")),
            BusLine("lineId7", "134", setOf(), setOf(""), setOf("stopId1", "stopId7")),
            BusLine("lineId8", "43", setOf(), setOf(""), setOf("stopId4")),
            BusLine("lineId9", "W10", setOf(), setOf(""), setOf("stopId5", "stopId8")),
            BusLine("lineId10", "69", setOf(), setOf(""), setOf("stopId6")),
            BusLine("lineId11", "261", setOf(), setOf(""), setOf("stopId5", "stopId8")),
        )

        val routes =
            lines.map {
                val lineRoutes = BusLineRoutes(
                    it.id, setOf(
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

                val routeIds = lineRoutes.routes.map { it.id }.toSet()

                it.routes = routeIds

                lineRoutes
            }.toSet()

        busStopsViewModel.debugSet(stops)
        busLinesViewModel.debugSet(lines)
        busRoutesViewModel.debugSet(routes)
    }

    private fun randomLocation(): LatLngPoint {
        val bounds = Locality.COVENTRY.location

        val rLat = Random.nextDouble(bounds.southwest.lat, bounds.northeast.lat)
        val rLng = Random.nextDouble(bounds.southwest.lng, bounds.northeast.lng)

        return LatLngPoint(rLat, rLng)
    }
}