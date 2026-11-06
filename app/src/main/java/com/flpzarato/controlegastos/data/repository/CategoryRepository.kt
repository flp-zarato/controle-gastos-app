package com.flpzarato.controlegastos.data.repository

import com.flpzarato.controlegastos.data.local.dao.CategoryDao
import com.flpzarato.controlegastos.data.local.entity.CategoryEntity
import com.flpzarato.controlegastos.sync.SyncScheduler
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao,
    private val syncScheduler: SyncScheduler,
) {
    fun observeActive(): Flow<List<CategoryEntity>> = categoryDao.observeActive()

    suspend fun addCategory(name: String) {
        categoryDao.upsert(CategoryEntity(name = name, dirty = true))
        syncScheduler.requestImmediateSync()
    }

    suspend fun removeCategory(id: String) {
        categoryDao.markDeleted(id)
        syncScheduler.requestImmediateSync()
    }
}
