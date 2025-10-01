package com.example.classsync.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

private val Context.userPreferencesStore: DataStore<UserPreferences> by dataStore(
    fileName = "user_prefs.pb",
    serializer = UserPreferencesSerializer
)

class UserPreferencesRepository(private val context: Context) {

    val userPreferencesFlow: Flow<UserPreferences> = context.userPreferencesStore.data

    suspend fun fetchInitialPreferences(): UserPreferences = userPreferencesFlow.first()

    suspend fun updateSchedules(schedules: List<ScheduleData>) {
        context.userPreferencesStore.updateData { preferences ->
            preferences.copy(schedules = schedules)
        }
    }
}
