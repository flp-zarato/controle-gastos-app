package com.flpzarato.controlegastos.ui.recurring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flpzarato.controlegastos.ui.common.ControleGastosTopBar
import java.text.NumberFormat
import java.util.Locale

@Composable
fun RecurringListScreen(
    onAddRecurring: () -> Unit,
    onEditRecurring: (String) -> Unit,
    viewModel: RecurringListViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsState()
    val currencyFormat = remember(Locale("pt", "BR")) { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = { ControleGastosTopBar(title = "Recorrentes") },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddRecurring) {
                Icon(Icons.Filled.Add, contentDescription = "Novo recorrente")
            }
        },
    ) { padding ->
        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Nenhum gasto recorrente cadastrado", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items, key = { it.id }) { item ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.description, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "${currencyFormat.format(item.amount)} · dia ${item.dayOfMonth} · " +
                                        item.categoryName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(checked = item.active, onCheckedChange = { viewModel.setActive(item.id, it) })
                            IconButton(onClick = { onEditRecurring(item.id) }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Editar")
                            }
                            IconButton(onClick = { pendingDeleteId = item.id }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Excluir")
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDeleteId?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text("Excluir recorrente?") },
            text = {
                Text(
                    "Isso não apaga os lançamentos já gerados — só impede que novos sejam " +
                        "lançados nos próximos meses.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(id)
                    pendingDeleteId = null
                }) { Text("Excluir") }
            },
            dismissButton = { TextButton(onClick = { pendingDeleteId = null }) { Text("Cancelar") } },
        )
    }
}
