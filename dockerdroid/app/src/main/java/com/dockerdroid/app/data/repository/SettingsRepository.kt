package com.dockerdroid.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/** User preferences persisted across reboots via Jetpack DataStore. */
class SettingsRepository(private val context: Context) {

    val startOnBoot: Flow<Boolean> =
        context.dataStore.data.map { it[KEY_START_ON_BOOT] ?: false }

    val autoRestart: Flow<Boolean> =
        context.dataStore.data.map { it[KEY_AUTO_RESTART] ?: true }

    /** Require biometric confirmation before using stored SSH credentials. */
    val requireBiometric: Flow<Boolean> =
        context.dataStore.data.map { it[KEY_REQUIRE_BIOMETRIC] ?: false }

    suspend fun setStartOnBoot(enabled: Boolean) {
        context.dataStore.edit { it[KEY_START_ON_BOOT] = enabled }
    }

    suspend fun setAutoRestart(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_RESTART] = enabled }
    }

    suspend fun setRequireBiometric(enabled: Boolean) {
        context.dataStore.edit { it[KEY_REQUIRE_BIOMETRIC] = enabled }
    }

    /** One-shot read used by the boot receiver before the UI exists. */
    suspend fun startOnBootOnce(): Boolean =
        context.dataStore.data.first()[KEY_START_ON_BOOT] ?: false

    suspend fun requireBiometricOnce(): Boolean =
        context.dataStore.data.first()[KEY_REQUIRE_BIOMETRIC] ?: false

    companion object {
        private val KEY_START_ON_BOOT = booleanPreferencesKey("start_on_boot")
        private val KEY_AUTO_RESTART = booleanPreferencesKey("auto_restart")
        private val KEY_REQUIRE_BIOMETRIC = booleanPreferencesKey("require_biometric")
    }
}
