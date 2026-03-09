package com.codekokeshi.kokefinanceapp.ui.screens

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
import com.codekokeshi.kokefinanceapp.model.WalletKind
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
    val visibleWallets = remember(wallets) { wallets.filter { it.isAutoIncludedInTotals() } }
    val debtWallets = remember(wallets) { wallets.filter { it.kind == WalletKind.DEBT } }
    val hiddenStandardWallets = remember(wallets) {
        wallets.filter { it.kind == WalletKind.STANDARD && it.isHidden }
    }

    val visibleTotal = remember(visibleWallets, transactions) {
        visibleWallets.sumOf { it.computeBalance(transactions) }
    }
    val debtTotal = remember(debtWallets, transactions) {
        debtWallets.sumOf { it.computeBalance(transactions) }
    }
    val lockedFundsTotal = remember(hiddenStandardWallets, transactions) {
        hiddenStandardWallets.sumOf { it.computeBalance(transactions) }
    }
    val spendableAfterDebt = visibleTotal - debtTotal

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
                        text = "This screen keeps debt wallets and locked funds visible without mixing them into your automatic balance total.",
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
                            text = if (debtWallets.isEmpty()) {
                                "No debt wallets yet. Hidden wallets can still stay off your main total."
                            } else {
                                "Visible wallets minus remaining debt. Locked funds stay separate below."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(onClick = onNavigateToTransactions) {
                            Text("Manage wallets and debt entries")
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