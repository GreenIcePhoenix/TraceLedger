package com.greenicephoenix.traceledger.feature.accountimport.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.greenicephoenix.traceledger.domain.model.AccountUiModel
import com.greenicephoenix.traceledger.domain.model.CategoryUiModel
import com.greenicephoenix.traceledger.feature.accountimport.categorizer.AutoCategorizer
import com.greenicephoenix.traceledger.feature.accountimport.model.BalanceStrategy
import com.greenicephoenix.traceledger.feature.accountimport.model.BankFormat
import com.greenicephoenix.traceledger.feature.accountimport.model.ImportReviewItem
import com.greenicephoenix.traceledger.feature.accountimport.model.ParsedTransaction
import com.greenicephoenix.traceledger.feature.accountimport.parser.ParseResult
import com.greenicephoenix.traceledger.feature.accountimport.parser.StatementParserFactory
import com.greenicephoenix.traceledger.feature.accountimport.repository.StatementImportRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

// ── Filter modes ──────────────────────────────────────────────────────────────

enum class ReviewFilter { ALL, INCLUDED, DUPLICATES }

// ── UI State ──────────────────────────────────────────────────────────────────

sealed class ImportReviewState {

    object Idle    : ImportReviewState()
    object Parsing : ImportReviewState()

    /**
     * File is password-protected.
     *
     * @param wasWrongPassword  True when user already entered a password but it
     *                          was incorrect. The UI shows an error message inside
     *                          the password dialog.
     */
    data class NeedsPassword(
        val fileUri:         Uri,
        val account:         AccountUiModel,
        val categories:      List<CategoryUiModel>,
        val wasWrongPassword: Boolean = false   // ← shows "Incorrect password" in dialog
    ) : ImportReviewState()

    data class Reviewing(
        val items:               List<ImportReviewItem>,
        val format:              BankFormat,
        val account:             AccountUiModel,
        val balanceStrategy:     BalanceStrategy,
        val closingBalanceInput: String,
        val filterMode:          ReviewFilter,
        val existingTxCount:     Int
    ) : ImportReviewState() {

        val filteredItems: List<ImportReviewItem> get() = when (filterMode) {
            ReviewFilter.ALL        -> items
            ReviewFilter.INCLUDED   -> items.filter { it.isIncluded && !it.hasDateError }
            ReviewFilter.DUPLICATES -> items.filter { it.isDuplicate }
        }

        val includedItems: List<ImportReviewItem> get() =
            items.filter { it.isIncluded && !it.hasDateError }

        val duplicateCount: Int     get() = items.count { it.isDuplicate }
        val dateErrorCount: Int     get() = items.count { it.hasDateError }
        val totalAmount: BigDecimal get() = includedItems.fold(BigDecimal.ZERO) { acc, item ->
            if (item.parsed.isCredit) acc.add(item.parsed.amount)
            else acc.subtract(item.parsed.amount)
        }
    }

    data class Importing(val current: Int, val total: Int) : ImportReviewState()

    data class Completed(
        val imported:   Int,
        val skipped:    Int,
        val duplicates: Int
    ) : ImportReviewState()

