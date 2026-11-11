package com.flpzarato.controlegastos.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flpzarato.controlegastos.data.local.dao.CategoryDao
import com.flpzarato.controlegastos.data.local.dao.UserDao
import com.flpzarato.controlegastos.data.repository.ExpenseRepository
import com.flpzarato.controlegastos.ui.common.formatCompetenceLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.math.BigDecimal
import java.time.YearMonth
import javax.inject.Inject

data class CategoryBreakdown(val categoryId: String, val categoryName: String, val total: BigDecimal)
data class UserBreakdown(val email: String, val displayName: String, val total: BigDecimal)

data class HomeUiState(
    val monthLabel: String = "",
    val total: BigDecimal = BigDecimal.ZERO,
    val byCategory: List<CategoryBreakdown> = emptyList(),
    val byUser: List<UserBreakdown> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    expenseRepository: ExpenseRepository,
    categoryDao: CategoryDao,
    userDao: UserDao,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        expenseRepository.observeMonths(listOf(YearMonth.now())),
        categoryDao.observeActive(),
        userDao.observeAll(),
    ) { expenses, categories, users ->
        val categoryNames = categories.associate { it.id to it.name }
        val userNames = users.associate { it.email to it.displayName }

        val byCategory = expenses.groupBy { it.categoryId }
            .map { (categoryId, items) ->
                CategoryBreakdown(
                    categoryId = categoryId,
                    categoryName = categoryNames[categoryId] ?: "Outros",
                    total = items.sumOf { it.installmentAmount },
                )
            }
            .sortedByDescending { it.total }

        val byUser = expenses.groupBy { it.loggedByEmail }
            .map { (email, items) ->
                UserBreakdown(
                    email = email,
                    displayName = userNames[email] ?: email,
                    total = items.sumOf { it.installmentAmount },
                )
            }
            .sortedByDescending { it.total }

        HomeUiState(
            monthLabel = currentMonthLabel(),
            total = expenses.sumOf { it.installmentAmount },
            byCategory = byCategory,
            byUser = byUser,
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}

private fun currentMonthLabel(): String = formatCompetenceLabel(YearMonth.now().toString())
