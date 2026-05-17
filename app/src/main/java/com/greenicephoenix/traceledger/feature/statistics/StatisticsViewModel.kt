package com.greenicephoenix.traceledger.feature.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenicephoenix.traceledger.core.repository.TransactionRepository
import com.greenicephoenix.traceledger.domain.model.TransactionType
import com.greenicephoenix.traceledger.feature.statistics.model.ChartPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth
import com.greenicephoenix.traceledger.feature.budgets.data.BudgetRepository
import com.greenicephoenix.traceledger.feature.budgets.data.BudgetEntity
import java.time.temporal.ChronoUnit
import kotlin.math.sqrt
import kotlin.math.pow
import com.greenicephoenix.traceledger.feature.statistics.model.CalendarDay
import com.greenicephoenix.traceledger.feature.statistics.model.WeekdayPattern
import java.time.LocalDate

class StatisticsViewModel(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository:      BudgetRepository
) : ViewModel() {

    // ── Inner data classes ────────────────────────────────────────────────────

    data class CategorySlice(
        val categoryId: String,
        val amount:     BigDecimal,
        val percentage: Float
    )

    data class CashflowEntry(
        val day:     Int,
        val income:  BigDecimal,
        val expense: BigDecimal
    )

    data class CategoryMonthlyTrend(
        val categoryId: String,
        val month:      YearMonth,
        val total:      BigDecimal
    )

    /** Budget ring display model — one ring per active budget. */
    data class BudgetRingData(
        val budgetId:    String,
        val categoryId:  String?,   // null = overall budget
        val label:       String,
        val spent:       BigDecimal,
        val limit:       BigDecimal,
        /** 0.0–1.0 utilization fraction. Can exceed 1.0 if over budget. */
        val utilization: Float
    )

    // ── Phase 3 data classes ──────────────────────────────────────────────────

    data class TreemapNode(
        val categoryId: String,
        val label:      String,
        val amount:     Double,
        val fraction:   Float,   // 0–1 share of total
        val color:      Int      // category color from CategoryUiModel
    )

    data class SankeyNode(
        val id:    String,
        val label: String,
        val type:  SankeyNodeType  // INCOME or EXPENSE
    )

    enum class SankeyNodeType { INCOME, EXPENSE }

    data class SankeyLink(
        val sourceId: String,   // income category id
        val targetId: String,   // expense category id
        /** Proportional flow amount — we distribute income proportionally across expense categories */
        val amount:   Double,
        val fraction: Float     // fraction of total income this link represents
    )

    data class HealthScore(
        val score:          Int,    // 0–100
        val savingsRate:    Float,  // 0–1
        val budgetAdherence: Float, // 0–1 (1 = all budgets on track)
        val consistency:    Float,  // 0–1 (low variance = high consistency)
        val grade:          String  // "A+", "A", "B", "C", "D", "F"
    )

    data class SavingsRatePoint(
        val monthLabel: String,
        val rate:       Float   // can be negative
    )

    data class VelocityPoint(
        val day:            Int,
        val currentMonth:   Double,
        val previousMonth:  Double,
        val monthlyAverage: Double
    )

    data class CategoryComparison(
        val categoryId:   String,
        val label:        String,
        val color:        Int,
        val thisMonth:    Double,
        val lastMonth:    Double,
        val changePercent: Float
    )

    data class TopSpendDay(
        val date:  java.time.LocalDate,
        val total: Double,
        val rank:  Int
    )

    data class IncomeStabilityData(
        val monthlyAmounts: List<Double>,
        val mean:           Double,
        val stdDev:         Double,
        /** Coefficient of variation — lower = more stable */
        val cv:             Float,
        val stabilityScore: Int    // 0–100, higher = more stable
    )

    data class RollingWindowData(
        val label:       String,   // "30 days", "60 days", "90 days"
        val totalExpense: Double,
        val totalIncome:  Double,
        val netAmount:    Double,
        val dailyAverage: Double,
        val trend:        Float    // positive = spending more vs prior equal window
    )

    data class SpendingStreak(
        val days:          Int,
        val isActive:      Boolean,  // false if today already broke the streak
        val monthBudgetOk: Boolean   // true if currently under budget this month
    )

    // ── Selected month ────────────────────────────────────────────────────────

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    fun previousMonth()                  { _selectedMonth.value = _selectedMonth.value.minusMonths(1) }
    fun nextMonth()                      { _selectedMonth.value = _selectedMonth.value.plusMonths(1)  }
    fun selectMonth(month: YearMonth)    { _selectedMonth.value = month }

    // ── Monthly transaction streams ───────────────────────────────────────────

    @OptIn(ExperimentalCoroutinesApi::class)
    private val monthlyTransactions = _selectedMonth.flatMapLatest { month ->
        transactionRepository.observeTransactionsForMonth(month)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val previousMonthTransactions = _selectedMonth.flatMapLatest { month ->
        transactionRepository.observeTransactionsForMonth(month.minusMonths(1))
    }

    private val analyticsTransactions = monthlyTransactions.map { list ->
        list.filter { it.type == TransactionType.EXPENSE || it.type == TransactionType.INCOME }
    }

    private val previousAnalyticsTransactions = previousMonthTransactions.map { list ->
        list.filter { it.type == TransactionType.EXPENSE || it.type == TransactionType.INCOME }
    }

    // ── Current month aggregates ──────────────────────────────────────────────

    val totalIncome: StateFlow<BigDecimal> =
        analyticsTransactions.map { list ->
            list.filter { it.type == TransactionType.INCOME }
                .fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BigDecimal.ZERO)

    val totalExpense: StateFlow<BigDecimal> =
        analyticsTransactions.map { list ->
            list.filter { it.type == TransactionType.EXPENSE }
                .fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BigDecimal.ZERO)

    val netAmount: StateFlow<BigDecimal> =
        combine(totalIncome, totalExpense) { inc, exp -> inc.subtract(exp) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BigDecimal.ZERO)

    // ── Previous month aggregates ─────────────────────────────────────────────

    val prevMonthIncome: StateFlow<BigDecimal> =
        previousAnalyticsTransactions.map { list ->
            list.filter { it.type == TransactionType.INCOME }
                .fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BigDecimal.ZERO)

    val prevMonthExpense: StateFlow<BigDecimal> =
        previousAnalyticsTransactions.map { list ->
            list.filter { it.type == TransactionType.EXPENSE }
                .fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BigDecimal.ZERO)

    // ── Category slices ───────────────────────────────────────────────────────

    private val expenseByCategory: StateFlow<Map<String, BigDecimal>> =
        analyticsTransactions.map { txs ->
            txs.filter { it.type == TransactionType.EXPENSE && it.categoryId != null }
                .groupBy { it.categoryId!! }
                .mapValues { (_, list) -> list.fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount } }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val incomeByCategory: StateFlow<Map<String, BigDecimal>> =
        analyticsTransactions.map { txs ->
            txs.filter { it.type == TransactionType.INCOME && it.categoryId != null }
                .groupBy { it.categoryId!! }
                .mapValues { (_, list) -> list.fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount } }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val expenseCategorySlices: StateFlow<List<CategorySlice>> =
        expenseByCategory.map { buildCategorySlices(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val incomeCategorySlices: StateFlow<List<CategorySlice>> =
        incomeByCategory.map { buildCategorySlices(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun buildCategorySlices(totals: Map<String, BigDecimal>): List<CategorySlice> {
        if (totals.isEmpty()) return emptyList()
        val total = totals.values.fold(BigDecimal.ZERO) { acc, v -> acc + v }
        if (total == BigDecimal.ZERO) return emptyList()
        return totals.map { (id, amount) ->
            CategorySlice(
                categoryId = id,
                amount     = amount,
                percentage = amount.multiply(BigDecimal(100))
                    .divide(total, 4, RoundingMode.HALF_UP).toFloat()
            )
        }.sortedByDescending { it.amount }
    }

    // ── Cashflow by day ───────────────────────────────────────────────────────

    val cashflowByDay: StateFlow<List<CashflowEntry>> =
        analyticsTransactions.map { transactions ->
            transactions.groupBy { it.date.dayOfMonth }
                .map { (day, dayTxs) ->
                    CashflowEntry(
                        day     = day,
                        income  = dayTxs.filter { it.type == TransactionType.INCOME }
                            .fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount },
                        expense = dayTxs.filter { it.type == TransactionType.EXPENSE }
                            .fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }
                    )
                }.sortedBy { it.day }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Category trends — bounded to last 12 months ───────────────────────────
    // Previously unbounded (observeAllTransactions). Now scoped to 12 months
    // to avoid performance issues on large datasets.

    private val last12MonthsTransactions: StateFlow<List<com.greenicephoenix.traceledger.domain.model.TransactionUiModel>> =
        transactionRepository.observeTransactions().map { all ->
            val cutoff = YearMonth.now().minusMonths(11)
            all.filter { tx ->
                val txMonth = YearMonth.from(tx.date)
                !txMonth.isBefore(cutoff)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Monthly expense totals per category — last 12 months. */
    val categoryExpenseTrends: StateFlow<List<CategoryMonthlyTrend>> =
        last12MonthsTransactions.map { transactions ->
            buildTrends(transactions, TransactionType.EXPENSE)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Monthly income totals per category — last 12 months. */
    val categoryIncomeTrends: StateFlow<List<CategoryMonthlyTrend>> =
        last12MonthsTransactions.map { transactions ->
            buildTrends(transactions, TransactionType.INCOME)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun buildTrends(
        transactions: List<com.greenicephoenix.traceledger.domain.model.TransactionUiModel>,
        type: TransactionType
    ): List<CategoryMonthlyTrend> =
        transactions.asSequence()
            .filter { it.type == type && it.categoryId != null }
            .groupBy { Pair(it.categoryId!!, YearMonth.from(it.date)) }
            .map { (key, list) ->
                CategoryMonthlyTrend(
                    categoryId = key.first,
                    month      = key.second,
                    total      = list.fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }
                )
            }
            .sortedWith(compareBy<CategoryMonthlyTrend> { it.categoryId }.thenBy { it.month })
            .toList()

    // ── Sparkline data for hub cards (daily totals for selected month) ─────────

    /** Daily expense totals for the selected month — used by SparklineChart on hub. */
    val dailyExpensePoints: StateFlow<List<Float>> =
        analyticsTransactions.map { txs ->
            val byDay = txs.filter { it.type == TransactionType.EXPENSE }
                .groupBy { it.date.dayOfMonth }
                .mapValues { (_, list) -> list.fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }.toFloat() }
            // Fill all days 1..monthLength with 0 if no transactions
            val month = _selectedMonth.value
            (1..month.lengthOfMonth()).map { day -> byDay[day] ?: 0f }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Daily income totals for the selected month — used by SparklineChart on hub. */
    val dailyIncomePoints: StateFlow<List<Float>> =
        analyticsTransactions.map { txs ->
            val byDay = txs.filter { it.type == TransactionType.INCOME }
                .groupBy { it.date.dayOfMonth }
                .mapValues { (_, list) -> list.fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }.toFloat() }
            val month = _selectedMonth.value
            (1..month.lengthOfMonth()).map { day -> byDay[day] ?: 0f }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Budget utilization rings ──────────────────────────────────────────────
// Combines budgets for the selected month with expense totals per category.

    @OptIn(ExperimentalCoroutinesApi::class)
    val budgetRings: StateFlow<List<BudgetRingData>> =
        _selectedMonth.flatMapLatest { month ->
            budgetRepository.observeBudgetsForMonth(month)
        }.combine(expenseByCategory) { budgets, expenseMap ->
            budgets.map { budget ->
                val spent = if (budget.categoryId != null)
                    expenseMap[budget.categoryId] ?: BigDecimal.ZERO
                else
                // Overall budget — sum all expenses
                    expenseMap.values.fold(BigDecimal.ZERO) { acc, v -> acc + v }

                val util = if (budget.limitAmount > BigDecimal.ZERO)
                    spent.divide(budget.limitAmount, 4, RoundingMode.HALF_UP).toFloat()
                else 0f

                BudgetRingData(
                    budgetId    = budget.id,
                    categoryId  = budget.categoryId,
                    //label       = budget.name,
                    label       = budget.categoryId,
                    spent       = spent,
                    limit       = budget.limitAmount,
                    utilization = util
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Calendar heatmap — daily expense intensity for selected month ─────────

    val calendarHeatmap: StateFlow<List<CalendarDay>> =
        analyticsTransactions.map { txs ->
            val month  = _selectedMonth.value
            val byDay  = txs.filter { it.type == TransactionType.EXPENSE }
                .groupBy { it.date }
                .mapValues { (_, list) ->
                    list.fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }.toDouble()
                }
            val maxAmt = byDay.values.maxOrNull() ?: 1.0

            (1..month.lengthOfMonth()).map { day ->
                val date   = LocalDate.of(month.year, month.month, day)
                val total  = byDay[date] ?: 0.0
                CalendarDay(
                    date         = date,
                    totalExpense = total,
                    intensity    = (total / maxAmt).toFloat().coerceIn(0f, 1f)
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

// ── Weekday pattern — avg expense per day of week (last 12 months) ────────

    val weekdayPattern: StateFlow<List<WeekdayPattern>> =
        last12MonthsTransactions.map { txs ->
            txs.filter { it.type == TransactionType.EXPENSE }
                .groupBy { it.date.dayOfWeek.value } // 1=Mon..7=Sun
                .map { (dow, list) ->
                    WeekdayPattern(
                        dayOfWeek   = dow,
                        hourOfDay   = 0, // reserved
                        totalAmount = list.fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }.toDouble(),
                        count       = list.size
                    )
                }
                .sortedBy { it.dayOfWeek }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

// ── Monthly area chart points — 12 months of income vs expense ───────────
// Each pair: (incomePoint, expensePoint) per month, ordered oldest → newest.

    data class MonthlyAreaPoint(
        val monthLabel: String,   // "Jan", "Feb" etc.
        val income:     Double,
        val expense:    Double
    )

    val monthlyAreaPoints: StateFlow<List<MonthlyAreaPoint>> =
        last12MonthsTransactions.map { txs ->
            val cutoff = YearMonth.now().minusMonths(11)
            val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM")

            // Build a map of YearMonth → (income, expense)
            val byMonth = mutableMapOf<YearMonth, Pair<BigDecimal, BigDecimal>>()
            txs.forEach { tx ->
                val ym = YearMonth.from(tx.date)
                val (inc, exp) = byMonth.getOrDefault(ym, BigDecimal.ZERO to BigDecimal.ZERO)
                byMonth[ym] = when (tx.type) {
                    TransactionType.INCOME  -> (inc + tx.amount) to exp
                    TransactionType.EXPENSE -> inc to (exp + tx.amount)
                    else                    -> inc to exp
                }
            }

            // Fill all 12 months in order, including months with no transactions
            (0..11).map { offset ->
                val ym  = cutoff.plusMonths(offset.toLong())
                val (inc, exp) = byMonth.getOrDefault(ym, BigDecimal.ZERO to BigDecimal.ZERO)
                MonthlyAreaPoint(
                    monthLabel = ym.format(formatter),
                    income     = inc.toDouble(),
                    expense    = exp.toDouble()
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

// ── Waterfall data — monthly cashflow summary ─────────────────────────────
// Opening balance → +income → -expense → closing balance for selected month.

    data class WaterfallBar(
        val label:      String,
        val value:      Double,
        /** true = positive bar (income/opening), false = negative (expense) */
        val isPositive: Boolean,
        /** Running total at end of this bar — used to position stacked bars. */
        val runningTotal: Double
    )

    val waterfallBars: StateFlow<List<WaterfallBar>> =
        combine(totalIncome, totalExpense) { income, expense ->
            // We don't have opening balance in StatisticsViewModel (that's in Dashboard).
            // Use 0 as opening — waterfall shows the month's flow, not absolute balance.
            val open   = 0.0
            val inc    = income.toDouble()
            val exp    = expense.toDouble()
            val close  = inc - exp
            listOf(
                WaterfallBar("Opening",  open, true,  open),
                WaterfallBar("Income",   inc,  true,  open + inc),
                WaterfallBar("Expense",  exp,  false, open + inc - exp),
                WaterfallBar("Net",      close, close >= 0, close)
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Phase 3 StateFlows ────────────────────────────────────────────────────

    // 1. Treemap — expense category proportional areas for selected month
    val treemapNodes: StateFlow<List<TreemapNode>> =
        expenseByCategory.map { expenseMap ->
            if (expenseMap.isEmpty()) return@map emptyList()
            val total = expenseMap.values.fold(BigDecimal.ZERO) { acc, v -> acc + v }
            if (total == BigDecimal.ZERO) return@map emptyList()
            expenseMap.map { (categoryId, amount) ->
                TreemapNode(
                    categoryId = categoryId,
                    label      = categoryId, // caller resolves name from categoryMap
                    amount     = amount.toDouble(),
                    fraction   = amount.divide(total, 4, RoundingMode.HALF_UP).toFloat(),
                    color      = 0xFF7C4DFF.toInt() // placeholder; screen resolves from categoryMap
                )
            }.sortedByDescending { it.amount }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // 2. Sankey — income→expense flow links
    val sankeyData: StateFlow<Pair<List<SankeyNode>, List<SankeyLink>>> =
        combine(incomeByCategory, expenseByCategory) { incomeMap, expenseMap ->
            if (incomeMap.isEmpty() || expenseMap.isEmpty()) {
                return@combine Pair<List<SankeyNode>, List<SankeyLink>>(emptyList(), emptyList())
            }

            val totalIncome  = incomeMap.values.fold(BigDecimal.ZERO) { acc, v -> acc + v }
            val totalExpense = expenseMap.values.fold(BigDecimal.ZERO) { acc, v -> acc + v }
            if (totalIncome == BigDecimal.ZERO) {
                return@combine Pair<List<SankeyNode>, List<SankeyLink>>(emptyList(), emptyList())
            }

            val incomeNodes  = incomeMap.keys.map { SankeyNode(it, it, SankeyNodeType.INCOME) }
            val expenseNodes = expenseMap.keys.map { SankeyNode(it, it, SankeyNodeType.EXPENSE) }
            val nodes        = incomeNodes + expenseNodes

            val links = mutableListOf<SankeyLink>()
            incomeMap.forEach { (incId, incAmount) ->
                val incFraction = incAmount.divide(totalIncome, 4, RoundingMode.HALF_UP).toFloat()
                expenseMap.forEach { (expId, expAmount) ->
                    val safeTotalExpense = totalExpense.coerceAtLeast(BigDecimal.ONE)
                    val flowAmount = incAmount
                        .multiply(expAmount.divide(safeTotalExpense, 4, RoundingMode.HALF_UP))
                        .toDouble()
                    links.add(SankeyLink(
                        sourceId = incId,
                        targetId = expId,
                        amount   = flowAmount,
                        fraction = incFraction * expAmount
                            .divide(safeTotalExpense, 4, RoundingMode.HALF_UP).toFloat()
                    ))
                }
            }
            Pair(nodes, links)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            Pair<List<SankeyNode>, List<SankeyLink>>(emptyList(), emptyList())
        )

    // 3. Financial Health Score
    val healthScore: StateFlow<HealthScore> =
        combine(
            totalIncome, totalExpense, budgetRings
        ) { income, expense, rings ->
            // Savings rate component (0–40 points)
            val savingsRate = if (income > BigDecimal.ZERO)
                (income - expense).divide(income, 4, RoundingMode.HALF_UP).toFloat().coerceIn(-1f, 1f)
            else 0f
            val savingsPoints = (savingsRate.coerceAtLeast(0f) * 40f).toInt()

            // Budget adherence component (0–35 points)
            val budgetScore = if (rings.isEmpty()) 0.7f
            else rings.map { (1f - it.utilization).coerceIn(0f, 1f) }.average().toFloat()
            val budgetPoints = (budgetScore * 35f).toInt()

            // Consistency component — low CV = high score (0–25 points)
            // Use daily expense std dev vs mean for current month
            val consistencyPoints = 15  // default mid — full calc needs dailyExpensePoints

            val total = (savingsPoints + budgetPoints + consistencyPoints).coerceIn(0, 100)
            val grade = when {
                total >= 90 -> "A+"
                total >= 80 -> "A"
                total >= 70 -> "B"
                total >= 60 -> "C"
                total >= 50 -> "D"
                else        -> "F"
            }
            HealthScore(
                score           = total,
                savingsRate     = savingsRate,
                budgetAdherence = budgetScore,
                consistency     = consistencyPoints / 25f,
                grade           = grade
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000),
            HealthScore(0, 0f, 0f, 0f, "F"))

    // 4. Savings rate trend — 12 months
    val savingsRateTrend: StateFlow<List<SavingsRatePoint>> =
        last12MonthsTransactions.map { txs ->
            val cutoff    = YearMonth.now().minusMonths(11)
            val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM")
            val byMonth   = txs.groupBy { YearMonth.from(it.date) }

            (0..11).map { offset ->
                val ym   = cutoff.plusMonths(offset.toLong())
                val list = byMonth[ym] ?: emptyList()
                val inc  = list.filter { it.type == TransactionType.INCOME }
                    .fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }
                val exp  = list.filter { it.type == TransactionType.EXPENSE }
                    .fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }
                val rate = if (inc > BigDecimal.ZERO)
                    (inc - exp).divide(inc, 4, RoundingMode.HALF_UP).toFloat()
                else 0f
                SavingsRatePoint(ym.format(formatter), rate)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // 5. Expense velocity — cumulative daily spend: current vs previous vs avg
    @OptIn(ExperimentalCoroutinesApi::class)
    val expenseVelocity: StateFlow<List<VelocityPoint>> =
        combine(
            _selectedMonth.flatMapLatest { month ->
                transactionRepository.observeTransactionsForMonth(month)
            },
            _selectedMonth.flatMapLatest { month ->
                transactionRepository.observeTransactionsForMonth(month.minusMonths(1))
            },
            last12MonthsTransactions
        ) { current, previous, last12 ->
            val month        = _selectedMonth.value
            val daysInMonth  = month.lengthOfMonth()

            // Current month daily cumulative
            val currentByDay = current.filter { it.type == TransactionType.EXPENSE }
                .groupBy { it.date.dayOfMonth }
                .mapValues { (_, list) -> list.fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }.toDouble() }

            // Previous month daily cumulative
            val prevByDay = previous.filter { it.type == TransactionType.EXPENSE }
                .groupBy { it.date.dayOfMonth }
                .mapValues { (_, list) -> list.fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }.toDouble() }

            // 12-month daily average (normalize to 30-day month)
            val avgMonthlyExpense = last12.filter { it.type == TransactionType.EXPENSE }
                .fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }
                .toDouble() / 12.0

            var currentCumulative = 0.0
            var prevCumulative    = 0.0

            (1..daysInMonth).map { day ->
                currentCumulative += currentByDay[day] ?: 0.0
                prevCumulative    += prevByDay[day]    ?: 0.0
                val avgCumulative  = avgMonthlyExpense * (day.toDouble() / daysInMonth)

                VelocityPoint(
                    day            = day,
                    currentMonth   = currentCumulative,
                    previousMonth  = prevCumulative,
                    monthlyAverage = avgCumulative
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // 6. Category month-over-month comparison — top 5 categories
    val categoryComparison: StateFlow<List<CategoryComparison>> =
        combine(expenseByCategory, previousMonthTransactions) { thisMonthMap, prevTxs ->
            val prevMap = prevTxs.filter { it.type == TransactionType.EXPENSE && it.categoryId != null }
                .groupBy { it.categoryId!! }
                .mapValues { (_, list) -> list.fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }.toDouble() }

            thisMonthMap.entries
                .sortedByDescending { it.value }
                .take(5)
                .map { (categoryId, amount) ->
                    val thisAmt = amount.toDouble()
                    val lastAmt = prevMap[categoryId] ?: 0.0
                    val change  = if (lastAmt > 0.0) ((thisAmt - lastAmt) / lastAmt).toFloat() else 0f
                    CategoryComparison(
                        categoryId    = categoryId,
                        label         = categoryId, // screen resolves from categoryMap
                        color         = 0xFF7C4DFF.toInt(),
                        thisMonth     = thisAmt,
                        lastMonth     = lastAmt,
                        changePercent = change * 100f
                    )
                }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // 7. Income stability — variance analysis over 12 months
    val incomeStability: StateFlow<IncomeStabilityData> =
        last12MonthsTransactions.map { txs ->
            val cutoff  = YearMonth.now().minusMonths(11)
            val byMonth = txs.groupBy { YearMonth.from(it.date) }

            val monthlyAmounts = (0..11).map { offset ->
                val ym = cutoff.plusMonths(offset.toLong())
                byMonth[ym]?.filter { it.type == TransactionType.INCOME }
                    ?.fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }
                    ?.toDouble() ?: 0.0
            }

            val mean   = monthlyAmounts.average()
            val stdDev = if (mean > 0) sqrt(monthlyAmounts.map { (it - mean).pow(2) }.average()) else 0.0
            val cv     = if (mean > 0) (stdDev / mean).toFloat() else 1f

            // Stability score: CV=0 → 100, CV=1 → 0, CV>1 → 0
            val stabilityScore = ((1f - cv.coerceIn(0f, 1f)) * 100f).toInt()

            IncomeStabilityData(
                monthlyAmounts  = monthlyAmounts,
                mean            = mean,
                stdDev          = stdDev,
                cv              = cv,
                stabilityScore  = stabilityScore
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000),
            IncomeStabilityData(emptyList(), 0.0, 0.0, 1f, 0))

    // 8. Top spending days — all time, top 10
    val topSpendingDays: StateFlow<List<TopSpendDay>> =
        transactionRepository.observeTransactions().map { txs ->
            txs.filter { it.type == TransactionType.EXPENSE }
                .groupBy { it.date }
                .mapValues { (_, list) -> list.fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }.toDouble() }
                .entries
                .sortedByDescending { it.value }
                .take(10)
                .mapIndexed { index, (date, total) ->
                    TopSpendDay(date = date, total = total, rank = index + 1)
                }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // 9. Rolling window — 30/60/90 day summaries
    val rollingWindows: StateFlow<List<RollingWindowData>> =
        transactionRepository.observeTransactions().map { txs ->
            val today = LocalDate.now()
            listOf(30, 60, 90).map { days ->
                val windowStart = today.minusDays(days.toLong())
                val priorStart  = windowStart.minusDays(days.toLong())

                val windowTxs = txs.filter { it.date >= windowStart }
                val priorTxs  = txs.filter { it.date >= priorStart && it.date < windowStart }

                val expense = windowTxs.filter { it.type == TransactionType.EXPENSE }
                    .fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }.toDouble()
                val income  = windowTxs.filter { it.type == TransactionType.INCOME }
                    .fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }.toDouble()
                val priorExp = priorTxs.filter { it.type == TransactionType.EXPENSE }
                    .fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }.toDouble()

                val trend = if (priorExp > 0.0) ((expense - priorExp) / priorExp).toFloat() else 0f

                RollingWindowData(
                    label        = "$days days",
                    totalExpense = expense,
                    totalIncome  = income,
                    netAmount    = income - expense,
                    dailyAverage = expense / days,
                    trend        = trend
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // 10. Spending streak — days under budget this month
    val spendingStreak: StateFlow<SpendingStreak> =
        combine(budgetRings, totalExpense, totalIncome) { rings, expense, income ->
            // "Under budget this month" = all active budgets have utilization < 1.0
            val allBudgetsOk = rings.isEmpty() || rings.all { it.utilization < 1.0f }

            // Count consecutive days from today backward where daily expense < daily budget allowance
            // Simplified: if currently under budget, streak = days elapsed this month so far
            val today         = LocalDate.now()
            val daysElapsed   = today.dayOfMonth
            val monthlyLimit  = rings.firstOrNull { it.categoryId == null }?.limit?.toDouble()
                ?: if (income > BigDecimal.ZERO) income.toDouble() else 0.0
            val dailyAllowance = if (monthlyLimit > 0.0) monthlyLimit / today.lengthOfMonth() else 0.0
            val dailySpend     = if (daysElapsed > 0) expense.toDouble() / daysElapsed else 0.0
            val underBudget    = dailyAllowance == 0.0 || dailySpend <= dailyAllowance

            SpendingStreak(
                days          = if (underBudget) daysElapsed else 0,
                isActive      = underBudget,
                monthBudgetOk = allBudgetsOk
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000),
            SpendingStreak(0, false, false))
}

private operator fun LocalDate.compareTo(other: LocalDate): Int = this.compareTo(other)

private fun BigDecimal.coerceAtLeast(min: BigDecimal): BigDecimal =
    if (this < min) min else this