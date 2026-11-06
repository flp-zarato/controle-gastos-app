package com.flpzarato.controlegastos.ui.invoice

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flpzarato.controlegastos.data.repository.CardRepository
import com.flpzarato.controlegastos.data.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.math.BigDecimal
import javax.inject.Inject

data class InvoiceSummary(val competenceMonth: String, val total: BigDecimal, val count: Int)

data class InvoiceListUiState(val cardNickname: String = "", val invoices: List<InvoiceSummary> = emptyList())

/** Lists every competência (fatura) that has at least one expense for this card — read-only, no paid/unpaid status. */
@HiltViewModel
class InvoiceListViewModel @Inject constructor(
    expenseRepository: ExpenseRepository,
    cardRepository: CardRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val cardId: String = requireNotNull(savedStateHandle.get<String>("cardId")) { "cardId é obrigatório" }

    val uiState: StateFlow<InvoiceListUiState> = combine(
        expenseRepository.observeByCard(cardId),
        cardRepository.observeActive(),
    ) { expenses, cards ->
        val invoices = expenses.groupBy { it.competenceMonth }
            .map { (month, items) -> InvoiceSummary(month, items.sumOf { it.installmentAmount }, items.size) }
            .sortedByDescending { it.competenceMonth }
        InvoiceListUiState(
            cardNickname = cards.firstOrNull { it.id == cardId }?.nickname ?: "Cartão",
            invoices = invoices,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InvoiceListUiState())
}
