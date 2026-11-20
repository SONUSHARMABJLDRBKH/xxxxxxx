package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.QuizQuestion
import com.example.data.model.QuizScoreSummary
import com.example.data.model.Wallet
import com.example.data.quiz.BiharBoardQuestionBank
import com.example.ui.components.GlassCard
import com.example.ui.components.GlowingGoldButton
import com.example.ui.components.formatCountdown
import com.example.ui.strings.LocalAppStrings
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun QuizScoreboardScreen(
    score: QuizScoreSummary,
    wallet: Wallet?,
    allQuestions: List<QuizQuestion>,
    nowMillis: Long,
    onBuyAdFreePass: () -> Unit
) {
    val strings = LocalAppStrings.current
    var showConfirmPassDialog by remember { mutableStateOf(false) }

    val passRemaining = ((wallet?.adFreePassExpiresAt ?: 0L) - nowMillis).coerceAtLeast(0L)
    val passActive = passRemaining > 0L

    val subjectRows = listOf(
        "Mathematics" to score.mathScore,
        "Physics" to score.physicsScore,
        "Chemistry" to score.chemistryScore,
        "Biology" to score.biologyScore,
        "Hindi" to score.hindiScore,
        "English" to score.englishScore
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(strings.scoreboard, style = MaterialTheme.typography.headlineMedium)

        if (score.accuracyPercent >= 95.0 && score.totalAttempted >= 5L) {
            GlassCard(borderColor = GoldPrimary, containerColor = GoldContainerDark) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = GoldBright, modifier = Modifier.size(32.dp))
                    Text(
                        text = strings.excellent95Banner,
                        style = MaterialTheme.typography.bodyMedium,
                        color = GoldBright,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Overall Target 100 Summary
        GlassCard(borderColor = GoldPrimary.copy(alpha = 0.6f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Overall Score (Target 100)", style = MaterialTheme.typography.titleMedium)
                    Text("0.09999 pts per correct answer", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    text = String.format(Locale.US, "%05.2f", score.overallScore),
                    style = MaterialTheme.typography.headlineMedium,
                    color = GoldPrimary,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { (score.overallScore / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(6.dp)),
                color = GoldPrimary
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ScoreStat("Attempted", score.totalAttempted.toString())
                ScoreStat("Correct", score.correctCount.toString(), EmeraldSuccess)
                ScoreStat("Incorrect", score.incorrectCount.toString(), RubyError)
                ScoreStat("Accuracy", String.format(Locale.US, "%.1f%%", score.accuracyPercent), CyanAccent)
            }
        }

        // Subject-wise Performance Table
        GlassCard(borderColor = PurpleVibrant.copy(alpha = 0.5f)) {
            Text("Subject-wise Score & Question Bank", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Subject", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Questions / 1,000", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Score", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            subjectRows.forEach { (subject, value) ->
                val qCount = allQuestions.count { it.subject.equals(subject, ignoreCase = true) }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(subject, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1.2f))
                    Text(
                        text = "$qCount / ${BiharBoardQuestionBank.TARGET_QUESTIONS_PER_SUBJECT}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = String.format(Locale.US, "%05.2f", value),
                        style = MaterialTheme.typography.labelMedium,
                        color = GoldPrimary
                    )
                }
            }
        }

        // 24h Quiz Ad-Free Pass (6,999 Coins)
        GlassCard(borderColor = EmeraldSuccess.copy(alpha = 0.6f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Rounded.Block, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(30.dp))
                Text(strings.adFreePassTitle, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = strings.adFreePassDesc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (passActive) {
                Surface(color = EmeraldContainer, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Active! Expires in ${formatCountdown(passRemaining)}",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                GlowingGoldButton(
                    text = strings.buyPassCta,
                    onClick = { showConfirmPassDialog = true },
                    enabled = (wallet?.availableCoins ?: 0L) >= 6999L,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "buy_ad_free_pass_btn"
                )
            }
        }
    }

    if (showConfirmPassDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmPassDialog = false },
            title = { Text("Confirm Quiz Ad-Free Pass") },
            text = {
                Text("Deduct 6,999 coins for a 24-hour Quiz Ad-Free Pass? This applies ONLY inside the Objective Quiz section and does not remove ads on Home, Spin, or Scratch Cards.")
            },
            confirmButton = {
                Button(onClick = {
                    showConfirmPassDialog = false
                    onBuyAdFreePass()
                }) { Text(strings.confirm) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmPassDialog = false }) { Text(strings.cancel) }
            }
        )
    }
}

@Composable
private fun ScoreStat(label: String, value: String, color: Color = GoldPrimary) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.ExtraBold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
