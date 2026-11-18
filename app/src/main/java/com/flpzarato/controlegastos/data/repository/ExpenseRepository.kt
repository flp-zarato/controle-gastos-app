package com.flpzarato.controlegastos.data.repository

import com.flpzarato.controlegastos.data.local.dao.CardDao
import com.flpzarato.controlegastos.data.local.dao.ExpenseDao
import com.flpzarato.controlegastos.data.local.entity.ExpenseEntity
import com.flpzarato.controlegastos.data.local.entity.PaymentMethod
import com.flpzarato.controlegastos.domain.InstallmentCalculator
import com.flpzarato.controlegastos.domain.PaymentStatusRules
import com.flpzarato.controlegastos.sync.SyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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

    /**
     * One row per purchase (its first installment, carrying [ExpenseEntity.totalAmount]) made
     * in any of [months] ("yyyy-MM"-keyed by purchase date, not invoice competência) — used by
     * Lançamentos/Início. See [com.flpzarato.controlegastos.data.local.dao.ExpenseDao.observeFirstInstallmentsByPurchaseMonth].
     */
    fun observePurchaseGroupsForMonths(months: List<YearMonth>): Flow<List<ExpenseEntity>> {
        val flows = months.map { expenseDao.observeFirstInstallmentsByPurchaseMonth(it.format(competenceFormatter)) }
        return combine(flows) { results -> results.flatMap { it }.sortedByDescending { it.purchaseDate } }
    }

    suspend fun getGroup(installmentGroupId: String): List<ExpenseEntity> =
        expenseDao.getByGroup(installmentGroupId)

    fun observeByCard(cardId: String): Flow<List<ExpenseEntity>> = expenseDao.observeByCard(cardId)

    suspend fun addExpense(
        description: String,
        totalAmount: BigDecimal,
        categoryId: String,
        purchaseDate: LocalDate,
        paymentMethod: PaymentMethod,
        loggedByEmail: String,
        cardId: String?,
        installmentCount: Int,
        observation: String? = null,
        recurringTemplateId: String? = null,
        paid: Boolean = false,
    ) {
        insertExpenseRows(
            description = description,
            totalAmount = totalAmount,
            categoryId = categoryId,
            purchaseDate = purchaseDate,
            paymentMethod = paymentMethod,
            loggedByEmail = loggedByEmail,
            cardId = cardId,
            installmentCount = installmentCount,
            observation = observation,
            recurringTemplateId = recurringTemplateId,
            paid = paid,
        )
        syncScheduler.requestImmediateSync()
    }

    /**
     * Same insertion as [addExpense] but without scheduling a sync — used only by the sync
     * job itself when auto-launching a due recurring expense, where triggering another sync
     * mid-sync would cancel the one currently running (WorkManager's unique-work replace policy).
     */
    suspend fun insertExpenseRows(
        description: String,
        totalAmount: BigDecimal,
        categoryId: String,
        purchaseDate: LocalDate,
        paymentMethod: PaymentMethod,
        loggedByEmail: String,
        cardId: String?,
        installmentCount: Int,
        observation: String? = null,
        recurringTemplateId: String? = null,
        paid: Boolean = false,
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
                        observation = observation,
                        recurringTemplateId = recurringTemplateId,
                        paid = paid,
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
                    observation = observation,
                    recurringTemplateId = recurringTemplateId,
                    paid = paid,
                    dirty = true,
                ),
            )
        }
    }

    /** Deletes every installment of a purchase together, as agreed in Q25. */
    suspend fun deleteGroup(installmentGroupId: String) {
        expenseDao.markGroupDeleted(installmentGroupId)
        syncScheduler.requestImmediateSync()
    }

    /**
     * Editing never patches rows in place: it marks the whole old group deleted and
     * creates a brand new one from the edited fields, so installments/competências are
     * always recalculated consistently rather than drifting out of sync with each other.
     */
    suspend fun updateExpense(
        oldGroupId: String,
        description: String,
        totalAmount: BigDecimal,
        categoryId: String,
        purchaseDate: LocalDate,
        paymentMethod: PaymentMethod,
        loggedByEmail: String,
        cardId: String?,
        installmentCount: Int,
        observation: String? = null,
    ) {
        // Fetched before markGroupDeleted, which flips deleted = true and would make
        // getByGroup (deleted = 0) return nothing. Editing must never reset or reapply the
        // auto-paid rule, so the original group's paid status always carries over as-is.
        val originalPaid = expenseDao.getByGroup(oldGroupId).firstOrNull()?.paid ?: false
        expenseDao.markGroupDeleted(oldGroupId)
        addExpense(
            description = description,
            totalAmount = totalAmount,
            categoryId = categoryId,
            purchaseDate = purchaseDate,
            paymentMethod = paymentMethod,
            loggedByEmail = loggedByEmail,
            cardId = cardId,
            installmentCount = installmentCount,
            observation = observation,
            paid = originalPaid,
        )
    }

    suspend fun markPaid(id: String) {
        val expense = expenseDao.getById(id)
        if (expense != null && !PaymentStatusRules.canManuallyMarkPaid(expense.paymentMethod)) {
            error("Lançamentos em crédito só podem ser marcados como pagos pela fatura")
        }
        expenseDao.markPaid(id)
        syncScheduler.requestImmediateSync()
    }

    suspend fun markUnpaid(id: String) {
        expenseDao.markUnpaid(id)
        syncScheduler.requestImmediateSync()
    }

    suspend fun markInvoicePaid(cardId: String, competenceMonth: String) {
        expenseDao.markInvoicePaid(cardId, competenceMonth)
        syncScheduler.requestImmediateSync()
    }
}
