package com.flpzarato.controlegastos.ui.invoice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flpzarato.controlegastos.ui.common.ControleGastosTopBar
import com.flpzarato.controlegastos.ui.common.formatCompetenceLabel
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun InvoiceDetailScreen(
    onBack: () -> Unit,
    viewModel: InvoiceDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val currencyFormat = remember(Locale("pt", "BR")) { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }

    Scaffold(
        topBar = {
            ControleGastosTopBar(
                title = if (state.competenceMonth.isBlank()) "Fatura" else formatCompetenceLabel(state.competenceMonth),
                onBack = onBack,
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Total da fatura: ", style = MaterialTheme.typography.titleMedium)
                Text(
                    currencyFormat.format(state.total),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
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
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(expense.description + parcelaLabel, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "${expense.purchaseDate.format(dateFormatter)} · " +
                                        "${state.categoryNames[expense.categoryId] ?: "—"} · " +
                                        (state.userNames[expense.loggedByEmail] ?: expense.loggedByEmail),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                currencyFormat.format(expense.installmentAmount),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}
