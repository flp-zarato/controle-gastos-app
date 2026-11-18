package com.flpzarato.controlegastos.ui.listing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flpzarato.controlegastos.data.local.dao.CategoryDao
import com.flpzarato.controlegastos.data.local.dao.UserDao
import com.flpzarato.controlegastos.data.local.entity.CategoryEntity
import com.flpzarato.controlegastos.data.local.entity.ExpenseEntity
import com.flpzarato.controlegastos.data.local.entity.PaymentMethod
import com.flpzarato.controlegastos.data.local.entity.UserEntity
import com.flpzarato.controlegastos.data.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

enum class MonthFilter { ATUAL, ANTERIOR }

data class ExpenseListUiState(
    val filter: MonthFilter = MonthFilter.ATUAL,
    val expenses: List<ExpenseEntity> = emptyList(),
    val categoryNames: Map<String, String> = emptyMap(),
    val userNames: Map<String, String> = emptyMap(),
    val allCategories: List<CategoryEntity> = emptyList(),
    val allUsers: List<UserEntity> = emptyList(),
    val selectedUserEmails: Set<String> = emptySet(),
    val selectedCategoryIds: Set<String> = emptySet(),
    val selectedPaymentMethods: Set<PaymentMethod> = emptySet(),
    val pendingUserEmails: Set<String> = emptySet(),
    val pendingCategoryIds: Set<String> = emptySet(),
    val pendingPaymentMethods: Set<PaymentMethod> = emptySet(),
    val activeFilterCount: Int = 0,
    val isLoading: Boolean = true,
) {
    val total get() = expenses.sumOf { it.totalAmount ?: it.installmentAmount }
}

private fun <T> Set<T>.toggled(item: T): Set<T> = if (item in this) this - item else this + item

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExpenseListViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    categoryDao: CategoryDao,
    userDao: UserDao,
) : ViewModel() {

    private val filter = MutableStateFlow(MonthFilter.ATUAL)
    private val selectedUserEmails = MutableStateFlow<Set<String>>(emptySet())
    private val selectedCategoryIds = MutableStateFlow<Set<String>>(emptySet())
    private val selectedPaymentMethods = MutableStateFlow<Set<PaymentMethod>>(emptySet())

    /** Draft selection shown/edited inside the filters bottom sheet until "Aplicar" is tapped. */
    private val pendingUserEmails = MutableStateFlow<Set<String>>(emptySet())
    private val pendingCategoryIds = MutableStateFlow<Set<String>>(emptySet())
    private val pendingPaymentMethods = MutableStateFlow<Set<PaymentMethod>>(emptySet())

    private val expensesForFilter = filter.flatMapLatest { selected ->
        val month = if (selected == MonthFilter.ATUAL) YearMonth.now() else YearMonth.now().minusMonths(1)
        expenseRepository.observePurchaseGroupsForMonths(listOf(month))
    }

    private val selectedFilters = combine(
        selectedUserEmails,
        selectedCategoryIds,
        selectedPaymentMethods,
        ::Triple,
    )

    private val pendingFilters = combine(
        pendingUserEmails,
        pendingCategoryIds,
        pendingPaymentMethods,
        ::Triple,
    )

    private val baseState: Flow<ExpenseListUiState> = combine(
        filter,
        expensesForFilter,
        categoryDao.observeActive(),
        userDao.observeAll(),
        selectedFilters,
    ) { selectedFilter, expenses, categories, users, (selUsers, selCategories, selMethods) ->
        val filtered = expenses.filter { expense ->
            (selUsers.isEmpty() || expense.loggedByEmail in selUsers) &&
                (selCategories.isEmpty() || expense.categoryId in selCategories) &&
                (selMethods.isEmpty() || expense.paymentMethod in selMethods)
        }
        ExpenseListUiState(
            filter = selectedFilter,
            expenses = filtered,
            categoryNames = categories.associate { it.id to it.name },
            userNames = users.associate { it.email to it.displayName },
            allCategories = categories,
            allUsers = users,
            selectedUserEmails = selUsers,
            selectedCategoryIds = selCategories,
            selectedPaymentMethods = selMethods,
            isLoading = false,
        )
    }

    val uiState: StateFlow<ExpenseListUiState> = combine(
        baseState,
        pendingFilters,
    ) { state, (pendUsers, pendCategories, pendMethods) ->
        state.copy(
            pendingUserEmails = pendUsers,
            pendingCategoryIds = pendCategories,
            pendingPaymentMethods = pendMethods,
            activeFilterCount = listOf(
                state.selectedPaymentMethods.isNotEmpty(),
                state.selectedCategoryIds.isNotEmpty(),
                state.selectedUserEmails.isNotEmpty(),
            ).count { it },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExpenseListUiState())

    fun onFilterChanged(newFilter: MonthFilter) {
        filter.value = newFilter
    }

    fun onUserFilterToggled(email: String) {
        selectedUserEmails.value = selectedUserEmails.value.toggled(email)
    }

    fun onCategoryFilterToggled(categoryId: String) {
        selectedCategoryIds.value = selectedCategoryIds.value.toggled(categoryId)
    }

    fun onPaymentMethodFilterToggled(method: PaymentMethod) {
        selectedPaymentMethods.value = selectedPaymentMethods.value.toggled(method)
    }

    /** Called right before showing the sheet: seeds the draft from the already-applied filters. */
    fun onFiltersSheetOpened() {
        pendingUserEmails.value = selectedUserEmails.value
        pendingCategoryIds.value = selectedCategoryIds.value
        pendingPaymentMethods.value = selectedPaymentMethods.value
    }

    fun onPendingUserFilterToggled(email: String) {
        pendingUserEmails.value = pendingUserEmails.value.toggled(email)
    }

    fun onPendingCategoryFilterToggled(categoryId: String) {
        pendingCategoryIds.value = pendingCategoryIds.value.toggled(categoryId)
    }

    fun onPendingPaymentMethodFilterToggled(method: PaymentMethod) {
        pendingPaymentMethods.value = pendingPaymentMethods.value.toggled(method)
    }

    fun onPendingUserFiltersCleared() {
        pendingUserEmails.value = emptySet()
    }

    fun onPendingCategoryFiltersCleared() {
        pendingCategoryIds.value = emptySet()
    }

    fun onPendingPaymentMethodFiltersCleared() {
        pendingPaymentMethods.value = emptySet()
    }

    /** Copies the draft into the applied filters — the only point where the shown list actually changes. */
    fun onFiltersApplied() {
        selectedUserEmails.value = pendingUserEmails.value
        selectedCategoryIds.value = pendingCategoryIds.value
        selectedPaymentMethods.value = pendingPaymentMethods.value
    }

    /** Discards the draft. Applied filters are untouched; the next sheet open reseeds the draft from them. */
    fun onFiltersCancelled() = Unit

    fun deleteGroup(groupId: String) {
        viewModelScope.launch { expenseRepository.deleteGroup(groupId) }
    }

    fun togglePaid(expenseId: String, paid: Boolean) {
        viewModelScope.launch {
            if (paid) expenseRepository.markPaid(expenseId) else expenseRepository.markUnpaid(expenseId)
        }
    }
}
