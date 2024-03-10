package com.cmd.myapplication.data.repositories

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Looper
import android.util.Log
import com.cmd.myapplication.data.LatLngOrientation
import com.cmd.myapplication.data.LatLngPoint
import com.cmd.myapplication.data.Orientation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LastLocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import java.util.concurrent.CompletableFuture

@SuppressLint("MissingPermission")
class DeviceLocationRepository(application: Application) {
    private val locationClient: FusedLocationProviderClient
    private val sensorManager: SensorManager
    private val sensorAccelerometer: Sensor
    private val sensorMagneticField: Sensor

    private var locationCallback: LocationCallback? = null
    private var orientationCallback: SensorEventListener? = null

    private var lastLocation: LatLngPoint? = null
    private var lastOrientation: Orientation? = null

    init {
        Log.e(TAG, "app - ${application.applicationContext}")
        locationClient = LocationServices.getFusedLocationProviderClient(application)
        sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!!
        sensorMagneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)!!
    }

    suspend fun requestCurrentLocation(): LatLngOrientation = withContext(Dispatchers.IO) {
        return@withContext with(CompletableFuture<LatLngOrientation>()) {
            val rotationMatrix = FloatArray(9)
            val orientationAngles = FloatArray(3)

            val accelerometerReading = FloatArray(3)
            val magnetometerReading = FloatArray(3)

            var location: LatLngPoint? = null
            var orientation: Orientation? = null

            var isAccelerometerRead = false
            var isMagnetometerRead = false

            val callback = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val isValidResult = event.sensor.type == Sensor.TYPE_ACCELEROMETER
                            || event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD

                    if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                        System.arraycopy(
                            event.values,
                            0,
                            accelerometerReading,
                            0,
                            accelerometerReading.size
                        )
                        isAccelerometerRead = true
                    } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                        System.arraycopy(
                            event.values,
                            0,
                            magnetometerReading,
                            0,
                            magnetometerReading.size
                        )
                        isMagnetometerRead = true
                    }

                    if (isValidResult && isAccelerometerRead && isMagnetometerRead) {
                        sensorManager.unregisterListener(this)

                        SensorManager.getRotationMatrix(
                            rotationMatrix,
                            null,
                            accelerometerReading,
                            magnetometerReading
                        )
                        SensorManager.getOrientation(rotationMatrix, orientationAngles)

                        orientation = Orientation.fromFloatArray(orientationAngles)

                        if (location != null) {
                            complete(LatLngOrientation.from(location!!, orientation!!))
                        }
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

                }
            }

            val locationRequest = LastLocationRequest.Builder()
                .setGranularity(Granularity.GRANULARITY_FINE)
                .build()

            sensorManager.registerListener(
                callback,
                sensorAccelerometer,
                SensorManager.SENSOR_DELAY_NORMAL,
                SensorManager.SENSOR_DELAY_UI
            )

            sensorManager.registerListener(
                callback,
                sensorMagneticField,
                SensorManager.SENSOR_DELAY_NORMAL,
                SensorManager.SENSOR_DELAY_UI
            )

            locationClient.getLastLocation(locationRequest).addOnSuccessListener {
                location = it.toLatLngPoint()

                Log.e(TAG, "received location - $location")
                if (orientation != null) {
                    complete(LatLngOrientation.from(location!!, orientation!!))
                }
            }

            await()
        }
    }

    fun listen(
        period: Long,
        callback: (location: LatLngPoint, orientation: Orientation) -> Unit,
    ) {
        val rotationMatrix = FloatArray(9)
        val orientationAngles = FloatArray(3)

        val accelerometerReading = FloatArray(3)
        val magnetometerReading = FloatArray(3)

        var location: LatLngPoint? = null
        var orientation: Orientation? = null

        if (orientationCallback == null) {
            orientationCallback = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val isValidResult = event.sensor.type == Sensor.TYPE_ACCELEROMETER
                            || event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD

                    if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                        System.arraycopy(
                            event.values,
                            0,
                            accelerometerReading,
                            0,
                            accelerometerReading.size
                        )
                    } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                        System.arraycopy(
                            event.values,
                            0,
                            magnetometerReading,
                            0,
                            magnetometerReading.size
                        )
                    }
                    if (isValidResult) {
                        SensorManager.getRotationMatrix(
                            rotationMatrix,
                            null,
                            accelerometerReading,
                            magnetometerReading
                        )
                        SensorManager.getOrientation(rotationMatrix, orientationAngles)

                        orientation = Orientation.fromFloatArray(orientationAngles)

                        if (location != null) {
                            callback(location!!, orientation!!)
                        }
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

                }
            }
        }
        if (locationCallback == null) {
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    val l = locationResult.lastLocation

                    if (l != null) {
                        location = l.toLatLngPoint()
                    }

                    if (orientation != null) {
                        callback(location!!, orientation!!)
                    }
                }
            }
        }

        sensorManager.registerListener(
            orientationCallback,
            sensorAccelerometer,
            SensorManager.SENSOR_DELAY_NORMAL,
            SensorManager.SENSOR_DELAY_UI
        )

        sensorManager.registerListener(
            orientationCallback,
            sensorMagneticField,
            SensorManager.SENSOR_DELAY_NORMAL,
            SensorManager.SENSOR_DELAY_UI
        )

        val request = buildLocationRequest(period)

        locationClient.requestLocationUpdates(
            request,
            locationCallback!!,
            Looper.getMainLooper()
        )
    }

    fun stopListening() {
        locationClient.removeLocationUpdates(locationCallback!!)
        sensorManager.unregisterListener(orientationCallback)
    }

    private fun buildLocationRequest(
        period: Long,
        priority: Int = Priority.PRIORITY_HIGH_ACCURACY,
    ): LocationRequest {
        val locationRequest = LocationRequest.Builder(period)
            .setMinUpdateIntervalMillis(period)
            .setPriority(priority)
            .build()

        return locationRequest
    }

    private fun buildCallback(callback: (location: LatLngPoint?, orientation: Orientation?) -> Unit): LocationCallback {
        return object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val l = locationResult.lastLocation

                callback(l?.toLatLngPoint(), lastOrientation)
            }
        }
    }

    companion object {
        const val TAG = "LocationRepository"
    }
}

fun Location.toLatLngPoint(): LatLngPoint {
    return LatLngPoint(
        this.latitude,
        this.longitude
    )
}