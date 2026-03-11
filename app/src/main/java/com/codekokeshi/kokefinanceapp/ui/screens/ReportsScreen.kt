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
import androidx.compose.material.icons.filled.Description
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
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.compose.material3.OutlinedButton
import com.codekokeshi.kokefinanceapp.model.Tag
import com.codekokeshi.kokefinanceapp.model.Transaction
import com.codekokeshi.kokefinanceapp.model.TransactionType
import com.codekokeshi.kokefinanceapp.model.Wallet
import com.codekokeshi.kokefinanceapp.model.WalletKind
import com.codekokeshi.kokefinanceapp.model.computeBalance
import com.codekokeshi.kokefinanceapp.model.isAutoIncludedInTotals
import com.codekokeshi.kokefinanceapp.ui.components.AppScreenBackground
import java.io.ByteArrayOutputStream
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
    var pendingPdfData by remember { mutableStateOf<ByteArray?>(null) }

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

    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val data = pendingPdfData
        if (uri != null && data != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(data)
                }
                Toast.makeText(context, "PDF exported successfully", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(context, "PDF export failed", Toast.LENGTH_SHORT).show()
            }
            pendingPdfData = null
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
                            text = "Export as CSV for spreadsheets or as a formatted PDF report with wallet, tag, and transaction breakdowns.",
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
                        OutlinedButton(
                            onClick = {
                                pendingPdfData = generatePdf(
                                    period = period,
                                    transactions = filtered,
                                    wallets = wallets,
                                    walletMap = walletMap,
                                    totalIncome = totalIncome,
                                    totalExpense = totalExpense,
                                    net = net,
                                    expenseByTag = expenseByTag,
                                    incomeByTag = incomeByTag,
                                )
                                pdfLauncher.launch("koke_finance_report.pdf")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = transactions.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Export as PDF")
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

private fun String.stripEmoji(): String = replace(Regex("[^\\x20-\\x7E]"), "").trim()

private fun generateCsv(
    transactions: List<Transaction>,
    walletMap: Map<String, Wallet>,
): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val sb = StringBuilder()
    sb.appendLine("Date,Type,Tag,Amount,Note,Wallet")
    for (transaction in transactions.sortedByDescending { it.timestamp }) {
        val date = dateFormat.format(Date(transaction.timestamp))
        val wallet = (walletMap[transaction.walletId]?.name ?: "Unknown").stripEmoji()
        fun esc(v: String): String = '"' + v.replace("\"", "\"\"") + '"'
        val safeTag = transaction.tagLabel.stripEmoji()
        val safeNote = transaction.note.stripEmoji()
        sb.appendLine(
            "${esc(date)},${esc(transaction.type.name)},${esc(safeTag)},${transaction.amount},${esc(safeNote)},${esc(wallet)}"
        )
    }
    return sb.toString()
}

@Suppress("LongMethod")
private fun generatePdf(
    period: ReportPeriod,
    transactions: List<Transaction>,
    wallets: List<Wallet>,
    walletMap: Map<String, Wallet>,
    totalIncome: Double,
    totalExpense: Double,
    net: Double,
    expenseByTag: List<TagBreakdown>,
    incomeByTag: List<TagBreakdown>,
): ByteArray {
    val doc = PdfDocument()
    val pageW = 595
    val pageH = 842
    val ml = 48f
    val cw = pageW - ml * 2f

    val cPrimary = android.graphics.Color.parseColor("#1E1E2E")
    val cAccent  = android.graphics.Color.parseColor("#4A90D9")
    val cIncome  = android.graphics.Color.parseColor("#27AE60")
    val cExpense = android.graphics.Color.parseColor("#E74C3C")
    val cText    = android.graphics.Color.parseColor("#1A1A1A")
    val cSub     = android.graphics.Color.parseColor("#777777")
    val cLine    = android.graphics.Color.parseColor("#DDDDDD")
    val cRowAlt  = android.graphics.Color.parseColor("#F8F8F8")
    val cSecBg   = android.graphics.Color.parseColor("#F2F2F2")

    fun mkPaint(
        color: Int,
        size: Float,
        bold: Boolean = false,
        align: Paint.Align = Paint.Align.LEFT,
    ) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = size
        typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
        textAlign = align
    }

    val pLineDivider = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cLine; strokeWidth = 0.5f }
    val pHeaderBg   = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cPrimary }
    val pAccentLine = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cAccent; strokeWidth = 2f }
    val pLeftBar    = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cAccent; strokeWidth = 2.5f }
    val pSecBg      = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cSecBg }
    val pRowAlt     = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cRowAlt }
    val pTitle      = mkPaint(android.graphics.Color.WHITE, 16f, bold = true)
    val pSubTitle   = mkPaint(android.graphics.Color.WHITE, 8f)
    val pSecLabel   = mkPaint(cText, 8.5f, bold = true)
    val pBody       = mkPaint(cText, 8f)
    val pBoldBody   = mkPaint(cText, 8f, bold = true)
    val pDim        = mkPaint(cSub, 7.5f)
    val pAmtR       = mkPaint(cText, 8f, bold = true, align = Paint.Align.RIGHT)
    val pAmtInR     = mkPaint(cIncome, 8f, bold = true, align = Paint.Align.RIGHT)
    val pAmtExR     = mkPaint(cExpense, 8f, bold = true, align = Paint.Align.RIGHT)

    val dfHeader = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
    val dfRow    = SimpleDateFormat("MM/dd/yy HH:mm", Locale.getDefault())

    var page: PdfDocument.Page? = null
    var cv: Canvas? = null
    var y = 0f
    var pageNum = 0

    fun drawShell() {
        val c = cv!!
        c.drawRect(0f, 0f, pageW.toFloat(), 68f, pHeaderBg)
        c.drawText("Koke Finance Report", ml, 30f, pTitle)
        c.drawText(
            "Period: ${period.label}   |   Generated: ${dfHeader.format(Date())}",
            ml, 50f, pSubTitle,
        )
        c.drawLine(0f, 68f, pageW.toFloat(), 68f, pAccentLine)
        val pFooter = mkPaint(cSub, 7f, align = Paint.Align.CENTER)
        c.drawLine(ml, pageH - 30f, ml + cw, pageH - 30f, pLineDivider)
        c.drawText("Page $pageNum  |  Koke Finance", pageW / 2f, pageH - 15f, pFooter)
    }

    fun finishPage() { page?.let { doc.finishPage(it) }; page = null; cv = null }

    fun newPage() {
        finishPage()
        pageNum++
        val info = PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create()
        page = doc.startPage(info)
        cv = page!!.canvas
        drawShell()
        y = 80f
    }

    fun checkSpace(h: Float) { if (y + h > pageH - 44f) newPage() }
    fun sp(h: Float = 8f) { y += h }

    fun sectionHeader(title: String) {
        sp(6f)
        checkSpace(22f)
        cv!!.drawRect(ml, y, ml + cw, y + 18f, pSecBg)
        cv!!.drawLine(ml, y, ml, y + 18f, pLeftBar)
        cv!!.drawText(title.uppercase(Locale.getDefault()), ml + 8f, y + 13f, pSecLabel)
        y += 22f
    }

    fun bodyRow(
        left: String,
        right: String,
        rightPaint: Paint = pAmtR,
        altBg: Boolean = false,
        indent: Float = 6f,
    ) {
        checkSpace(13f)
        if (altBg) cv!!.drawRect(ml, y, ml + cw, y + 12f, pRowAlt)
        cv!!.drawText(left, ml + indent, y + 9.5f, pBody)
        cv!!.drawText(right, ml + cw, y + 9.5f, rightPaint)
        y += 12f
    }

    // ---- Start rendering ----
    newPage()

    sectionHeader("Financial Summary")
    sp(4f)
    checkSpace(58f)
    val boxW = (cw - 12f) / 3f

    fun summaryBox(label: String, value: String, barColor: Int, ox: Float) {
        val c = cv!!
        val bx = ml + ox
        c.drawRect(bx, y, bx + boxW, y + 52f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cSecBg })
        c.drawRect(bx, y, bx + boxW, y + 3f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = barColor })
        c.drawText(label, bx + boxW / 2f, y + 22f, mkPaint(cSub, 7f, align = Paint.Align.CENTER))
        c.drawText(value, bx + boxW / 2f, y + 41f, mkPaint(barColor, 12f, bold = true, align = Paint.Align.CENTER))
    }

    val netColor = if (net >= 0) cIncome else cExpense
    summaryBox("NET", formatPeso(net), netColor, 0f)
    summaryBox("TOTAL INCOME", formatPeso(totalIncome), cIncome, boxW + 6f)
    summaryBox("TOTAL EXPENSES", formatPeso(totalExpense), cExpense, (boxW + 6f) * 2f)
    y += 58f

    sp(4f)
    checkSpace(13f)
    cv!!.drawText("${transactions.size} transaction(s) in selected period", ml + 6f, y + 9f, pDim)
    y += 16f

    val activeWallets = wallets.filter { it.isAutoIncludedInTotals() }
    if (activeWallets.isNotEmpty()) {
        sectionHeader("Wallet Balances")
        activeWallets.forEachIndexed { i, w ->
            bodyRow(w.name.stripEmoji(), formatPeso(w.computeBalance(transactions)), altBg = i % 2 == 1)
        }
    }

    val debtWallets = wallets.filter { it.kind == WalletKind.DEBT }
    if (debtWallets.isNotEmpty()) {
        sectionHeader("Debt Wallets (Remaining Balance)")
        debtWallets.forEachIndexed { i, w ->
            bodyRow(w.name.stripEmoji(), formatPeso(w.computeBalance(transactions)), altBg = i % 2 == 1)
        }
    }

    val hiddenWallets = wallets.filter { it.isHidden && it.kind == WalletKind.STANDARD }
    if (hiddenWallets.isNotEmpty()) {
        sectionHeader("Off-Book Wallets")
        hiddenWallets.forEachIndexed { i, w ->
            bodyRow(w.name.stripEmoji(), formatPeso(w.computeBalance(transactions)), altBg = i % 2 == 1)
        }
    }

    if (expenseByTag.isNotEmpty()) {
        sectionHeader("Expense by Category")
        val barMax = cw * 0.28f
        expenseByTag.forEachIndexed { i, tag ->
            checkSpace(16f)
            val frac = if (totalExpense > 0) (tag.amount / totalExpense).toFloat().coerceIn(0f, 1f) else 0f
            if (i % 2 == 1) cv!!.drawRect(ml, y, ml + cw, y + 15f, pRowAlt)
            cv!!.drawText(tag.label.stripEmoji(), ml + 6f, y + 11f, pBody)
            val barX = ml + cw * 0.52f
            cv!!.drawRect(barX, y + 4f, barX + barMax, y + 11f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.parseColor("#F5D5D0") })
            if (frac > 0f) cv!!.drawRect(barX, y + 4f, barX + barMax * frac, y + 11f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cExpense })
            cv!!.drawText("${formatPeso(tag.amount)}  ${(frac * 100).toInt()}%", ml + cw, y + 11f, pAmtExR)
            y += 15f
        }
    }

    if (incomeByTag.isNotEmpty()) {
        sectionHeader("Income by Category")
        val barMax = cw * 0.28f
        incomeByTag.forEachIndexed { i, tag ->
            checkSpace(16f)
            val frac = if (totalIncome > 0) (tag.amount / totalIncome).toFloat().coerceIn(0f, 1f) else 0f
            if (i % 2 == 1) cv!!.drawRect(ml, y, ml + cw, y + 15f, pRowAlt)
            cv!!.drawText(tag.label.stripEmoji(), ml + 6f, y + 11f, pBody)
            val barX = ml + cw * 0.52f
            cv!!.drawRect(barX, y + 4f, barX + barMax, y + 11f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.parseColor("#D0EDD8") })
            if (frac > 0f) cv!!.drawRect(barX, y + 4f, barX + barMax * frac, y + 11f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cIncome })
            cv!!.drawText("${formatPeso(tag.amount)}  ${(frac * 100).toInt()}%", ml + cw, y + 11f, pAmtInR)
            y += 15f
        }
    }

    sectionHeader("Full Transaction History")
    val xDate = ml + 6f
    val xType = ml + 82f
    val xTag  = ml + 128f
    val xAmtR = ml + 308f
    val xWal  = ml + 316f

    checkSpace(14f)
    cv!!.drawRect(ml, y, ml + cw, y + 13f, pSecBg)
    cv!!.drawText("DATE", xDate, y + 9.5f, pBoldBody)
    cv!!.drawText("TYPE", xType, y + 9.5f, pBoldBody)
    cv!!.drawText("CATEGORY", xTag, y + 9.5f, pBoldBody)
    cv!!.drawText("AMOUNT", xAmtR, y + 9.5f, mkPaint(cText, 8f, bold = true, align = Paint.Align.RIGHT))
    cv!!.drawText("WALLET", xWal, y + 9.5f, pBoldBody)
    y += 15f

    fun clip(s: String, max: Int) = if (s.length > max) s.take(max - 1) + "." else s

    transactions.sortedByDescending { it.timestamp }.forEachIndexed { i, tx ->
        checkSpace(12f)
        if (i % 2 == 1) cv!!.drawRect(ml, y, ml + cw, y + 12f, pRowAlt)
        val date    = dfRow.format(Date(tx.timestamp))
        val type    = if (tx.type == TransactionType.INCOME) "Income" else "Expense"
        val tagLbl  = clip(tx.tagLabel.stripEmoji(), 14)
        val walletN = clip((walletMap[tx.walletId]?.name ?: "?").stripEmoji(), 22)
        val amtStr  = if (tx.type == TransactionType.INCOME) "+${formatPeso(tx.amount)}"
                      else "-${formatPeso(tx.amount)}"
        val amtP    = if (tx.type == TransactionType.INCOME) pAmtInR else pAmtExR
        cv!!.drawText(date, xDate, y + 9f, pBody)
        cv!!.drawText(type, xType, y + 9f, pBody)
        cv!!.drawText(tagLbl, xTag, y + 9f, pBody)
        cv!!.drawText(amtStr, xAmtR, y + 9f, amtP)
        cv!!.drawText(walletN, xWal, y + 9f, pBody)
        y += 12f
    }

    if (transactions.isEmpty()) {
        checkSpace(13f)
        cv!!.drawText("No transactions in this period.", ml + 6f, y + 9f, pDim)
        y += 13f
    }

    finishPage()
    val out = ByteArrayOutputStream()
    doc.writeTo(out)
    doc.close()
    return out.toByteArray()
}
