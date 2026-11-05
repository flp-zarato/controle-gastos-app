package com.flpzarato.controlegastos.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.flpzarato.controlegastos.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    /** [months] is the list of "yyyy-MM" competence months to include, e.g. current + previous. */
    @Query("SELECT * FROM expenses WHERE deleted = 0 AND competenceMonth IN (:months) ORDER BY purchaseDate DESC")
    fun observeByCompetenceMonths(months: List<String>): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE installmentGroupId = :groupId AND deleted = 0")
    suspend fun getByGroup(groupId: String): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE dirty = 1")
    suspend fun getDirty(): List<ExpenseEntity>

    @Upsert
    suspend fun upsert(expense: ExpenseEntity)

    @Upsert
    suspend fun upsertAll(expenses: List<ExpenseEntity>)

    @Query("UPDATE expenses SET dirty = 0 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("UPDATE expenses SET deleted = 1, dirty = 1 WHERE installmentGroupId = :groupId")
    suspend fun markGroupDeleted(groupId: String)

    @Query("DELETE FROM expenses WHERE installmentGroupId = :groupId")
    suspend fun hardDeleteGroup(groupId: String)
}
