package com.codekokeshi.kokefinanceapp.data

import android.content.Context
import com.codekokeshi.kokefinanceapp.model.Tag
import com.codekokeshi.kokefinanceapp.model.Transaction
import com.codekokeshi.kokefinanceapp.model.TransactionType
import com.codekokeshi.kokefinanceapp.model.Wallet
import com.codekokeshi.kokefinanceapp.model.WalletKind
import org.json.JSONArray
import org.json.JSONObject

class FinanceRepository(context: Context) {

    private val prefs = context.getSharedPreferences("koke_finance", Context.MODE_PRIVATE)

    fun saveTags(tags: List<Tag>) {
        val arr = JSONArray()
        for (tag in tags) {
            arr.put(JSONObject().apply {
                put("id", tag.id)
                put("name", tag.name)
                put("emoji", tag.emoji)
                put("type", tag.type.name)
                put("createdAt", tag.createdAt)
            })
        }
        prefs.edit().putString("tags", arr.toString()).apply()
    }

    fun loadTags(): List<Tag> {
        val json = prefs.getString("tags", null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { index ->
                val obj = arr.getJSONObject(index)
                Tag(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    emoji = obj.getString("emoji"),
                    type = TransactionType.valueOf(obj.getString("type")),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveWallets(wallets: List<Wallet>) {
        val arr = JSONArray()
        for (w in wallets) {
            arr.put(JSONObject().apply {
                put("id", w.id)
                put("name", w.name)
                put("initialBalance", w.initialBalance)
                put("isHidden", w.isHidden)
                put("kind", w.kind.name)
                put("createdAt", w.createdAt)
            })
        }
        prefs.edit().putString("wallets", arr.toString()).apply()
    }

    fun loadWallets(): List<Wallet> {
        val json = prefs.getString("wallets", null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                Wallet(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    initialBalance = obj.getDouble("initialBalance"),
                    isHidden = obj.optBoolean("isHidden", false),
                    kind = obj.optString("kind", WalletKind.STANDARD.name)
                        .let { value -> WalletKind.entries.firstOrNull { it.name == value } ?: WalletKind.STANDARD },
                    createdAt = obj.getLong("createdAt")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveTransactions(transactions: List<Transaction>) {
        val arr = JSONArray()
        for (t in transactions) {
            arr.put(JSONObject().apply {
                put("id", t.id)
                put("walletId", t.walletId)
                put("type", t.type.name)
                put("amount", t.amount)
                put("tagId", t.tagId)
                put("tagLabel", t.tagLabel)
                put("tagEmoji", t.tagEmoji)
                put("note", t.note)
                put("timestamp", t.timestamp)
            })
        }
        prefs.edit().putString("transactions", arr.toString()).apply()
    }

    fun loadTransactions(): List<Transaction> {
        val json = prefs.getString("transactions", null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                val legacy = legacyCategory(obj.optString("category", ""))
                Transaction(
                    id = obj.getString("id"),
                    walletId = obj.getString("walletId"),
                    type = TransactionType.valueOf(obj.getString("type")),
                    amount = obj.getDouble("amount"),
                    tagId = obj.optString("tagId", "").ifBlank { null },
                    tagLabel = obj.optString("tagLabel", legacy.first),
                    tagEmoji = obj.optString("tagEmoji", legacy.second),
                    note = obj.optString("note", ""),
                    timestamp = obj.getLong("timestamp")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun legacyCategory(categoryName: String): Pair<String, String> {
        return when (categoryName) {
            "SALARY" -> "Salary" to "\uD83D\uDCB0"
            "FREELANCE" -> "Freelance" to "\uD83D\uDCBB"
            "BUSINESS" -> "Business" to "\uD83D\uDCBC"
            "INVESTMENT" -> "Investment" to "\uD83D\uDCC8"
            "GIFT_RECEIVED" -> "Gift" to "\uD83C\uDF81"
            "FOOD" -> "Food & Drinks" to "\uD83C\uDF54"
            "TRANSPORT" -> "Transport" to "\uD83D\uDE97"
            "SHOPPING" -> "Shopping" to "\uD83D\uDED2"
            "BILLS" -> "Bills & Utilities" to "\uD83D\uDCC4"
            "ENTERTAINMENT" -> "Entertainment" to "\uD83C\uDFAC"
            "HEALTH" -> "Health" to "\uD83D\uDC8A"
            "EDUCATION" -> "Education" to "\uD83D\uDCDA"
            "RENT" -> "Rent" to "\uD83C\uDFE0"
            else -> "Untagged" to "\uD83D\uDCC1"
        }
    }
}
