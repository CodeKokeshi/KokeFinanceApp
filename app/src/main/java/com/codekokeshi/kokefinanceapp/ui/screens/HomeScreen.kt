package com.codekokeshi.kokefinanceapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codekokeshi.kokefinanceapp.model.Transaction
import com.codekokeshi.kokefinanceapp.model.TransactionType
import com.codekokeshi.kokefinanceapp.model.Wallet
import com.codekokeshi.kokefinanceapp.model.computeBalance
import com.codekokeshi.kokefinanceapp.model.totalBalance
import com.codekokeshi.kokefinanceapp.ui.components.AppScreenBackground
import com.codekokeshi.kokefinanceapp.ui.components.EmptyStateCard
import com.codekokeshi.kokefinanceapp.ui.components.ExpenseColor
import com.codekokeshi.kokefinanceapp.ui.components.IncomeColor
import com.codekokeshi.kokefinanceapp.ui.components.SectionHeader
import com.codekokeshi.kokefinanceapp.ui.components.SectionCard
import com.codekokeshi.kokefinanceapp.ui.components.StatCard
import com.codekokeshi.kokefinanceapp.ui.components.TransactionListItem
import com.codekokeshi.kokefinanceapp.ui.components.WalletBalanceCard
import java.util.Calendar

@Composable
fun HomeScreen(
    wallets: List<Wallet>,
    transactions: List<Transaction>,
    onNavigateToTransactions: () -> Unit,
) {
    val totalBalance = remember(wallets, transactions) {
        wallets.totalBalance(transactions)
    }

    val monthStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val monthlyIncome = remember(transactions, monthStart) {
        transactions.filter { it.type == TransactionType.INCOME && it.timestamp >= monthStart }
            .sumOf { it.amount }
    }

    val monthlyExpense = remember(transactions, monthStart) {
        transactions.filter { it.type == TransactionType.EXPENSE && it.timestamp >= monthStart }
            .sumOf { it.amount }
    }

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    val recentTransactions = remember(transactions) {
        transactions.sortedByDescending { it.timestamp }.take(5)
    }

    val topWallets = remember(wallets, transactions) {
        wallets.map { it to it.computeBalance(transactions) }
            .sortedByDescending { it.second }
            .take(3)
    }

    val walletMap = remember(wallets) { wallets.associateBy { it.id } }

    AppScreenBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "hero") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Home",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        )
                    }
                    Text(
                        text = "$greeting",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "A quick read on where your money stands today.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item(key = "balance") {
                SectionCard {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "TOTAL BALANCE",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = com.codekokeshi.kokefinanceapp.ui.components.formatPeso(totalBalance),
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when {
                                wallets.isEmpty() -> "No wallets yet. Create one to start tracking."
                                wallets.size == 1 -> "1 wallet active"
                                else -> "${wallets.size} wallets active"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(onClick = onNavigateToTransactions) {
                            Text("Open transactions")
                        }
                    }
                }
            }

            item(key = "pulseHeader") {
                SectionHeader(
                    title = "This month",
                    caption = "Income versus expense for the current month."
                )
            }

            item(key = "monthlyStats") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        label = "Income",
                        amount = monthlyIncome,
                        color = IncomeColor,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Expense",
                        amount = monthlyExpense,
                        color = ExpenseColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item(key = "walletHeader") {
                SectionHeader(
                    title = "Wallet snapshot",
                    caption = if (wallets.isEmpty()) "No active wallets yet." else "A compact look at your strongest wallets."
                )
            }

            if (topWallets.isEmpty()) {
                item(key = "walletEmpty") {
                    EmptyStateCard(
                        title = "No wallets yet",
                        subtitle = "Create one from the Transactions tab so this dashboard can start telling a story."
                    )
                }
            } else {
                items(topWallets, key = { it.first.id }) { (wallet, balance) ->
                    SectionCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = wallet.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Opening ${com.codekokeshi.kokefinanceapp.ui.components.formatPeso(wallet.initialBalance)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = com.codekokeshi.kokefinanceapp.ui.components.formatPeso(balance),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item(key = "recentHeader") {
                SectionHeader(
                    title = "Recent transactions",
                    caption = if (recentTransactions.isEmpty()) "Nothing recorded yet." else "Latest money movement across your wallets."
                )
            }

            if (recentTransactions.isEmpty()) {
                item(key = "recentEmpty") {
                    EmptyStateCard(
                        title = "No transactions yet",
                        subtitle = "Record an income or expense from the Transactions screen and it will show up here."
                    )
                }
            } else {
                items(recentTransactions, key = { it.id }) { transaction ->
                    TransactionListItem(
                        transaction = transaction,
                        walletName = walletMap[transaction.walletId]?.name ?: "Unknown",
                    )
                }
            }
        }
    }
}
