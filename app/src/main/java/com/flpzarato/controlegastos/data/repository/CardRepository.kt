package com.flpzarato.controlegastos.data.repository

import com.flpzarato.controlegastos.data.local.dao.CardDao
import com.flpzarato.controlegastos.data.local.entity.CardEntity
import com.flpzarato.controlegastos.sync.SyncScheduler
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardRepository @Inject constructor(
    private val cardDao: CardDao,
    private val syncScheduler: SyncScheduler,
) {
    fun observeActive(): Flow<List<CardEntity>> = cardDao.observeActive()

    suspend fun addCard(nickname: String, holderEmail: String, bank: String, closingDay: Int, dueDay: Int) {
        require(closingDay in 1..28) { "Dia de fechamento deve estar entre 1 e 28" }
        require(dueDay in 1..28) { "Dia de vencimento deve estar entre 1 e 28" }
        cardDao.upsert(
            CardEntity(
                nickname = nickname,
                holderEmail = holderEmail,
                bank = bank,
                closingDay = closingDay,
                dueDay = dueDay,
                dirty = true,
            ),
        )
        syncScheduler.requestImmediateSync()
    }

    suspend fun updateCard(id: String, nickname: String, holderEmail: String, bank: String, closingDay: Int, dueDay: Int) {
        require(closingDay in 1..28) { "Dia de fechamento deve estar entre 1 e 28" }
        require(dueDay in 1..28) { "Dia de vencimento deve estar entre 1 e 28" }
        cardDao.upsert(
            CardEntity(
                id = id,
                nickname = nickname,
                holderEmail = holderEmail,
                bank = bank,
                closingDay = closingDay,
                dueDay = dueDay,
                dirty = true,
            ),
        )
        syncScheduler.requestImmediateSync()
    }

    suspend fun removeCard(id: String) {
        cardDao.markDeleted(id)
        syncScheduler.requestImmediateSync()
    }
}
