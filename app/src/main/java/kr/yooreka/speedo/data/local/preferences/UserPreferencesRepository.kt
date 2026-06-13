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

                    UserPreferences(
                        showTpmsData = showTpmsData,
                        speedUnit = speedUnit,
                        pressureUnit = pressureUnit,
                        frontTpmsId = frontTpmsId,
                        rearTpmsId = rearTpmsId,
                        launchCount = launchCount,
                    )
                }

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
    }
