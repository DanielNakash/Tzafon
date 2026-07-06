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
        val planningPreset = stringPreferencesKey("planning_preset") // ActionLogic.RangePreset name
        val planningCustomEnd = stringPreferencesKey("planning_custom_end") // ISO date
        val slippageDismissed = stringPreferencesKey("slippage_dismissed_on") // ISO date
        val overloadDismissed = stringPreferencesKey("overload_dismissed_on") // ISO date
        val remindersEnabled = booleanPreferencesKey("reminders_enabled") // FR-NOTIF opt-in
    }

    val welcomeSeen: Flow<Boolean> = context.dataStore.data.map { it[Keys.welcomeSeen] ?: false }

    suspend fun setWelcomeSeen() {
        context.dataStore.edit { it[Keys.welcomeSeen] = true }
    }

    val weekStart: Flow<String> = context.dataStore.data.map { it[Keys.weekStart] ?: "SUNDAY" }

    suspend fun setWeekStart(day: String) {
        context.dataStore.edit { it[Keys.weekStart] = day }
    }

    // ── Planning range (FR-PLAN-2) ────────────────────────────

    val planningPreset: Flow<String> = context.dataStore.data.map { it[Keys.planningPreset] ?: "DAYS_7" }
    val planningCustomEnd: Flow<String?> = context.dataStore.data.map { it[Keys.planningCustomEnd] }

    suspend fun setPlanningRange(preset: String, customEnd: String?) {
        context.dataStore.edit {
            it[Keys.planningPreset] = preset
            if (customEnd != null) it[Keys.planningCustomEnd] = customEnd else it.remove(Keys.planningCustomEnd)
        }
    }

    // ── per-day dismissals (FR-TODAY-4/5 — calm, closeable) ───

    val slippageDismissedOn: Flow<String?> = context.dataStore.data.map { it[Keys.slippageDismissed] }
    val overloadDismissedOn: Flow<String?> = context.dataStore.data.map { it[Keys.overloadDismissed] }

    suspend fun dismissSlippage(today: String) {
        context.dataStore.edit { it[Keys.slippageDismissed] = today }
    }

    suspend fun dismissOverload(today: String) {
        context.dataStore.edit { it[Keys.overloadDismissed] = today }
    }

    // ── cue reminders (FR-NOTIF-2 — strictly opt-in, PRIN-9) ──

    val remindersEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.remindersEnabled] ?: false }

    suspend fun setRemindersEnabled(on: Boolean) {
        context.dataStore.edit { it[Keys.remindersEnabled] = on }
    }
}
