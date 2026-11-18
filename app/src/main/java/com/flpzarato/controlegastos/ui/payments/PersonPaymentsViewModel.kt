package com.flpzarato.controlegastos.ui.payments

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
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

data class PersonPaymentsUiState(
    val displayName: String = "",
    val paid: List<ExpenseEntity> = emptyList(),
    val unpaid: List<ExpenseEntity> = emptyList(),
    val categoryNames: Map<String, String> = emptyMap(),
) {
    val paidTotal: BigDecimal get() = paid.sumOf { it.installmentAmount }
    val unpaidTotal: BigDecimal get() = unpaid.sumOf { it.installmentAmount }
}

@HiltViewModel
class PersonPaymentsViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    categoryDao: CategoryDao,
    userDao: UserDao,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val email: String = requireNotNull(savedStateHandle.get<String>("email")) { "email é obrigatório" }

    val uiState: StateFlow<PersonPaymentsUiState> = combine(
        expenseRepository.observeCurrentAndPreviousMonth(),
        categoryDao.observeActive(),
        userDao.observeAll(),
    ) { expenses, categories, users ->
        val ofPerson = expenses.filter { it.loggedByEmail == email }
        PersonPaymentsUiState(
            displayName = users.firstOrNull { it.email == email }?.displayName ?: email,
            paid = ofPerson.filter { it.paid },
            unpaid = ofPerson.filter { !it.paid },
            categoryNames = categories.associate { it.id to it.name },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PersonPaymentsUiState())

    fun togglePaid(expenseId: String, paid: Boolean) {
        viewModelScope.launch {
            if (paid) expenseRepository.markPaid(expenseId) else expenseRepository.markUnpaid(expenseId)
        }
    }
}
