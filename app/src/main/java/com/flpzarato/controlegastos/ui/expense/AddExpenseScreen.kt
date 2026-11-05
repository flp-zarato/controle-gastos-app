package com.flpzarato.controlegastos.ui.expense

import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flpzarato.controlegastos.data.local.entity.PaymentMethod

@Composable
fun AddExpenseScreen(
    onSaved: () -> Unit,
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

    Scaffold { padding ->
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
            OutlinedTextField(
                value = form.amount,
                onValueChange = viewModel::onAmountChanged,
                label = { Text("Valor (R$)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
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
                Text(if (form.isSaving) "Salvando…" else "Salvar gasto")
            }
        }
    }
}

private fun PaymentMethod.label(): String = when (this) {
    PaymentMethod.DINHEIRO -> "Dinheiro"
    PaymentMethod.DEBITO -> "Débito"
    PaymentMethod.CREDITO -> "Crédito"
    PaymentMethod.PIX -> "Pix"
}

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
