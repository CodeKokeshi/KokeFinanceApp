package com.codekokeshi.kokefinanceapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.codekokeshi.kokefinanceapp.model.Tag
import com.codekokeshi.kokefinanceapp.model.Transaction
import com.codekokeshi.kokefinanceapp.model.TransactionType
import com.codekokeshi.kokefinanceapp.model.Wallet
import com.codekokeshi.kokefinanceapp.model.computeBalance
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

@Composable
fun TransactionsScreen(
    wallets: List<Wallet>,
    tags: List<Tag>,
    transactions: List<Transaction>,
    onCreateWallet: (name: String, initialBalance: Double) -> Unit,
    onCreateTag: (name: String, emoji: String, type: TransactionType) -> Tag,
    onEditWallet: (Wallet) -> Unit,
    onDeleteWallet: (Wallet) -> Unit,
    onAddTransaction: (Transaction) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
) {
    var selectedWalletId by rememberSaveable { mutableStateOf(wallets.firstOrNull()?.id ?: "") }
    var selectedTagId by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(wallets) {
        if (wallets.none { it.id == selectedWalletId }) {
            selectedWalletId = wallets.firstOrNull()?.id ?: ""
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
    val walletBalance = remember(selectedWallet, transactions) {
        selectedWallet?.computeBalance(transactions) ?: 0.0
    }

    val typeTags = remember(tags, transactionType) {
        tags.filter { it.type == transactionType }.sortedBy { it.name.lowercase() }
    }

    LaunchedEffect(typeTags) {
        if (typeTags.none { it.id == selectedTagId }) {
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
                        text = "Manage wallets, build your own emoji tags, and keep the ledger tidy.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item(key = "walletHeader") {
                SectionHeader(
                    title = "Wallets",
                    caption = if (wallets.isEmpty()) "Start with a wallet." else "Choose the wallet you want to work in."
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
                            label = { Text("${wallet.name}  ${formatPeso(balance)}") },
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
                        subtitle = "Tap to edit the wallet name, opening balance, or delete it.",
                        modifier = Modifier.clickable { showEditWalletDialog = true }
                    )
                }

                item(key = "walletStats") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            label = "Opening",
                            amount = selectedWallet.initialBalance,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Current",
                            amount = walletBalance,
                            color = if (walletBalance >= selectedWallet.initialBalance) IncomeColor else ExpenseColor,
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

            if (wallets.isNotEmpty()) {
                item(key = "recordHeader") {
                    SectionHeader(
                        title = "Record transaction",
                        caption = "Direction, amount, tag, then note. Keep it quick."
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
                                    label = { Text("Income") },
                                    leadingIcon = if (transactionType == TransactionType.INCOME) {
                                        { Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                    } else null,
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = transactionType == TransactionType.EXPENSE,
                                    onClick = { transactionType = TransactionType.EXPENSE },
                                    label = { Text("Expense") },
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

                            OutlinedTextField(
                                value = note,
                                onValueChange = { note = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Note") },
                                supportingText = { Text("Optional context for this entry.") },
                                singleLine = true,
                            )

                            Text(
                                text = buildString {
                                    append("Wallet: ")
                                    append(selectedWallet?.name ?: "none")
                                    if (selectedTag != null) {
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
                                        selectedTag == null -> formMessage = "Create or select a tag first."
                                        else -> {
                                            onAddTransaction(
                                                Transaction(
                                                    walletId = selectedWallet.id,
                                                    type = transactionType,
                                                    amount = parsedAmount,
                                                    tagId = selectedTag.id,
                                                    tagLabel = selectedTag.name,
                                                    tagEmoji = selectedTag.emoji,
                                                    note = note.trim()
                                                )
                                            )
                                            amount = ""
                                            note = ""
                                            formMessage = "${transactionType.label} recorded successfully."
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Record ${transactionType.label}")
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
            onConfirm = { name, balance ->
                onCreateWallet(name, balance)
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
private fun CreateWalletDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var balance by rememberSaveable { mutableStateOf("0") }
    var step by rememberSaveable { mutableIntStateOf(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (step == 1) "Name your wallet" else "Set initial balance") },
        text = {
            if (step == 1) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Wallet name") },
                    singleLine = true,
                )
            } else {
                OutlinedTextField(
                    value = balance,
                    onValueChange = { balance = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Initial balance") },
                    prefix = { Text(PESO_SIGN) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (step == 1) {
                        step = 2
                    } else {
                        onConfirm(name.trim(), balance.toDoubleOrNull() ?: 0.0)
                    }
                },
                enabled = if (step == 1) name.isNotBlank() else balance.toDoubleOrNull() != null
            ) {
                Text(if (step == 1) "Next" else "Create")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                if (step == 2) step = 1 else onDismiss()
            }) {
                Text(if (step == 2) "Back" else "Cancel")
            }
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit wallet") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Wallet name") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = balance,
                    onValueChange = { balance = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Initial balance") },
                    prefix = { Text(PESO_SIGN) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
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
                            initialBalance = balance.toDoubleOrNull() ?: wallet.initialBalance
                        )
                    )
                },
                enabled = name.isNotBlank()
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
