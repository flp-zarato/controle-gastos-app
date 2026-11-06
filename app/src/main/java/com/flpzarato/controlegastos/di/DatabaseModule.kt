package com.flpzarato.controlegastos.di

import android.content.Context
import androidx.room.Room
import com.flpzarato.controlegastos.data.local.AppDatabase
import com.flpzarato.controlegastos.data.local.dao.CardDao
import com.flpzarato.controlegastos.data.local.dao.CategoryDao
import com.flpzarato.controlegastos.data.local.dao.ExpenseDao
import com.flpzarato.controlegastos.data.local.dao.RecurringExpenseDao
import com.flpzarato.controlegastos.data.local.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            // Room is only a synced cache of the shared sheet, so a schema bump just
            // wipes and resyncs rather than needing a hand-written Migration.
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideExpenseDao(db: AppDatabase): ExpenseDao = db.expenseDao()

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideCardDao(db: AppDatabase): CardDao = db.cardDao()

    @Provides
    fun provideUserDao(db: AppDatabase): UserDao = db.userDao()

    @Provides
    fun provideRecurringExpenseDao(db: AppDatabase): RecurringExpenseDao = db.recurringExpenseDao()
}

@Module
@InstallIn(SingletonComponent::class)
object ContextModule {
    @Provides
    fun provideContext(@ApplicationContext context: Context): Context = context
}
