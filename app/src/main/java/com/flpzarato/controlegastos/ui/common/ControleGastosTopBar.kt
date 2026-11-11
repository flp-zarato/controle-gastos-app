package com.flpzarato.controlegastos.ui.common

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flpzarato.controlegastos.sync.SyncStatus
import com.flpzarato.controlegastos.ui.navigation.LocalNavController
import com.flpzarato.controlegastos.ui.navigation.Routes

/**
 * Shared app bar for every screen. Bottom-nav destinations (Lançamentos, Categorias,
 * Cartões) omit [onBack] since they are top-level and already reachable from the bottom
 * bar — a back arrow there would wrongly suggest a parent screen exists. Screens pushed
 * on top of them (e.g. Adicionar gasto) pass [onBack] so the user always has a visible
 * way out besides the system gesture/button.
 *
 * Also renders the current Sheets sync status (a small spinner while syncing, an alert
 * icon on error) sourced from [SyncStatusViewModel] — informative only, never blocking
 * navigation or input.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControleGastosTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    syncStatusViewModel: SyncStatusViewModel = hiltViewModel(),
) {
    val status by syncStatusViewModel.status.collectAsState()
    var showErrorDialog by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                }
            }
        },
        actions = {
            when (status) {
                is SyncStatus.Syncing -> CircularProgressIndicator(
                    modifier = Modifier.padding(end = 16.dp).size(20.dp),
                    strokeWidth = 2.dp,
                )
                is SyncStatus.Error -> IconButton(onClick = { showErrorDialog = true }) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = "Erro de sincronização",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
                SyncStatus.Idle -> {}
            }
        },
    )

    val currentStatus = status
    if (showErrorDialog && currentStatus is SyncStatus.Error) {
        val navController = LocalNavController.current
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("Sincronização") },
            text = { Text(currentStatus.message) },
            confirmButton = {
                if (currentStatus.needsLogin) {
                    TextButton(onClick = {
                        showErrorDialog = false
                        navController?.navigate(Routes.LOGIN) { popUpTo(0) }
                    }) { Text("Entrar novamente") }
                } else {
                    TextButton(onClick = { showErrorDialog = false }) { Text("OK") }
                }
            },
            dismissButton = {
                if (currentStatus.needsLogin) {
                    TextButton(onClick = { showErrorDialog = false }) { Text("Fechar") }
                }
            },
        )
    }
}
