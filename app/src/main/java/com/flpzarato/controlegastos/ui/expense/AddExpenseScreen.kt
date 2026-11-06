package com.flpzarato.controlegastos.ui.expense

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.flpzarato.controlegastos.data.local.entity.PaymentMethod
import com.flpzarato.controlegastos.ui.common.ControleGastosTopBar
import com.flpzarato.controlegastos.ui.common.CurrencyAmountField
import com.flpzarato.controlegastos.ui.common.formatCompetenceLabel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Composable
fun AddExpenseScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: AddExpenseViewModel = hiltViewModel(),
) {
    val form by viewModel.form.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val cards by viewModel.cards.collectAsState()

    LaunchedEffect(form.saved) {
        if (form.saved) {
            viewModel.consumeSavedFlag()
            onSaved()
        }
    }

    Scaffold(
        topBar = {
            ControleGastosTopBar(
                title = if (viewModel.isEditing) "Editar gasto" else "Novo gasto",
                onBack = onBack,
            )
        },
    ) { padding ->
        if (form.isLoadingForEdit) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            OutlinedTextField(
                value = form.description,
                onValueChange = viewModel::onDescriptionChanged,
                label = { Text("Descrição") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            CurrencyAmountField(
                digitsValue = form.amount,
                onDigitsChanged = viewModel::onAmountChanged,
                label = "Valor (R$)",
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))

            PurchaseDateField(date = form.purchaseDate, onDateSelected = viewModel::onDateChanged)
            Spacer(Modifier.height(12.dp))

            CategoryDropdown(
                categories = categories.map { it.id to it.name },
                selectedId = form.categoryId,
                onSelected = viewModel::onCategoryChanged,
            )
            Spacer(Modifier.height(12.dp))

            Text("Forma de pagamento", style = MaterialTheme.typography.labelLarge)
            Row(modifier = Modifier.padding(top = 4.dp)) {
                PaymentMethod.entries.forEach { method ->
                    FilterChip(
                        selected = form.paymentMethod == method,
                        onClick = { viewModel.onPaymentMethodChanged(method) },
                        label = { Text(method.label()) },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }

            if (form.paymentMethod == PaymentMethod.CREDITO) {
                Spacer(Modifier.height(12.dp))
                CategoryDropdown(
                    categories = cards.map { it.id to it.nickname },
                    selectedId = form.cardId,
                    onSelected = viewModel::onCardChanged,
                    label = "Cartão",
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = form.installmentCount,
                    onValueChange = viewModel::onInstallmentCountChanged,
                    label = { Text("Número de parcelas") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = form.observation,
                    onValueChange = viewModel::onObservationChanged,
                    label = { Text("Observação (opcional)") },
                    supportingText = { Text("${form.observation.length}/$OBSERVATION_MAX_LENGTH") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            form.error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = viewModel::save,
                enabled = !form.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    when {
                        form.isSaving -> "Salvando…"
                        viewModel.isEditing -> "Salvar alterações"
                        else -> "Salvar gasto"
                    },
                )
            }
        }
    }

    form.invoiceWarningMonth?.let { competence ->
        AlertDialog(
            onDismissRequest = viewModel::dismissInvoiceWarning,
            title = { Text("Atenção") },
            text = {
                Text(
                    "Como esse gasto foi feito no cartão de crédito, ele será lançado na fatura de " +
                        "${formatCompetenceLabel(competence)}, com base no fechamento do cartão, e não " +
                        "necessariamente na fatura atual. Confira se está correto antes de salvar.",
                )
            },
            confirmButton = { TextButton(onClick = viewModel::confirmInvoiceWarning) { Text("Confirmar") } },
            dismissButton = { TextButton(onClick = viewModel::dismissInvoiceWarning) { Text("Revisar") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PurchaseDateField(date: LocalDate, onDateSelected: (LocalDate) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    val formatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }

    OutlinedTextField(
        value = date.format(formatter),
        onValueChange = {},
        readOnly = true,
        label = { Text("Data da compra") },
        trailingIcon = {
            IconButton(onClick = { showPicker = true }) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = "Selecionar data")
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )

    if (showPicker) {
        val initialMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis <= System.currentTimeMillis()
            },
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onDateSelected(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancelar") } },
        ) {
            DatePicker(state = state)
        }
    }
}

private fun PaymentMethod.label(): String = when (this) {
    PaymentMethod.DINHEIRO -> "Dinheiro"
    PaymentMethod.DEBITO -> "Débito"
    PaymentMethod.CREDITO -> "Crédito"
    PaymentMethod.PIX -> "Pix"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    categories: List<Pair<String, String>>,
    selectedId: String?,
    onSelected: (String) -> Unit,
    label: String = "Categoria",
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = categories.firstOrNull { it.first == selectedId }?.second.orEmpty()

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            categories.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onSelected(id)
                        expanded = false
                    },
                )
            }
        }
    }
}
