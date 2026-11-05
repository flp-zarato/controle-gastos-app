package com.flpzarato.controlegastos.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.flpzarato.controlegastos.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE deleted = 0 ORDER BY name")
    fun observeActive(): Flow<List<CategoryEntity>>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Query("SELECT * FROM categories WHERE dirty = 1")
    suspend fun getDirty(): List<CategoryEntity>

    @Upsert
    suspend fun upsert(category: CategoryEntity)

    @Upsert
    suspend fun upsertAll(categories: List<CategoryEntity>)

    @Query("UPDATE categories SET dirty = 0 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("UPDATE categories SET deleted = 1, dirty = 1 WHERE id = :id")
    suspend fun markDeleted(id: String)
}
