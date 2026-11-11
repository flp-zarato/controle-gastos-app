package com.flpzarato.controlegastos.ui.login

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flpzarato.controlegastos.R

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val consentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        viewModel.onConsentResult(result.resultCode == Activity.RESULT_OK)
    }

    LaunchedEffect(state) {
        val current = state
        if (current is LoginUiState.NeedsConsent) {
            consentLauncher.launch(IntentSenderRequest.Builder(current.pendingIntent.intentSender).build())
        }
        if (current is LoginUiState.Done) {
            onLoggedIn()
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.login_title), style = MaterialTheme.typography.headlineMedium)
            Text(
                stringResource(R.string.login_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp),
            )

            when (val s = state) {
                is LoginUiState.NeedsSpreadsheetId -> SpreadsheetIdForm(
                    onSubmit = viewModel::onSpreadsheetIdSubmitted,
                    error = s.error,
                )
                is LoginUiState.Loading -> CircularProgressIndicator()
                is LoginUiState.Error -> {
                    Text(s.message, color = MaterialTheme.colorScheme.error)
                    Button(
                        onClick = { viewModel.onSignInClicked(context) },
                        modifier = Modifier.padding(top = 16.dp),
                    ) {
                        Text(stringResource(R.string.login_button))
                    }
                }
                else -> Button(onClick = { viewModel.onSignInClicked(context) }) {
                    Text(stringResource(R.string.login_button))
                }
            }
        }
    }
}

@Composable
private fun SpreadsheetIdForm(onSubmit: (String) -> Unit, error: String? = null) {
    var text by remember { mutableStateOf("") }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text("ID ou link da planilha compartilhada") },
        modifier = Modifier.fillMaxWidth(),
    )
    error?.let {
        Text(
            it,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
    Button(onClick = { onSubmit(text) }, modifier = Modifier.padding(top = 16.dp)) {
        Text("Continuar")
    }
}
