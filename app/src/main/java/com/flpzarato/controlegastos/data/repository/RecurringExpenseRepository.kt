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
     * paused/active state. Editing also deletes this month's already-launched expense (if
     * any) so the next sync regenerates it from the corrected template — otherwise a fix
     * made here (e.g. wrong payment method or card) would never reach a lançamento that
     * already exists for the current month, since relaunching is normally skipped once one
     * exists.
     */
    suspend fun save(
        id: String?,
        description: String,
        amount: BigDecimal,
        categoryId: String,
        paymentMethod: PaymentMethod,
        cardId: String?,
        dayOfMonth: Int,
        observation: String? = null,
    ) {
        require(dayOfMonth in 1..28) { "Dia do mês deve estar entre 1 e 28" }
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
                observation = observation,
                active = existingActive,
                dirty = true,
            ),
        )
        if (id != null) {
            val currentMonth = YearMonth.now().toString()
            expenseDao.getByRecurringTemplateAndMonth(id, currentMonth).forEach {
                expenseDao.markGroupDeleted(it.installmentGroupId)
            }
        }
        syncScheduler.requestImmediateSync()
    }

    suspend fun setActive(id: String, active: Boolean) {
        val current = recurringExpenseDao.getById(id) ?: return
        recurringExpenseDao.upsert(current.copy(active = active, dirty = true))
        syncScheduler.requestImmediateSync()
    }

    suspend fun delete(id: String) {
        recurringExpenseDao.markDeleted(id)
        syncScheduler.requestImmediateSync()
    }
}
