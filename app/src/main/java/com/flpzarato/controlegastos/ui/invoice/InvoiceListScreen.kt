package com.flpzarato.controlegastos.ui.invoice

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
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flpzarato.controlegastos.ui.common.ControleGastosTopBar
import com.flpzarato.controlegastos.ui.common.formatCompetenceLabel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun InvoiceListScreen(
    onOpenInvoice: (competenceMonth: String) -> Unit,
    onBack: () -> Unit,
    viewModel: InvoiceListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val currencyFormat = remember(Locale("pt", "BR")) { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }

    Scaffold(
        topBar = { ControleGastosTopBar(title = "Faturas · ${state.cardNickname}", onBack = onBack) },
    ) { padding ->
        if (state.invoices.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Nenhum lançamento neste cartão ainda", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.invoices, key = { it.competenceMonth }) { invoice ->
                    Card(
                        onClick = { onOpenInvoice(invoice.competenceMonth) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    formatCompetenceLabel(invoice.competenceMonth),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Text(
                                    "${invoice.count} lançamento(s)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                currencyFormat.format(invoice.total),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}
