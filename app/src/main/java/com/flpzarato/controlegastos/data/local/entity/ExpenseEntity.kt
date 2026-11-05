package com.flpzarato.controlegastos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

/**
 * One installment (parcela) is one row. A cash/debit/pix expense is always
 * exactly one row with [installmentNumber] = 1 and [installmentCount] = 1.
 * A credit purchase split in N times produces N rows that share the same
 * [installmentGroupId] so they can be edited or deleted together.
 */
@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val installmentGroupId: String,
    val purchaseDate: LocalDate,
    /** Year-month this installment falls due on the invoice, e.g. "2026-12". Equal to purchaseDate's month for non-credit expenses. */
    val competenceMonth: String,
    val description: String,
    val categoryId: String,
    val installmentAmount: BigDecimal,
    /** Total original purchase amount; null when the expense is not split into installments. */
    val totalAmount: BigDecimal?,
    val installmentNumber: Int,
    val installmentCount: Int,
    val paymentMethod: PaymentMethod,
    val cardId: String?,
    val loggedByEmail: String,
    val dirty: Boolean = false,
    val deleted: Boolean = false,
)
