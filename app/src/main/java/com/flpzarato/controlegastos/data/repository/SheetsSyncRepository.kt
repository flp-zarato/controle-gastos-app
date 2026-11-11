package com.flpzarato.controlegastos.data.repository

import com.flpzarato.controlegastos.data.local.dao.CardDao
import com.flpzarato.controlegastos.data.local.dao.CategoryDao
import com.flpzarato.controlegastos.data.local.dao.ExpenseDao
import com.flpzarato.controlegastos.data.local.dao.RecurringExpenseDao
import com.flpzarato.controlegastos.data.local.dao.UserDao
import com.flpzarato.controlegastos.data.local.entity.CategoryEntity
import com.flpzarato.controlegastos.data.local.entity.PaymentMethod
import com.flpzarato.controlegastos.data.prefs.UserPreferences
import com.flpzarato.controlegastos.data.remote.SheetSchema
import com.flpzarato.controlegastos.data.remote.SheetsApi
import com.flpzarato.controlegastos.data.remote.dto.BatchUpdateValuesRequest
import com.flpzarato.controlegastos.data.remote.dto.ValueRange
import com.flpzarato.controlegastos.data.remote.toCardEntity
import com.flpzarato.controlegastos.data.remote.toCategoryEntity
import com.flpzarato.controlegastos.data.remote.toExpenseEntity
import com.flpzarato.controlegastos.data.remote.toRecurringExpenseEntity
import com.flpzarato.controlegastos.data.remote.toRow
import com.flpzarato.controlegastos.data.remote.toUserEntity
import com.flpzarato.controlegastos.domain.InstallmentCalculator
import kotlinx.coroutines.flow.first
import java.time.YearMonth
import java.time.format.DateTimeFormatter
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
    private val recurringExpenseDao: RecurringExpenseDao,
    private val expenseRepository: ExpenseRepository,
    private val userPreferences: UserPreferences,
) {
    private val competenceFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

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
                SheetSchema.RANGE_RECORRENTES,
            ),
        )
        val lancamentosRows = batch.valueRanges.getOrNull(0)?.values.orEmpty()
        val categoriasRows = batch.valueRanges.getOrNull(1)?.values.orEmpty()
        val cartoesRows = batch.valueRanges.getOrNull(2)?.values.orEmpty()
        val usuariosRows = batch.valueRanges.getOrNull(3)?.values.orEmpty()
        val recorrentesRows = batch.valueRanges.getOrNull(4)?.values.orEmpty()

        pullExpenses(lancamentosRows)
        pullCategories(categoriasRows)
        pullCards(cartoesRows)
        pullUsers(usuariosRows)
        pullRecurringExpenses(recorrentesRows)

        ensureCurrentUserRegistered(usuariosRows)
        launchDueRecurringExpenses()

        pushDirtyExpenses(spreadsheetId, lancamentosRows)
        pushDirtyCategories(spreadsheetId, categoriasRows)
        pushDirtyCards(spreadsheetId, cartoesRows)
        pushDirtyUsers(spreadsheetId, usuariosRows)
        pushDirtyRecurringExpenses(spreadsheetId, recorrentesRows)

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
        ensureDefaultCategoriesSeeded()
    }

    /**
     * Runs only after a pull, so it only fires when the shared "Categorias" tab is truly
     * empty (nobody has ever synced categories into it yet) — never on a second device
     * joining a sheet that already has categories, which would otherwise create duplicates.
     * The seeded rows are marked dirty so the push step below writes them to the sheet.
     */
    private suspend fun ensureDefaultCategoriesSeeded() {
        if (categoryDao.count() == 0) {
            categoryDao.upsertAll(CategoryEntity.defaultSeed().map { it.copy(dirty = true) })
        }
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

    private suspend fun pullRecurringExpenses(rows: List<List<String>>) {
        val dirtyIds = recurringExpenseDao.getDirty().map { it.id }.toSet()
        val remote = rows.mapNotNull { it.toRecurringExpenseEntity() }.filter { it.id !in dirtyIds }
        if (remote.isNotEmpty()) recurringExpenseDao.upsertAll(remote)
    }

    /**
     * Keeps a rolling forecast of [RecurringExpenseEntity.monthsAhead] competências
     * populated for every active recurring template, starting from the current month (or,
     * for a credit template, the card's next not-yet-closed invoice — so a bill is never
     * forecast into an invoice that already closed). Each competência in the window gets
     * exactly one expense, skipped if one already exists. Runs inside the same sync pass so
     * the newly-dirty rows are picked up by [pushDirtyExpenses] right after, without
     * scheduling a separate sync (which would cancel this one).
     */
    private suspend fun launchDueRecurringExpenses() {
        val signedInEmail = userPreferences.signedInEmail.first() ?: return

        for (template in recurringExpenseDao.getActiveTemplates()) {
            val day = template.dayOfMonth.coerceIn(1, 28)
            val monthsAhead = template.monthsAhead.coerceIn(1, 12)

            val card = if (template.paymentMethod == PaymentMethod.CREDITO) {
                template.cardId?.let { cardDao.getById(it) } ?: continue
            } else {
                null
            }
            val startCompetence = if (card != null) {
                InstallmentCalculator.earliestOpenCompetenceMonth(card.closingDay)
            } else {
                YearMonth.now()
            }

            for (offset in 0 until monthsAhead) {
                val targetCompetence = startCompetence.plusMonths(offset.toLong())
                val competenceKey = targetCompetence.format(competenceFormatter)
                val alreadyLaunched = expenseDao
                    .getByRecurringTemplateAndCompetence(template.id, competenceKey)
                    .isNotEmpty()
                if (alreadyLaunched) continue

                val purchaseDate = if (card != null) {
                    InstallmentCalculator.purchaseDateForCompetence(targetCompetence, day, card.closingDay)
                } else {
                    targetCompetence.atDay(day)
                }

                expenseRepository.insertExpenseRows(
                    description = template.description,
                    totalAmount = template.amount,
                    categoryId = template.categoryId,
                    purchaseDate = purchaseDate,
                    paymentMethod = template.paymentMethod,
                    loggedByEmail = signedInEmail,
                    cardId = template.cardId,
                    installmentCount = 1,
                    observation = template.observation,
                    recurringTemplateId = template.id,
                )
            }
        }
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
        pushDirtyBatch(
            spreadsheetId, SheetSchema.TAB_LANCAMENTOS, SheetSchema.RANGE_LANCAMENTOS, "L", existingRows,
            dirty = expenseDao.getDirty(),
            keyOf = { it.id },
            toRow = { it.toRow() },
            markSynced = { expenseDao.markSynced(it.id) },
        )
    }

    private suspend fun pushDirtyCategories(spreadsheetId: String, existingRows: List<List<String>>) {
        pushDirtyBatch(
            spreadsheetId, SheetSchema.TAB_CATEGORIAS, SheetSchema.RANGE_CATEGORIAS, "C", existingRows,
            dirty = categoryDao.getDirty(),
            keyOf = { it.id },
            toRow = { it.toRow() },
            markSynced = { categoryDao.markSynced(it.id) },
        )
    }

    private suspend fun pushDirtyCards(spreadsheetId: String, existingRows: List<List<String>>) {
        pushDirtyBatch(
            spreadsheetId, SheetSchema.TAB_CARTOES, SheetSchema.RANGE_CARTOES, "G", existingRows,
            dirty = cardDao.getDirty(),
            keyOf = { it.id },
            toRow = { it.toRow() },
            markSynced = { cardDao.markSynced(it.id) },
        )
    }

    private suspend fun pushDirtyUsers(spreadsheetId: String, existingRows: List<List<String>>) {
        pushDirtyBatch(
            spreadsheetId, SheetSchema.TAB_USUARIOS, SheetSchema.RANGE_USUARIOS, "B", existingRows,
            dirty = userDao.getDirty(),
            keyOf = { it.email },
            toRow = { it.toRow() },
            markSynced = { userDao.markSynced(it.email) },
        )
    }

    private suspend fun pushDirtyRecurringExpenses(spreadsheetId: String, existingRows: List<List<String>>) {
        pushDirtyBatch(
            spreadsheetId, SheetSchema.TAB_RECORRENTES, SheetSchema.RANGE_RECORRENTES, "K", existingRows,
            dirty = recurringExpenseDao.getDirty(),
            keyOf = { it.id },
            toRow = { it.toRow() },
            markSynced = { recurringExpenseDao.markSynced(it.id) },
        )
    }

    /** Maps the value in column A (the record's id/email) to its 1-based sheet row number. Data starts at row 2. */
    private fun indexRowsById(rows: List<List<String>>): Map<String, Int> =
        rows.mapIndexedNotNull { index, row -> row.getOrNull(0)?.let { it to (index + 2) } }.toMap()

    /**
     * Pushes every dirty record for one tab in as few HTTP calls as possible: every record
     * that already has a row in the sheet is folded into a single `values:batchUpdate` call
     * (one HTTP request for the whole tab, no matter how many rows changed); records with no
     * existing row yet (new since the last pull) are appended together in a single
     * `values:append` call, since that endpoint already accepts multiple rows per request.
     * Each record is marked synced only once its corresponding call has actually succeeded.
     */
    private suspend fun <T> pushDirtyBatch(
        spreadsheetId: String,
        tab: String,
        appendRange: String,
        lastColumn: String,
        existingRows: List<List<String>>,
        dirty: List<T>,
        keyOf: (T) -> String,
        toRow: (T) -> List<String>,
        markSynced: suspend (T) -> Unit,
    ) {
        if (dirty.isEmpty()) return
        val rowNumberById = indexRowsById(existingRows)

        val updates = mutableListOf<T>()
        val updateRanges = mutableListOf<ValueRange>()
        val appends = mutableListOf<T>()
        for (item in dirty) {
            val rowNumber = rowNumberById[keyOf(item)]
            if (rowNumber != null) {
                updates += item
                updateRanges += ValueRange(range = SheetSchema.rowRange(tab, rowNumber, lastColumn), values = listOf(toRow(item)))
            } else {
                appends += item
            }
        }

        if (updateRanges.isNotEmpty()) {
            sheetsApi.batchUpdate(spreadsheetId, BatchUpdateValuesRequest(data = updateRanges))
            updates.forEach { markSynced(it) }
        }

        if (appends.isNotEmpty()) {
            sheetsApi.append(spreadsheetId = spreadsheetId, range = appendRange, body = ValueRange(values = appends.map(toRow)))
            appends.forEach { markSynced(it) }
        }
    }
}
