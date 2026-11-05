package com.flpzarato.controlegastos.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class InstallmentCalculatorTest {

    @Test
    fun `R$500 em 2x soma de volta ao total`() {
        val result = InstallmentCalculator.calculate(
            totalAmount = BigDecimal("500.00"),
            installmentCount = 2,
            purchaseDate = LocalDate.of(2026, 11, 5),
            cardClosingDay = 10,
        )
        assertEquals(2, result.size)
        assertEquals(BigDecimal("250.00"), result[0].amount)
        assertEquals(BigDecimal("250.00"), result[1].amount)
    }

    @Test
    fun `R$100 em 3x ajusta o arredondamento na ultima parcela`() {
        val result = InstallmentCalculator.calculate(
            totalAmount = BigDecimal("100.00"),
            installmentCount = 3,
            purchaseDate = LocalDate.of(2026, 11, 5),
            cardClosingDay = 10,
        )
        assertEquals(BigDecimal("33.33"), result[0].amount)
        assertEquals(BigDecimal("33.33"), result[1].amount)
        assertEquals(BigDecimal("33.34"), result[2].amount)
        val total = result.sumOf { it.amount }
        assertEquals(BigDecimal("100.00"), total)
    }

    @Test
    fun `compra antes do fechamento cai na fatura do mesmo mes`() {
        val result = InstallmentCalculator.calculate(
            totalAmount = BigDecimal("300.00"),
            installmentCount = 1,
            purchaseDate = LocalDate.of(2026, 11, 5),
            cardClosingDay = 10,
        )
        assertEquals("2026-11", result[0].competenceMonth)
    }

    @Test
    fun `compra no dia do fechamento cai na fatura do mes seguinte`() {
        val result = InstallmentCalculator.calculate(
            totalAmount = BigDecimal("300.00"),
            installmentCount = 1,
            purchaseDate = LocalDate.of(2026, 11, 10),
            cardClosingDay = 10,
        )
        assertEquals("2026-12", result[0].competenceMonth)
    }

    @Test
    fun `parcelas seguintes avancam um mes a partir da competencia inicial`() {
        val result = InstallmentCalculator.calculate(
            totalAmount = BigDecimal("300.00"),
            installmentCount = 3,
            purchaseDate = LocalDate.of(2026, 11, 15),
            cardClosingDay = 10,
        )
        assertEquals("2026-12", result[0].competenceMonth)
        assertEquals("2027-01", result[1].competenceMonth)
        assertEquals("2027-02", result[2].competenceMonth)
    }

    @Test
    fun `pagamento em dinheiro usa o mes da propria compra`() {
        val competence = InstallmentCalculator.singleInstallmentCompetence(LocalDate.of(2026, 11, 5))
        assertEquals("2026-11", competence)
    }
}
