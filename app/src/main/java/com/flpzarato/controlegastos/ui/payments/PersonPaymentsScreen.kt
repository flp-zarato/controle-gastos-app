package com.flpzarato.controlegastos.ui.payments

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flpzarato.controlegastos.data.local.entity.ExpenseEntity
import com.flpzarato.controlegastos.data.local.entity.PaymentMethod
import com.flpzarato.controlegastos.ui.common.ControleGastosTopBar
import com.flpzarato.controlegastos.ui.common.EmptyState
import com.flpzarato.controlegastos.ui.common.paymentMethodIcon
import com.flpzarato.controlegastos.ui.common.paymentMethodLabel
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PersonPaymentsScreen(
    onBack: () -> Unit,
    viewModel: PersonPaymentsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val currencyFormat = remember(Locale("pt", "BR")) { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    var selectedTab by remember { mutableIntStateOf(1) }
    var pendingUnmarkExpenseId by remember { mutableStateOf<String?>(null) }

    val expenses = if (selectedTab == 0) state.paid else state.unpaid
    val total = if (selectedTab == 0) state.paidTotal else state.unpaidTotal

    Scaffold(
        topBar = {
            ControleGastosTopBar(
                title = state.displayName.ifBlank { "Pagamentos" },
                onBack = onBack,
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Pagos") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("A pagar") })
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(PaymentMethod.entries.toList()) { method ->
                    FilterChip(
                        selected = method in state.selectedPaymentMethods,
                        onClick = { viewModel.onPaymentMethodFilterToggled(method) },
                        label = { Text(paymentMethodLabel(method)) },
                        leadingIcon = {
                            Icon(
                                paymentMethodIcon(method),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                    )
                }
            }
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.allCategories, key = { it.id }) { category ->
                    FilterChip(
                        selected = category.id in state.selectedCategoryIds,
                        onClick = { viewModel.onCategoryFilterToggled(category.id) },
                        label = { Text(category.name) },
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Total: ", style = MaterialTheme.typography.titleMedium)
                Text(
                    currencyFormat.format(total),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            if (expenses.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.List,
                    message = if (selectedTab == 0) "Nenhum lançamento pago ainda" else "Nenhum lançamento a pagar",
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(expenses, key = ExpenseEntity::id) { expense ->
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
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "${expense.purchaseDate.format(dateFormatter)} · " +
                                                (state.categoryNames[expense.categoryId] ?: "—"),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Icon(
                                            paymentMethodIcon(expense.paymentMethod),
                                            contentDescription = paymentMethodLabel(expense.paymentMethod),
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                Row {
                                    Text(
                                        currencyFormat.format(expense.installmentAmount),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    if (selectedTab == 0) {
                                        IconButton(onClick = { pendingUnmarkExpenseId = expense.id }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Remover marcação de pago")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pendingUnmarkExpenseId?.let { expenseId ->
        AlertDialog(
            onDismissRequest = { pendingUnmarkExpenseId = null },
            title = { Text("Remover marcação de pago?") },
            text = { Text("Remover a marcação de pago desse lançamento?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.togglePaid(expenseId, paid = false)
                    pendingUnmarkExpenseId = null
                }) { Text("Remover") }
            },
            dismissButton = { TextButton(onClick = { pendingUnmarkExpenseId = null }) { Text("Cancelar") } },
        )
    }
}
