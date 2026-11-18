package com.flpzarato.controlegastos.data.repository

import com.flpzarato.controlegastos.data.local.dao.CardDao
import com.flpzarato.controlegastos.data.local.dao.ExpenseDao
import com.flpzarato.controlegastos.data.local.entity.ExpenseEntity
import com.flpzarato.controlegastos.data.local.entity.PaymentMethod
import com.flpzarato.controlegastos.sync.SyncScheduler
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate

class ExpenseRepositoryTest {

    private val expenseDao = mock<ExpenseDao>()
    private val cardDao = mock<CardDao>()
    private val syncScheduler = mock<SyncScheduler>()
    private val repository = ExpenseRepository(expenseDao, cardDao, syncScheduler)

    /**
     * Regression test for the "Casa" duplicate bug: editing a recurring-generated expense
     * dropped recurringTemplateId, so the next sync's launchDueRecurringExpenses() found no
     * linked row for that template+competência and recreated the original (unedited) amount.
     */
    @Test
    fun `editar um lancamento vinculado a recorrente preserva o vinculo com o template`() = runTest {
        val original = ExpenseEntity(
            id = "expense-1",
            installmentGroupId = "group-1",
            purchaseDate = LocalDate.of(2026, 11, 5),
            competenceMonth = "2026-11",
            description = "Casa",
            categoryId = "cat-moradia",
            installmentAmount = BigDecimal("1545.00"),
            totalAmount = null,
            installmentNumber = 1,
            installmentCount = 1,
            paymentMethod = PaymentMethod.DEBITO,
            cardId = null,
            loggedByEmail = "a@x.com",
            recurringTemplateId = "template-casa",
            paid = true,
        )
        whenever(expenseDao.getByGroup("group-1")).thenReturn(listOf(original))

        repository.updateExpense(
            oldGroupId = "group-1",
            description = "Casa",
            totalAmount = BigDecimal("1000.00"),
            categoryId = "cat-moradia",
            purchaseDate = LocalDate.of(2026, 11, 5),
            paymentMethod = PaymentMethod.DEBITO,
            loggedByEmail = "a@x.com",
            cardId = null,
            installmentCount = 1,
        )

        verify(expenseDao).markGroupDeleted("group-1")

        val captor = argumentCaptor<ExpenseEntity>()
        verify(expenseDao).upsert(captor.capture())
        val edited = captor.firstValue

        assertEquals("template-casa", edited.recurringTemplateId)
        assertEquals(BigDecimal("1000.00"), edited.installmentAmount)
        assertEquals("2026-11", edited.competenceMonth)
        assertTrue(edited.paid)
    }

    @Test
    fun `editar um lancamento avulso sem recorrente continua sem vinculo com template`() = runTest {
        val original = ExpenseEntity(
            id = "expense-2",
            installmentGroupId = "group-2",
            purchaseDate = LocalDate.of(2026, 11, 5),
            competenceMonth = "2026-11",
            description = "Mercado",
            categoryId = "cat-mercado",
            installmentAmount = BigDecimal("50.00"),
            totalAmount = null,
            installmentNumber = 1,
            installmentCount = 1,
            paymentMethod = PaymentMethod.PIX,
            cardId = null,
            loggedByEmail = "a@x.com",
            recurringTemplateId = null,
        )
        whenever(expenseDao.getByGroup("group-2")).thenReturn(listOf(original))

        repository.updateExpense(
            oldGroupId = "group-2",
            description = "Mercado",
            totalAmount = BigDecimal("60.00"),
            categoryId = "cat-mercado",
            purchaseDate = LocalDate.of(2026, 11, 5),
            paymentMethod = PaymentMethod.PIX,
            loggedByEmail = "a@x.com",
            cardId = null,
            installmentCount = 1,
        )

        val captor = argumentCaptor<ExpenseEntity>()
        verify(expenseDao).upsert(captor.capture())

        assertNull(captor.firstValue.recurringTemplateId)
    }
}
