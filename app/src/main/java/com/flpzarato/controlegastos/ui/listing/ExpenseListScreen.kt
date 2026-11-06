package com.flpzarato.controlegastos.ui.listing

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flpzarato.controlegastos.ui.common.ControleGastosTopBar
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ExpenseListScreen(
    onAddExpense: () -> Unit,
    viewModel: ExpenseListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val currencyFormat = remember(Locale("pt", "BR")) { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }

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

            LazyColumn {
                items(state.expenses, key = { it.id }) { expense ->
                    val parcelaLabel = if (expense.installmentCount > 1) {
                        " · ${expense.installmentNumber}/${expense.installmentCount}"
                    } else {
                        ""
                    }
                    ListItem(
                        headlineContent = { Text(expense.description + parcelaLabel) },
                        supportingContent = {
                            Text(
                                "${state.categoryNames[expense.categoryId] ?: "—"} · " +
                                    (state.userNames[expense.loggedByEmail] ?: expense.loggedByEmail),
                            )
                        },
                        trailingContent = {
                            Row {
                                Text(currencyFormat.format(expense.installmentAmount))
                                IconButton(onClick = { viewModel.deleteGroup(expense.installmentGroupId) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Excluir")
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}
