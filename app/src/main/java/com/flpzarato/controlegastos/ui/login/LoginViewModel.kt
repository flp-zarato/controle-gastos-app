package com.flpzarato.controlegastos.ui.login

import android.app.PendingIntent
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flpzarato.controlegastos.data.prefs.UserPreferences
import com.flpzarato.controlegastos.data.remote.auth.GoogleAuthManager
import com.flpzarato.controlegastos.data.remote.auth.SheetsAuthorizationOutcome
import com.flpzarato.controlegastos.data.repository.SheetsSyncRepository
import com.flpzarato.controlegastos.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data class NeedsConsent(val pendingIntent: PendingIntent) : LoginUiState
    data class NeedsSpreadsheetId(val error: String? = null) : LoginUiState
    data object Done : LoginUiState
    data class Error(val message: String) : LoginUiState
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authManager: GoogleAuthManager,
    private val userPreferences: UserPreferences,
    private val syncScheduler: SyncScheduler,
    private val syncRepository: SheetsSyncRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onSignInClicked(activityContext: Context) {
        viewModelScope.launch {
            _state.value = LoginUiState.Loading
            authManager.signIn(activityContext)
                .onSuccess { user ->
                    userPreferences.saveSignedInUser(user.email, user.displayName)
                    when (val outcome = authManager.requestSheetsAuthorization()) {
                        is SheetsAuthorizationOutcome.Granted -> proceedAfterAuthorization()
                        is SheetsAuthorizationOutcome.NeedsUserConsent ->
                            _state.value = LoginUiState.NeedsConsent(outcome.pendingIntent)
                    }
                }
                .onFailure { _state.value = LoginUiState.Error(it.message ?: "Não foi possível entrar") }
        }
    }

    fun onConsentResult(granted: Boolean) {
        viewModelScope.launch {
            if (granted) {
                proceedAfterAuthorization()
            } else {
                _state.value = LoginUiState.Error("É necessário autorizar o acesso à planilha para usar o app")
            }
        }
    }

    fun onSpreadsheetIdSubmitted(rawInput: String) {
        viewModelScope.launch {
            val id = extractSpreadsheetId(rawInput)
            if (id.isBlank()) {
                _state.value = LoginUiState.NeedsSpreadsheetId(error = "Cole o ID ou o link da planilha")
                return@launch
            }
            _state.value = LoginUiState.Loading
            userPreferences.saveSpreadsheetId(id)
            syncScheduler.schedulePeriodicSync()
            // Awaited (not fired via WorkManager) so categories/cards from the sheet — or the
            // seeded defaults if the sheet has none yet — are in Room before we ever show the
            // main app; otherwise "Adicionar gasto" could open with an empty category dropdown.
            syncRepository.sync()
                .onSuccess { _state.value = LoginUiState.Done }
                .onFailure {
                    _state.value = LoginUiState.NeedsSpreadsheetId(
                        error = "Não foi possível acessar a planilha. Confira o ID/link, sua conexão, " +
                            "e se a planilha foi compartilhada com este e-mail, e tente novamente.",
                    )
                }
        }
    }

    private suspend fun proceedAfterAuthorization() {
        _state.value = if (userPreferences.isOnboardingDone.first()) {
            syncScheduler.schedulePeriodicSync()
            syncScheduler.requestImmediateSync()
            LoginUiState.Done
        } else {
            LoginUiState.NeedsSpreadsheetId()
        }
    }

    /** Accepts either a bare spreadsheet ID or a full Google Sheets URL. */
    private fun extractSpreadsheetId(input: String): String {
        val trimmed = input.trim()
        val urlMatch = Regex("/spreadsheets/d/([a-zA-Z0-9-_]+)").find(trimmed)
        return urlMatch?.groupValues?.get(1) ?: trimmed
    }
}
