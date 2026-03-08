package com.codekokeshi.kokefinanceapp.model

import java.util.UUID

enum class TransactionType(val label: String) {
    INCOME("Income"),
    EXPENSE("Expense")
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

fun Wallet.computeBalance(transactions: List<Transaction>): Double {
    return initialBalance + transactions
        .filter { it.walletId == id }
        .sumOf { if (it.type == TransactionType.INCOME) it.amount else -it.amount }
}

fun List<Wallet>.totalBalance(transactions: List<Transaction>): Double {
    return sumOf { it.computeBalance(transactions) }
}
