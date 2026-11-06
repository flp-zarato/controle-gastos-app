package com.flpzarato.controlegastos.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.flpzarato.controlegastos.data.local.dao.CardDao
import com.flpzarato.controlegastos.data.local.dao.CategoryDao
import com.flpzarato.controlegastos.data.local.dao.ExpenseDao
import com.flpzarato.controlegastos.data.local.dao.RecurringExpenseDao
import com.flpzarato.controlegastos.data.local.dao.UserDao
import com.flpzarato.controlegastos.data.local.entity.CardEntity
import com.flpzarato.controlegastos.data.local.entity.CategoryEntity
import com.flpzarato.controlegastos.data.local.entity.ExpenseEntity
import com.flpzarato.controlegastos.data.local.entity.RecurringExpenseEntity
import com.flpzarato.controlegastos.data.local.entity.UserEntity

@Database(
    entities = [
        ExpenseEntity::class, CategoryEntity::class, CardEntity::class, UserEntity::class,
        RecurringExpenseEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao
    abstract fun cardDao(): CardDao
    abstract fun userDao(): UserDao
    abstract fun recurringExpenseDao(): RecurringExpenseDao

    companion object {
        const val DATABASE_NAME = "controle_gastos.db"
    }
}
