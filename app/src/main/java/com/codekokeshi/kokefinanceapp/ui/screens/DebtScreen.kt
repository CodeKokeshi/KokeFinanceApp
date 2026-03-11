package com.codekokeshi.kokefinanceapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codekokeshi.kokefinanceapp.model.Transaction
import com.codekokeshi.kokefinanceapp.model.TransactionType
import com.codekokeshi.kokefinanceapp.model.Wallet
import com.codekokeshi.kokefinanceapp.model.WalletKind
import com.codekokeshi.kokefinanceapp.model.balanceMap
import com.codekokeshi.kokefinanceapp.model.computeBalance
import com.codekokeshi.kokefinanceapp.model.isAutoIncludedInTotals
import com.codekokeshi.kokefinanceapp.ui.components.AppScreenBackground
import com.codekokeshi.kokefinanceapp.ui.components.EmptyStateCard
import com.codekokeshi.kokefinanceapp.ui.components.ExpenseColor
import com.codekokeshi.kokefinanceapp.ui.components.SectionCard
import com.codekokeshi.kokefinanceapp.ui.components.SectionHeader
import com.codekokeshi.kokefinanceapp.ui.components.StatCard
import com.codekokeshi.kokefinanceapp.ui.components.formatPeso

@Composable
fun DebtScreen(
    wallets: List<Wallet>,
    transactions: List<Transaction>,
    onNavigateToTransactions: () -> Unit,
) {
    val walletBalances = remember(wallets, transactions) { wallets.balanceMap(transactions) }
    val visibleWallets = remember(wallets) { wallets.filter { it.isAutoIncludedInTotals() } }
    val debtWallets = remember(wallets) { wallets.filter { it.kind == WalletKind.DEBT } }
    val hiddenStandardWallets = remember(wallets) {
        wallets.filter { it.kind == WalletKind.STANDARD && it.isHidden }
    }
    var selectedVisibleWalletIds by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var selectedDebtWalletIds by rememberSaveable { mutableStateOf(emptyList<String>()) }

    LaunchedEffect(visibleWallets, debtWallets) {
        val visibleIds = visibleWallets.map { it.id }.toSet()
        val debtIds = debtWallets.map { it.id }.toSet()
        selectedVisibleWalletIds = if (selectedVisibleWalletIds.isEmpty()) {
            visibleWallets.map { it.id }
        } else {
            selectedVisibleWalletIds.filter { it in visibleIds }
        }
        selectedDebtWalletIds = if (selectedDebtWalletIds.isEmpty()) {
            debtWallets.map { it.id }
        } else {
            selectedDebtWalletIds.filter { it in debtIds }
        }
    }

    val visibleTotal = remember(visibleWallets, transactions) {
        visibleWallets.sumOf { walletBalances[it.id] ?: 0.0 }
    }
    val debtTotal = remember(debtWallets, transactions) {
        debtWallets.sumOf { walletBalances[it.id] ?: 0.0 }
    }
    val lockedFundsTotal = remember(hiddenStandardWallets, transactions) {
        hiddenStandardWallets.sumOf { walletBalances[it.id] ?: 0.0 }
    }
    val selectedVisibleWallets = remember(visibleWallets, selectedVisibleWalletIds) {
        visibleWallets.filter { it.id in selectedVisibleWalletIds }
    }
    val selectedDebtWallets = remember(debtWallets, selectedDebtWalletIds) {
        debtWallets.filter { it.id in selectedDebtWalletIds }
    }
    val selectedVisibleTotal = remember(selectedVisibleWallets, walletBalances) {
        selectedVisibleWallets.sumOf { walletBalances[it.id] ?: 0.0 }
    }
    val selectedDebtTotal = remember(selectedDebtWallets, walletBalances) {
        selectedDebtWallets.sumOf { walletBalances[it.id] ?: 0.0 }
    }
    val spendableAfterDebt = selectedVisibleTotal - selectedDebtTotal

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
                            text = "Debts",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        )
                    }
                    Text(
                        text = "See the pressure clearly.",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pick which spendable wallets to count and which debt wallets to subtract. Locked funds stay separate by design.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item(key = "overview") {
                SectionCard {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "SPENDABLE AFTER DEBT",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = formatPeso(spendableAfterDebt),
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (selectedDebtWallets.isEmpty()) {
                                "No debt wallets selected. Locked funds remain below and never subtract from this figure."
                            } else {
                                "Selected visible wallets minus selected debt wallets. Locked funds stay separate below."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(onClick = onNavigateToTransactions) {
                            Text("Manage wallets and debt logs")
                        }
                    }
                }
            }

            item(key = "spendableSelector") {
                SectionCard {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Spendable wallets",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Only visible standard wallets belong here. Locked funds do not.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (visibleWallets.isEmpty()) {
                                Text(
                                    text = "No visible wallets available.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(visibleWallets, key = { it.id }) { wallet ->
                                        FilterChip(
                                            selected = wallet.id in selectedVisibleWalletIds,
                                            onClick = {
                                                selectedVisibleWalletIds = toggleSelection(
                                                    current = selectedVisibleWalletIds,
                                                    walletId = wallet.id
                                                )
                                            },
                                            label = { Text(wallet.name) }
                                        )
                                    }
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Debt wallets to subtract",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Select only the debt wallets you want counted against spendable money.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (debtWallets.isEmpty()) {
                                Text(
                                    text = "No debt wallets created yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(debtWallets, key = { it.id }) { wallet ->
                                        FilterChip(
                                            selected = wallet.id in selectedDebtWalletIds,
                                            onClick = {
                                                selectedDebtWalletIds = toggleSelection(
                                                    current = selectedDebtWalletIds,
                                                    walletId = wallet.id
                                                )
                                            },
                                            label = { Text(wallet.name) }
                                        )
                                    }
                                }
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                DetailRow("Selected spendable", formatPeso(selectedVisibleTotal))
                                DetailRow("Selected debt", formatPeso(selectedDebtTotal))
                                DetailRow("Spendable after debt", formatPeso(spendableAfterDebt))
                                Text(
                                    text = "Locked funds are tracked below only. They are not part of this subtraction tool.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item(key = "summary") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        label = "Visible cash",
                        amount = visibleTotal,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Debt total",
                        amount = debtTotal,
                        color = ExpenseColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item(key = "locked") {
                StatCard(
                    label = "Locked funds",
                    amount = lockedFundsTotal,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item(key = "debtHeader") {
                SectionHeader(
                    title = "Debt wallets",
                    caption = if (debtWallets.isEmpty()) "No debt wallets created yet." else "Paid lowers remaining debt. Unpaid raises it."
                )
            }

            if (debtWallets.isEmpty()) {
                item(key = "debtEmpty") {
                    EmptyStateCard(
                        title = "No debt wallets yet",
                        subtitle = "Create one from Transactions, switch it to Debt, and it will show here automatically."
                    )
                }
            } else {
                items(debtWallets, key = { it.id }) { wallet ->
                    DebtWalletCard(wallet = wallet, transactions = transactions)
                }
            }

            item(key = "hiddenHeader") {
                SectionHeader(
                    title = "Hidden wallets",
                    caption = if (hiddenStandardWallets.isEmpty()) "No hidden standard wallets yet." else "Useful for locked balances, thresholds, and money you do not want in your automatic total."
                )
            }

            if (hiddenStandardWallets.isEmpty()) {
                item(key = "hiddenEmpty") {
                    EmptyStateCard(
                        title = "No hidden wallets yet",
                        subtitle = "Hide a wallet from Transactions when you want to track it separately from spendable money."
                    )
                }
            } else {
                items(hiddenStandardWallets, key = { it.id }) { wallet ->
                    HiddenWalletCard(wallet = wallet, transactions = transactions)
                }
            }
        }
    }
}

@Composable
private fun DebtWalletCard(
    wallet: Wallet,
    transactions: List<Transaction>,
) {
    val walletTransactions = remember(wallet, transactions) {
        transactions.filter { it.walletId == wallet.id }
    }
    val remaining = remember(wallet, walletTransactions) { wallet.computeBalance(walletTransactions) }
    val paid = remember(walletTransactions) {
        walletTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    }
    val unpaid = remember(walletTransactions) {
        walletTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }

    SectionCard {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = wallet.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    label = "Remaining",
                    amount = remaining,
                    color = ExpenseColor,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Started",
                    amount = wallet.initialBalance,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    label = "Paid",
                    amount = paid,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Unpaid",
                    amount = unpaid,
                    color = ExpenseColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HiddenWalletCard(
    wallet: Wallet,
    transactions: List<Transaction>,
) {
    val balance = remember(wallet, transactions) { wallet.computeBalance(transactions) }

    SectionCard {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = wallet.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = formatPeso(balance),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Tracked separately from automatic totals.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun toggleSelection(
    current: List<String>,
    walletId: String,
): List<String> {
    return if (walletId in current) {
        current.filterNot { it == walletId }
    } else {
        current + walletId
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}