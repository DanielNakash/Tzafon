package com.thefoxworks.tzafon.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "tzafon_settings")

/**
 * Device-local preferences (FR-SET). Week start (DM-REL-2, default Sunday)
 * drives habit periods, Review day and fresh-start framing from M4/M7 on.
 */
class SettingsStore(private val context: Context) {

    private object Keys {
        val welcomeSeen = booleanPreferencesKey("welcome_seen")
        val weekStart = stringPreferencesKey("week_start") // SUNDAY | MONDAY | SATURDAY
    }

    val welcomeSeen: Flow<Boolean> = context.dataStore.data.map { it[Keys.welcomeSeen] ?: false }

    suspend fun setWelcomeSeen() {
        context.dataStore.edit { it[Keys.welcomeSeen] = true }
    }

    val weekStart: Flow<String> = context.dataStore.data.map { it[Keys.weekStart] ?: "SUNDAY" }

    suspend fun setWeekStart(day: String) {
        context.dataStore.edit { it[Keys.weekStart] = day }
    }
}
