package com.flpzarato.controlegastos.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * Splits a credit card purchase into its installments (parcelas) and works out
 * which invoice (fatura) month each one belongs to, following the rule used by
 * every Brazilian card: a purchase made before the card's closing day lands on
 * the invoice that closes that same month; made on or after the closing day,
 * it only lands on next month's invoice. Each following installment advances
 * one month from there.
 */
object InstallmentCalculator {

    private val competenceFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

    data class Installment(
        val number: Int,
        val amount: BigDecimal,
        val competenceMonth: String,
    )

    /**
     * @param totalAmount total purchase value, e.g. R$500,00
     * @param installmentCount number of parcelas, e.g. 2
     * @param purchaseDate the date the purchase was made
     * @param cardClosingDay the card's invoice closing day of month (1-28)
     */
    fun calculate(
        totalAmount: BigDecimal,
        installmentCount: Int,
        purchaseDate: LocalDate,
        cardClosingDay: Int,
    ): List<Installment> {
        require(installmentCount >= 1) { "installmentCount must be at least 1" }
        require(totalAmount.signum() > 0) { "totalAmount must be positive" }

        val firstCompetence = firstCompetenceMonth(purchaseDate, cardClosingDay)
        val amounts = splitAmount(totalAmount, installmentCount)

        return amounts.mapIndexed { index, amount ->
            Installment(
                number = index + 1,
                amount = amount,
                competenceMonth = firstCompetence.plusMonths(index.toLong()).format(competenceFormatter),
            )
        }
    }

    /** Non-credit payments (cash, debit, pix) always fall on the month of the purchase itself. */
    fun singleInstallmentCompetence(purchaseDate: LocalDate): String =
        YearMonth.from(purchaseDate).format(competenceFormatter)

    private fun firstCompetenceMonth(purchaseDate: LocalDate, cardClosingDay: Int): YearMonth {
        val purchaseMonth = YearMonth.from(purchaseDate)
        return if (purchaseDate.dayOfMonth < cardClosingDay) purchaseMonth else purchaseMonth.plusMonths(1)
    }

    /**
     * Divides [total] into [count] parts of two decimal places. Since the division
     * may not be exact (e.g. R$100,00 / 3), the remainder is added to the last
     * installment so the parcelas always add back up to the original total.
     */
    private fun splitAmount(total: BigDecimal, count: Int): List<BigDecimal> {
        val rounded = total.setScale(2, RoundingMode.HALF_UP)
        val base = rounded.divide(BigDecimal(count), 2, RoundingMode.DOWN)
        val amounts = MutableList(count) { base }
        val distributed = base.multiply(BigDecimal(count))
        val remainder = rounded.subtract(distributed)
        amounts[count - 1] = amounts[count - 1].add(remainder)
        return amounts
    }
}
