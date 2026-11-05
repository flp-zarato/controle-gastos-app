package com.flpzarato.controlegastos.ui.listing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flpzarato.controlegastos.data.local.dao.CategoryDao
import com.flpzarato.controlegastos.data.local.dao.UserDao
import com.flpzarato.controlegastos.data.local.entity.ExpenseEntity
import com.flpzarato.controlegastos.data.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
) {
    val total get() = expenses.sumOf { it.installmentAmount }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExpenseListViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    categoryDao: CategoryDao,
    userDao: UserDao,
) : ViewModel() {

    private val filter = MutableStateFlow(MonthFilter.ATUAL)

    private val expensesForFilter = filter.flatMapLatest { selected ->
        val month = if (selected == MonthFilter.ATUAL) YearMonth.now() else YearMonth.now().minusMonths(1)
        expenseRepository.observeMonths(listOf(month))
    }

    val uiState: StateFlow<ExpenseListUiState> = combine(
        filter,
        expensesForFilter,
        categoryDao.observeActive(),
        userDao.observeAll(),
    ) { selectedFilter, expenses, categories, users ->
        ExpenseListUiState(
            filter = selectedFilter,
            expenses = expenses,
            categoryNames = categories.associate { it.id to it.name },
            userNames = users.associate { it.email to it.displayName },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExpenseListUiState())

    fun onFilterChanged(newFilter: MonthFilter) {
        filter.value = newFilter
    }

    fun deleteGroup(groupId: String) {
        viewModelScope.launch { expenseRepository.deleteGroup(groupId) }
    }
}
