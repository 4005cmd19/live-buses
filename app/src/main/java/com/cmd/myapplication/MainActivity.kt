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
import com.cmd.myapplication.data.viewModels.BusLinesViewModel
import com.cmd.myapplication.data.viewModels.BusStopsViewModel
import com.cmd.myapplication.data.viewModels.DeviceLocationViewModel
import com.cmd.myapplication.data.viewModels.NearbyBusesViewModel
import com.cmd.myapplication.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"

        const val PERMISSION_REQUEST_CODE = 1
    }

    val deviceLocationViewModel: DeviceLocationViewModel by viewModels { DeviceLocationViewModel.Factory }
    val busStopsViewModel: BusStopsViewModel by viewModels { BusStopsViewModel.Factory }
    val busLinesViewModel: BusLinesViewModel by viewModels { BusLinesViewModel.Factory }
    val nearbyBusesViewModel: NearbyBusesViewModel by viewModels { NearbyBusesViewModel.Factory }

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    private lateinit var locationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

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
}