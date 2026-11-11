package com.flpzarato.controlegastos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.util.UUID

@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val nickname: String,
    val holderEmail: String,
    val bank: String,
    /** Day of the month (1-28) the invoice closes for new purchases. */
    val closingDay: Int,
    /** Day of the month (1-28) the invoice is due for payment. */
    val dueDay: Int,
    /** Optional monthly spending limit for this card's open invoice; null means no limit set. */
    val monthlyLimit: BigDecimal? = null,
    val dirty: Boolean = false,
    val deleted: Boolean = false,
)
