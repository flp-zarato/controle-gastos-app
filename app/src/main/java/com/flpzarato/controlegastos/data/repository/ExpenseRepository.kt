package com.flpzarato.controlegastos.data.repository

import com.flpzarato.controlegastos.data.local.dao.CardDao
import com.flpzarato.controlegastos.data.local.dao.ExpenseDao
import com.flpzarato.controlegastos.data.local.entity.ExpenseEntity
import com.flpzarato.controlegastos.data.local.entity.PaymentMethod
import com.flpzarato.controlegastos.domain.InstallmentCalculator
import com.flpzarato.controlegastos.sync.SyncScheduler
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepository @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val cardDao: CardDao,
    private val syncScheduler: SyncScheduler,
) {
    private val competenceFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

    /** Current month and the previous one, as shown in the app's listing screen (Q30). */
    fun observeCurrentAndPreviousMonth(): Flow<List<ExpenseEntity>> {
        val now = YearMonth.now()
        val months = listOf(now.format(competenceFormatter), now.minusMonths(1).format(competenceFormatter))
        return expenseDao.observeByCompetenceMonths(months)
    }

    fun observeMonths(months: List<YearMonth>): Flow<List<ExpenseEntity>> =
        expenseDao.observeByCompetenceMonths(months.map { it.format(competenceFormatter) })

    suspend fun addExpense(
        description: String,
        totalAmount: BigDecimal,
        categoryId: String,
        purchaseDate: LocalDate,
        paymentMethod: PaymentMethod,
        loggedByEmail: String,
        cardId: String?,
        installmentCount: Int,
    ) {
        val groupId = UUID.randomUUID().toString()

        if (paymentMethod == PaymentMethod.CREDITO) {
            requireNotNull(cardId) { "Um cartão precisa ser selecionado para pagamento no crédito" }
            val card = requireNotNull(cardDao.getById(cardId)) { "Cartão não encontrado" }
            val installments = InstallmentCalculator.calculate(
                totalAmount = totalAmount,
                installmentCount = installmentCount,
                purchaseDate = purchaseDate,
                cardClosingDay = card.closingDay,
            )
            installments.forEach { installment ->
                expenseDao.upsert(
                    ExpenseEntity(
                        installmentGroupId = groupId,
                        purchaseDate = purchaseDate,
                        competenceMonth = installment.competenceMonth,
                        description = description,
                        categoryId = categoryId,
                        installmentAmount = installment.amount,
                        totalAmount = if (installmentCount > 1) totalAmount else null,
                        installmentNumber = installment.number,
                        installmentCount = installmentCount,
                        paymentMethod = paymentMethod,
                        cardId = cardId,
                        loggedByEmail = loggedByEmail,
                        dirty = true,
                    ),
                )
            }
        } else {
            expenseDao.upsert(
                ExpenseEntity(
                    installmentGroupId = groupId,
                    purchaseDate = purchaseDate,
                    competenceMonth = InstallmentCalculator.singleInstallmentCompetence(purchaseDate),
                    description = description,
                    categoryId = categoryId,
                    installmentAmount = totalAmount,
                    totalAmount = null,
                    installmentNumber = 1,
                    installmentCount = 1,
                    paymentMethod = paymentMethod,
                    cardId = null,
                    loggedByEmail = loggedByEmail,
                    dirty = true,
                ),
            )
        }

        syncScheduler.requestImmediateSync()
    }

    /** Deletes every installment of a purchase together, as agreed in Q25. */
    suspend fun deleteGroup(installmentGroupId: String) {
        expenseDao.markGroupDeleted(installmentGroupId)
        syncScheduler.requestImmediateSync()
    }
}
