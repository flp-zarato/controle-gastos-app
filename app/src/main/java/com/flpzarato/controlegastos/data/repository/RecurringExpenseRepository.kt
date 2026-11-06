package com.flpzarato.controlegastos.data.repository

import com.flpzarato.controlegastos.data.local.dao.ExpenseDao
import com.flpzarato.controlegastos.data.local.dao.RecurringExpenseDao
import com.flpzarato.controlegastos.data.local.entity.PaymentMethod
import com.flpzarato.controlegastos.data.local.entity.RecurringExpenseEntity
import com.flpzarato.controlegastos.sync.SyncScheduler
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.time.YearMonth
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringExpenseRepository @Inject constructor(
    private val recurringExpenseDao: RecurringExpenseDao,
    private val expenseDao: ExpenseDao,
    private val syncScheduler: SyncScheduler,
) {
    fun observeAll(): Flow<List<RecurringExpenseEntity>> = recurringExpenseDao.observeAll()

    suspend fun getById(id: String): RecurringExpenseEntity? = recurringExpenseDao.getById(id)

    /**
     * [id] null creates a new template; non-null edits it in place, preserving its
     * paused/active state. Editing also clears the whole still-forecast window (this month
     * onward) so the next sync regenerates it from the corrected template — otherwise a fix
     * made here (e.g. wrong payment method, card or months-ahead) would never reach a
     * lançamento that already exists, since relaunching a competência is normally skipped
     * once one exists for it.
     */
    suspend fun save(
        id: String?,
        description: String,
        amount: BigDecimal,
        categoryId: String,
        paymentMethod: PaymentMethod,
        cardId: String?,
        dayOfMonth: Int,
        monthsAhead: Int,
        observation: String? = null,
    ) {
        require(dayOfMonth in 1..28) { "Dia do mês deve estar entre 1 e 28" }
        require(monthsAhead in 1..12) { "Meses de antecedência deve estar entre 1 e 12" }
        val existingActive = id?.let { recurringExpenseDao.getById(it)?.active } ?: true
        recurringExpenseDao.upsert(
            RecurringExpenseEntity(
                id = id ?: UUID.randomUUID().toString(),
                description = description,
                amount = amount,
                categoryId = categoryId,
                paymentMethod = paymentMethod,
                cardId = cardId,
                dayOfMonth = dayOfMonth,
                monthsAhead = monthsAhead,
                observation = observation,
                active = existingActive,
                dirty = true,
            ),
        )
        if (id != null) clearForecastFrom(id)
        syncScheduler.requestImmediateSync()
    }

    /** Pausing clears the still-forecast window too, since it's no longer a valid commitment; resuming lets the next sync rebuild it. */
    suspend fun setActive(id: String, active: Boolean) {
        val current = recurringExpenseDao.getById(id) ?: return
        recurringExpenseDao.upsert(current.copy(active = active, dirty = true))
        if (!active) clearForecastFrom(id)
        syncScheduler.requestImmediateSync()
    }

    suspend fun delete(id: String) {
        recurringExpenseDao.markDeleted(id)
        clearForecastFrom(id)
        syncScheduler.requestImmediateSync()
    }

    /** Deletes every not-yet-past expense from this template; past competências are left untouched as history. */
    private suspend fun clearForecastFrom(templateId: String) {
        val currentMonth = YearMonth.now().toString()
        expenseDao.getByRecurringTemplateFromCompetence(templateId, currentMonth).forEach {
            expenseDao.markGroupDeleted(it.installmentGroupId)
        }
    }
}
