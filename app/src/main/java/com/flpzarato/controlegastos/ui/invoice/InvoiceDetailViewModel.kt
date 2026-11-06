package com.flpzarato.controlegastos.ui.invoice

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flpzarato.controlegastos.data.local.dao.CategoryDao
import com.flpzarato.controlegastos.data.local.dao.UserDao
import com.flpzarato.controlegastos.data.local.entity.ExpenseEntity
import com.flpzarato.controlegastos.data.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.math.BigDecimal
import javax.inject.Inject

data class InvoiceDetailUiState(
    val competenceMonth: String = "",
    val total: BigDecimal = BigDecimal.ZERO,
    val expenses: List<ExpenseEntity> = emptyList(),
    val categoryNames: Map<String, String> = emptyMap(),
    val userNames: Map<String, String> = emptyMap(),
)

@HiltViewModel
class InvoiceDetailViewModel @Inject constructor(
    expenseRepository: ExpenseRepository,
    categoryDao: CategoryDao,
    userDao: UserDao,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val cardId: String = requireNotNull(savedStateHandle.get<String>("cardId")) { "cardId é obrigatório" }
    private val competenceMonth: String =
        requireNotNull(savedStateHandle.get<String>("competence")) { "competence é obrigatório" }

    val uiState: StateFlow<InvoiceDetailUiState> = combine(
        expenseRepository.observeByCard(cardId),
        categoryDao.observeActive(),
        userDao.observeAll(),
    ) { expenses, categories, users ->
        val filtered = expenses.filter { it.competenceMonth == competenceMonth }
        InvoiceDetailUiState(
            competenceMonth = competenceMonth,
            total = filtered.sumOf { it.installmentAmount },
            expenses = filtered,
            categoryNames = categories.associate { it.id to it.name },
            userNames = users.associate { it.email to it.displayName },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InvoiceDetailUiState())
}
