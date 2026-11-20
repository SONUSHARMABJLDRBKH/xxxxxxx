package com.example.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ads.findActivity
import com.example.data.model.QuizQuestion
import com.example.data.model.Wallet
import com.example.data.quiz.BiharBoardQuestionBank
import com.example.ui.QuizSessionState
import com.example.ui.components.GlassCard
import com.example.ui.components.GlowingGoldButton
import com.example.ui.components.GlowingPurpleButton
import com.example.ui.components.formatCountdown
import com.example.ui.strings.LocalAppStrings
import com.example.ui.theme.*

@Composable
fun QuizEngineScreen(
    session: QuizSessionState,
    allQuestions: List<QuizQuestion>,
    wallet: Wallet?,
    nowMillis: Long,
    onSelectClassSubject: (Int, String) -> Unit,
    onSubmitOption: (String) -> Unit,
    onWatchUnlockAd: (Activity?) -> Unit,
    onNextQuestion: () -> Unit,
    onOpenScoreboard: () -> Unit
) {
    val strings = LocalAppStrings.current
    val activity = LocalContext.current.findActivity()

    LaunchedEffect(Unit) {
        if (session.activeQuestion == null) {
            onSelectClassSubject(session.selectedClass, session.selectedSubject)
        }
    }

    val passRemaining = ((wallet?.adFreePassExpiresAt ?: 0L) - nowMillis).coerceAtLeast(0L)
    val adFreeActive = passRemaining > 0L

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header + Scoreboard button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Bihar Board Quiz", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "Class ${session.selectedClass} • ${session.selectedSubject}",
                    style = MaterialTheme.typography.bodySmall,
                    color = GoldPrimary
                )
            }
            OutlinedButton(
                onClick = onOpenScoreboard,
                modifier = Modifier.testTag("open_scoreboard_btn")
            ) {
                Icon(Icons.Rounded.Assessment, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.scoreboard)
            }
        }

        if (adFreeActive) {
            Surface(
                color = EmeraldContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.Verified, contentDescription = null, tint = EmeraldSuccess)
                    Text(
                        text = "Quiz Ad-Free Pass Active • ${formatCountdown(passRemaining)} left",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            }
        }

        // Class Selector (9, 10, 11, 12)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(9, 10, 11, 12).forEach { cls ->
                FilterChip(
                    selected = session.selectedClass == cls,
                    onClick = { onSelectClassSubject(cls, session.selectedSubject) },
                    label = { Text("Class $cls") },
                    modifier = Modifier.testTag("quiz_class_$cls")
                )
            }
        }

        // Subject Selector
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BiharBoardQuestionBank.subjectsForClass(session.selectedClass).forEach { subj ->
                val count = allQuestions.count { it.subject.equals(subj, ignoreCase = true) }
                FilterChip(
                    selected = session.selectedSubject.equals(subj, ignoreCase = true),
                    onClick = { onSelectClassSubject(session.selectedClass, subj) },
                    label = { Text("$subj ($count)") },
                    modifier = Modifier.testTag("quiz_subject_${subj.lowercase()}")
                )
            }
        }

        // 95% Achievement Celebration Banner
        AnimatedVisibility(visible = session.show95PercentCelebration) {
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

        val question = session.activeQuestion
        if (question == null) {
            GlassCard {
                Text("No questions available for this filter yet.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            GlassCard(borderColor = PurpleVibrant.copy(alpha = 0.6f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ch: ${question.chapter}",
                        style = MaterialTheme.typography.labelMedium,
                        color = CyanAccent
                    )
                    if (question.examYear.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PurpleContainerDark
                        ) {
                            Text(
                                text = "BSEB ${question.examYear}",
                                style = MaterialTheme.typography.bodySmall,
                                color = GoldBright,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Source: ${question.source}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = question.questionText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                val displayLetters = listOf("A", "B", "C", "D")
                session.shuffledOptions.forEachIndexed { idx, (origKey, optionText) ->
                    val isSelected = session.selectedOptionKey == origKey
                    val isCorrectOption = origKey == question.correctOption
                    val revealColors = session.selectedOptionKey != null && (session.explanationRevealed || isSelected)

                    val bgColor = when {
                        revealColors && isCorrectOption -> EmeraldContainer
                        revealColors && isSelected && !isCorrectOption -> RubyContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    }
                    val borderColor = when {
                        revealColors && isCorrectOption -> EmeraldSuccess
                        revealColors && isSelected && !isCorrectOption -> RubyError
                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = bgColor,
                        border = BorderStroke(1.2.dp, borderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .heightIn(min = 52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(enabled = session.selectedOptionKey == null) {
                                onSubmitOption(origKey)
                            }
                            .testTag("quiz_option_$idx")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PurplePrimary.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(displayLetters.getOrElse(idx) { origKey }, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = optionText,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Feedback after answer
                if (session.answeredCorrectly == true) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(color = EmeraldContainer, shape = RoundedCornerShape(12.dp)) {
                        Text(
                            text = "Correct! +${session.lastCoinsEarned} coins & +0.09999 pts added. Loading next question...",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp).fillMaxWidth()
                        )
                    }
                }

                // Wrong answer: 3 consecutive rewarded ads lock OR unlocked explanation
                if (session.answeredCorrectly == false) {
                    Spacer(modifier = Modifier.height(12.dp))
                    if (session.wrongAnswerLocked) {
                        Surface(
                            color = RubyContainer,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Incorrect Answer — Next Question Locked",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Complete 3 consecutive Rewarded Ads to reveal the correct answer & explanation (${session.adsWatchedForUnlock}/3 completed).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFFECACA)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { session.adsWatchedForUnlock / 3f },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                    color = GoldPrimary
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                GlowingPurpleButton(
                                    text = "Watch Rewarded Ad (${session.adsWatchedForUnlock}/3)",
                                    onClick = { onWatchUnlockAd(activity) },
                                    modifier = Modifier.fillMaxWidth(),
                                    testTag = "quiz_unlock_ad_btn"
                                )
                            }
                        }
                    }

                    if (session.explanationRevealed) {
                        Surface(
                            color = MidnightSurfaceElevated,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Correct Answer: Option ${question.correctOption}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = GoldBright,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = question.explanation,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                GlowingGoldButton(
                                    text = strings.nextQuestion,
                                    onClick = onNextQuestion,
                                    modifier = Modifier.fillMaxWidth(),
                                    testTag = "quiz_next_question_btn"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
