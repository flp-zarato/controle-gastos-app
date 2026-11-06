package com.flpzarato.controlegastos.ui.expense

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flpzarato.controlegastos.data.local.entity.CardEntity
import com.flpzarato.controlegastos.data.local.entity.CategoryEntity
import com.flpzarato.controlegastos.data.local.entity.PaymentMethod
import com.flpzarato.controlegastos.data.prefs.UserPreferences
import com.flpzarato.controlegastos.data.repository.CardRepository
import com.flpzarato.controlegastos.data.repository.CategoryRepository
import com.flpzarato.controlegastos.data.repository.ExpenseRepository
import com.flpzarato.controlegastos.domain.InstallmentCalculator
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

const val OBSERVATION_MAX_LENGTH = 30

data class AddExpenseFormState(
    val description: String = "",
    val amount: String = "",
    val observation: String = "",
    val categoryId: String? = null,
    val purchaseDate: LocalDate = LocalDate.now(),
    val paymentMethod: PaymentMethod = PaymentMethod.DINHEIRO,
    val cardId: String? = null,
    val installmentCount: String = "1",
    val isSaving: Boolean = false,
    val isLoadingForEdit: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
    /** Non-null while the credit-card invoice warning dialog is awaiting confirmation; holds the computed "yyyy-MM". */
    val invoiceWarningMonth: String? = null,
    /** Set only when editing: who originally logged this, preserved instead of swapped to the editor. */
    val originalLoggedByEmail: String? = null,
    /** Set only when editing: the group's current competência, used as the baseline the warning compares against. */
    val originalCompetenceMonth: String? = null,
)

@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    categoryRepository: CategoryRepository,
    cardRepository: CardRepository,
    private val userPreferences: UserPreferences,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val editingGroupId: String? = savedStateHandle["groupId"]
    val isEditing: Boolean = editingGroupId != null

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val cards: StateFlow<List<CardEntity>> = cardRepository.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _form = MutableStateFlow(AddExpenseFormState(isLoadingForEdit = isEditing))
    val form: StateFlow<AddExpenseFormState> = _form.asStateFlow()

    init {
        editingGroupId?.let { groupId ->
            viewModelScope.launch {
                val original = expenseRepository.getGroup(groupId).minByOrNull { it.installmentNumber }
                if (original == null) {
                    update { it.copy(isLoadingForEdit = false, error = "Lançamento não encontrado") }
                    return@launch
                }
                _form.value = AddExpenseFormState(
                    description = original.description,
                    amount = (original.totalAmount ?: original.installmentAmount).toPlainString(),
                    observation = original.observation.orEmpty(),
                    categoryId = original.categoryId,
                    purchaseDate = original.purchaseDate,
                    paymentMethod = original.paymentMethod,
                    cardId = original.cardId,
                    installmentCount = original.installmentCount.toString(),
                    originalLoggedByEmail = original.loggedByEmail,
                    originalCompetenceMonth = original.competenceMonth,
                )
            }
        }
    }

    fun onDescriptionChanged(value: String) = update { it.copy(description = value) }
    fun onAmountChanged(value: String) = update { it.copy(amount = value) }
    fun onObservationChanged(value: String) = update { it.copy(observation = value.take(OBSERVATION_MAX_LENGTH)) }
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

    fun save() = attemptSave(skipWarning = false)

    fun confirmInvoiceWarning() = attemptSave(skipWarning = true)

    fun dismissInvoiceWarning() = update { it.copy(invoiceWarningMonth = null) }

    fun consumeSavedFlag() = update { it.copy(saved = false) }

    private fun attemptSave(skipWarning: Boolean) {
        val current = _form.value
        val amount = current.amount.replace(",", ".").toBigDecimalOrNull()
        val categoryId = current.categoryId
        val installmentCount = current.installmentCount.toIntOrNull()?.coerceAtLeast(1) ?: 1
        when {
            amount == null || amount <= BigDecimal.ZERO -> update { it.copy(error = "Informe um valor válido") }
            categoryId == null -> update { it.copy(error = "Escolha uma categoria") }
            current.description.isBlank() -> update { it.copy(error = "Descreva o gasto") }
            current.paymentMethod == PaymentMethod.CREDITO && current.cardId == null ->
                update { it.copy(error = "Escolha o cartão usado") }
            else -> viewModelScope.launch {
                if (!skipWarning && current.paymentMethod == PaymentMethod.CREDITO) {
                    val card = cards.value.firstOrNull { it.id == current.cardId }
                    if (card != null) {
                        val newCompetence = InstallmentCalculator.calculate(
                            totalAmount = amount,
                            installmentCount = installmentCount,
                            purchaseDate = current.purchaseDate,
                            cardClosingDay = card.closingDay,
                        ).first().competenceMonth
                        // Creating: compare against the actual current month. Editing: compare
                        // against the group's own prior competência, so untouched old expenses
                        // don't re-trigger the warning just for having a past purchase date.
                        val baseline = current.originalCompetenceMonth
                            ?: InstallmentCalculator.singleInstallmentCompetence(LocalDate.now())
                        if (newCompetence != baseline) {
                            update { it.copy(invoiceWarningMonth = newCompetence, error = null) }
                            return@launch
                        }
                    }
                }

                update { it.copy(invoiceWarningMonth = null, isSaving = true, error = null) }
                runCatching {
                    val email = current.originalLoggedByEmail
                        ?: requireNotNull(userPreferences.signedInEmail.first()) { "Usuário não autenticado" }
                    val groupId = editingGroupId
                    val observation = current.observation.trim().takeIf { it.isNotEmpty() }
                    if (groupId != null) {
                        expenseRepository.updateExpense(
                            oldGroupId = groupId,
                            description = current.description.trim(),
                            totalAmount = amount,
                            categoryId = categoryId,
                            purchaseDate = current.purchaseDate,
                            paymentMethod = current.paymentMethod,
                            loggedByEmail = email,
                            cardId = current.cardId,
                            installmentCount = installmentCount,
                            observation = observation,
                        )
                    } else {
                        expenseRepository.addExpense(
                            description = current.description.trim(),
                            totalAmount = amount,
                            categoryId = categoryId,
                            purchaseDate = current.purchaseDate,
                            paymentMethod = current.paymentMethod,
                            loggedByEmail = email,
                            cardId = current.cardId,
                            installmentCount = installmentCount,
                            observation = observation,
                        )
                    }
                }.onSuccess {
                    _form.value = AddExpenseFormState(saved = true)
                }.onFailure { e ->
                    update { it.copy(isSaving = false, error = e.message ?: "Erro ao salvar") }
                }
            }
        }
    }
}
