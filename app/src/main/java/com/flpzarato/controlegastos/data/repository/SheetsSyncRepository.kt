package com.flpzarato.controlegastos.data.repository

import com.flpzarato.controlegastos.data.local.dao.CardDao
import com.flpzarato.controlegastos.data.local.dao.CategoryDao
import com.flpzarato.controlegastos.data.local.dao.ExpenseDao
import com.flpzarato.controlegastos.data.local.dao.UserDao
import com.flpzarato.controlegastos.data.prefs.UserPreferences
import com.flpzarato.controlegastos.data.remote.SheetSchema
import com.flpzarato.controlegastos.data.remote.SheetsApi
import com.flpzarato.controlegastos.data.remote.dto.ValueRange
import com.flpzarato.controlegastos.data.remote.toCardEntity
import com.flpzarato.controlegastos.data.remote.toCategoryEntity
import com.flpzarato.controlegastos.data.remote.toExpenseEntity
import com.flpzarato.controlegastos.data.remote.toRow
import com.flpzarato.controlegastos.data.remote.toUserEntity
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pulls the shared spreadsheet down into the local Room cache, then pushes every
 * locally changed (dirty) record back up. Rows the other phone changed concurrently
 * simply arrive on the next pull; rows this phone has pending unsynced edits for are
 * never overwritten by a pull until they have been pushed (local wins until synced).
 */
