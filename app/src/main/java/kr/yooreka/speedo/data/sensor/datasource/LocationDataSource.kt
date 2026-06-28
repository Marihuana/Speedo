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

        // GPS 신호 튐 방지 가드(F-01a). 이상치 좌표/속도를 폐기하여 경로/속도 튐을 차단한다.
        private val signalGuard =
            GpsSignalGuard { lat1, lng1, lat2, lng2 ->
                val results = FloatArray(1)
                Location.distanceBetween(lat1, lng1, lat2, lng2, results)
                results[0]
            }

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

                    // 이상치(F-01a)면 폐기: dataFlow 를 갱신하지 않아 직전 신뢰 값이 유지되고,
                    // 빈 구간은 이후 정상 좌표 수신 시 시간 비례 보간(F-13c/F-13d)으로 메워진다.
                    val elapsedMs = location.elapsedRealtimeNanos / NANOS_PER_MILLI
                    if (!signalGuard.accept(location.latitude, location.longitude, speedKmh, elapsedMs)) {
                        return
                    }

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
            signalGuard.reset()
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
            signalGuard.reset()
            _dataFlow.value = LocationData()
        }

        private companion object {
            private const val NANOS_PER_MILLI = 1_000_000L
        }
    }
