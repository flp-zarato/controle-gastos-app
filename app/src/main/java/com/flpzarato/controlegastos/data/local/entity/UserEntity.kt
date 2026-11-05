package com.flpzarato.controlegastos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A known member of the shared household budget. The row for the account
 * currently signed in on this device is created locally on first login;
 * the other member's row arrives through sync once they have logged in
 * and synced at least once from their own phone.
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val email: String,
    val displayName: String,
    val dirty: Boolean = false,
)
