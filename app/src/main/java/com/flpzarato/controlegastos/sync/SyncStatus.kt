package com.flpzarato.controlegastos.sync

/** Current state of the Sheets sync, observed by the shared top bar so it never needs WorkManager logs to notice a problem. */
sealed interface SyncStatus {
    data object Idle : SyncStatus
    data object Syncing : SyncStatus

    /** [needsLogin] is true only for an auth failure (revoked/expired token) — the UI offers a way back to Login just for that case. */
    data class Error(val message: String, val needsLogin: Boolean = false) : SyncStatus
}
