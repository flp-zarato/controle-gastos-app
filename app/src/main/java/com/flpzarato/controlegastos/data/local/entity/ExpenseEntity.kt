package com.flpzarato.controlegastos.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

/**
 * One installment (parcela) is one row. A cash/debit/pix expense is always
 * exactly one row with [installmentNumber] = 1 and [installmentCount] = 1.
 * A credit purchase split in N times produces N rows that share the same
 * [installmentGroupId] so they can be edited or deleted together.
 *
 * Indices: (deleted, competenceMonth) covers [ExpenseDao.observeByCompetenceMonths], which
 * always filters both together, and also serves a standalone `deleted` lookup via its
 * leftmost column; cardId and dirty get their own indices since they're filtered on alone
 * (fatura screens, and every push-to-sheet query, respectively).
 */
@Entity(
    tableName = "expenses",
    indices = [
        Index(value = ["deleted", "competenceMonth"]),
        Index(value = ["cardId"]),
        Index(value = ["dirty"]),
    ],
)
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
    /** Free-text note, capped at 30 chars by the UI. */
    val observation: String? = null,
    /** Set only on expenses auto-generated from a recurring template; used to avoid relaunching the same month twice. */
    val recurringTemplateId: String? = null,
    val dirty: Boolean = false,
    val deleted: Boolean = false,
)
