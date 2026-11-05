package com.flpzarato.controlegastos.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.flpzarato.controlegastos.data.repository.SheetsSyncRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SheetsSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncRepository: SheetsSyncRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val result = syncRepository.sync()
        return if (result.isSuccess) {
            Result.success()
        } else {
            // Keep the local, offline-first copy intact and simply try again later.
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "sheets_sync"
    }
}
