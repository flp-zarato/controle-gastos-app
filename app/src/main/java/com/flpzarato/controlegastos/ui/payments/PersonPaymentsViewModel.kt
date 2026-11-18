package com.flpzarato.controlegastos.ui.payments

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flpzarato.controlegastos.data.local.dao.CategoryDao
import com.flpzarato.controlegastos.data.local.dao.UserDao
import com.flpzarato.controlegastos.data.local.entity.CategoryEntity
import com.flpzarato.controlegastos.data.local.entity.ExpenseEntity
import com.flpzarato.controlegastos.data.local.entity.PaymentMethod
import com.flpzarato.controlegastos.data.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

data class PersonPaymentsUiState(
    val displayName: String = "",
    val paid: List<ExpenseEntity> = emptyList(),
    val unpaid: List<ExpenseEntity> = emptyList(),
    val categoryNames: Map<String, String> = emptyMap(),
    val allCategories: List<CategoryEntity> = emptyList(),
    val selectedCategoryIds: Set<String> = emptySet(),
    val selectedPaymentMethods: Set<PaymentMethod> = emptySet(),
) {
    val paidTotal: BigDecimal get() = paid.sumOf { it.installmentAmount }
    val unpaidTotal: BigDecimal get() = unpaid.sumOf { it.installmentAmount }
}

private fun <T> Set<T>.toggled(item: T): Set<T> = if (item in this) this - item else this + item

@HiltViewModel
class PersonPaymentsViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    categoryDao: CategoryDao,
    userDao: UserDao,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val email: String = requireNotNull(savedStateHandle.get<String>("email")) { "email é obrigatório" }
    private val selectedCategoryIds = MutableStateFlow<Set<String>>(emptySet())
    private val selectedPaymentMethods = MutableStateFlow<Set<PaymentMethod>>(emptySet())

    val uiState: StateFlow<PersonPaymentsUiState> = combine(
        expenseRepository.observeCurrentAndPreviousMonth(),
        categoryDao.observeActive(),
        userDao.observeAll(),
        combine(selectedCategoryIds, selectedPaymentMethods, ::Pair),
    ) { expenses, categories, users, (selCategories, selMethods) ->
        val filtered = expenses.filter { expense ->
            expense.loggedByEmail == email &&
                (selCategories.isEmpty() || expense.categoryId in selCategories) &&
                (selMethods.isEmpty() || expense.paymentMethod in selMethods)
        }
        PersonPaymentsUiState(
            displayName = users.firstOrNull { it.email == email }?.displayName ?: email,
            paid = filtered.filter { it.paid },
            unpaid = filtered.filter { !it.paid },
            categoryNames = categories.associate { it.id to it.name },
            allCategories = categories,
            selectedCategoryIds = selCategories,
            selectedPaymentMethods = selMethods,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PersonPaymentsUiState())

    fun onCategoryFilterToggled(categoryId: String) {
        selectedCategoryIds.value = selectedCategoryIds.value.toggled(categoryId)
    }

    fun onPaymentMethodFilterToggled(method: PaymentMethod) {
        selectedPaymentMethods.value = selectedPaymentMethods.value.toggled(method)
    }

    fun togglePaid(expenseId: String, paid: Boolean) {
        viewModelScope.launch {
            if (paid) expenseRepository.markPaid(expenseId) else expenseRepository.markUnpaid(expenseId)
        }
    }
}
