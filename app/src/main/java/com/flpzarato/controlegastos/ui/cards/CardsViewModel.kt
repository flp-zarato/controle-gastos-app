package com.flpzarato.controlegastos.ui.cards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flpzarato.controlegastos.data.local.entity.CardEntity
import com.flpzarato.controlegastos.data.local.entity.UserEntity
import com.flpzarato.controlegastos.data.local.dao.UserDao
import com.flpzarato.controlegastos.data.repository.CardRepository
import com.flpzarato.controlegastos.data.repository.ExpenseRepository
import com.flpzarato.controlegastos.domain.InstallmentCalculator
import com.flpzarato.controlegastos.ui.common.bigDecimalToCentsString
import com.flpzarato.controlegastos.ui.common.centsStringToBigDecimal
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class NewCardFormState(
    val nickname: String = "",
    val holderEmail: String = "",
    val bank: String = "",
    val closingDay: String = "",
    val dueDay: String = "",
    /** Digits-only "amount in cents" string, same convention as [com.flpzarato.controlegastos.ui.common.CurrencyAmountField]. Blank = no limit. */
    val monthlyLimit: String = "",
    val editingCardId: String? = null,
    val error: String? = null,
)

/** How much of a card's open invoice has been spent against its [monthlyLimit]. */
data class CardProgress(val spent: BigDecimal, val limit: BigDecimal, val percentage: Int)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CardsViewModel @Inject constructor(
    private val cardRepository: CardRepository,
    private val expenseRepository: ExpenseRepository,
    userDao: UserDao,
) : ViewModel() {

    private val competenceFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

    val cards: StateFlow<List<CardEntity>> = cardRepository.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val knownUsers: StateFlow<List<UserEntity>> = userDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Keyed by card id; only present for cards with a [CardEntity.monthlyLimit] set. */
    val cardProgress: StateFlow<Map<String, CardProgress>> = cards.flatMapLatest { cardList ->
        val withLimit = cardList.filter { it.monthlyLimit != null }
        if (withLimit.isEmpty()) {
            flowOf(emptyMap())
        } else {
            combine(withLimit.map { expenseRepository.observeByCard(it.id) }) { expensesByCard ->
                withLimit.mapIndexed { index, card ->
                    val openCompetence = InstallmentCalculator.earliestOpenCompetenceMonth(card.closingDay)
                        .format(competenceFormatter)
                    val spent = expensesByCard[index]
                        .filter { it.competenceMonth == openCompetence }
                        .sumOf { it.installmentAmount }
                    val limit = card.monthlyLimit!!
                    val percentage = if (limit.signum() > 0) {
                        spent.divide(limit, 4, RoundingMode.HALF_UP).multiply(BigDecimal(100)).toInt()
                    } else {
                        0
                    }
                    card.id to CardProgress(spent, limit, percentage)
                }.toMap()
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val _form = MutableStateFlow(NewCardFormState())
    val form: StateFlow<NewCardFormState> = _form.asStateFlow()

    fun onNicknameChanged(value: String) = update { it.copy(nickname = value) }
    fun onHolderChanged(email: String) = update { it.copy(holderEmail = email) }
    fun onBankChanged(value: String) = update { it.copy(bank = value) }
    fun onClosingDayChanged(value: String) = update { it.copy(closingDay = value.filter(Char::isDigit)) }
    fun onDueDayChanged(value: String) = update { it.copy(dueDay = value.filter(Char::isDigit)) }
    fun onMonthlyLimitChanged(value: String) = update { it.copy(monthlyLimit = value.filter(Char::isDigit).take(12)) }

    private fun update(transform: (NewCardFormState) -> NewCardFormState) {
        _form.value = transform(_form.value)
    }

    fun startEditing(card: CardEntity) {
        _form.value = NewCardFormState(
            nickname = card.nickname,
            holderEmail = card.holderEmail,
            bank = card.bank,
            closingDay = card.closingDay.toString(),
            dueDay = card.dueDay.toString(),
            monthlyLimit = card.monthlyLimit?.let { bigDecimalToCentsString(it) } ?: "",
            editingCardId = card.id,
        )
    }

    fun cancelEditing() {
        _form.value = NewCardFormState()
    }

    fun save() {
        val current = _form.value
        val closing = current.closingDay.toIntOrNull()
        val due = current.dueDay.toIntOrNull()
        when {
            current.nickname.isBlank() -> update { it.copy(error = "Dê um apelido ao cartão") }
            current.holderEmail.isBlank() -> update { it.copy(error = "Escolha o titular") }
            current.bank.isBlank() -> update { it.copy(error = "Informe o banco") }
            closing == null || closing !in 1..28 -> update { it.copy(error = "Dia de fechamento inválido (1-28)") }
            due == null || due !in 1..28 -> update { it.copy(error = "Dia de vencimento inválido (1-28)") }
            else -> viewModelScope.launch {
                runCatching {
                    val editingId = current.editingCardId
                    val limit = current.monthlyLimit.takeIf { it.isNotBlank() }?.let { centsStringToBigDecimal(it) }
                    if (editingId != null) {
                        cardRepository.updateCard(
                            editingId, current.nickname.trim(), current.holderEmail, current.bank.trim(), closing, due, limit,
                        )
                    } else {
                        cardRepository.addCard(
                            current.nickname.trim(), current.holderEmail, current.bank.trim(), closing, due, limit,
                        )
                    }
                }.onSuccess {
                    _form.value = NewCardFormState()
                }.onFailure { e ->
                    update { it.copy(error = e.message ?: "Erro ao salvar") }
                }
            }
        }
    }

    fun removeCard(id: String) {
        viewModelScope.launch { cardRepository.removeCard(id) }
    }
}