@Singleton
class SheetsSyncRepository @Inject constructor(
    private val sheetsApi: SheetsApi,
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao,
    private val cardDao: CardDao,
    private val userDao: UserDao,
    private val userPreferences: UserPreferences,
) {
    suspend fun sync(): Result<Unit> = runCatching {
        val spreadsheetId = userPreferences.spreadsheetId.first()
            ?: error("Nenhuma planilha configurada ainda")

        val batch = sheetsApi.batchGet(
            spreadsheetId,
            listOf(
                SheetSchema.RANGE_LANCAMENTOS,
                SheetSchema.RANGE_CATEGORIAS,
                SheetSchema.RANGE_CARTOES,
                SheetSchema.RANGE_USUARIOS,
            ),
        )
        val lancamentosRows = batch.valueRanges.getOrNull(0)?.values.orEmpty()
        val categoriasRows = batch.valueRanges.getOrNull(1)?.values.orEmpty()
        val cartoesRows = batch.valueRanges.getOrNull(2)?.values.orEmpty()
        val usuariosRows = batch.valueRanges.getOrNull(3)?.values.orEmpty()

        pullExpenses(lancamentosRows)
        pullCategories(categoriasRows)
        pullCards(cartoesRows)
        pullUsers(usuariosRows)

        ensureCurrentUserRegistered(usuariosRows)

        pushDirtyExpenses(spreadsheetId, lancamentosRows)
        pushDirtyCategories(spreadsheetId, categoriasRows)
        pushDirtyCards(spreadsheetId, cartoesRows)
        pushDirtyUsers(spreadsheetId, usuariosRows)

        userPreferences.saveLastSync(System.currentTimeMillis())
    }

    private suspend fun pullExpenses(rows: List<List<String>>) {
        val dirtyIds = expenseDao.getDirty().map { it.id }.toSet()
        val remote = rows.mapNotNull { it.toExpenseEntity() }.filter { it.id !in dirtyIds }
        if (remote.isNotEmpty()) expenseDao.upsertAll(remote)
    }

    private suspend fun pullCategories(rows: List<List<String>>) {
        val dirtyIds = categoryDao.getDirty().map { it.id }.toSet()
        val remote = rows.mapNotNull { it.toCategoryEntity() }.filter { it.id !in dirtyIds }
        if (remote.isNotEmpty()) categoryDao.upsertAll(remote)
    }

    private suspend fun pullCards(rows: List<List<String>>) {
        val dirtyIds = cardDao.getDirty().map { it.id }.toSet()
        val remote = rows.mapNotNull { it.toCardEntity() }.filter { it.id !in dirtyIds }
        if (remote.isNotEmpty()) cardDao.upsertAll(remote)
    }

    private suspend fun pullUsers(rows: List<List<String>>) {
        val remote = rows.mapNotNull { it.toUserEntity() }
        if (remote.isNotEmpty()) userDao.upsertAll(remote)
    }

    /** The first time this account syncs, make sure it shows up in the shared "Usuários" tab. */
    private suspend fun ensureCurrentUserRegistered(usuariosRows: List<List<String>>) {
        val email = userPreferences.signedInEmail.first() ?: return
        val alreadyThere = usuariosRows.any { it.getOrNull(0) == email }
        if (!alreadyThere) {
            val name = userPreferences.signedInName.first() ?: email
            userDao.upsert(com.flpzarato.controlegastos.data.local.entity.UserEntity(email, name, dirty = true))
        }
    }

    private suspend fun pushDirtyExpenses(spreadsheetId: String, existingRows: List<List<String>>) {
        val dirty = expenseDao.getDirty()
        if (dirty.isEmpty()) return
        val rowNumberById = indexRowsById(existingRows)
        for (expense in dirty) {
            upsertRow(spreadsheetId, SheetSchema.TAB_LANCAMENTOS, SheetSchema.RANGE_LANCAMENTOS, "L", rowNumberById[expense.id], expense.toRow())
            expenseDao.markSynced(expense.id)
        }
    }

    private suspend fun pushDirtyCategories(spreadsheetId: String, existingRows: List<List<String>>) {
        val dirty = categoryDao.getDirty()
        if (dirty.isEmpty()) return
        val rowNumberById = indexRowsById(existingRows)
        for (category in dirty) {
            upsertRow(spreadsheetId, SheetSchema.TAB_CATEGORIAS, SheetSchema.RANGE_CATEGORIAS, "C", rowNumberById[category.id], category.toRow())
            categoryDao.markSynced(category.id)
        }
    }

    private suspend fun pushDirtyCards(spreadsheetId: String, existingRows: List<List<String>>) {
        val dirty = cardDao.getDirty()
        if (dirty.isEmpty()) return
        val rowNumberById = indexRowsById(existingRows)
        for (card in dirty) {
            upsertRow(spreadsheetId, SheetSchema.TAB_CARTOES, SheetSchema.RANGE_CARTOES, "G", rowNumberById[card.id], card.toRow())
            cardDao.markSynced(card.id)
        }
    }

    private suspend fun pushDirtyUsers(spreadsheetId: String, existingRows: List<List<String>>) {
        val dirty = userDao.getDirty()
        if (dirty.isEmpty()) return
        val rowNumberByEmail = indexRowsById(existingRows)
        for (user in dirty) {
            upsertRow(spreadsheetId, SheetSchema.TAB_USUARIOS, SheetSchema.RANGE_USUARIOS, "B", rowNumberByEmail[user.email], user.toRow())
            userDao.markSynced(user.email)
        }
    }

    /** Maps the value in column A (the record's id/email) to its 1-based sheet row number. Data starts at row 2. */
    private fun indexRowsById(rows: List<List<String>>): Map<String, Int> =
        rows.mapIndexedNotNull { index, row -> row.getOrNull(0)?.let { it to (index + 2) } }.toMap()

    private suspend fun upsertRow(
        spreadsheetId: String,
        tab: String,
        appendRange: String,
        lastColumn: String,
        existingRowNumber: Int?,
        row: List<String>,
    ) {
        if (existingRowNumber != null) {
            sheetsApi.update(
                spreadsheetId = spreadsheetId,
                range = SheetSchema.rowRange(tab, existingRowNumber, lastColumn),
                body = ValueRange(values = listOf(row)),
            )
        } else {
            sheetsApi.append(
                spreadsheetId = spreadsheetId,
                range = appendRange,
                body = ValueRange(values = listOf(row)),
            )
        }
    }
}
