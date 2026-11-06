package com.flpzarato.controlegastos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.util.UUID

/**
 * A template for a monthly recurring expense (subscription, fixed bill). Every sync, the
 * app keeps a rolling forecast window of [monthsAhead] competências populated with one
 * normal [ExpenseEntity] each (tagged via [ExpenseEntity.recurringTemplateId]) — starting
 * from the current month (or, for a credit template, the card's next not-yet-closed
 * invoice) — never split into installments. Editing, pausing or deleting the template
 * clears and regenerates that forecast; competências already in the past are never touched.
 */
@Entity(tableName = "recurring_expenses")
data class RecurringExpenseEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val description: String,
    val amount: BigDecimal,
    val categoryId: String,
    val paymentMethod: PaymentMethod,
    val cardId: String?,
    /** Day of the month (1-28) the expense is launched on. */
    val dayOfMonth: Int,
    /** How many competências ahead (including the current/next-open one) to keep forecasted, 1-12. */
    val monthsAhead: Int = 3,
    /** Free-text note, capped at 30 chars by the UI; carried over to every expense this template launches. */
    val observation: String? = null,
    /** False when paused: no new months are launched, but past generated expenses stay untouched. */
    val active: Boolean = true,
    val dirty: Boolean = false,
    val deleted: Boolean = false,
)
