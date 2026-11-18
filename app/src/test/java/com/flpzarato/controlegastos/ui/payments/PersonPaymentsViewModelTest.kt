package com.flpzarato.controlegastos.ui.payments

import androidx.lifecycle.SavedStateHandle
import com.flpzarato.controlegastos.MainDispatcherRule
import com.flpzarato.controlegastos.data.local.dao.CategoryDao
import com.flpzarato.controlegastos.data.local.dao.UserDao
import com.flpzarato.controlegastos.data.local.entity.CategoryEntity
import com.flpzarato.controlegastos.data.local.entity.ExpenseEntity
import com.flpzarato.controlegastos.data.local.entity.PaymentMethod
import com.flpzarato.controlegastos.data.local.entity.UserEntity
import com.flpzarato.controlegastos.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate

class PersonPaymentsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val personEmail = "a@x.com"

    private val mercadoPago = fakeExpense(
        id = "1", categoryId = "cat-mercado", method = PaymentMethod.PIX, paid = true,
    )
    private val farmaciaAberto = fakeExpense(
        id = "2", categoryId = "cat-farmacia", method = PaymentMethod.CREDITO, paid = false,
    )
    private val outraPessoa = fakeExpense(
        id = "3", categoryId = "cat-mercado", method = PaymentMethod.PIX, paid = false,
        loggedByEmail = "b@x.com",
    )

    private fun buildViewModel(): PersonPaymentsViewModel {
        val expenseRepository = mock<ExpenseRepository>()
        val categoryDao = mock<CategoryDao>()
        val userDao = mock<UserDao>()

        whenever(expenseRepository.observeCurrentAndPreviousMonth())
            .thenReturn(flowOf(listOf(mercadoPago, farmaciaAberto, outraPessoa)))
        whenever(categoryDao.observeActive()).thenReturn(
            flowOf(
                listOf(
                    CategoryEntity(id = "cat-mercado", name = "Mercado"),
                    CategoryEntity(id = "cat-farmacia", name = "Farmácia"),
                ),
            ),
        )
        whenever(userDao.observeAll()).thenReturn(
            flowOf(listOf(UserEntity(email = personEmail, displayName = "Ana"))),
        )

        val savedStateHandle = SavedStateHandle(mapOf("email" to personEmail))
        return PersonPaymentsViewModel(expenseRepository, categoryDao, userDao, savedStateHandle)
    }

    @Test
    fun `sem filtro mostra pagos e a pagar apenas da pessoa`() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = buildViewModel()
        val job = launch { viewModel.uiState.collect {} }

        assertEquals(1, viewModel.uiState.value.paid.size)
        assertEquals(1, viewModel.uiState.value.unpaid.size)

        job.cancel()
    }

    @Test
    fun `filtro combinado de categoria e forma de pagamento restringe os dois status`() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = buildViewModel()
        val job = launch { viewModel.uiState.collect {} }

        viewModel.onCategoryFilterToggled("cat-farmacia")
        viewModel.onPaymentMethodFilterToggled(PaymentMethod.CREDITO)

        assertEquals(0, viewModel.uiState.value.paid.size)
        assertEquals(1, viewModel.uiState.value.unpaid.size)
        assertEquals(farmaciaAberto.id, viewModel.uiState.value.unpaid.first().id)

        job.cancel()
    }

    @Test
    fun `desmarcar o filtro volta a mostrar tudo da pessoa`() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = buildViewModel()
        val job = launch { viewModel.uiState.collect {} }

        viewModel.onPaymentMethodFilterToggled(PaymentMethod.CREDITO)
        assertEquals(0, viewModel.uiState.value.paid.size)

        viewModel.onPaymentMethodFilterToggled(PaymentMethod.CREDITO)
        assertEquals(1, viewModel.uiState.value.paid.size)

        job.cancel()
    }

    @Test
    fun `aplicar filtro pendente atualiza a lista e o contador de filtros ativos`() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = buildViewModel()
        val job = launch { viewModel.uiState.collect {} }

        viewModel.onFiltersSheetOpened()
        viewModel.onPendingPaymentMethodFilterToggled(PaymentMethod.CREDITO)

        // Draft only — applied filters untouched until onFiltersApplied().
        assertEquals(1, viewModel.uiState.value.paid.size)
        assertEquals(0, viewModel.uiState.value.activeFilterCount)

        viewModel.onFiltersApplied()

        assertEquals(0, viewModel.uiState.value.paid.size)
        assertEquals(1, viewModel.uiState.value.unpaid.size)
        assertEquals(1, viewModel.uiState.value.activeFilterCount)

        job.cancel()
    }

    @Test
    fun `cancelar o rascunho de filtro nao altera os filtros aplicados`() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = buildViewModel()
        val job = launch { viewModel.uiState.collect {} }

        viewModel.onFiltersSheetOpened()
        viewModel.onPendingPaymentMethodFilterToggled(PaymentMethod.CREDITO)
        viewModel.onFiltersCancelled()

        assertEquals(1, viewModel.uiState.value.paid.size)
        assertEquals(0, viewModel.uiState.value.activeFilterCount)

        job.cancel()
    }

    private fun fakeExpense(
        id: String,
        categoryId: String,
        method: PaymentMethod,
        paid: Boolean,
        loggedByEmail: String = personEmail,
    ) = ExpenseEntity(
        id = id,
        installmentGroupId = id,
        purchaseDate = LocalDate.of(2026, 11, 10),
        competenceMonth = "2026-11",
        description = "Teste $id",
        categoryId = categoryId,
        installmentAmount = BigDecimal("10.00"),
        totalAmount = null,
        installmentNumber = 1,
        installmentCount = 1,
        paymentMethod = method,
        cardId = null,
        loggedByEmail = loggedByEmail,
        paid = paid,
    )
}
