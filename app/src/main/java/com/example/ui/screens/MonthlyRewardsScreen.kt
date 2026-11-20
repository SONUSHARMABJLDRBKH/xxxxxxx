package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Announcement
import com.example.data.model.AppSettings
import com.example.data.model.RewardSubmission
import com.example.data.model.Wallet
import com.example.ui.components.GlassCard
import com.example.ui.components.GlowingGoldButton
import com.example.ui.components.GlowingPurpleButton
import com.example.ui.strings.LocalAppStrings
import com.example.ui.theme.*

data class MonthlyRewardOption(
    val title: String,
    val imageKey: String,
    val coinCost: Long
)

private val MONTHLY_CATEGORIES = listOf(
    MonthlyRewardOption("₹200 UPI Reward", "upi", 12_000L),
    MonthlyRewardOption("Normal Watch", "watch", 30_000L),
    MonthlyRewardOption("Smartwatch", "smartwatch", 50_000L),
    MonthlyRewardOption("Study Table", "study_table", 85_000L),
    MonthlyRewardOption("Smartphone", "smartphone", 250_000L)
)

@Composable
fun MonthlyRewardsScreen(
    announcements: List<Announcement>,
    submissions: List<RewardSubmission>,
    settings: AppSettings,
    wallet: Wallet?,
    onSubmitParticipation: (category: String, coinCost: Long, igHandle: String, screenshotName: String) -> Unit
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val primaryAnnouncement = announcements.firstOrNull()
    val instagramUrl = primaryAnnouncement?.instagramUrl?.ifBlank { settings.instagramUrl }
        ?: settings.instagramUrl

    val availableCoins = wallet?.availableCoins ?: 0L
    var selectedOption by remember { mutableStateOf(MONTHLY_CATEGORIES.first()) }
    var igHandle by remember { mutableStateOf("") }
    var selectedScreenshotName by remember { mutableStateOf("") }
    val hasEnoughCoins = availableCoins >= selectedOption.coinCost

    // Zero-permission Android Photo Picker (Google Play policy compliant)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let {
                selectedScreenshotName = it.lastPathSegment ?: "screenshot_${System.currentTimeMillis()}.jpg"
            }
        }
    )

    val monthlySubs = remember(submissions) {
        submissions.filter { it.submissionType == "MONTHLY_PARTICIPATION" }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(strings.monthlyRewards, style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "Earn required StudyWin coins via Quizzes, Daily Bonus & Spin to unlock and submit a Monthly Reward.",
                style = MaterialTheme.typography.bodySmall,
                color = GoldBright
            )
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = GoldContainerDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.55f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Your Coin Balance", style = MaterialTheme.typography.labelLarge, color = Color.White)
                    Text("$availableCoins Coins", style = MaterialTheme.typography.titleMedium, color = GoldBright, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        if (primaryAnnouncement != null) {
            item {
                GlassCard(borderColor = GoldPrimary.copy(alpha = 0.6f)) {
                    Text(primaryAnnouncement.title, style = MaterialTheme.typography.titleLarge, color = GoldBright)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(primaryAnnouncement.message, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Closing: ${primaryAnnouncement.closingDate}", style = MaterialTheme.typography.labelMedium)
                        Text("Result: ${primaryAnnouncement.resultDate}", style = MaterialTheme.typography.labelMedium, color = CyanAccent)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Slots: ${primaryAnnouncement.availableSlots} • Verified Submissions: ${monthlySubs.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (primaryAnnouncement.winnerNames.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(color = PurpleContainerDark, shape = RoundedCornerShape(10.dp)) {
                            Text(
                                text = "Winners & Announcements: ${primaryAnnouncement.winnerNames}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                modifier = Modifier.padding(10.dp).fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        // Coin-Gated Categories + Instagram Verification + Screenshot Upload
        item {
            GlassCard(borderColor = PurpleVibrant.copy(alpha = 0.55f)) {
                Text("Select Monthly Reward Category (Coins Required)", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                MONTHLY_CATEGORIES.forEach { option ->
                    val unlocked = availableCoins >= option.coinCost
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RadioButton(
                            selected = selectedOption.title == option.title,
                            onClick = { selectedOption = option }
                        )
                        Box(modifier = Modifier.width(64.dp).height(46.dp)) {
                            RewardProductArtwork(imageKey = option.imageKey, title = option.title, modifier = Modifier.fillMaxSize())
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(option.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = if (unlocked) "${option.coinCost} Coins • Unlocked" else "${option.coinCost} Coins • Need ${option.coinCost - availableCoins} more",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (unlocked) EmeraldSuccess else AmberWarning
                            )
                        }
                    }
                }

                if (!hasEnoughCoins) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = RubyContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Not enough coins for ${selectedOption.title}. Required: ${selectedOption.coinCost} coins (You have $availableCoins coins). Earn more coins via Quizzes, Scratch Cards, or Spin first.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Follow StudyWin India on Instagram (never share your Instagram password) and attach your confirmation screenshot along with the required coins.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(instagramUrl))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("open_instagram_btn")
                ) {
                    Icon(Icons.Rounded.OpenInNew, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Official Instagram Page")
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = igHandle,
                    onValueChange = { igHandle = it },
                    label = { Text("Your Instagram Username (@handle)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                GlowingPurpleButton(
                    text = if (selectedScreenshotName.isBlank()) "Pick Screenshot (Photo Picker)" else "Selected: $selectedScreenshotName",
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "pick_screenshot_btn"
                )
                Spacer(modifier = Modifier.height(10.dp))
                GlowingGoldButton(
                    text = if (hasEnoughCoins) {
                        "Submit with ${selectedOption.coinCost} Coins"
                    } else {
                        "Need ${selectedOption.coinCost} Coins to Submit"
                    },
                    onClick = {
                        onSubmitParticipation(
                            selectedOption.title,
                            selectedOption.coinCost,
                            igHandle,
                            selectedScreenshotName
                        )
                        igHandle = ""
                        selectedScreenshotName = ""
                    },
                    enabled = hasEnoughCoins && igHandle.isNotBlank() && selectedScreenshotName.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "submit_monthly_btn"
                )
            }
        }

        if (monthlySubs.isNotEmpty()) {
            item { Text("Your Monthly Submissions", style = MaterialTheme.typography.titleMedium) }
            items(monthlySubs, key = { it.id }) { sub ->
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(sub.rewardTitle, style = MaterialTheme.typography.titleSmall)
                            Text("${sub.coinCost} Coins • IG: ${sub.instagramHandle}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        val statusColor = when (sub.status) {
                            "APPROVED", "FULFILLED" -> EmeraldSuccess
                            "REJECTED" -> RubyError
                            else -> AmberWarning
                        }
                        Text(sub.status, style = MaterialTheme.typography.labelLarge, color = statusColor, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
