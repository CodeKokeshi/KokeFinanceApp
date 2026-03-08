package com.codekokeshi.kokefinanceapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.codekokeshi.kokefinanceapp.data.FinanceRepository
import com.codekokeshi.kokefinanceapp.model.Tag
import com.codekokeshi.kokefinanceapp.model.Transaction
import com.codekokeshi.kokefinanceapp.model.TransactionType
import com.codekokeshi.kokefinanceapp.model.Wallet
import com.codekokeshi.kokefinanceapp.ui.screens.HomeScreen
import com.codekokeshi.kokefinanceapp.ui.screens.ReportsScreen
import com.codekokeshi.kokefinanceapp.ui.screens.TransactionsScreen
import com.codekokeshi.kokefinanceapp.ui.theme.KokeFinanceAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KokeFinanceAppTheme {
                KokeFinanceApp()
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Home", Icons.Default.Home),
    TRANSACTIONS("Transactions", Icons.Default.ReceiptLong),
    REPORTS("Reports", Icons.Default.BarChart),
}

@Composable
fun KokeFinanceApp() {
    val context = LocalContext.current
    val repo = remember { FinanceRepository(context) }

    var currentDestination by remember { mutableStateOf(AppDestinations.HOME) }
    var tags by remember { mutableStateOf(repo.loadTags()) }
    var wallets by remember { mutableStateOf(repo.loadWallets()) }
    var transactions by remember { mutableStateOf(repo.loadTransactions()) }

    fun persist() {
        repo.saveTags(tags)
        repo.saveWallets(wallets)
        repo.saveTransactions(transactions)
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach { dest ->
                item(
                    icon = { Icon(dest.icon, contentDescription = dest.label) },
                    label = { Text(dest.label) },
                    selected = dest == currentDestination,
                    onClick = { currentDestination = dest }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentDestination) {
                    AppDestinations.HOME -> HomeScreen(
                        wallets = wallets,
                        transactions = transactions,
                        onNavigateToTransactions = {
                            currentDestination = AppDestinations.TRANSACTIONS
                        }
                    )

                    AppDestinations.TRANSACTIONS -> TransactionsScreen(
                        wallets = wallets,
                        tags = tags,
                        transactions = transactions,
                        onCreateWallet = { name, initialBalance ->
                            wallets = wallets + Wallet(name = name, initialBalance = initialBalance)
                            persist()
                        },
                        onCreateTag = { name, emoji, type ->
                            val tag = Tag(name = name, emoji = emoji, type = type)
                            tags = tags + tag
                            persist()
                            tag
                        },
                        onEditWallet = { updated ->
                            wallets = wallets.map { if (it.id == updated.id) updated else it }
                            persist()
                        },
                        onDeleteWallet = { wallet ->
                            wallets = wallets.filter { it.id != wallet.id }
                            transactions = transactions.filter { it.walletId != wallet.id }
                            persist()
                        },
                        onAddTransaction = { transaction ->
                            transactions = transactions + transaction
                            persist()
                        },
                        onDeleteTransaction = { transaction ->
                            transactions = transactions.filter { it.id != transaction.id }
                            persist()
                        }
                    )

                    AppDestinations.REPORTS -> ReportsScreen(
                        tags = tags,
                        wallets = wallets,
                        transactions = transactions,
                    )
                }
            }
        }
    }
}