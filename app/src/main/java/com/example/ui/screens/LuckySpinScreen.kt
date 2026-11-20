package com.example.ui.screens

import android.app.Activity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ads.findActivity
import com.example.data.model.AppSettings
import com.example.data.model.Wallet
import com.example.ui.components.GlassCard
import com.example.ui.components.GlowingGoldButton
import com.example.ui.components.GlowingPurpleButton
import com.example.ui.components.formatCountdown
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val WHEEL_VALUES = listOf(1L, 10L, 49L, 99L, 199L, 299L, 399L, 499L, 599L, 999L)
private val WHEEL_COLORS = listOf(
    Color(0xFF4C1D95),
    Color(0xFFE09F00),
    Color(0xFF1E3A8A),
    Color(0xFF7C3AED),
    Color(0xFF047857),
    Color(0xFFD97706),
    Color(0xFF312E81),
    Color(0xFFBE185D),
    Color(0xFF0284C7),
    Color(0xFFFFC83D)
)

@Composable
fun LuckySpinScreen(
    wallet: Wallet?,
    settings: AppSettings,
    nowMillis: Long,
    onSpinCompleted: (coins: Long, spinType: String, onSaved: () -> Unit) -> Unit,
    onWatchAdForExtraSpin: (Activity?, () -> Unit) -> Unit
) {
    val activity = LocalContext.current.findActivity()
    val scope = rememberCoroutineScope()

    val lastSpinAt = wallet?.lastFreeSpinAt ?: 0L
    val freeCooldown = ((lastSpinAt + 24L * 60L * 60L * 1000L) - nowMillis).coerceAtLeast(0L)
    val canFreeSpin = lastSpinAt == 0L || freeCooldown == 0L

    val adCount = wallet?.adRewardsCount ?: 0L
    val specialUnlocked = wallet?.specialSpinsUnlocked ?: 0L
    val specialUsed = wallet?.specialSpinsUsed ?: 0L
    val specialRemaining = (specialUnlocked - specialUsed).coerceAtLeast(0L)
    val milestoneProgress = (adCount % 100L).toFloat() / 100f

    val rotationAnim = remember { Animatable(0f) }
    var isSpinning by remember { mutableStateOf(false) }
    var lastWonCoins by remember { mutableStateOf<Long?>(null) }

    fun launchWheelSpin(spinType: String) {
        if (isSpinning) return
        isSpinning = true
        val weights = settings.spinWeightsCsv.split(",").mapNotNull { it.trim().toIntOrNull() }
            .takeIf { it.size == WHEEL_VALUES.size }
            ?: listOf(25, 25, 16, 12, 8, 5, 4, 3, 1, 1)

        val pickedIndex = when (spinType) {
            "DAILY_FREE" -> {
                // Free daily spin gives random reward between 1 and 200 coins (indices 0..4 => 1, 10, 49, 99, 199)
                val freeWeights = weights.take(5)
                pickWeightedIndex(freeWeights)
            }
            "SPECIAL_999" -> 9 // 999 coins special milestone opportunity!
            else -> pickWeightedIndex(weights)
        }
        val sliceAngle = 360f / WHEEL_VALUES.size
        val targetSliceCenter = pickedIndex * sliceAngle + sliceAngle / 2f
        val targetFinalMod = (270f - targetSliceCenter + 360f) % 360f
        val currentMod = rotationAnim.value % 360f
        val extraTurns = 360f * 5f
        val targetAngle = rotationAnim.value + extraTurns + ((targetFinalMod - currentMod + 360f) % 360f)

        scope.launch {
            rotationAnim.animateTo(
                targetValue = targetAngle,
                animationSpec = tween(durationMillis = 3600, easing = FastOutSlowInEasing)
            )
            val wonCoins = WHEEL_VALUES[pickedIndex]
            lastWonCoins = wonCoins
            onSpinCompleted(wonCoins, spinType) {
                isSpinning = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Lucky Spin Wheel", style = MaterialTheme.typography.headlineMedium)

        // Wheel + Pointer
        Box(
            modifier = Modifier.size(300.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val diameter = size.minDimension - 16.dp.toPx()
                val radius = diameter / 2f
                val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                val sweep = 360f / WHEEL_VALUES.size

                rotate(degrees = rotationAnim.value, pivot = center) {
                    WHEEL_VALUES.forEachIndexed { idx, value ->
                        drawArc(
                            color = WHEEL_COLORS[idx % WHEEL_COLORS.size],
                            startAngle = idx * sweep,
                            sweepAngle = sweep,
                            useCenter = true,
                            topLeft = topLeft,
                            size = Size(diameter, diameter)
                        )
                        val midRad = Math.toRadians((idx * sweep + sweep / 2f).toDouble())
                        val textDist = radius * 0.68f
                        val tx = center.x + (textDist * cos(midRad)).toFloat()
                        val ty = center.y + (textDist * sin(midRad)).toFloat()
                        drawContext.canvas.nativeCanvas.apply {
                            val paint = android.graphics.Paint().apply {
                                color = if (idx == 9) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                                textSize = 36f
                                isFakeBoldText = true
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                            drawText(value.toString(), tx, ty + 12f, paint)
                        }
                    }
                }

                // Outer golden ring
                drawCircle(
                    color = GoldPrimary,
                    radius = radius + 4.dp.toPx(),
                    center = center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8.dp.toPx())
                )

                // Top pointer triangle
                val pointer = Path().apply {
                    moveTo(center.x, 22.dp.toPx())
                    lineTo(center.x - 16.dp.toPx(), 0f)
                    lineTo(center.x + 16.dp.toPx(), 0f)
                    close()
                }
                drawPath(pointer, color = GoldBright)
            }

            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MidnightBg,
                border = androidx.compose.foundation.BorderStroke(3.dp, GoldPrimary),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = GoldBright)
                }
            }
        }

        if (lastWonCoins != null) {
            GlassCard(borderColor = GoldPrimary) {
                Text(
                    text = "Congratulations! You won +$lastWonCoins Coins!",
                    style = MaterialTheme.typography.titleMedium,
                    color = GoldBright,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Daily Free Spin + Ad Extra Spin
        GlassCard(borderColor = PurpleVibrant.copy(alpha = 0.5f)) {
            Text("Spin Eligibility & Rules", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "• 1 Free Daily Spin every 24 hours (1–199 coin range).\n• Extra spins require 1 completed Rewarded Ad before the wheel spins (1–999 coin range, admin-configured weights).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (canFreeSpin) {
                GlowingGoldButton(
                    text = if (isSpinning) "Spinning..." else "Spin Daily Free Wheel",
                    onClick = { launchWheelSpin("DAILY_FREE") },
                    enabled = !isSpinning,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "free_spin_btn"
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(PurpleContainerDark)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Timer, contentDescription = null, tint = GoldBright)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Free spin resets in ${formatCountdown(freeCooldown)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = GoldBright
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                GlowingPurpleButton(
                    text = if (isSpinning) "Spinning..." else "Watch Ad & Spin Extra Wheel",
                    onClick = {
                        onWatchAdForExtraSpin(activity) {
                            launchWheelSpin("AD_EXTRA")
                        }
                    },
                    enabled = !isSpinning,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "ad_extra_spin_btn"
                )
            }
        }

        // Special 100-Ad Loyalty Milestone
        GlassCard(borderColor = GoldPrimary.copy(alpha = 0.55f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("100-Ad Special 999 Spin Milestone", style = MaterialTheme.typography.titleMedium)
                Text("${adCount % 100}/100 Ads", style = MaterialTheme.typography.labelMedium, color = GoldPrimary)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { milestoneProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = GoldPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Every 100 verified Rewarded Ads unlocks 5 Special Spin opportunities (999 coins each). Available Special Spins: $specialRemaining",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (specialRemaining > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                GlowingGoldButton(
                    text = "Use Special 999 Spin ($specialRemaining left)",
                    onClick = { launchWheelSpin("SPECIAL_999") },
                    enabled = !isSpinning,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "special_spin_btn"
                )
            }
        }
    }
}

private fun pickWeightedIndex(weights: List<Int>): Int {
    val total = weights.sum().coerceAtLeast(1)
    var r = Random.nextInt(total)
    weights.forEachIndexed { index, w ->
        r -= w
        if (r < 0) return index
    }
    return 0
}
