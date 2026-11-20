package com.example.ui.screens

import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ads.findActivity
import com.example.data.model.AppSettings
import com.example.data.model.Wallet
import com.example.data.model.WalletTransaction
import com.example.ui.components.GlassCard
import com.example.ui.components.GlowingGoldButton
import com.example.ui.components.GlowingPurpleButton
import com.example.ui.components.formatCoins
import com.example.ui.components.formatCountdown
import com.example.ui.strings.LocalAppStrings
import com.example.ui.theme.*
import java.util.UUID

@Composable
fun DailyBonusAndScratchScreen(
    wallet: Wallet?,
    settings: AppSettings,
    todayKey: String,
    nowMillis: Long,
    transactions: List<WalletTransaction>,
    onClaimDailyBonus: () -> Unit,
    onClaimFreeScratch: () -> Unit,
    onWatchAdForExtraScratch: (Activity?) -> Unit,
    onClaimExtraScratch: (String) -> Unit,
    onWatchAdEarn: (Activity?) -> Unit,
    onNavigateSpin: () -> Unit
) {
    val strings = LocalAppStrings.current
    val activity = LocalContext.current.findActivity()

    val lastDaily = wallet?.lastDailyBonusAt ?: 0L
    val dailyCooldownRemaining = ((lastDaily + 24L * 60L * 60L * 1000L) - nowMillis).coerceAtLeast(0L)
    val canClaimDaily = lastDaily == 0L || dailyCooldownRemaining == 0L

    val usedToday = if (wallet?.dailyScratchDate == todayKey) (wallet.dailyScratchUsed.toInt()).coerceIn(0, 3) else 0
    val availableFreeCards = (3 - usedToday).coerceAtLeast(0)
    val extraAvailable = (wallet?.extraScratchAvailable ?: 0L).toInt()
    val totalScratchEarned = wallet?.scratchCoins ?: 0L

    // Interactive scratch foil state
    var scratchPoints by remember { mutableStateOf(listOf<Offset>()) }
    var scratchProgress by remember { mutableFloatStateOf(0f) }
    val scratchRevealed = scratchProgress >= 0.45f

    val dailyHistory = remember(transactions) {
        transactions.filter { it.txType == "DAILY_BONUS" || it.txType == "SCRATCH_REWARD" }.take(8)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Earn Coins", style = MaterialTheme.typography.headlineMedium)
                GlowingPurpleButton(
                    text = strings.luckySpin,
                    onClick = onNavigateSpin,
                    testTag = "earn_open_spin_btn"
                )
            }
        }

        // 1. DAILY BONUS CARD
        item {
            GlassCard(borderColor = GoldPrimary.copy(alpha = 0.65f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Rounded.CardGiftcard, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(32.dp))
                        Column {
                            Text(strings.dailyBonus, style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = "Win ${settings.dailyBonusMin}–${settings.dailyBonusMax} random coins every 24h",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (!canClaimDaily) {
                    Surface(
                        color = PurpleContainerDark,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Timer, contentDescription = null, tint = GoldBright)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${strings.nextIn}: ${formatCountdown(dailyCooldownRemaining)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = GoldBright
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
                GlowingGoldButton(
                    text = if (canClaimDaily) strings.claimNow else "Claimed for 24h",
                    onClick = onClaimDailyBonus,
                    enabled = canClaimDaily,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "claim_daily_bonus_btn"
                )
            }
        }

        // 2. SCRATCH CARD SYSTEM
        item {
            GlassCard(borderColor = CyanAccent.copy(alpha = 0.6f)) {
                Text(strings.scratchCards, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatMiniChip("Available", "${availableFreeCards + extraAvailable}")
                    StatMiniChip("Scratched Today", "$usedToday / 3")
                    StatMiniChip("Total Won", "${formatCoins(totalScratchEarned)}")
                }
                Spacer(modifier = Modifier.height(14.dp))

                // Interactive Foil Scratch Pad
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF28125E), Color(0xFF422C00)))
                        )
                        .border(2.dp, GoldPrimary, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.Stars, contentDescription = null, tint = GoldBright, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (availableFreeCards > 0 || extraAvailable > 0) {
                                if (scratchRevealed) "Ready to Claim ${settings.scratchMin}–${settings.scratchMax} Coins!"
                                else "Rub Here to Scratch!"
                            } else "All 3 Free Cards Used Today",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if ((availableFreeCards > 0 || extraAvailable > 0) && !scratchRevealed) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(usedToday, extraAvailable) {
                                    detectDragGestures { change, _ ->
                                        change.consume()
                                        scratchPoints = scratchPoints + change.position
                                        scratchProgress = (scratchPoints.size / 26f).coerceIn(0f, 1f)
                                    }
                                }
                        ) {
                            drawRect(
                                brush = Brush.linearGradient(
                                    listOf(Color(0xFF94A3B8), Color(0xFFCBD5E1), Color(0xFF64748B))
                                ),
                                alpha = (1f - scratchProgress * 0.9f).coerceIn(0.15f, 1f)
                            )
                            if (scratchPoints.size > 1) {
                                val path = Path().apply {
                                    moveTo(scratchPoints.first().x, scratchPoints.first().y)
                                    scratchPoints.drop(1).forEach { lineTo(it.x, it.y) }
                                }
                                drawPath(
                                    path = path,
                                    color = GoldPrimary.copy(alpha = 0.65f),
                                    style = Stroke(width = 36f, cap = StrokeCap.Round)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (availableFreeCards > 0) {
                    GlowingGoldButton(
                        text = if (scratchRevealed) "Claim Free Scratch Coins" else "Scratch Above (or Tap to Reveal)",
                        onClick = {
                            if (!scratchRevealed) {
                                scratchProgress = 1f
                            } else {
                                onClaimFreeScratch()
                                scratchPoints = emptyList()
                                scratchProgress = 0f
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "claim_free_scratch_btn"
                    )
                } else if (extraAvailable > 0) {
                    GlowingGoldButton(
                        text = if (scratchRevealed) "Claim Bonus Ad Scratch Coins" else "Scratch Bonus Card Above",
                        onClick = {
                            if (!scratchRevealed) {
                                scratchProgress = 1f
                            } else {
                                onClaimExtraScratch("ad_sc_${UUID.randomUUID()}")
                                scratchPoints = emptyList()
                                scratchProgress = 0f
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "claim_extra_scratch_btn"
                    )
                } else {
                    GlowingPurpleButton(
                        text = "Watch Ad to Get Another Card",
                        onClick = { onWatchAdForExtraScratch(activity) },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "watch_ad_scratch_btn"
                    )
                }
            }
        }

        // 3. WATCH ADS & EARN DIRECTLY
        item {
            GlassCard(borderColor = EmeraldSuccess.copy(alpha = 0.5f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Rounded.PlayCircleFilled, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(36.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.watchAdsEarn, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Earn 10–25 coins per verified Rewarded Ad. Verified ads also count toward your 100-Ad Special 999 Spin milestone (${wallet?.adRewardsCount ?: 0}/100)!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                GlowingPurpleButton(
                    text = "Watch Rewarded Ad (+10–25 Coins)",
                    onClick = { onWatchAdEarn(activity) },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "watch_ad_earn_btn"
                )
            }
        }

        if (dailyHistory.isNotEmpty()) {
            item {
                Text("Recent Daily & Scratch Claims", style = MaterialTheme.typography.titleMedium)
            }
            items(dailyHistory, key = { it.id }) { tx ->
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tx.description, style = MaterialTheme.typography.bodyMedium)
                            Text(tx.txType, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("+${tx.amount}", style = MaterialTheme.typography.labelLarge, color = GoldPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatMiniChip(label: String, value: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, color = GoldPrimary, fontWeight = FontWeight.Bold)
    }
}
