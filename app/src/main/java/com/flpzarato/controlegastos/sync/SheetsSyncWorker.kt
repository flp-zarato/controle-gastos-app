package com.flpzarato.controlegastos.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.flpzarato.controlegastos.data.remote.auth.SheetsAuthException
import com.flpzarato.controlegastos.data.repository.SheetsSyncRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

private const val TAG = "SheetsSync"

@HiltWorker
class SheetsSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncRepository: SheetsSyncRepository,
    private val syncStatusRepository: SyncStatusRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        syncStatusRepository.markSyncing()
        val result = syncRepository.sync()
        return result.fold(
            onSuccess = {
                syncStatusRepository.markIdle()
                Result.success()
            },
            onFailure = { error ->
                if (error is SheetsAuthException) {
                    Log.e(TAG, "Sync failed: auth error — ${error.message}", error)
                    syncStatusRepository.markError(
                        message = "Não foi possível sincronizar. Faça login novamente.",
                        needsLogin = true,
                    )
                } else {
                    Log.e(TAG, "Sync failed: ${error.message}", error)
                    syncStatusRepository.markError(
                        message = "Não foi possível sincronizar agora. Vamos tentar de novo em breve.",
                    )
                }
                // Keep the local, offline-first copy intact and simply try again later.
                Result.retry()
            },
        )
    }

    companion object {
        const val WORK_NAME = "sheets_sync"
    }
}
