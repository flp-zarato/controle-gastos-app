package com.flpzarato.controlegastos.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory, process-wide holder of the last/current Sheets sync status — written by
 * [SheetsSyncWorker] right as it runs, read by the shared top bar. Deliberately not derived
 * from WorkManager's own WorkInfo: a failed sync always returns Result.retry() (so it never
 * reaches WorkInfo.State.FAILED), and WorkInfo carries no detail about *why* it failed, which
 * is exactly what the UI needs to tell an auth failure apart from a transient one.
 */
@Singleton
class SyncStatusRepository @Inject constructor() {
    private val _status = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    fun markSyncing() {
        _status.value = SyncStatus.Syncing
    }

    fun markIdle() {
        _status.value = SyncStatus.Idle
    }

    fun markError(message: String, needsLogin: Boolean = false) {
        _status.value = SyncStatus.Error(message, needsLogin)
    }
}
