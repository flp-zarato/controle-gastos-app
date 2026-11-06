package com.flpzarato.controlegastos.ui.recurring

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flpzarato.controlegastos.data.local.entity.CardEntity
import com.flpzarato.controlegastos.data.local.entity.CategoryEntity
import com.flpzarato.controlegastos.data.local.entity.PaymentMethod
import com.flpzarato.controlegastos.data.repository.CardRepository
import com.flpzarato.controlegastos.data.repository.CategoryRepository
import com.flpzarato.controlegastos.data.repository.RecurringExpenseRepository
import com.flpzarato.controlegastos.ui.common.bigDecimalToCentsString
import com.flpzarato.controlegastos.ui.common.centsStringToBigDecimal
import com.flpzarato.controlegastos.ui.expense.OBSERVATION_MAX_LENGTH
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

data class RecurringFormState(
    val description: String = "",
    val amount: String = "",
    val observation: String = "",
    val categoryId: String? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.DINHEIRO,
    val cardId: String? = null,
    val dayOfMonth: String = "1",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
)

@HiltViewModel
class RecurringFormViewModel @Inject constructor(
    private val recurringExpenseRepository: RecurringExpenseRepository,
    categoryRepository: CategoryRepository,
    cardRepository: CardRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val editingId: String? = savedStateHandle["recurringId"]
    val isEditing: Boolean = editingId != null

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val cards: StateFlow<List<CardEntity>> = cardRepository.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _form = MutableStateFlow(RecurringFormState(isLoading = isEditing))
    val form: StateFlow<RecurringFormState> = _form.asStateFlow()

    init {
        editingId?.let { id ->
            viewModelScope.launch {
                val existing = recurringExpenseRepository.getById(id)
                if (existing == null) {
                    update { it.copy(isLoading = false, error = "Recorrente não encontrado") }
                    return@launch
                }
                _form.value = RecurringFormState(
                    description = existing.description,
                    amount = bigDecimalToCentsString(existing.amount),
                    observation = existing.observation.orEmpty(),
                    categoryId = existing.categoryId,
                    paymentMethod = existing.paymentMethod,
                    cardId = existing.cardId,
                    dayOfMonth = existing.dayOfMonth.toString(),
                )
            }
        }
    }

    fun onDescriptionChanged(value: String) = update { it.copy(description = value) }
    fun onAmountChanged(value: String) = update { it.copy(amount = value.filter(Char::isDigit).take(12)) }
    fun onObservationChanged(value: String) = update { it.copy(observation = value.take(OBSERVATION_MAX_LENGTH)) }
    fun onCategoryChanged(id: String) = update { it.copy(categoryId = id) }
    fun onPaymentMethodChanged(method: PaymentMethod) = update {
        it.copy(
            paymentMethod = method,
            cardId = if (method == PaymentMethod.CREDITO) it.cardId else null,
            observation = if (method == PaymentMethod.CREDITO) it.observation else "",
        )
    }
    fun onCardChanged(id: String) = update { it.copy(cardId = id) }
    fun onDayOfMonthChanged(value: String) = update { it.copy(dayOfMonth = value.filter(Char::isDigit).take(2)) }

    private fun update(transform: (RecurringFormState) -> RecurringFormState) {
        _form.value = transform(_form.value)
    }

    fun consumeSavedFlag() = update { it.copy(saved = false) }

    fun save() {
        val current = _form.value
        val amount = centsStringToBigDecimal(current.amount)
        val categoryId = current.categoryId
        val day = current.dayOfMonth.toIntOrNull()
        when {
            amount == null || amount <= BigDecimal.ZERO -> update { it.copy(error = "Informe um valor válido") }
            categoryId == null -> update { it.copy(error = "Escolha uma categoria") }
            current.description.isBlank() -> update { it.copy(error = "Descreva o gasto recorrente") }
            current.paymentMethod == PaymentMethod.CREDITO && current.cardId == null ->
                update { it.copy(error = "Escolha o cartão usado") }
            day == null || day !in 1..28 -> update { it.copy(error = "Dia do mês deve estar entre 1 e 28") }
            else -> viewModelScope.launch {
                update { it.copy(isSaving = true, error = null) }
                runCatching {
                    recurringExpenseRepository.save(
                        id = editingId,
                        description = current.description.trim(),
                        amount = amount,
                        categoryId = categoryId,
                        paymentMethod = current.paymentMethod,
                        cardId = current.cardId,
                        dayOfMonth = day,
                        observation = current.observation.trim().takeIf { it.isNotEmpty() },
                    )
                }.onSuccess {
                    _form.value = RecurringFormState(saved = true)
                }.onFailure { e ->
                    update { it.copy(isSaving = false, error = e.message ?: "Erro ao salvar") }
                }
            }
        }
    }
}
