package com.flpzarato.controlegastos.ui.recurring

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flpzarato.controlegastos.data.local.entity.PaymentMethod
import com.flpzarato.controlegastos.ui.common.ControleGastosTopBar
import com.flpzarato.controlegastos.ui.common.CurrencyAmountField
import com.flpzarato.controlegastos.ui.expense.OBSERVATION_MAX_LENGTH

@Composable
fun RecurringFormScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: RecurringFormViewModel = hiltViewModel(),
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
                title = if (viewModel.isEditing) "Editar recorrente" else "Novo recorrente",
                onBack = onBack,
            )
        },
    ) { padding ->
        if (form.isLoading) {
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
                label = { Text("Descrição (ex: Netflix, Aluguel)") },
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
            OutlinedTextField(
                value = form.dayOfMonth,
                onValueChange = viewModel::onDayOfMonthChanged,
                label = { Text("Dia do mês (1-28)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))

            RecurringDropdown(
                options = categories.map { it.id to it.name },
                selectedId = form.categoryId,
                onSelected = viewModel::onCategoryChanged,
                label = "Categoria",
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
                RecurringDropdown(
                    options = cards.map { it.id to it.nickname },
                    selectedId = form.cardId,
                    onSelected = viewModel::onCardChanged,
                    label = "Cartão",
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
                        else -> "Salvar recorrente"
                    },
                )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurringDropdown(
    options: List<Pair<String, String>>,
    selectedId: String?,
    onSelected: (String) -> Unit,
    label: String,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = options.firstOrNull { it.first == selectedId }?.second.orEmpty()

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
            options.forEach { (id, name) ->
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
