package com.flpzarato.controlegastos.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.flpzarato.controlegastos.data.local.entity.CardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    @Query("SELECT * FROM cards WHERE deleted = 0 ORDER BY nickname")
    fun observeActive(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun getById(id: String): CardEntity?

    @Query("SELECT * FROM cards WHERE dirty = 1")
    suspend fun getDirty(): List<CardEntity>

    @Upsert
    suspend fun upsert(card: CardEntity)

    @Upsert
    suspend fun upsertAll(cards: List<CardEntity>)

    @Query("UPDATE cards SET dirty = 0 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("UPDATE cards SET deleted = 1, dirty = 1 WHERE id = :id")
    suspend fun markDeleted(id: String)
}
