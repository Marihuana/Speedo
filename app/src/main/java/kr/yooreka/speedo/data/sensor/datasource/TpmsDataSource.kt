package kr.yooreka.speedo.data.sensor.datasource

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kr.yooreka.speedo.data.local.preferences.UserPreferencesRepository
import kr.yooreka.speedo.domain.model.TpmsData
import kr.yooreka.speedo.domain.repository.CrashReporter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@SuppressLint("MissingPermission")
class TpmsDataSource
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val prefsRepo: UserPreferencesRepository,
        private val crashReporter: CrashReporter,
    ) : SensorDataSource<TpmsData> {
        private val _dataFlow = MutableStateFlow(TpmsData())
        override val dataFlow: StateFlow<TpmsData> = _dataFlow.asStateFlow()

        // BLE 페이로드 파싱 실패는 패킷마다 반복될 수 있어, 세션당 1회만 비치명적 리포트한다(스팸 방지).
        private var payloadParseReported = false

        private var scanCallback: ScanCallback? = null
        private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        private val bluetoothAdapter = bluetoothManager.adapter
        private val scanner: BluetoothLeScanner?
            get() = bluetoothAdapter?.bluetoothLeScanner

        private var frontId = ""
        private var rearId = ""
        private var job: Job? = null
        private val scope = CoroutineScope(Dispatchers.IO)

        override fun start() {
            if (scanCallback != null) return

            payloadParseReported = false
            job =
                scope.launch {
                    val prefs = prefsRepo.userPreferencesFlow.first()
                    frontId = prefs.frontTpmsId
                    rearId = prefs.rearTpmsId

                    // Only start scanning if IDs are actually configured
                    if (frontId.isNotBlank() || rearId.isNotBlank()) {
                        startBleScan()
                    }
                }
        }

        private fun startBleScan() {
            if (scanner == null || bluetoothAdapter?.isEnabled != true) return

            scanCallback =
                object : ScanCallback() {
                    override fun onScanResult(
                        callbackType: Int,
                        result: ScanResult,
                    ) {
                        val device = result.device
                        val address = device.address.replace(":", "")
                        val name = result.scanRecord?.deviceName ?: ""

                        // Match the device by MAC address or Name containing the user input ID
                        val isFront = (frontId.isNotBlank() && (address.contains(frontId, true) || name.contains(frontId, true)))
                        val isRear = (rearId.isNotBlank() && (address.contains(rearId, true) || name.contains(rearId, true)))

                        if (isFront || isRear) {
                            parseTpmsData(result, isFront)
                        }
                    }
                }

            val settings =
                ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build()

            try {
                scanner?.startScan(null, settings, scanCallback)
            } catch (e: Exception) {
                // BLE 스캔 시작 실패(어댑터 상태/권한 등). start() 당 1회만 발생하므로 그대로 리포트(PRD §7.1).
                Log.e("TpmsDataSource", "BLE Scan start failed: ${e.message}")
                crashReporter.recordNonFatal(e, "TPMS BLE scan start failed")
            }
        }

        private fun parseTpmsData(
            result: ScanResult,
            isFront: Boolean,
        ) {
            val record = result.scanRecord ?: return
            val manufacturerData = record.manufacturerSpecificData

            try {
                // -- Generic iBar / SYKIK / Sysgration TPMS Parser --
                // This is a robust fallback if specific offsets are unknown.
                // Log the payload so developers can inspect exactly what their ali-express sensor sends.
                if (manufacturerData != null && manufacturerData.size() > 0) {
                    for (i in 0 until manufacturerData.size()) {
                        val key = manufacturerData.keyAt(i)
                        val payload = manufacturerData.valueAt(i)

                        // Android's manufacturerData.valueAt(i) strips the first 2 bytes (Company ID).
                        // So a typical 18-byte payload (0x00 to 0x11) becomes 16 bytes (index 0 to 15).
                        if (payload.size >= 16) {
                            val buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)

                            // 0x08 ~ 0x0B (offset 6~9): Pressure (Pa). 1 Pa = 0.000145038 PSI
                            val pressurePa = buffer.getInt(6)
                            val parsedPressure = pressurePa * 0.000145038f

                            // 0x0C ~ 0x0F (offset 10~13): Temperature (0.01 ℃)
                            val tempRaw = buffer.getInt(10)
                            val parsedTemp = tempRaw / 100.0f

                            // 0x10 (offset 14): Battery (%)
                            // Convert 0~100% to roughly 2.0V ~ 3.0V for UI display
                            val batteryPct = payload[14].toUByte().toInt()
                            val parsedBat = 2.0f + (batteryPct / 100.0f) * 1.0f

                            // 0x11 (offset 15): Status Flag (0x00: Normal, 0x01: Leak)
                            val statusFlag = payload[15].toUByte().toInt()
                            val isLeak = statusFlag == 0x01

                            if (isLeak) {
                                Log.w("TpmsDataSource", "Air leak detected on ${if (isFront) "Front" else "Rear"} tire!")
                            }

                            val current = _dataFlow.value
                            _dataFlow.value =
                                if (isFront) {
                                    current.copy(
                                        frontPressurePsi = parsedPressure,
                                        frontTemperature = parsedTemp,
                                        frontBatteryVoltage = parsedBat,
                                        timestamp = System.currentTimeMillis(),
                                    )
                                } else {
                                    current.copy(
                                        rearPressurePsi = parsedPressure,
                                        rearTemperature = parsedTemp,
                                        rearBatteryVoltage = parsedBat,
                                        timestamp = System.currentTimeMillis(),
                                    )
                                }
                        }
                    }
                }
            } catch (e: Exception) {
                // 센서 정합성 오류(PRD §7.1). 패킷마다 반복될 수 있어 세션당 1회만 리포트한다(스팸 방지).
                Log.e("TpmsDataSource", "Failed to parse TPMS payload", e)
                if (!payloadParseReported) {
                    payloadParseReported = true
                    crashReporter.recordNonFatal(e, "TPMS payload parse failed")
                }
            }
        }

        override fun stop() {
            try {
                scanCallback?.let {
                    scanner?.stopScan(it)
                }
            } catch (e: Exception) {
                // Ignore if bluetooth was turned off
            }
            scanCallback = null
            job?.cancel()
            job = null
        }
    }
