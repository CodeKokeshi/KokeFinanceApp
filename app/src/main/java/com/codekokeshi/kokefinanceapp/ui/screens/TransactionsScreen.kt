package com.codekokeshi.kokefinanceapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.codekokeshi.kokefinanceapp.model.Tag
import com.codekokeshi.kokefinanceapp.model.Transaction
import com.codekokeshi.kokefinanceapp.model.TransactionType
import com.codekokeshi.kokefinanceapp.model.Wallet
import com.codekokeshi.kokefinanceapp.model.WalletKind
import com.codekokeshi.kokefinanceapp.model.computeBalance
import com.codekokeshi.kokefinanceapp.model.isAutoIncludedInTotals
import com.codekokeshi.kokefinanceapp.ui.components.AppScreenBackground
import com.codekokeshi.kokefinanceapp.ui.components.EmptyStateCard
import com.codekokeshi.kokefinanceapp.ui.components.ExpenseColor
import com.codekokeshi.kokefinanceapp.ui.components.IncomeColor
import com.codekokeshi.kokefinanceapp.ui.components.PESO_SIGN
import com.codekokeshi.kokefinanceapp.ui.components.SectionCard
import com.codekokeshi.kokefinanceapp.ui.components.SectionHeader
import com.codekokeshi.kokefinanceapp.ui.components.StatCard
import com.codekokeshi.kokefinanceapp.ui.components.TransactionListItem
import com.codekokeshi.kokefinanceapp.ui.components.WalletBalanceCard
import com.codekokeshi.kokefinanceapp.ui.components.formatPeso
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun TransactionsScreen(
    wallets: List<Wallet>,
    tags: List<Tag>,
    transactions: List<Transaction>,
    onCreateWallet: (Wallet) -> Unit,
    onCreateTag: (name: String, emoji: String, type: TransactionType) -> Tag,
    onEditWallet: (Wallet) -> Unit,
    onDeleteWallet: (Wallet) -> Unit,
    onAddTransaction: (Transaction) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
) {
    var selectedWalletId by rememberSaveable { mutableStateOf(wallets.firstOrNull()?.id ?: "") }
    var selectedTagId by rememberSaveable { mutableStateOf("") }
    var comparisonWalletIds by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var comparisonSourceWalletIds by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var comparisonInitialized by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(wallets) {
        if (wallets.none { it.id == selectedWalletId }) {
            selectedWalletId = wallets.firstOrNull()?.id ?: ""
        }
        val defaultDebtIds = wallets.filter { it.kind == WalletKind.DEBT }.map { it.id }
        if (!comparisonInitialized && wallets.isNotEmpty()) {
            comparisonWalletIds = defaultDebtIds.ifEmpty {
                listOfNotNull(wallets.firstOrNull { it.isHidden }?.id)
            }
            comparisonSourceWalletIds = wallets
                .filter { it.kind == WalletKind.STANDARD && !it.isHidden && it.id !in comparisonWalletIds }
                .map { it.id }
            comparisonInitialized = true
        } else {
            comparisonWalletIds = comparisonWalletIds.filter { id -> wallets.any { it.id == id } }
            comparisonSourceWalletIds = comparisonSourceWalletIds.filter { sourceId ->
                wallets.any { it.id == sourceId && it.kind == WalletKind.STANDARD && !it.isHidden } &&
                    sourceId !in comparisonWalletIds
            }
        }
    }

    var showCreateWalletDialog by rememberSaveable { mutableStateOf(false) }
    var showEditWalletDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteWalletConfirm by rememberSaveable { mutableStateOf(false) }
    var showCreateTagDialog by rememberSaveable { mutableStateOf(false) }
    var showTransactionDetail by rememberSaveable { mutableStateOf(false) }
    var detailTransactionId by rememberSaveable { mutableStateOf("") }

    var transactionType by rememberSaveable { mutableStateOf(TransactionType.EXPENSE) }
    var amount by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var formMessage by rememberSaveable { mutableStateOf("") }

    val selectedWallet = wallets.find { it.id == selectedWalletId }
    val selectedWalletIsDebt = selectedWallet?.kind == WalletKind.DEBT
    val walletBalance = remember(selectedWallet, transactions) {
        selectedWallet?.computeBalance(transactions) ?: 0.0
    }

    val typeTags = remember(tags, transactionType, selectedWalletIsDebt) {
        if (selectedWalletIsDebt) {
            emptyList()
        } else {
            tags.filter { it.type == transactionType }.sortedBy { it.name.lowercase() }
        }
    }

    LaunchedEffect(typeTags, selectedWalletIsDebt) {
        if (selectedWalletIsDebt) {
            selectedTagId = ""
        } else if (typeTags.none { it.id == selectedTagId }) {
            selectedTagId = typeTags.firstOrNull()?.id ?: ""
        }
    }

    val selectedTag = typeTags.find { it.id == selectedTagId }

    val filteredTransactions = remember(transactions, selectedWalletId) {
        transactions
            .filter { selectedWalletId.isEmpty() || it.walletId == selectedWalletId }
            .sortedByDescending { it.timestamp }
    }

    val walletMap = remember(wallets) { wallets.associateBy { it.id } }

    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val today = remember { dateFormat.format(Date()) }
    val yesterday = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        dateFormat.format(cal.time)
    }
    val grouped = remember(filteredTransactions) {
        filteredTransactions.groupBy { transaction ->
            val dateStr = dateFormat.format(Date(transaction.timestamp))
            when (dateStr) {
                today -> "Today"
                yesterday -> "Yesterday"
                else -> dateStr
            }
        }
    }

    val walletTypePositiveLabel = if (selectedWalletIsDebt) "Paid" else "Income"
    val walletTypeNegativeLabel = if (selectedWalletIsDebt) "Unpaid" else "Expense"

    AppScreenBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "hero") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Transactions",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        )
                    }
                    Text(
                        text = "Log every move.",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Manage wallets, hide balances from auto totals, and preview what debt would subtract from spendable money.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item(key = "walletHeader") {
                SectionHeader(
                    title = "Wallets",
                    caption = if (wallets.isEmpty()) "Start with a wallet." else "Select a wallet, then manage its balance and options."
                )
            }

            item(key = "walletChips") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(wallets, key = { it.id }) { wallet ->
                        val isSelected = wallet.id == selectedWalletId
                        val balance = remember(wallet, transactions) { wallet.computeBalance(transactions) }
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedWalletId = wallet.id },
                            label = { Text(walletChipLabel(wallet, balance)) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else null
                        )
                    }
                    item(key = "newWallet") {
                        AssistChip(
                            onClick = { showCreateWalletDialog = true },
                            label = { Text("New wallet") },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                    }
                }
            }

            if (selectedWallet != null) {
                item(key = "walletCard") {
                    WalletBalanceCard(
                        title = selectedWallet.name,
                        balance = walletBalance,
                        subtitle = walletSubtitle(selectedWallet),
                        modifier = Modifier.clickable { showEditWalletDialog = true }
                    )
                }

                item(key = "walletActions") {
                    OutlinedButton(onClick = { showEditWalletDialog = true }) {
                        Text("Manage selected wallet")
                    }
                }

                item(key = "walletStats") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            label = if (selectedWallet.kind == WalletKind.DEBT) "Starting debt" else "Opening",
                            amount = selectedWallet.initialBalance,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = if (selectedWallet.kind == WalletKind.DEBT) "Remaining" else "Current",
                            amount = walletBalance,
                            color = when {
                                selectedWallet.kind == WalletKind.DEBT -> ExpenseColor
                                walletBalance >= selectedWallet.initialBalance -> IncomeColor
                                else -> ExpenseColor
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                item(key = "walletEmpty") {
                    EmptyStateCard(
                        title = "No wallet selected",
                        subtitle = "Create a wallet first, then record transactions against it."
                    )
                }
            }

            item(key = "comparisonHeader") {
                SectionHeader(
                    title = "Wallet comparison",
                    caption = "Select any wallets as your base, then pick any wallets to subtract. Both sides support multi-selection."
                )
            }

            item(key = "comparisonCard") {
                WalletComparisonCard(
                    wallets = wallets,
                    transactions = transactions,
                    comparisonWalletIds = comparisonWalletIds,
                    comparisonSourceWalletIds = comparisonSourceWalletIds,
                    onToggleComparisonWallet = { walletId ->
                        val updated = if (comparisonWalletIds.contains(walletId)) {
                            comparisonWalletIds.filterNot { it == walletId }
                        } else {
                            comparisonWalletIds + walletId
                        }
                        comparisonWalletIds = updated
                        comparisonSourceWalletIds = comparisonSourceWalletIds.filterNot { it in updated }
                    },
                    onToggleComparisonSourceWallet = { walletId ->
                        val updated = if (comparisonSourceWalletIds.contains(walletId)) {
                            comparisonSourceWalletIds.filterNot { it == walletId }
                        } else {
                            comparisonSourceWalletIds + walletId
                        }
                        comparisonSourceWalletIds = updated
                        comparisonWalletIds = comparisonWalletIds.filterNot { it in updated }
                    }
                )
            }

            if (wallets.isNotEmpty()) {
                item(key = "recordHeader") {
                    SectionHeader(
                        title = if (selectedWalletIsDebt) "Update debt wallet" else "Record transaction",
                        caption = if (selectedWalletIsDebt) {
                            "Paid lowers the remaining debt. Unpaid raises it."
                        } else {
                            "Direction, amount, tag, then note. Keep it quick."
                        }
                    )
                }

                item(key = "recordForm") {
                    SectionCard {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = transactionType == TransactionType.INCOME,
                                    onClick = { transactionType = TransactionType.INCOME },
                                    label = { Text(walletTypePositiveLabel) },
                                    leadingIcon = if (transactionType == TransactionType.INCOME) {
                                        { Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                    } else null,
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = transactionType == TransactionType.EXPENSE,
                                    onClick = { transactionType = TransactionType.EXPENSE },
                                    label = { Text(walletTypeNegativeLabel) },
                                    leadingIcon = if (transactionType == TransactionType.EXPENSE) {
                                        { Icon(Icons.Default.TrendingDown, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                    } else null,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            OutlinedTextField(
                                value = amount,
                                onValueChange = {
                                    amount = it
                                    formMessage = ""
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Amount") },
                                prefix = { Text(PESO_SIGN) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                            )

                            if (selectedWalletIsDebt) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Debt state",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = if (transactionType == TransactionType.INCOME) {
                                                "This entry will be saved as Paid and reduce the remaining debt."
                                            } else {
                                                "This entry will be saved as Unpaid and increase the remaining debt."
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                SectionHeader(
                                    title = "Tags",
                                    caption = if (typeTags.isEmpty()) {
                                        "Create your first ${transactionType.label.lowercase()} tag."
                                    } else {
                                        "These are your custom tags for this transaction type."
                                    }
                                )

                                if (typeTags.isEmpty()) {
                                    EmptyStateCard(
                                        title = "No ${transactionType.label.lowercase()} tags yet",
                                        subtitle = "Make your own, choose your own emoji, and stop depending on preset labels."
                                    )
                                } else {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(typeTags, key = { it.id }) { tag ->
                                            FilterChip(
                                                selected = tag.id == selectedTagId,
                                                onClick = { selectedTagId = tag.id },
                                                label = { Text("${tag.emoji} ${tag.name}") }
                                            )
                                        }
                                    }
                                }

                                AssistChip(
                                    onClick = { showCreateTagDialog = true },
                                    label = { Text("Create custom tag") },
                                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                )
                            }

                            OutlinedTextField(
                                value = note,
                                onValueChange = { note = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Note") },
                                supportingText = {
                                    Text(
                                        if (selectedWalletIsDebt) {
                                            "Optional note for this payment or unpaid amount."
                                        } else {
                                            "Optional context for this entry."
                                        }
                                    )
                                },
                                singleLine = true,
                            )

                            Text(
                                text = buildString {
                                    append("Wallet: ")
                                    append(selectedWallet?.name ?: "none")
                                    if (selectedWalletIsDebt) {
                                        append("  •  State: ")
                                        append(if (transactionType == TransactionType.INCOME) "Paid" else "Unpaid")
                                    } else if (selectedTag != null) {
                                        append("  •  Tag: ${selectedTag.emoji} ${selectedTag.name}")
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Button(
                                onClick = {
                                    val parsedAmount = amount.toDoubleOrNull()
                                    when {
                                        selectedWallet == null -> formMessage = "Select a wallet first."
                                        parsedAmount == null || parsedAmount <= 0 -> formMessage = "Enter a valid amount greater than zero."
                                        !selectedWalletIsDebt && selectedTag == null -> formMessage = "Create or select a tag first."
                                        else -> {
                                            val descriptor = if (selectedWalletIsDebt) {
                                                debtDescriptor(transactionType)
                                            } else {
                                                selectedTag!!.emoji to selectedTag.name
                                            }
                                            onAddTransaction(
                                                Transaction(
                                                    walletId = selectedWallet.id,
                                                    type = transactionType,
                                                    amount = parsedAmount,
                                                    tagId = if (selectedWalletIsDebt) null else selectedTag?.id,
                                                    tagLabel = descriptor.second,
                                                    tagEmoji = descriptor.first,
                                                    note = note.trim()
                                                )
                                            )
                                            amount = ""
                                            note = ""
                                            formMessage = "${descriptor.second} recorded successfully."
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (selectedWalletIsDebt) {
                                        "Record ${if (transactionType == TransactionType.INCOME) "Paid" else "Unpaid"}"
                                    } else {
                                        "Record ${transactionType.label}"
                                    }
                                )
                            }

                            if (formMessage.isNotEmpty()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = formMessage,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item(key = "historyHeader") {
                SectionHeader(
                    title = "History",
                    caption = if (filteredTransactions.isEmpty()) "No entries yet." else "Tap an item to inspect or delete it."
                )
            }

            if (filteredTransactions.isEmpty()) {
                item(key = "historyEmpty") {
                    EmptyStateCard(
                        title = "History is empty",
                        subtitle = "Once you start recording transactions, the timeline builds here."
                    )
                }
            }

            grouped.forEach { (dateLabel, txns) ->
                item(key = "date_$dateLabel") {
                    SectionHeader(
                        title = dateLabel,
                        caption = "${txns.size} transaction${if (txns.size == 1) "" else "s"}"
                    )
                }
                items(txns, key = { it.id }) { transaction ->
                    TransactionListItem(
                        transaction = transaction,
                        walletName = walletMap[transaction.walletId]?.name ?: "Unknown",
                        modifier = Modifier.clickable {
                            detailTransactionId = transaction.id
                            showTransactionDetail = true
                        }
                    )
                }
            }
        }
    }

    if (showCreateWalletDialog) {
        CreateWalletDialog(
            onDismiss = { showCreateWalletDialog = false },
            onConfirm = { wallet ->
                onCreateWallet(wallet)
                selectedWalletId = wallet.id
                showCreateWalletDialog = false
            }
        )
    }

    if (showEditWalletDialog && selectedWallet != null) {
        EditWalletDialog(
            wallet = selectedWallet,
            onDismiss = { showEditWalletDialog = false },
            onSave = { updated ->
                onEditWallet(updated)
                showEditWalletDialog = false
            },
            onDelete = {
                showEditWalletDialog = false
                showDeleteWalletConfirm = true
            }
        )
    }

    if (showDeleteWalletConfirm && selectedWallet != null) {
        AlertDialog(
            onDismissRequest = { showDeleteWalletConfirm = false },
            title = { Text("Delete wallet?") },
            text = { Text("This will delete \"${selectedWallet.name}\" and all its transactions. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteWallet(selectedWallet)
                    showDeleteWalletConfirm = false
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteWalletConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showCreateTagDialog) {
        CreateTagDialog(
            type = transactionType,
            onDismiss = { showCreateTagDialog = false },
            onConfirm = { name, emoji ->
                val created = onCreateTag(name, emoji, transactionType)
                selectedTagId = created.id
                showCreateTagDialog = false
            }
        )
    }

    val detailTransaction = transactions.find { it.id == detailTransactionId }
    if (showTransactionDetail && detailTransaction != null) {
        TransactionDetailDialog(
            transaction = detailTransaction,
            walletName = walletMap[detailTransaction.walletId]?.name ?: "Unknown",
            onDismiss = { showTransactionDetail = false },
            onDelete = {
                onDeleteTransaction(detailTransaction)
                showTransactionDetail = false
            }
        )
    }
}

@Composable
private fun WalletComparisonCard(
    wallets: List<Wallet>,
    transactions: List<Transaction>,
    comparisonWalletIds: List<String>,
    comparisonSourceWalletIds: List<String>,
    onToggleComparisonWallet: (String) -> Unit,
    onToggleComparisonSourceWallet: (String) -> Unit,
) {
    val selectedSources = remember(wallets, comparisonSourceWalletIds) {
        wallets.filter { it.id in comparisonSourceWalletIds }
    }
    val selectedSubtracts = remember(wallets, comparisonWalletIds) {
        wallets.filter { it.id in comparisonWalletIds }
    }
    val sourceTotal = remember(selectedSources, transactions) {
        selectedSources.sumOf { it.computeBalance(transactions) }
    }
    val subtractTotal = remember(selectedSubtracts, transactions) {
        selectedSubtracts.sumOf { abs(it.computeBalance(transactions)) }
    }
    val result = sourceTotal - subtractTotal

    SectionCard {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Base wallets",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Wallets whose real money you're counting. Multi-select.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (wallets.isEmpty()) {
                    EmptyStateCard(
                        title = "No wallets yet",
                        subtitle = "Create a wallet first."
                    )
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(
                            wallets.filter { it.id !in comparisonWalletIds },
                            key = { it.id }
                        ) { wallet ->
                            FilterChip(
                                selected = comparisonSourceWalletIds.contains(wallet.id),
                                onClick = { onToggleComparisonSourceWallet(wallet.id) },
                                label = { Text(wallet.name) }
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Wallets to subtract",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Debts, locked funds, or any wallet to deduct. Multi-select.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (wallets.isEmpty()) {
                    Text(
                        text = "No wallets available yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(
                            wallets.filter { it.id !in comparisonSourceWalletIds },
                            key = { it.id }
                        ) { wallet ->
                            FilterChip(
                                selected = wallet.id in comparisonWalletIds,
                                onClick = { onToggleComparisonWallet(wallet.id) },
                                label = { Text(walletComparisonLabel(wallet)) }
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
                    DetailRow("Base total", formatPeso(sourceTotal))
                    DetailRow(
                        "Subtracting",
                        if (comparisonWalletIds.isEmpty()) "None selected" else "${formatPeso(subtractTotal)} across ${comparisonWalletIds.size} wallet${if (comparisonWalletIds.size == 1) "" else "s"}"
                    )
                    DetailRow("Preview", formatPeso(result))
                    Text(
                        text = when {
                            comparisonWalletIds.isEmpty() -> "Select wallets on the subtract side to see a preview."
                            comparisonSourceWalletIds.isEmpty() -> "Select base wallets to calculate what would be left."
                            else -> "After subtracting ${comparisonWalletIds.size} wallet${if (comparisonWalletIds.size == 1) "" else "s"} from ${comparisonSourceWalletIds.size} base wallet${if (comparisonSourceWalletIds.size == 1) "" else "s"}."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun debtDescriptor(type: TransactionType): Pair<String, String> {
    return if (type == TransactionType.INCOME) {
        "✅" to "Paid"
    } else {
        "⏳" to "Unpaid"
    }
}

private fun walletChipLabel(wallet: Wallet, balance: Double): String {
    val status = when {
        wallet.kind == WalletKind.DEBT -> "Debt"
        wallet.isHidden -> "Hidden"
        else -> null
    }
    return buildString {
        append(wallet.name)
        if (status != null) {
            append(" • ")
            append(status)
        }
        append("  ")
        append(formatPeso(balance))
    }
}

private fun walletComparisonLabel(wallet: Wallet): String {
    return when {
        wallet.kind == WalletKind.DEBT -> "${wallet.name} • Debt"
        wallet.isHidden -> "${wallet.name} • Hidden"
        else -> wallet.name
    }
}

private fun walletSubtitle(wallet: Wallet): String {
    return when {
        wallet.kind == WalletKind.DEBT -> "Debt wallet. Paid entries reduce it, Unpaid entries increase it. Excluded from automatic totals. Tap to rename, edit, or delete it."
        wallet.isAutoIncludedInTotals() -> "Included in automatic totals. Tap to rename, edit, hide, or delete it."
        else -> "Hidden from automatic totals. Tap to rename, edit, unhide, or delete it."
    }
}

@Composable
private fun CreateWalletDialog(
    onDismiss: () -> Unit,
    onConfirm: (Wallet) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var balance by rememberSaveable { mutableStateOf("0") }
    var kind by rememberSaveable { mutableStateOf(WalletKind.STANDARD) }
    var isHidden by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create wallet") },
        text = {
            WalletEditorFields(
                name = name,
                onNameChange = { name = it },
                balance = balance,
                onBalanceChange = { balance = it },
                kind = kind,
                onKindChange = { kind = it },
                isHidden = isHidden,
                onHiddenChange = { isHidden = it },
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        Wallet(
                            name = name.trim(),
                            initialBalance = balance.toDoubleOrNull() ?: 0.0,
                            isHidden = isHidden || kind == WalletKind.DEBT,
                            kind = kind,
                        )
                    )
                },
                enabled = name.isNotBlank() && balance.toDoubleOrNull() != null
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun EditWalletDialog(
    wallet: Wallet,
    onDismiss: () -> Unit,
    onSave: (Wallet) -> Unit,
    onDelete: () -> Unit,
) {
    var name by rememberSaveable(wallet.id) { mutableStateOf(wallet.name) }
    var balance by rememberSaveable(wallet.id) { mutableStateOf(wallet.initialBalance.toString()) }
    var kind by rememberSaveable(wallet.id) { mutableStateOf(wallet.kind) }
    var isHidden by rememberSaveable(wallet.id) { mutableStateOf(wallet.isHidden) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage wallet") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                WalletEditorFields(
                    name = name,
                    onNameChange = { name = it },
                    balance = balance,
                    onBalanceChange = { balance = it },
                    kind = kind,
                    onKindChange = { kind = it },
                    isHidden = isHidden,
                    onHiddenChange = { isHidden = it },
                )
                TextButton(onClick = onDelete) {
                    Text("Delete this wallet", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        wallet.copy(
                            name = name.trim(),
                            initialBalance = balance.toDoubleOrNull() ?: wallet.initialBalance,
                            isHidden = isHidden || kind == WalletKind.DEBT,
                            kind = kind,
                        )
                    )
                },
                enabled = name.isNotBlank() && balance.toDoubleOrNull() != null
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun WalletEditorFields(
    name: String,
    onNameChange: (String) -> Unit,
    balance: String,
    onBalanceChange: (String) -> Unit,
    kind: WalletKind,
    onKindChange: (WalletKind) -> Unit,
    isHidden: Boolean,
    onHiddenChange: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Wallet name") },
            singleLine = true,
        )
        OutlinedTextField(
            value = balance,
            onValueChange = onBalanceChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(if (kind == WalletKind.DEBT) "Starting debt" else "Initial balance") },
            prefix = { Text(PESO_SIGN) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Wallet type",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = kind == WalletKind.STANDARD,
                    onClick = { onKindChange(WalletKind.STANDARD) },
                    label = { Text("Standard") }
                )
                FilterChip(
                    selected = kind == WalletKind.DEBT,
                    onClick = { onKindChange(WalletKind.DEBT) },
                    label = { Text("Debt") }
                )
            }
        }
        FilterChip(
            selected = isHidden || kind == WalletKind.DEBT,
            onClick = { onHiddenChange(!isHidden) },
            enabled = kind != WalletKind.DEBT,
            label = {
                Text(
                    if (kind == WalletKind.DEBT) {
                        "Hidden from automatic totals"
                    } else {
                        "Hide from automatic totals"
                    }
                )
            }
        )
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (kind == WalletKind.DEBT) {
                    "Debt wallets are automatically kept out of the Home total and use Paid / Unpaid states instead of custom tags."
                } else if (isHidden) {
                    "Hidden wallets keep their own balance and transaction history, but they stay out of automatic total balance cards."
                } else {
                    "Visible wallets are included in the automatic balance total on Home."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun CreateTagDialog(
    type: TransactionType,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    val emojiSuggestions = if (type == TransactionType.INCOME) {
        listOf("💼", "💸", "📈", "🎁", "🪙", "🧾", "🌟", "💡")
    } else {
        listOf("🍔", "🚌", "🛒", "🏠", "🎬", "💊", "📚", "☕")
    }

    var name by rememberSaveable(type) { mutableStateOf("") }
    var emoji by rememberSaveable(type) { mutableStateOf(emojiSuggestions.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create ${type.label.lowercase()} tag") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Tag name") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = emoji,
                    onValueChange = { emoji = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Emoji") },
                    supportingText = { Text("Use one emoji or a short symbol.") },
                    singleLine = true,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(emojiSuggestions) { suggestion ->
                        FilterChip(
                            selected = suggestion == emoji,
                            onClick = { emoji = suggestion },
                            label = { Text(suggestion) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim(), emoji.trim()) },
                enabled = name.isNotBlank() && emoji.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun TransactionDetailDialog(
    transaction: Transaction,
    walletName: String,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
) {
    val fullDateFormat = remember { SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${transaction.tagEmoji} ${transaction.tagLabel}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRow("Type", transaction.type.label)
                DetailRow("Amount", formatPeso(transaction.amount))
                DetailRow("Wallet", walletName)
                if (transaction.note.isNotBlank()) {
                    DetailRow("Note", transaction.note)
                }
                DetailRow("Date", fullDateFormat.format(Date(transaction.timestamp)))
            }
        },
        confirmButton = {
            TextButton(onClick = onDelete) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}