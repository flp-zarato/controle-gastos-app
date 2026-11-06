package com.flpzarato.controlegastos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.util.UUID

/**
 * A template for a monthly recurring expense (subscription, fixed bill). Each month,
 * the sync step creates one normal [ExpenseEntity] from it (tagged via
 * [ExpenseEntity.recurringTemplateId]) once [dayOfMonth] has passed and no such
 * expense exists yet for that competência — never split into installments.
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
    /** False when paused: no new months are launched, but past generated expenses stay untouched. */
    val active: Boolean = true,
    val dirty: Boolean = false,
    val deleted: Boolean = false,
)
