package com.flpzarato.controlegastos.ui.listing

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
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate

class ExpenseListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mercado = fakeExpense(id = "1", categoryId = "cat-mercado", loggedByEmail = "a@x.com", method = PaymentMethod.PIX)
    private val farmacia = fakeExpense(id = "2", categoryId = "cat-farmacia", loggedByEmail = "a@x.com", method = PaymentMethod.CREDITO)
    private val lazer = fakeExpense(id = "3", categoryId = "cat-lazer", loggedByEmail = "b@x.com", method = PaymentMethod.DEBITO)

    private fun buildViewModel(): ExpenseListViewModel {
        val expenseRepository = mock<ExpenseRepository>()
        val categoryDao = mock<CategoryDao>()
        val userDao = mock<UserDao>()

        whenever(expenseRepository.observePurchaseGroupsForMonths(any()))
            .thenReturn(flowOf(listOf(mercado, farmacia, lazer)))
        whenever(categoryDao.observeActive()).thenReturn(
            flowOf(
                listOf(
                    CategoryEntity(id = "cat-mercado", name = "Mercado"),
                    CategoryEntity(id = "cat-farmacia", name = "Farmácia"),
                    CategoryEntity(id = "cat-lazer", name = "Lazer"),
                ),
            ),
        )
        whenever(userDao.observeAll()).thenReturn(
            flowOf(
                listOf(
                    UserEntity(email = "a@x.com", displayName = "Ana"),
                    UserEntity(email = "b@x.com", displayName = "Bruno"),
                ),
            ),
        )

        return ExpenseListViewModel(expenseRepository, categoryDao, userDao)
    }

    @Test
    fun `sem nenhum filtro selecionado mostra todos os lancamentos`() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = buildViewModel()
        val job = launch { viewModel.uiState.collect {} }

        assertEquals(3, viewModel.uiState.value.expenses.size)

        job.cancel()
    }

    @Test
    fun `filtro combinado de categoria e forma de pagamento restringe a intersecao`() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = buildViewModel()
        val job = launch { viewModel.uiState.collect {} }

        viewModel.onCategoryFilterToggled("cat-farmacia")
        viewModel.onPaymentMethodFilterToggled(PaymentMethod.CREDITO)

        val result = viewModel.uiState.value.expenses
        assertEquals(1, result.size)
        assertEquals(farmacia.id, result.first().id)

        job.cancel()
    }

    @Test
    fun `desmarcar o unico filtro ativo volta a mostrar todos`() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = buildViewModel()
        val job = launch { viewModel.uiState.collect {} }

        viewModel.onUserFilterToggled("b@x.com")
        assertEquals(1, viewModel.uiState.value.expenses.size)

        viewModel.onUserFilterToggled("b@x.com")
        assertEquals(3, viewModel.uiState.value.expenses.size)

        job.cancel()
    }

    @Test
    fun `aplicar filtro pendente atualiza a lista e o contador de filtros ativos`() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = buildViewModel()
        val job = launch { viewModel.uiState.collect {} }

        viewModel.onFiltersSheetOpened()
        viewModel.onPendingCategoryFilterToggled("cat-farmacia")
        viewModel.onPendingPaymentMethodFilterToggled(PaymentMethod.CREDITO)

        // Draft only — applied filters/list untouched until onFiltersApplied().
        assertEquals(3, viewModel.uiState.value.expenses.size)
        assertEquals(0, viewModel.uiState.value.activeFilterCount)

        viewModel.onFiltersApplied()

        assertEquals(1, viewModel.uiState.value.expenses.size)
        assertEquals(farmacia.id, viewModel.uiState.value.expenses.first().id)
        assertEquals(2, viewModel.uiState.value.activeFilterCount)

        job.cancel()
    }

    @Test
    fun `cancelar o rascunho de filtro nao altera os filtros aplicados`() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = buildViewModel()
        val job = launch { viewModel.uiState.collect {} }

        viewModel.onUserFilterToggled("b@x.com")
        assertEquals(1, viewModel.uiState.value.expenses.size)

        viewModel.onFiltersSheetOpened()
        viewModel.onPendingUserFiltersCleared()
        viewModel.onPendingCategoryFilterToggled("cat-mercado")
        viewModel.onFiltersCancelled()

        assertEquals(1, viewModel.uiState.value.expenses.size)
        assertEquals(lazer.id, viewModel.uiState.value.expenses.first().id)

        job.cancel()
    }

    private fun fakeExpense(id: String, categoryId: String, loggedByEmail: String, method: PaymentMethod) = ExpenseEntity(
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
    )
}
