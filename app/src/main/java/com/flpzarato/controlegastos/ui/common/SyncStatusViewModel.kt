package com.flpzarato.controlegastos.ui.common

import androidx.lifecycle.ViewModel
import com.flpzarato.controlegastos.sync.SyncStatus
import com.flpzarato.controlegastos.sync.SyncStatusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Thin relay onto the singleton [SyncStatusRepository] so [ControleGastosTopBar] can collect it as state. */
@HiltViewModel
class SyncStatusViewModel @Inject constructor(
    syncStatusRepository: SyncStatusRepository,
) : ViewModel() {
    val status: StateFlow<SyncStatus> = syncStatusRepository.status
}
