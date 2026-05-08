// ─────────────────────────────────────────────────────────────────────────────
// FILE: feature/accountimport/model/BalanceStrategy.kt
//
// What this is:
//   Controls what happens to the account's balance after import completes.
//
// Why this matters (the core problem):
//   A user has ICICI with ₹50,000 in TraceLedger. That balance is correct
//   TODAY. They import a statement from Jan 2025 with 10 transactions.
//
//   If we blindly call insertTransactionWithBalance() for all 10, TraceLedger
//   will change the account balance — but the ₹50,000 already REFLECTS those
//   10 transactions in real life. We'd double-count the impact.
//
//   Solution: let the user decide. Present three options with clear explanations.
//   Pre-select the safest option based on context (see ImportReviewViewModel).
// ─────────────────────────────────────────────────────────────────────────────
package com.greenicephoenix.traceledger.feature.accountimport.model

import java.math.BigDecimal

/**
 * Determines how the account's balance is handled after statement import.
 */
sealed class BalanceStrategy {

    /**
     * KEEP_EXISTING — Don't touch the account balance at all.
     *
     * When to use: Account already has a correct current balance in TraceLedger.
     * You're importing old statement records for history/statistics purposes.
     * The transactions are inserted as records only — no balance delta applied.
     *
     * Pre-selected when: account already has 1 or more existing transactions.
     *
     * Implementation: insert via transactionDao.insertTransaction() directly,
     * bypassing the balance-update logic in insertTransactionWithBalance().
     */
    object KeepExisting : BalanceStrategy()

    /**
     * SET_TO_STATEMENT — After inserting all records, set balance to a
     * user-provided closing balance from the last page of their statement.
     *
     * When to use: User wants TraceLedger balance to match what their bank
     * shows — most useful after a full account reconciliation.
     *
     * Pre-selected when: never (user must explicitly choose this).
     *
     * Implementation: insert records without balance delta, then call
     * accountDao.setBalance(accountId, closingBalance) once at the end.
     *
     * @param closingBalance The balance the user typed in from their statement.
     */
    data class SetToStatement(val closingBalance: BigDecimal) : BalanceStrategy()

    /**
     * RECALCULATE_FROM_ALL — Insert transactions WITH balance deltas applied,
     * exactly as normal transaction adds work.
     *
     * When to use: Account was just created (zero transactions) and the
     * statement represents ALL activity on this account. Let TraceLedger
     * calculate the running balance from scratch.
     *
     * Pre-selected when: account has 0 existing transactions.
     *
     * ⚠️ WARNING shown in UI: Only use if this account is newly created and
     * has no prior transactions in TraceLedger. Using on an existing account
     * will corrupt the balance.
     *
     * Implementation: call insertTransactionWithBalance() for each transaction.
     */
    object RecalculateFromAll : BalanceStrategy()
}