package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlin.random.Random

@Composable
fun StudyWinBackground(
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val bgBrush = if (darkTheme) {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF0A0F2C), Color(0xFF130D35), Color(0xFF070B1E))
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color(0xFFF3F5FF), Color(0xFFEDE9FE), Color(0xFFF9FAFF))
        )
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgBrush),
        content = content
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
    containerColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    Surface(
        modifier = modifier
            .shadow(10.dp, shape, ambientColor = PurplePrimary.copy(alpha = 0.25f), spotColor = GoldPrimary.copy(alpha = 0.18f))
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun GlowingGoldButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    testTag: String = "gold_button"
) {
    val brush = if (enabled) {
        Brush.horizontalGradient(listOf(GoldBright, GoldPrimary, GoldDeep))
    } else {
        Brush.horizontalGradient(listOf(Color.Gray.copy(alpha = 0.4f), Color.Gray.copy(alpha = 0.5f)))
    }
    Box(
        modifier = modifier
            .heightIn(min = 50.dp)
            .shadow(if (enabled) 12.dp else 0.dp, RoundedCornerShape(16.dp), spotColor = GoldPrimary)
            .clip(RoundedCornerShape(16.dp))
            .background(brush)
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(testTag)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) Color(0xFF1F1600) else Color.White.copy(alpha = 0.7f),
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun GlowingPurpleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    testTag: String = "purple_button"
) {
    val brush = if (enabled) {
        Brush.horizontalGradient(listOf(PurpleVibrant, PurplePrimary, PurpleDeep))
    } else {
        Brush.horizontalGradient(listOf(Color.Gray.copy(alpha = 0.4f), Color.Gray.copy(alpha = 0.5f)))
    }
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .shadow(if (enabled) 10.dp else 0.dp, RoundedCornerShape(16.dp), spotColor = PurpleVibrant)
            .clip(RoundedCornerShape(16.dp))
            .background(brush)
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(testTag)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun CoinBalancePill(
    coins: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF2B1D00), Color(0xFF473000))
                )
            )
            .border(1.2.dp, GoldPrimary.copy(alpha = 0.85f), CircleShape)
            .clickable(onClick = onClick)
            .testTag("wallet_balance_pill")
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.MonetizationOn,
            contentDescription = "Coins",
            tint = GoldPrimary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = formatCoins(coins),
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
            color = GoldBright,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

private data class CoinParticle(
    val xFraction: Float,
    val speed: Float,
    val radius: Float,
    val initialOffset: Float,
    val rotationSpeed: Float
)

@Composable
fun CoinRainOverlay(trigger: Int, modifier: Modifier = Modifier) {
    if (trigger <= 0) return
    val progress = remember(trigger) { Animatable(0f) }
    val particles = remember(trigger) {
        List(28) {
            CoinParticle(
                xFraction = Random.nextFloat(),
                speed = 0.8f + Random.nextFloat() * 0.7f,
                radius = 12f + Random.nextFloat() * 14f,
                initialOffset = -Random.nextFloat() * 0.35f,
                rotationSpeed = 120f + Random.nextFloat() * 240f
            )
        }
    }

    LaunchedEffect(trigger) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 1900, easing = LinearEasing))
    }

    if (progress.value in 0.01f..0.99f) {
        Canvas(modifier = modifier.fillMaxSize()) {
            particles.forEach { p ->
                val t = (progress.value * p.speed + p.initialOffset).coerceIn(0f, 1.2f)
                val x = p.xFraction * size.width
                val y = t * (size.height + 100f) - 40f
                val alpha = (1f - progress.value).coerceIn(0f, 1f)
                rotate(degrees = progress.value * p.rotationSpeed, pivot = Offset(x, y)) {
                    drawCircle(
                        color = GoldPrimary.copy(alpha = alpha),
                        radius = p.radius,
                        center = Offset(x, y)
                    )
                    drawCircle(
                        color = GoldBright.copy(alpha = alpha * 0.85f),
                        radius = p.radius * 0.65f,
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}

fun formatCoins(value: Long): String {
    return String.format(java.util.Locale.US, "%,d", value)
}

fun formatCountdown(remainingMillis: Long): String {
    if (remainingMillis <= 0L) return "00:00:00"
    val totalSeconds = remainingMillis / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
}
