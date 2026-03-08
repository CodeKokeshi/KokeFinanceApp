package com.codekokeshi.kokefinanceapp.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codekokeshi.kokefinanceapp.model.Tag
import com.codekokeshi.kokefinanceapp.model.Transaction
import com.codekokeshi.kokefinanceapp.model.TransactionType
import com.codekokeshi.kokefinanceapp.model.Wallet
import com.codekokeshi.kokefinanceapp.ui.components.AppScreenBackground
import com.codekokeshi.kokefinanceapp.ui.components.EmptyStateCard
import com.codekokeshi.kokefinanceapp.ui.components.ExpenseColor
import com.codekokeshi.kokefinanceapp.ui.components.IncomeColor
import com.codekokeshi.kokefinanceapp.ui.components.SectionCard
import com.codekokeshi.kokefinanceapp.ui.components.SectionHeader
import com.codekokeshi.kokefinanceapp.ui.components.StatCard
import com.codekokeshi.kokefinanceapp.ui.components.WalletBalanceCard
import com.codekokeshi.kokefinanceapp.ui.components.formatPeso
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class ReportPeriod(val label: String) {
    THIS_MONTH("This Month"),
    ALL_TIME("All Time")
}

private data class TagBreakdown(
    val label: String,
    val emoji: String,
    val amount: Double,
)

@Composable
fun ReportsScreen(
    tags: List<Tag>,
    wallets: List<Wallet>,
    transactions: List<Transaction>,
) {
    val context = LocalContext.current
    var period by rememberSaveable { mutableStateOf(ReportPeriod.THIS_MONTH) }

    val monthStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val filtered = remember(transactions, period, monthStart) {
        when (period) {
            ReportPeriod.THIS_MONTH -> transactions.filter { it.timestamp >= monthStart }
            ReportPeriod.ALL_TIME -> transactions
        }
    }

    val totalIncome = remember(filtered) {
        filtered.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    }
    val totalExpense = remember(filtered) {
        filtered.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }
    val net = totalIncome - totalExpense

    val expenseByTag = remember(filtered) {
        filtered.filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.tagLabel to it.tagEmoji }
            .map { (key, txns) ->
                TagBreakdown(
                    label = key.first,
                    emoji = key.second,
                    amount = txns.sumOf { it.amount }
                )
            }
            .sortedByDescending { it.amount }
    }

    val incomeByTag = remember(filtered) {
        filtered.filter { it.type == TransactionType.INCOME }
            .groupBy { it.tagLabel to it.tagEmoji }
            .map { (key, txns) ->
                TagBreakdown(
                    label = key.first,
                    emoji = key.second,
                    amount = txns.sumOf { it.amount }
                )
            }
            .sortedByDescending { it.amount }
    }

    val walletMap = remember(wallets) { wallets.associateBy { it.id } }
    val tagCount = remember(tags) { tags.size.toDouble() }
    val transactionCount = remember(filtered) { filtered.size.toDouble() }
    val tagCountText = remember(tags) { tags.size.toString() }
    val transactionCountText = remember(filtered) { filtered.size.toString() }

    var pendingCsv by remember { mutableStateOf("") }

    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null && pendingCsv.isNotEmpty()) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(pendingCsv.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "CSV exported successfully", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
            }
            pendingCsv = ""
        }
    }

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
                            text = "Reports",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        )
                    }
                    Text(
                        text = "Read the ledger.",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Use tag-level breakdowns to see where money comes from and where it goes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item(key = "period") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportPeriod.entries.forEach { reportPeriod ->
                        FilterChip(
                            selected = reportPeriod == period,
                            onClick = { period = reportPeriod },
                            label = { Text(reportPeriod.label) }
                        )
                    }
                }
            }

            item(key = "net") {
                WalletBalanceCard(
                    title = if (period == ReportPeriod.THIS_MONTH) "Net this month" else "Net all time",
                    balance = net,
                    subtitle = if (net >= 0) "More money is coming in than going out." else "Spending is currently ahead of income."
                )
            }

            item(key = "summary") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        label = "Income",
                        amount = totalIncome,
                        color = IncomeColor,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Expense",
                        amount = totalExpense,
                        color = ExpenseColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item(key = "meta") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        label = "Tags",
                        amount = tagCount,
                        color = MaterialTheme.colorScheme.primary,
                        valueText = tagCountText,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Entries",
                        amount = transactionCount,
                        color = MaterialTheme.colorScheme.secondary,
                        valueText = transactionCountText,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (expenseByTag.isNotEmpty()) {
                item(key = "expenseHeader") {
                    SectionHeader(
                        title = "Expense tags",
                        caption = "Your biggest spending tags in the selected period."
                    )
                }
                items(expenseByTag, key = { "expense_${it.label}_${it.emoji}" }) { tag ->
                    TagBreakdownItem(
                        tag = tag,
                        total = totalExpense,
                        color = ExpenseColor,
                    )
                }
            }

            if (incomeByTag.isNotEmpty()) {
                item(key = "incomeHeader") {
                    SectionHeader(
                        title = "Income tags",
                        caption = "The tags doing most of the earning work."
                    )
                }
                items(incomeByTag, key = { "income_${it.label}_${it.emoji}" }) { tag ->
                    TagBreakdownItem(
                        tag = tag,
                        total = totalIncome,
                        color = IncomeColor,
                    )
                }
            }

            if (filtered.isEmpty()) {
                item(key = "empty") {
                    EmptyStateCard(
                        title = "No reportable data yet",
                        subtitle = "Once you log transactions with custom tags, this screen will start summarizing them."
                    )
                }
            }

            item(key = "export") {
                SectionCard {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Export ledger",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Save the full transaction history as CSV, including wallet and custom tag info.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = {
                                pendingCsv = generateCsv(transactions, walletMap)
                                csvLauncher.launch("koke_finance_export.csv")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = transactions.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Export as CSV")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TagBreakdownItem(
    tag: TagBreakdown,
    total: Double,
    color: Color,
) {
    val fraction = if (total > 0) (tag.amount / total).toFloat().coerceIn(0f, 1f) else 0f

    SectionCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${tag.emoji} ${tag.label}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${formatPeso(tag.amount)}  (${(fraction * 100).toInt()}%)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = color,
                trackColor = color.copy(alpha = 0.12f),
            )
        }
    }
}

private fun generateCsv(
    transactions: List<Transaction>,
    walletMap: Map<String, Wallet>,
): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val sb = StringBuilder()
    sb.appendLine("Date,Type,Tag,Amount,Note,Wallet")
    for (transaction in transactions.sortedByDescending { it.timestamp }) {
        val date = dateFormat.format(Date(transaction.timestamp))
        val wallet = walletMap[transaction.walletId]?.name ?: "Unknown"
        fun esc(value: String) = "\"${value.replace("\"", "\"\"")}\""
        sb.appendLine(
            "${esc(date)},${esc(transaction.type.name)},${esc("${transaction.tagEmoji} ${transaction.tagLabel}")},${transaction.amount},${esc(transaction.note)},${esc(wallet)}"
        )
    }
    return sb.toString()
}
