package com.flpzarato.controlegastos.ui.listing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flpzarato.controlegastos.ui.common.ControleGastosTopBar
import com.flpzarato.controlegastos.ui.common.categoryColor
import com.flpzarato.controlegastos.ui.common.categoryIcon
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ExpenseListScreen(
    onAddExpense: () -> Unit,
    onEditExpense: (String) -> Unit,
    viewModel: ExpenseListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val currencyFormat = remember(Locale("pt", "BR")) { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    var pendingDeleteGroupId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = { ControleGastosTopBar(title = "Lançamentos") },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddExpense) {
                Icon(Icons.Filled.Add, contentDescription = "Novo gasto")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = if (state.filter == MonthFilter.ATUAL) 0 else 1) {
                Tab(
                    selected = state.filter == MonthFilter.ATUAL,
                    onClick = { viewModel.onFilterChanged(MonthFilter.ATUAL) },
                    text = { Text("Mês atual") },
                )
                Tab(
                    selected = state.filter == MonthFilter.ANTERIOR,
                    onClick = { viewModel.onFilterChanged(MonthFilter.ANTERIOR) },
                    text = { Text("Mês anterior") },
                )
            }

            Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Total do período: ", style = MaterialTheme.typography.titleMedium)
                Text(
                    currencyFormat.format(state.total),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            if (state.expenses.isEmpty()) {
                Text(
                    "Nenhum lançamento neste período",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.expenses, key = { it.id }) { expense ->
                    val parcelaLabel = if (expense.installmentCount > 1) {
                        " · ${expense.installmentNumber}/${expense.installmentCount}"
                    } else {
                        ""
                    }
                    val color = categoryColor(expense.categoryId)

                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(color.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    categoryIcon(state.categoryNames[expense.categoryId].orEmpty()),
                                    contentDescription = null,
                                    tint = color,
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(expense.description + parcelaLabel, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "${expense.purchaseDate.format(dateFormatter)} · " +
                                        "${state.categoryNames[expense.categoryId] ?: "—"} · " +
                                        (state.userNames[expense.loggedByEmail] ?: expense.loggedByEmail),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (!expense.observation.isNullOrBlank()) {
                                    Text(
                                        expense.observation,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    currencyFormat.format(expense.installmentAmount),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                Row {
                                    IconButton(onClick = { onEditExpense(expense.installmentGroupId) }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Editar")
                                    }
                                    IconButton(onClick = { pendingDeleteGroupId = expense.installmentGroupId }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Excluir")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDeleteGroupId?.let { groupId ->
        AlertDialog(
            onDismissRequest = { pendingDeleteGroupId = null },
            title = { Text("Excluir lançamento?") },
            text = {
                Text(
                    "Essa ação não pode ser desfeita. Se for um gasto parcelado, todas as parcelas " +
                        "serão excluídas junto.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteGroup(groupId)
                    pendingDeleteGroupId = null
                }) { Text("Excluir") }
            },
            dismissButton = { TextButton(onClick = { pendingDeleteGroupId = null }) { Text("Cancelar") } },
        )
    }
}
