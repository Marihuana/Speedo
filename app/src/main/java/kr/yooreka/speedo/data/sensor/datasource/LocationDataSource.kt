package kr.yooreka.speedo.data.sensor.datasource

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kr.yooreka.speedo.domain.model.LocationData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationDataSource
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : SensorDataSource<LocationData> {
        private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        private val _dataFlow = MutableStateFlow(LocationData())
        override val dataFlow: StateFlow<LocationData> = _dataFlow.asStateFlow()

        private val locationCallback =
            object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val location: Location = result.lastLocation ?: return

                    val speedKmh =
                        SpeedResolver.toKmh(
                            hasSpeed = location.hasSpeed(),
                            speedMps = location.speed,
                            hasSpeedAccuracy = location.hasSpeedAccuracy(),
                            speedAccuracyMps = location.speedAccuracyMetersPerSecond,
                        )
                    val accuracy = if (location.hasAccuracy()) location.accuracy else 0f

                    _dataFlow.value =
                        LocationData(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            speed = speedKmh,
                            accuracy = accuracy,
                        )
                }
            }

        @SuppressLint("MissingPermission")
        override fun start() {
            val locationRequest =
                LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    1000L,
                ).apply {
                    setMinUpdateIntervalMillis(500L)
                }.build()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper(),
            )
        }

        override fun stop() {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            _dataFlow.value = LocationData()
        }
    }
