package com.flpzarato.controlegastos.ui.payments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flpzarato.controlegastos.data.local.entity.ExpenseEntity
import com.flpzarato.controlegastos.data.local.entity.PaymentMethod
import com.flpzarato.controlegastos.ui.common.ControleGastosTopBar
import com.flpzarato.controlegastos.ui.common.EmptyState
import com.flpzarato.controlegastos.ui.common.FilterOption
import com.flpzarato.controlegastos.ui.common.FilterSection
import com.flpzarato.controlegastos.ui.common.FiltersBottomSheet
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
    var filtersSheetOpen by remember { mutableStateOf(false) }

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

            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, start = 16.dp, end = 16.dp)) {
                BadgedBox(
                    badge = {
                        if (state.activeFilterCount > 0) Badge { Text("${state.activeFilterCount}") }
                    },
                ) {
                    AssistChip(
                        onClick = {
                            viewModel.onFiltersSheetOpened()
                            filtersSheetOpen = true
                        },
                        label = { Text("Filtros") },
                        leadingIcon = { Icon(Icons.Filled.FilterList, contentDescription = null) },
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
                                    Text(
                                        "${expense.purchaseDate.format(dateFormatter)} · " +
                                            (state.categoryNames[expense.categoryId] ?: "—"),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Icon(
                                        paymentMethodIcon(expense.paymentMethod),
                                        contentDescription = paymentMethodLabel(expense.paymentMethod),
                                        modifier = Modifier.padding(top = 2.dp).size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
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

    if (filtersSheetOpen) {
        FiltersBottomSheet(
            sections = listOf(
                FilterSection(
                    title = "Forma de pagamento",
                    options = PaymentMethod.entries.map { FilterOption(it.name, paymentMethodLabel(it)) },
                    selectedIds = state.pendingPaymentMethods.map { it.name }.toSet(),
                    onToggle = { id -> viewModel.onPendingPaymentMethodFilterToggled(PaymentMethod.valueOf(id)) },
                    onAllSelected = viewModel::onPendingPaymentMethodFiltersCleared,
                ),
                FilterSection(
                    title = "Categorias",
                    options = state.allCategories.map { FilterOption(it.id, it.name) },
                    selectedIds = state.pendingCategoryIds,
                    onToggle = viewModel::onPendingCategoryFilterToggled,
                    onAllSelected = viewModel::onPendingCategoryFiltersCleared,
                ),
            ),
            onCancel = {
                viewModel.onFiltersCancelled()
                filtersSheetOpen = false
            },
            onApply = {
                viewModel.onFiltersApplied()
                filtersSheetOpen = false
            },
        )
    }
}
