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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.codekokeshi.kokefinanceapp.model.Transaction
import com.codekokeshi.kokefinanceapp.model.TransactionType
import com.codekokeshi.kokefinanceapp.model.Wallet
import com.codekokeshi.kokefinanceapp.model.WalletKind
import com.codekokeshi.kokefinanceapp.model.balanceMap
import com.codekokeshi.kokefinanceapp.model.computeBalance
import com.codekokeshi.kokefinanceapp.model.isAutoIncludedInTotals
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
import com.codekokeshi.kokefinanceapp.ui.components.formatPeso
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    wallets: List<Wallet>,
    transactions: List<Transaction>,
    onNavigateToTransactions: () -> Unit,
    onNavigateToDebts: () -> Unit,
) {
    val autoWallets = remember(wallets) { wallets.filter { it.isAutoIncludedInTotals() } }
    val hiddenWallets = remember(wallets) { wallets.filterNot { it.isAutoIncludedInTotals() } }
    val debtWallets = remember(wallets) { wallets.filter { it.kind == WalletKind.DEBT } }
    val hiddenStandardWallets = remember(wallets) { wallets.filter { it.kind == WalletKind.STANDARD && it.isHidden } }
    val walletBalances = remember(wallets, transactions) { wallets.balanceMap(transactions) }

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

    val topWallets = remember(autoWallets, transactions) {
        autoWallets.map { wallet -> wallet to (walletBalances[wallet.id] ?: 0.0) }
            .sortedByDescending { it.second }
            .take(3)
    }

    val walletMap = remember(wallets) { wallets.associateBy { it.id } }
    var showAllTransactions by remember { mutableStateOf(false) }

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
                                autoWallets.isEmpty() && hiddenWallets.isEmpty() -> "No wallets yet. Create one to start tracking."
                                hiddenWallets.isEmpty() && autoWallets.size == 1 -> "1 wallet in automatic totals"
                                hiddenWallets.isEmpty() -> "${autoWallets.size} wallets in automatic totals"
                                autoWallets.isEmpty() -> "${hiddenWallets.size} hidden or debt wallet${if (hiddenWallets.size == 1) "" else "s"} kept out of totals"
                                else -> "${autoWallets.size} visible wallet${if (autoWallets.size == 1) "" else "s"}, ${hiddenWallets.size} hidden or debt"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = onNavigateToTransactions,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Transactions")
                            }
                            OutlinedButton(
                                onClick = onNavigateToDebts,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Debt desk")
                            }
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
                    caption = if (autoWallets.isEmpty()) "Hidden and debt wallets stay out of this summary." else "A compact look at the wallets included in your automatic total."
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

            item(key = "offBookHeader") {
                SectionHeader(
                    title = "Off-book wallets",
                    caption = if (hiddenWallets.isEmpty()) {
                        "No hidden or debt wallets yet."
                    } else {
                        "Tracked separately so your main total stays honest."
                    }
                )
            }

            if (hiddenWallets.isEmpty()) {
                item(key = "offBookEmpty") {
                    EmptyStateCard(
                        title = "Nothing off-book yet",
                        subtitle = "Hide locked funds or create debt wallets from Transactions when you want them visible but excluded from the total balance."
                    )
                }
            } else {
                item(key = "offBookStats") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            label = "Debt wallets",
                            amount = debtWallets.size.toDouble(),
                            color = ExpenseColor,
                            valueText = debtWallets.size.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Hidden funds",
                            amount = hiddenStandardWallets.size.toDouble(),
                            color = MaterialTheme.colorScheme.secondary,
                            valueText = hiddenStandardWallets.size.toString(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                items(hiddenWallets.take(3), key = { it.id }) { wallet ->
                    val balance = walletBalances[wallet.id] ?: 0.0
                    SectionCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = wallet.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (wallet.kind == WalletKind.DEBT) {
                                    "Debt wallet kept out of automatic totals"
                                } else {
                                    "Hidden wallet kept out of automatic totals"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatPeso(balance),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                item(key = "offBookAction") {
                    OutlinedButton(onClick = onNavigateToDebts) {
                        Text("Open debt and hidden wallet view")
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

            if (transactions.isNotEmpty()) {
                item(key = "viewAllButton") {
                    OutlinedButton(
                        onClick = { showAllTransactions = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("View all transaction history")
                    }
                }
            }
        }
    }

    if (showAllTransactions) {
        AllTransactionsDialog(
            wallets = wallets,
            transactions = transactions,
            onDismiss = { showAllTransactions = false }
        )
    }
}

private sealed class TimelineEntry {
    abstract val id: String
    abstract val timestamp: Long

    data class Tx(val transaction: Transaction) : TimelineEntry() {
        override val id get() = "tx_${transaction.id}"
        override val timestamp get() = transaction.timestamp
    }

    data class WalletCreated(val wallet: Wallet, override val timestamp: Long) : TimelineEntry() {
        override val id get() = "created_${wallet.id}"
    }
}

@Composable
private fun AllTransactionsDialog(
    wallets: List<Wallet>,
    transactions: List<Transaction>,
    onDismiss: () -> Unit,
) {
    val walletMap = remember(wallets) { wallets.associateBy { it.id } }
    val timelineItems = remember(wallets, transactions) {
        val items = mutableListOf<TimelineEntry>()
        for (tx in transactions) items.add(TimelineEntry.Tx(tx))
        for (wallet in wallets) {
            val firstTxTs = transactions
                .filter { it.walletId == wallet.id }
                .minOfOrNull { it.timestamp }
            val createdTs = if (firstTxTs != null && firstTxTs < wallet.createdAt) {
                firstTxTs - 60_000L
            } else {
                wallet.createdAt
            }
            items.add(TimelineEntry.WalletCreated(wallet, createdTs))
        }
        items.sortedByDescending { it.timestamp }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Full history",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
                LazyColumn(
                    contentPadding = PaddingValues(start = 24.dp, top = 0.dp, end = 24.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(timelineItems, key = { it.id }) { entry ->
                        when (entry) {
                            is TimelineEntry.Tx -> TransactionListItem(
                                transaction = entry.transaction,
                                walletName = walletMap[entry.transaction.walletId]?.name ?: "Unknown"
                            )
                            is TimelineEntry.WalletCreated -> WalletCreationEntry(
                                wallet = entry.wallet,
                                timestamp = entry.timestamp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WalletCreationEntry(wallet: Wallet, timestamp: Long) {
    val fmt = remember { SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault()) }
    SectionCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "${wallet.name} Created",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Opening balance ${formatPeso(wallet.initialBalance)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = fmt.format(Date(timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
