package kr.yooreka.speedo.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kr.yooreka.speedo.domain.model.LeanMode
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

data class UserPreferences(
    val showTpmsData: Boolean,
    val speedUnit: String,
    val pressureUnit: String,
    val frontTpmsId: String,
    val rearTpmsId: String,
    val launchCount: Int,
    val leanMeasurementMode: String,
    val autoStopThresholdMin: Int,
)

@Singleton
class UserPreferencesRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private object PreferencesKeys {
            val SHOW_TPMS_DATA = booleanPreferencesKey("show_tpms_data")
            val SPEED_UNIT = stringPreferencesKey("speed_unit")
            val PRESSURE_UNIT = stringPreferencesKey("pressure_unit")
            val FRONT_TPMS_ID = stringPreferencesKey("front_tpms_id")
            val REAR_TPMS_ID = stringPreferencesKey("rear_tpms_id")
            val LAUNCH_COUNT = intPreferencesKey("launch_count")
            val LEAN_MEASUREMENT_MODE = stringPreferencesKey("lean_measurement_mode")
            val AUTO_STOP_THRESHOLD = intPreferencesKey("auto_stop_threshold")
        }

        val userPreferencesFlow: Flow<UserPreferences> =
            context.dataStore.data
                .map { preferences ->
                    val showTpmsData = preferences[PreferencesKeys.SHOW_TPMS_DATA] ?: false
                    val speedUnit = preferences[PreferencesKeys.SPEED_UNIT] ?: "KM/H"
                    val pressureUnit = preferences[PreferencesKeys.PRESSURE_UNIT] ?: "PSI"
                    val frontTpmsId = preferences[PreferencesKeys.FRONT_TPMS_ID] ?: ""
                    val rearTpmsId = preferences[PreferencesKeys.REAR_TPMS_ID] ?: ""
                    val launchCount = preferences[PreferencesKeys.LAUNCH_COUNT] ?: 0
                    val leanMeasurementMode =
                        preferences[PreferencesKeys.LEAN_MEASUREMENT_MODE] ?: LeanMode.DEFAULT.name
                    val autoStopThresholdMin = preferences[PreferencesKeys.AUTO_STOP_THRESHOLD] ?: DEFAULT_AUTO_STOP_MIN

                    UserPreferences(
                        showTpmsData = showTpmsData,
                        speedUnit = speedUnit,
                        pressureUnit = pressureUnit,
                        frontTpmsId = frontTpmsId,
                        rearTpmsId = rearTpmsId,
                        launchCount = launchCount,
                        leanMeasurementMode = leanMeasurementMode,
                        autoStopThresholdMin = autoStopThresholdMin,
                    )
                }

        /** lean 측정 방식(F-03). 선택 전략을 런타임에 교체하기 위한 전용 스트림. */
        val leanMeasurementModeFlow: Flow<LeanMode> =
            context.dataStore.data
                .map { LeanMode.fromName(it[PreferencesKeys.LEAN_MEASUREMENT_MODE]) }

        /** 주행 종료 예상 감지 임계값(분, 0=OFF). 기록 중 저속 지속 감지에 사용(F-18a). */
        val autoStopThresholdFlow: Flow<Int> =
            context.dataStore.data
                .map { it[PreferencesKeys.AUTO_STOP_THRESHOLD] ?: DEFAULT_AUTO_STOP_MIN }

        suspend fun incrementLaunchCount() {
            context.dataStore.edit { preferences ->
                val current = preferences[PreferencesKeys.LAUNCH_COUNT] ?: 0
                preferences[PreferencesKeys.LAUNCH_COUNT] = current + 1
            }
        }

        suspend fun updateTpmsIds(
            frontId: String,
            rearId: String,
        ) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.FRONT_TPMS_ID] = frontId
                preferences[PreferencesKeys.REAR_TPMS_ID] = rearId
                preferences[PreferencesKeys.SHOW_TPMS_DATA] = true
            }
        }

        suspend fun resetTpmsIds() {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.FRONT_TPMS_ID] = ""
                preferences[PreferencesKeys.REAR_TPMS_ID] = ""
                // 센서 ID 초기화 시 TPMS 표시도 함께 끈다.
                preferences[PreferencesKeys.SHOW_TPMS_DATA] = false
            }
        }

        suspend fun updateShowTpmsData(show: Boolean) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.SHOW_TPMS_DATA] = show
            }
        }

        suspend fun updateSpeedUnit(unit: String) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.SPEED_UNIT] = unit
            }
        }

        suspend fun updatePressureUnit(unit: String) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.PRESSURE_UNIT] = unit
            }
        }

        suspend fun updateLeanMeasurementMode(mode: LeanMode) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.LEAN_MEASUREMENT_MODE] = mode.name
            }
        }

        /** 주행 종료 예상 감지 임계값(분, 0=OFF) 저장(F-18a). */
        suspend fun updateAutoStopThreshold(minutes: Int) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.AUTO_STOP_THRESHOLD] = minutes
            }
        }

        companion object {
            /** 주행 종료 예상 감지 기본 임계값(분). */
            const val DEFAULT_AUTO_STOP_MIN = 5
        }
    }
