package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Wallet
import com.example.data.model.WalletTransaction
import com.example.ui.components.GlassCard
import com.example.ui.components.formatCoins
import com.example.ui.strings.LocalAppStrings
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun WalletAndHistoryScreen(
    wallet: Wallet?,
    transactions: List<WalletTransaction>
) {
    val strings = LocalAppStrings.current
    var filter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Earned", "Spent", "Pending", "Completed")

    val filtered = remember(transactions, filter) {
        when (filter) {
            "Earned" -> transactions.filter { it.amount > 0L }
            "Spent" -> transactions.filter { it.amount < 0L }
            "Pending" -> transactions.filter { it.status == "PENDING" }
            "Completed" -> transactions.filter { it.status == "COMPLETED" }
            else -> transactions
        }
    }

    val dateFormatter = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.US) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Wallet & Ledger", style = MaterialTheme.typography.headlineMedium)
        }

        item {
            GlassCard(borderColor = GoldPrimary.copy(alpha = 0.65f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(strings.availableCoins, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatCoins(wallet?.availableCoins ?: 0L), style = MaterialTheme.typography.displaySmall, color = GoldPrimary)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(strings.totalEarned, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatCoins(wallet?.totalEarnedCoins ?: 0L), style = MaterialTheme.typography.headlineSmall, color = EmeraldSuccess)
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                WalletBreakdownRow("Welcome Bonus", wallet?.welcomeBonusCoins ?: 0L)
                WalletBreakdownRow("Daily Bonus", wallet?.dailyBonusCoins ?: 0L)
                WalletBreakdownRow("Scratch Card Rewards", wallet?.scratchCoins ?: 0L)
                WalletBreakdownRow("Lucky Spin Rewards", wallet?.spinCoins ?: 0L)
                WalletBreakdownRow("Quiz Rewards", wallet?.quizCoins ?: 0L)
                WalletBreakdownRow("Ad Rewards", wallet?.adCoins ?: 0L)
                WalletBreakdownRow("Redeemed / Pass Spent", wallet?.redeemedCoins ?: 0L, isDebit = true)
            }
        }

        item {
            Text(strings.transactionHistory, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEach { f ->
                    FilterChip(
                        selected = filter == f,
                        onClick = { filter = f },
                        label = { Text(f) },
                        modifier = Modifier.testTag("tx_filter_$f")
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                GlassCard {
                    Text("No transactions match this filter yet.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            items(filtered, key = { it.id }) { tx ->
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tx.description, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            val dateStr = tx.createdAt?.toDate()?.let { dateFormatter.format(it) } ?: "Syncing..."
                            Text(
                                text = "${tx.txType} • ${tx.status} • $dateStr",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val amtText = if (tx.amount >= 0) "+${tx.amount}" else "${tx.amount}"
                        Text(
                            text = amtText,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (tx.amount >= 0) EmeraldSuccess else RubyError,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WalletBreakdownRow(label: String, value: Long, isDebit: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = "${if (isDebit) "-" else "+"}${formatCoins(value)}",
            style = MaterialTheme.typography.labelMedium,
            color = if (isDebit) RubyError else GoldPrimary
        )
    }
}
