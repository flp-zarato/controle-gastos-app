package com.flpzarato.controlegastos.ui.cards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flpzarato.controlegastos.data.local.entity.CardEntity
import com.flpzarato.controlegastos.data.local.entity.UserEntity
import com.flpzarato.controlegastos.data.local.dao.UserDao
import com.flpzarato.controlegastos.data.repository.CardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NewCardFormState(
    val nickname: String = "",
    val holderEmail: String = "",
    val bank: String = "",
    val closingDay: String = "",
    val dueDay: String = "",
    val error: String? = null,
)

@HiltViewModel
class CardsViewModel @Inject constructor(
    private val cardRepository: CardRepository,
    userDao: UserDao,
) : ViewModel() {

    val cards: StateFlow<List<CardEntity>> = cardRepository.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val knownUsers: StateFlow<List<UserEntity>> = userDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _form = MutableStateFlow(NewCardFormState())
    val form: StateFlow<NewCardFormState> = _form.asStateFlow()

    fun onNicknameChanged(value: String) = update { it.copy(nickname = value) }
    fun onHolderChanged(email: String) = update { it.copy(holderEmail = email) }
    fun onBankChanged(value: String) = update { it.copy(bank = value) }
    fun onClosingDayChanged(value: String) = update { it.copy(closingDay = value.filter(Char::isDigit)) }
    fun onDueDayChanged(value: String) = update { it.copy(dueDay = value.filter(Char::isDigit)) }

    private fun update(transform: (NewCardFormState) -> NewCardFormState) {
        _form.value = transform(_form.value)
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
                    cardRepository.addCard(current.nickname.trim(), current.holderEmail, current.bank.trim(), closing, due)
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
