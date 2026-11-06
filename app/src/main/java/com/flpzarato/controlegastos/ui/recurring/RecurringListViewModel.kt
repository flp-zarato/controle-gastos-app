package com.flpzarato.controlegastos.ui.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flpzarato.controlegastos.data.local.dao.CategoryDao
import com.flpzarato.controlegastos.data.repository.RecurringExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

data class RecurringListItem(
    val id: String,
    val description: String,
    val amount: BigDecimal,
    val categoryName: String,
    val dayOfMonth: Int,
    val active: Boolean,
)

@HiltViewModel
class RecurringListViewModel @Inject constructor(
    private val recurringExpenseRepository: RecurringExpenseRepository,
    categoryDao: CategoryDao,
) : ViewModel() {

    val items: StateFlow<List<RecurringListItem>> = combine(
        recurringExpenseRepository.observeAll(),
        categoryDao.observeActive(),
    ) { templates, categories ->
        val categoryNames = categories.associate { it.id to it.name }
        templates.map { template ->
            RecurringListItem(
                id = template.id,
                description = template.description,
                amount = template.amount,
                categoryName = categoryNames[template.categoryId] ?: "—",
                dayOfMonth = template.dayOfMonth,
                active = template.active,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setActive(id: String, active: Boolean) {
        viewModelScope.launch { recurringExpenseRepository.setActive(id, active) }
    }

    fun delete(id: String) {
        viewModelScope.launch { recurringExpenseRepository.delete(id) }
    }
}
