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

    /** Every (non-deleted) expense charged to a card, across all competências — used by the fatura screens. */
    @Query("SELECT * FROM expenses WHERE cardId = :cardId AND deleted = 0 ORDER BY competenceMonth DESC, purchaseDate DESC")
    fun observeByCard(cardId: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE dirty = 1")
    suspend fun getDirty(): List<ExpenseEntity>

    /** The expense already launched from a recurring template for one specific competência (invoice month), if any. */
    @Query(
        "SELECT * FROM expenses WHERE recurringTemplateId = :templateId AND competenceMonth = :competenceMonth " +
            "AND deleted = 0",
    )
    suspend fun getByRecurringTemplateAndCompetence(templateId: String, competenceMonth: String): List<ExpenseEntity>

    /**
     * Every still-forecast expense from a recurring template — competência [fromCompetence]
     * or later ("yyyy-MM" strings sort chronologically). Used to clear the forecast window
     * when a template is edited, paused or deleted, without ever touching past competências.
     */
    @Query(
        "SELECT * FROM expenses WHERE recurringTemplateId = :templateId AND competenceMonth >= :fromCompetence " +
            "AND deleted = 0",
    )
    suspend fun getByRecurringTemplateFromCompetence(templateId: String, fromCompetence: String): List<ExpenseEntity>

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
