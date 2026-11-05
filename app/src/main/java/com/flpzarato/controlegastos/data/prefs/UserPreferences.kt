package com.flpzarato.controlegastos.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "controle_gastos_prefs")

@Singleton
class UserPreferences @Inject constructor(
    private val context: Context,
) {
    private object Keys {
        val SIGNED_IN_EMAIL = stringPreferencesKey("signed_in_email")
        val SIGNED_IN_NAME = stringPreferencesKey("signed_in_name")
        val SPREADSHEET_ID = stringPreferencesKey("spreadsheet_id")
        val LAST_SYNC_EPOCH_MS = longPreferencesKey("last_sync_epoch_ms")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    }

    val signedInEmail: Flow<String?> = context.dataStore.data.map { it[Keys.SIGNED_IN_EMAIL] }
    val signedInName: Flow<String?> = context.dataStore.data.map { it[Keys.SIGNED_IN_NAME] }
    val spreadsheetId: Flow<String?> = context.dataStore.data.map { it[Keys.SPREADSHEET_ID] }
    val lastSyncEpochMs: Flow<Long?> = context.dataStore.data.map { it[Keys.LAST_SYNC_EPOCH_MS] }
    val isOnboardingDone: Flow<Boolean> = context.dataStore.data.map { it[Keys.ONBOARDING_DONE] ?: false }

    suspend fun saveSignedInUser(email: String, displayName: String) {
        context.dataStore.edit {
            it[Keys.SIGNED_IN_EMAIL] = email
            it[Keys.SIGNED_IN_NAME] = displayName
        }
    }

    suspend fun saveSpreadsheetId(id: String) {
        context.dataStore.edit {
            it[Keys.SPREADSHEET_ID] = id
            it[Keys.ONBOARDING_DONE] = true
        }
    }

    suspend fun saveLastSync(epochMs: Long) {
        context.dataStore.edit { it[Keys.LAST_SYNC_EPOCH_MS] = epochMs }
    }

    suspend fun signOut() {
        context.dataStore.edit { it.clear() }
    }
}
