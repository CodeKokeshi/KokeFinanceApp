package com.codekokeshi.kokefinanceapp.model

import java.util.UUID

enum class TransactionType(val label: String) {
    INCOME("Income"),
    EXPENSE("Expense")
}

enum class WalletKind(val label: String) {
    STANDARD("Standard"),
    DEBT("Debt")
}

data class Tag(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val emoji: String,
    val type: TransactionType,
    val createdAt: Long = System.currentTimeMillis()
)

data class Wallet(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val initialBalance: Double = 0.0,
    val isHidden: Boolean = false,
    val kind: WalletKind = WalletKind.STANDARD,
    val createdAt: Long = System.currentTimeMillis()
)

data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val walletId: String,
    val type: TransactionType,
    val amount: Double,
    val tagId: String? = null,
    val tagLabel: String,
    val tagEmoji: String,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

fun Transaction.signedAmount(): Double {
    return if (type == TransactionType.INCOME) amount else -amount
}

fun Wallet.computeBalance(netTransactionTotal: Double): Double {
    return when (kind) {
        WalletKind.STANDARD -> initialBalance + netTransactionTotal
        WalletKind.DEBT -> initialBalance - netTransactionTotal
    }
}

fun Wallet.computeBalance(transactions: List<Transaction>): Double {
    val transactionTotal = transactions
        .filter { it.walletId == id }
        .sumOf { it.signedAmount() }

    return computeBalance(transactionTotal)
}

fun Wallet.isAutoIncludedInTotals(): Boolean {
    return kind == WalletKind.STANDARD && !isHidden
}

fun List<Wallet>.totalBalance(
    transactions: List<Transaction>,
    includeHidden: Boolean = false,
): Double {
    return filter { includeHidden || it.isAutoIncludedInTotals() }
        .sumOf { it.computeBalance(transactions) }
}

fun List<Transaction>.netAmountByWallet(): Map<String, Double> {
    return groupBy { it.walletId }
        .mapValues { (_, walletTransactions) -> walletTransactions.sumOf { it.signedAmount() } }
}

fun List<Wallet>.balanceMap(transactions: List<Transaction>): Map<String, Double> {
    val netByWallet = transactions.netAmountByWallet()
    return associate { wallet ->
        wallet.id to wallet.computeBalance(netByWallet[wallet.id] ?: 0.0)
    }
}
