package com.flpzarato.controlegastos.data.remote

import com.flpzarato.controlegastos.data.local.entity.CardEntity
import com.flpzarato.controlegastos.data.local.entity.CategoryEntity
import com.flpzarato.controlegastos.data.local.entity.ExpenseEntity
import com.flpzarato.controlegastos.data.local.entity.PaymentMethod
import com.flpzarato.controlegastos.data.local.entity.RecurringExpenseEntity
import com.flpzarato.controlegastos.data.local.entity.UserEntity
import java.math.BigDecimal
import java.time.LocalDate

/** Reads a cell as a non-blank string, or null if the row is too short / cell is empty. */
private fun List<String>.cell(index: Int): String? = getOrNull(index)?.takeIf { it.isNotBlank() }

fun ExpenseEntity.toRow(): List<String> = listOf(
    id,
    purchaseDate.toString(),
    competenceMonth,
    description,
    categoryId,
    installmentAmount.toPlainString(),
    totalAmount?.toPlainString().orEmpty(),
    if (installmentCount > 1) "$installmentNumber/$installmentCount" else "",
    paymentMethod.name,
    cardId.orEmpty(),
    loggedByEmail,
    deleted.toString(),
    observation.orEmpty(),
    recurringTemplateId.orEmpty(),
)

fun List<String>.toExpenseEntity(): ExpenseEntity? {
    val id = cell(0) ?: return null
    val parcelaInfo = cell(7)
    val (number, count) = parcelaInfo?.split("/")?.takeIf { it.size == 2 }
        ?.let { (n, c) -> n.toIntOrNull() to c.toIntOrNull() }
        ?: (1 to 1)
    return ExpenseEntity(
        id = id,
        installmentGroupId = id,
        purchaseDate = cell(1)?.let { LocalDate.parse(it) } ?: return null,
        competenceMonth = cell(2) ?: return null,
        description = cell(3).orEmpty(),
        categoryId = cell(4) ?: return null,
        installmentAmount = cell(5)?.let { BigDecimal(it) } ?: return null,
        totalAmount = cell(6)?.let { BigDecimal(it) },
        installmentNumber = number ?: 1,
        installmentCount = count ?: 1,
        paymentMethod = cell(8)?.let { PaymentMethod.fromStorage(it) } ?: PaymentMethod.DINHEIRO,
        cardId = cell(9),
        loggedByEmail = cell(10) ?: return null,
        observation = cell(12),
        recurringTemplateId = cell(13),
        dirty = false,
        deleted = cell(11)?.toBoolean() ?: false,
    )
}

fun CategoryEntity.toRow(): List<String> = listOf(id, name, deleted.toString())

fun List<String>.toCategoryEntity(): CategoryEntity? {
    val id = cell(0) ?: return null
    return CategoryEntity(
        id = id,
        name = cell(1) ?: return null,
        dirty = false,
        deleted = cell(2)?.toBoolean() ?: false,
    )
}

fun CardEntity.toRow(): List<String> = listOf(
    id, nickname, holderEmail, bank, closingDay.toString(), dueDay.toString(), deleted.toString(),
    monthlyLimit?.toPlainString().orEmpty(),
)

fun List<String>.toCardEntity(): CardEntity? {
    val id = cell(0) ?: return null
    return CardEntity(
        id = id,
        nickname = cell(1) ?: return null,
        holderEmail = cell(2) ?: return null,
        bank = cell(3) ?: return null,
        closingDay = cell(4)?.toIntOrNull() ?: return null,
        dueDay = cell(5)?.toIntOrNull() ?: return null,
        monthlyLimit = cell(7)?.let { BigDecimal(it) },
        dirty = false,
        deleted = cell(6)?.toBoolean() ?: false,
    )
}

fun UserEntity.toRow(): List<String> = listOf(email, displayName)

fun List<String>.toUserEntity(): UserEntity? {
    val email = cell(0) ?: return null
    return UserEntity(email = email, displayName = cell(1) ?: email, dirty = false)
}

fun RecurringExpenseEntity.toRow(): List<String> = listOf(
    id, description, amount.toPlainString(), categoryId, paymentMethod.name,
    cardId.orEmpty(), dayOfMonth.toString(), active.toString(), deleted.toString(), observation.orEmpty(),
    monthsAhead.toString(),
)

fun List<String>.toRecurringExpenseEntity(): RecurringExpenseEntity? {
    val id = cell(0) ?: return null
    return RecurringExpenseEntity(
        id = id,
        description = cell(1) ?: return null,
        amount = cell(2)?.let { BigDecimal(it) } ?: return null,
        categoryId = cell(3) ?: return null,
        paymentMethod = cell(4)?.let { PaymentMethod.fromStorage(it) } ?: PaymentMethod.DINHEIRO,
        cardId = cell(5),
        dayOfMonth = cell(6)?.toIntOrNull() ?: 1,
        monthsAhead = cell(10)?.toIntOrNull() ?: 3,
        observation = cell(9),
        active = cell(7)?.toBoolean() ?: true,
        dirty = false,
        deleted = cell(8)?.toBoolean() ?: false,
    )
}