    data class ParseError(val message: String) : ImportReviewState()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

class StatementImportViewModel(
    private val appContext:       Context,
    private val importRepository: StatementImportRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ImportReviewState>(ImportReviewState.Idle)
    val state: StateFlow<ImportReviewState> = _state.asStateFlow()

    // ── Parsing ───────────────────────────────────────────────────────────────

    fun startParsing(
        fileUri:    Uri,
        account:    AccountUiModel,
        categories: List<CategoryUiModel>,
        password:   String? = null
    ) {
        val current = _state.value
        if (current !is ImportReviewState.Idle &&
            current !is ImportReviewState.ParseError &&
            current !is ImportReviewState.NeedsPassword) return

        viewModelScope.launch {
            _state.value = ImportReviewState.Parsing

            val parseResult = withContext(Dispatchers.IO) {
                StatementParserFactory.parse(appContext, fileUri, password)
            }

            when (parseResult) {
                is ParseResult.NeedsPassword -> {
                    // If we already had a password attempt (password != null) and still got
                    // NeedsPassword back, the password was wrong. Set the flag so the dialog
                    // shows an "Incorrect password" error message.
                    val wasWrong = password != null
                    _state.value = ImportReviewState.NeedsPassword(
                        fileUri          = fileUri,
                        account          = account,
                        categories       = categories,
                        wasWrongPassword = wasWrong
                    )
                }
                is ParseResult.Failure -> {
                    _state.value = ImportReviewState.ParseError(parseResult.message)
                }
                is ParseResult.Success -> {
                    buildReviewState(parseResult, account, categories)
                }
            }
        }
    }

    /** Called when user submits password in the password dialog. */
    fun retryWithPassword(password: String) {
        val needs = _state.value as? ImportReviewState.NeedsPassword ?: return
        startParsing(
            fileUri    = needs.fileUri,
            account    = needs.account,
            categories = needs.categories,
            password   = password
        )
    }

    // ── Review edits ──────────────────────────────────────────────────────────

    fun toggleIncluded(itemId: String) {
        val current = _state.value as? ImportReviewState.Reviewing ?: return
        _state.value = current.copy(
            items = current.items.map { item ->
                if (item.id == itemId && !item.hasDateError)
                    item.copy(isIncluded = !item.isIncluded)
                else item
            }
        )
    }

    fun updateCategory(itemId: String, categoryId: String?) {
        val current = _state.value as? ImportReviewState.Reviewing ?: return
        _state.value = current.copy(
            items = current.items.map { item ->
                if (item.id == itemId) item.copy(categoryId = categoryId) else item
            }
        )
    }

    fun setFilter(filter: ReviewFilter) {
        val current = _state.value as? ImportReviewState.Reviewing ?: return
        _state.value = current.copy(filterMode = filter)
    }

    fun setBalanceStrategy(strategy: BalanceStrategy) {
        val current = _state.value as? ImportReviewState.Reviewing ?: return
        _state.value = current.copy(balanceStrategy = strategy)
    }

    fun updateClosingBalanceInput(text: String) {
        val current = _state.value as? ImportReviewState.Reviewing ?: return
        _state.value = current.copy(closingBalanceInput = text)
    }

    fun excludeAllDuplicates() {
        val current = _state.value as? ImportReviewState.Reviewing ?: return
        _state.value = current.copy(
            items = current.items.map { item ->
                if (item.isDuplicate) item.copy(isIncluded = false) else item
            }
        )
    }

    // ── Import execution ──────────────────────────────────────────────────────

    fun confirmImport() {
        val reviewing = _state.value as? ImportReviewState.Reviewing ?: return
        val toImport  = reviewing.includedItems
        if (toImport.isEmpty()) return

        viewModelScope.launch {
            _state.value = ImportReviewState.Importing(0, toImport.size)

            val skipped    = reviewing.items.count { !it.isIncluded || it.hasDateError }
            val duplicates = reviewing.items.count { it.isDuplicate }

            withContext(Dispatchers.IO) {
                when (val strategy = reviewing.balanceStrategy) {

                    is BalanceStrategy.KeepExisting -> {
                        val entities = toImport.map { item ->
                            importRepository.buildEntity(
                                accountId  = reviewing.account.id,
                                amount     = item.parsed.amount,
                                date       = item.parsed.date!!,
                                note       = item.note,
                                isCredit   = item.parsed.isCredit,
                                categoryId = item.categoryId
                            )
                        }
                        importRepository.bulkInsertRecordsOnly(entities) { p ->
                            _state.value = ImportReviewState.Importing(
                                current = (p * toImport.size) / 100,
                                total   = toImport.size
                            )
                        }
                    }

                    is BalanceStrategy.SetToStatement -> {
                        val entities = toImport.map { item ->
                            importRepository.buildEntity(
                                accountId  = reviewing.account.id,
                                amount     = item.parsed.amount,
                                date       = item.parsed.date!!,
                                note       = item.note,
                                isCredit   = item.parsed.isCredit,
                                categoryId = item.categoryId
                            )
                        }
                        importRepository.bulkInsertAndSetBalance(
                            transactions   = entities,
                            accountId      = reviewing.account.id,
                            currentBalance = reviewing.account.balance,
                            targetBalance  = strategy.closingBalance
                        ) { p ->
                            _state.value = ImportReviewState.Importing(
                                current = (p * toImport.size) / 100,
                                total   = toImport.size
                            )
                        }
                    }

                    is BalanceStrategy.RecalculateFromAll -> {
                        val uiModels = toImport.map { item ->
                            com.greenicephoenix.traceledger.domain.model.TransactionUiModel(
                                id            = java.util.UUID.randomUUID().toString(),
                                type          = if (item.parsed.isCredit)
                                    com.greenicephoenix.traceledger.domain.model.TransactionType.INCOME
                                else
                                    com.greenicephoenix.traceledger.domain.model.TransactionType.EXPENSE,
                                amount        = item.parsed.amount,
                                date          = item.parsed.date!!,
                                fromAccountId = if (!item.parsed.isCredit) reviewing.account.id else null,
                                toAccountId   = if (item.parsed.isCredit)  reviewing.account.id else null,
                                categoryId    = item.categoryId,
                                note          = item.note,
                                createdAt     = Instant.now(),
                                recurringId   = null
                            )
                        }
                        importRepository.bulkInsertWithBalanceUpdates(uiModels) { p ->
                            _state.value = ImportReviewState.Importing(
                                current = (p * toImport.size) / 100,
                                total   = toImport.size
                            )
                        }
                    }
                }
            }

            _state.value = ImportReviewState.Completed(
                imported   = toImport.size,
                skipped    = skipped,
                duplicates = duplicates
            )
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private suspend fun buildReviewState(
        parseResult: ParseResult.Success,
        account:     AccountUiModel,
        categories:  List<CategoryUiModel>
    ) {
        val dates   = parseResult.transactions.mapNotNull { it.date }
        val minDate = dates.minOrNull() ?: LocalDate.now()
        val maxDate = dates.maxOrNull() ?: LocalDate.now()

        val existingInRange = withContext(Dispatchers.IO) {
            importRepository.getExistingTransactionsInRange(
                accountId = account.id,
                startDate = minDate.minusDays(1),
                endDate   = maxDate.plusDays(1)
            )
        }

        val existingCount = withContext(Dispatchers.IO) {
            importRepository.getExistingTransactionCount(account.id)
        }

        val reviewItems = parseResult.transactions.mapIndexed { index, parsed ->
            // If the CSV provided a category name directly, look it up by name.
            // Otherwise run AutoCategorizer on the description.
            val suggestedCategoryId = if (!parsed.importedCategoryName.isNullOrEmpty()) {
                val targetTypeName = if (parsed.isCredit) "INCOME" else "EXPENSE"
                categories.firstOrNull { cat ->
                    cat.name.equals(parsed.importedCategoryName, ignoreCase = true) &&
                            cat.type.name == targetTypeName
                }?.id
                // If name not found in user's categories, fall back to auto-categoriser
                    ?: AutoCategorizer.suggest(parsed.description, parsed.isCredit, categories)
            } else {
                AutoCategorizer.suggest(parsed.description, parsed.isCredit, categories)
            }
            val isDuplicate = parsed.date != null && existingInRange.any { existing ->
                val sameDir = if (parsed.isCredit) existing.toAccountId == account.id
                else existing.fromAccountId == account.id
                val amtMatch = existing.amount.subtract(parsed.amount).abs() <= BigDecimal("0.01")
                val dateMatch = !existing.date.isBefore(parsed.date!!.minusDays(1)) &&
                        !existing.date.isAfter(parsed.date!!.plusDays(1))
                sameDir && amtMatch && dateMatch
            }
            ImportReviewItem(
                id                  = index.toString(),
                parsed              = parsed,
                suggestedCategoryId = suggestedCategoryId,
                categoryId          = suggestedCategoryId,
                note                = parsed.description,
                isIncluded          = parsed.date != null,
                isDuplicate         = isDuplicate,
                hasDateError        = parsed.date == null
            )
        }

        val defaultStrategy = if (existingCount == 0) BalanceStrategy.RecalculateFromAll
        else BalanceStrategy.KeepExisting

        _state.value = ImportReviewState.Reviewing(
            items               = reviewItems,
            format              = parseResult.format,
            account             = account,
            balanceStrategy     = defaultStrategy,
            closingBalanceInput = "",
            filterMode          = ReviewFilter.ALL,
            existingTxCount     = existingCount
        )
    }
}

// ── Factory ───────────────────────────────────────────────────────────────────

class StatementImportViewModelFactory(
    private val appContext:       Context,
    private val importRepository: StatementImportRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        StatementImportViewModel(appContext, importRepository) as T
}