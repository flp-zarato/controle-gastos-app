package com.flpzarato.controlegastos.ui.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flpzarato.controlegastos.ui.common.ControleGastosTopBar

@Composable
fun CardsScreen(
    onOpenInvoices: (cardId: String) -> Unit,
    viewModel: CardsViewModel = hiltViewModel(),
) {
    val cards by viewModel.cards.collectAsState()
    val knownUsers by viewModel.knownUsers.collectAsState()
    val form by viewModel.form.collectAsState()

    Scaffold(
        topBar = { ControleGastosTopBar(title = "Cartões") },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Column {
                    Text("Novo cartão", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = form.nickname,
                        onValueChange = viewModel::onNicknameChanged,
                        label = { Text("Apelido do cartão") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))

                    HolderDropdown(
                        users = knownUsers.map { it.email to it.displayName },
                        selectedEmail = form.holderEmail,
                        onSelected = viewModel::onHolderChanged,
                    )
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = form.bank,
                        onValueChange = viewModel::onBankChanged,
                        label = { Text("Banco") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row {
                        OutlinedTextField(
                            value = form.closingDay,
                            onValueChange = viewModel::onClosingDayChanged,
                            label = { Text("Dia de fechamento") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = form.dueDay,
                            onValueChange = viewModel::onDueDayChanged,
                            label = { Text("Dia de vencimento") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                    }

                    form.error?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }

                    Spacer(Modifier.height(16.dp))
                    Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) {
                        Text("Salvar cartão")
                    }

                    Spacer(Modifier.height(24.dp))
                    Text("Cartões cadastrados", style = MaterialTheme.typography.titleMedium)
                }
            }

            items(cards, key = { it.id }) { card ->
                ElevatedCard(
                    onClick = { onOpenInvoices(card.id) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.CreditCard,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 12.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(card.nickname, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "${card.bank} · fecha dia ${card.closingDay} · vence dia ${card.dueDay}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { viewModel.removeCard(card.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Remover")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HolderDropdown(
    users: List<Pair<String, String>>,
    selectedEmail: String,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = users.firstOrNull { it.first == selectedEmail }?.second.orEmpty()

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Titular") },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            users.forEach { (email, name) ->
                DropdownMenuItem(text = { Text(name) }, onClick = { onSelected(email); expanded = false })
            }
        }
    }
}
