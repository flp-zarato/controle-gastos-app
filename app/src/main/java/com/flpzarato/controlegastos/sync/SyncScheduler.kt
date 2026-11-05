package com.flpzarato.controlegastos.sync

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Entry point the rest of the app uses to ask for a sync. Every call requires network
 * connectivity (`NetworkType.CONNECTED`); if the phone is offline when an expense is
 * saved, WorkManager simply holds the request until connectivity returns, which is the
 * "salva local, sincroniza quando tiver internet" behavior the app promises.
 */
@Singleton
class SyncScheduler @Inject constructor(
    private val workManager: WorkManager,
) {
    private val networkConstraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /** Call right after any local write (new expense, new category, edited card, ...). */
    fun requestImmediateSync() {
        val request = OneTimeWorkRequestBuilder<SheetsSyncWorker>()
            .setConstraints(networkConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(SheetsSyncWorker.WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    /** Call once on app start / login so the two phones stay in sync even with no local edits. */
    fun schedulePeriodicSync() {
        val request = PeriodicWorkRequestBuilder<SheetsSyncWorker>(30, TimeUnit.MINUTES)
            .setConstraints(networkConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            "${SheetsSyncWorker.WORK_NAME}_periodic",
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
