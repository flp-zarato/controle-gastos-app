package com.flpzarato.controlegastos.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.flpzarato.controlegastos.data.local.entity.RecurringExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringExpenseDao {

    /** Everything not permanently deleted — includes paused templates, so the list screen can offer "reativar". */
    @Query("SELECT * FROM recurring_expenses WHERE deleted = 0 ORDER BY description")
    fun observeAll(): Flow<List<RecurringExpenseEntity>>

    /** Only the ones currently due to launch; paused/deleted templates are skipped by the sync check. */
    @Query("SELECT * FROM recurring_expenses WHERE deleted = 0 AND active = 1")
    suspend fun getActiveTemplates(): List<RecurringExpenseEntity>

    @Query("SELECT * FROM recurring_expenses WHERE id = :id")
    suspend fun getById(id: String): RecurringExpenseEntity?

    @Query("SELECT * FROM recurring_expenses WHERE dirty = 1")
    suspend fun getDirty(): List<RecurringExpenseEntity>

    @Upsert
    suspend fun upsert(template: RecurringExpenseEntity)

    @Upsert
    suspend fun upsertAll(templates: List<RecurringExpenseEntity>)

    @Query("UPDATE recurring_expenses SET dirty = 0 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("UPDATE recurring_expenses SET deleted = 1, dirty = 1 WHERE id = :id")
    suspend fun markDeleted(id: String)
}
