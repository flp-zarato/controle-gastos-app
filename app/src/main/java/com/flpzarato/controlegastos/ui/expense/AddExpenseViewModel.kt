package com.flpzarato.controlegastos.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flpzarato.controlegastos.data.local.entity.CardEntity
import com.flpzarato.controlegastos.data.local.entity.CategoryEntity
import com.flpzarato.controlegastos.data.local.entity.PaymentMethod
import com.flpzarato.controlegastos.data.prefs.UserPreferences
import com.flpzarato.controlegastos.data.repository.CardRepository
import com.flpzarato.controlegastos.data.repository.CategoryRepository
import com.flpzarato.controlegastos.data.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject

data class AddExpenseFormState(
    val description: String = "",
    val amount: String = "",
    val categoryId: String? = null,
    val purchaseDate: LocalDate = LocalDate.now(),
    val paymentMethod: PaymentMethod = PaymentMethod.DINHEIRO,
    val cardId: String? = null,
    val installmentCount: String = "1",
    val isSaving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
)

@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    categoryRepository: CategoryRepository,
    cardRepository: CardRepository,
    private val userPreferences: UserPreferences,
) : ViewModel() {

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val cards: StateFlow<List<CardEntity>> = cardRepository.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _form = MutableStateFlow(AddExpenseFormState())
    val form: StateFlow<AddExpenseFormState> = _form.asStateFlow()

    fun onDescriptionChanged(value: String) = update { it.copy(description = value) }
    fun onAmountChanged(value: String) = update { it.copy(amount = value) }
    fun onCategoryChanged(id: String) = update { it.copy(categoryId = id) }
    fun onDateChanged(date: LocalDate) = update { it.copy(purchaseDate = date) }
    fun onPaymentMethodChanged(method: PaymentMethod) = update {
        it.copy(paymentMethod = method, cardId = if (method == PaymentMethod.CREDITO) it.cardId else null)
    }
    fun onCardChanged(id: String) = update { it.copy(cardId = id) }
    fun onInstallmentCountChanged(value: String) = update { it.copy(installmentCount = value.filter(Char::isDigit)) }

    private fun update(transform: (AddExpenseFormState) -> AddExpenseFormState) {
        _form.value = transform(_form.value)
    }

    fun save() {
        val current = _form.value
        val amount = current.amount.replace(",", ".").toBigDecimalOrNull()
        val categoryId = current.categoryId
        when {
            amount == null || amount <= BigDecimal.ZERO -> update { it.copy(error = "Informe um valor válido") }
            categoryId == null -> update { it.copy(error = "Escolha uma categoria") }
            current.description.isBlank() -> update { it.copy(error = "Descreva o gasto") }
            current.paymentMethod == PaymentMethod.CREDITO && current.cardId == null ->
                update { it.copy(error = "Escolha o cartão usado") }
            else -> viewModelScope.launch {
                update { it.copy(isSaving = true, error = null) }
                runCatching {
                    val email = requireNotNull(userPreferences.signedInEmail.first()) { "Usuário não autenticado" }
                    expenseRepository.addExpense(
                        description = current.description.trim(),
                        totalAmount = amount,
                        categoryId = categoryId,
                        purchaseDate = current.purchaseDate,
                        paymentMethod = current.paymentMethod,
                        loggedByEmail = email,
                        cardId = current.cardId,
                        installmentCount = current.installmentCount.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                    )
                }.onSuccess {
                    _form.value = AddExpenseFormState(saved = true)
                }.onFailure { e ->
                    update { it.copy(isSaving = false, error = e.message ?: "Erro ao salvar") }
                }
            }
        }
    }

    fun consumeSavedFlag() = update { it.copy(saved = false) }
}
