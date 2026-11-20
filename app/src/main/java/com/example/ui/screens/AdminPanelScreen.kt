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
import com.example.data.model.*
import com.example.data.remote.AdminOverviewStats
import com.example.ui.components.GlassCard
import com.example.ui.components.GlowingGoldButton
import com.example.ui.components.GlowingPurpleButton
import com.example.ui.components.formatCoins
import com.example.ui.theme.*

@Composable
fun AdminPanelScreen(
    isAdmin: Boolean,
    stats: AdminOverviewStats?,
    questions: List<QuizQuestion>,
    rewards: List<RewardCatalogItem>,
    submissions: List<RewardSubmission>,
    settings: AppSettings,
    onLoadStats: () -> Unit,
    onSaveQuestion: (QuizQuestion) -> Unit,
    onImportJson: (String) -> Unit,
    onDeleteQuestion: (String) -> Unit,
    onReviewSubmission: (String, String, String) -> Unit,
    onSaveReward: (RewardCatalogItem) -> Unit,
    onSaveSettings: (AppSettings) -> Unit,
    onBroadcastNotification: (String, String) -> Unit,
    onBlockUser: (String, Boolean, String) -> Unit
) {
    if (!isAdmin) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            GlassCard(borderColor = RubyError) {
                Text("Access Restricted", style = MaterialTheme.typography.headlineSmall, color = RubyError)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Admin access is verified server-side by Firestore security rules. Your account does not have admin privileges.")
            }
        }
        return
    }

    LaunchedEffect(Unit) { onLoadStats() }

    var activeTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Questions", "Ads & Config", "Submissions", "Catalog & Notify")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Admin Dashboard", style = MaterialTheme.typography.headlineMedium, color = GoldBright)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabs.forEachIndexed { idx, label ->
                    FilterChip(
                        selected = activeTab == idx,
                        onClick = { activeTab = idx },
                        label = { Text(label) },
                        modifier = Modifier.testTag("admin_tab_$idx")
                    )
                }
            }
        }

        when (activeTab) {
            0 -> {
                item {
                    GlassCard(borderColor = GoldPrimary.copy(alpha = 0.5f)) {
                        Text("Platform Metrics", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Total Users: ${stats?.totalUsers ?: 0}")
                        Text("Active Users: ${stats?.activeUsers ?: 0}")
                        Text("Coins Distributed: ${formatCoins(stats?.totalCoinsDistributed ?: 0L)}")
                        Text("Verified Rewarded Ads: ${stats?.totalAdRewardsVerified ?: 0}")
                        Spacer(modifier = Modifier.height(8.dp))
                        GlowingPurpleButton(text = "Refresh Stats", onClick = onLoadStats)
                    }
                }
                items(stats?.users ?: emptyList(), key = { it.userId }) { u ->
                    var reason by remember(u.userId) { mutableStateOf(u.blockReason) }
                    GlassCard {
                        Text("${u.displayName} (${u.email})", style = MaterialTheme.typography.titleSmall)
                        Text("Role: ${u.role} • Blocked: ${u.blocked}", style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(
                            value = reason,
                            onValueChange = { reason = it },
                            label = { Text("Block / Audit Reason") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = { onBlockUser(u.userId, !u.blocked, reason.ifBlank { "Policy review" }) },
                            colors = ButtonDefaults.buttonColors(containerColor = if (u.blocked) EmeraldSuccess else RubyError)
                        ) {
                            Text(if (u.blocked) "Unblock User" else "Block User")
                        }
                    }
                }
            }

            1 -> {
                item {
                    QuestionEditorCard(onSaveQuestion = onSaveQuestion, onImportJson = onImportJson)
                }
                items(questions.take(30), key = { it.id }) { q ->
                    GlassCard {
                        Text("Class ${q.questionClass} • ${q.subject} (${q.chapter})", style = MaterialTheme.typography.labelMedium, color = CyanAccent)
                        Text(q.questionText, style = MaterialTheme.typography.bodyMedium)
                        Text("Ans: ${q.correctOption}", style = MaterialTheme.typography.bodySmall, color = EmeraldSuccess)
                        TextButton(onClick = { onDeleteQuestion(q.id) }) {
                            Text("Delete", color = RubyError)
                        }
                    }
                }
            }

            2 -> {
                item {
                    AdAndBonusConfigCard(settings = settings, onSaveSettings = onSaveSettings)
                }
            }

            3 -> {
                items(submissions, key = { it.id }) { sub ->
                    var note by remember(sub.id) { mutableStateOf(sub.adminNote) }
                    GlassCard {
                        Text("${sub.rewardTitle} (${sub.submissionType})", style = MaterialTheme.typography.titleSmall)
                        Text("User: ${sub.username} • Status: ${sub.status}", style = MaterialTheme.typography.bodySmall, color = GoldPrimary)
                        if (sub.upiOrAddress.isNotBlank()) Text("Destination: ${sub.upiOrAddress}", style = MaterialTheme.typography.bodySmall)
                        if (sub.instagramHandle.isNotBlank()) Text("IG: ${sub.instagramHandle} (${sub.screenshotName})", style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("Review Note") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onReviewSubmission(sub.id, "APPROVED", note.ifBlank { "Verified" }) },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                            ) { Text("Approve") }
                            Button(
                                onClick = { onReviewSubmission(sub.id, "REJECTED", note.ifBlank { "Invalid proof" }) },
                                colors = ButtonDefaults.buttonColors(containerColor = RubyError)
                            ) { Text("Reject") }
                        }
                    }
                }
            }

            else -> {
                item {
                    BroadcastNotificationCard(onBroadcast = onBroadcastNotification)
                }
                items(rewards, key = { it.id }) { item ->
                    var costText by remember(item.id) { mutableStateOf(item.coinCost.toString()) }
                    var stockText by remember(item.id) { mutableStateOf(item.inventoryCount.toString()) }
                    GlassCard {
                        Text(item.title, style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = costText,
                                onValueChange = { costText = it.filter { c -> c.isDigit() } },
                                label = { Text("Coins") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = stockText,
                                onValueChange = { stockText = it.filter { c -> c.isDigit() } },
                                label = { Text("Stock") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        GlowingGoldButton(
                            text = "Update Reward",
                            onClick = {
                                onSaveReward(
                                    item.copy(
                                        coinCost = costText.toLongOrNull() ?: item.coinCost,
                                        inventoryCount = stockText.toLongOrNull() ?: item.inventoryCount
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionEditorCard(
    onSaveQuestion: (QuizQuestion) -> Unit,
    onImportJson: (String) -> Unit
) {
    var qClass by remember { mutableStateOf("10") }
    var subject by remember { mutableStateOf("Mathematics") }
    var chapter by remember { mutableStateOf("") }
    var questionText by remember { mutableStateOf("") }
    var optA by remember { mutableStateOf("") }
    var optB by remember { mutableStateOf("") }
    var optC by remember { mutableStateOf("") }
    var optD by remember { mutableStateOf("") }
    var correct by remember { mutableStateOf("A") }
    var explanation by remember { mutableStateOf("") }
    var jsonImportText by remember { mutableStateOf("") }

    GlassCard(borderColor = GoldPrimary.copy(alpha = 0.5f)) {
        Text("Add Bihar Board Question", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = qClass, onValueChange = { qClass = it }, label = { Text("Class (9-12)") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Subject") }, modifier = Modifier.weight(1.5f))
        }
        OutlinedTextField(value = chapter, onValueChange = { chapter = it }, label = { Text("Chapter") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = questionText, onValueChange = { questionText = it }, label = { Text("Question") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = optA, onValueChange = { optA = it }, label = { Text("Option A") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = optB, onValueChange = { optB = it }, label = { Text("Option B") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = optC, onValueChange = { optC = it }, label = { Text("Option C") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = optD, onValueChange = { optD = it }, label = { Text("Option D") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = correct, onValueChange = { correct = it.uppercase().take(1) }, label = { Text("Correct (A-D)") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = explanation, onValueChange = { explanation = it }, label = { Text("Explanation") }, modifier = Modifier.weight(2f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        GlowingGoldButton(
            text = "Save Question",
            onClick = {
                onSaveQuestion(
                    QuizQuestion(
                        questionClass = qClass.toIntOrNull()?.coerceIn(9, 12) ?: 10,
                        subject = subject,
                        chapter = chapter.ifBlank { "General" },
                        questionText = questionText,
                        optionA = optA,
                        optionB = optB,
                        optionC = optC,
                        optionD = optD,
                        correctOption = correct.ifBlank { "A" },
                        explanation = explanation.ifBlank { "Verified Bihar Board syllabus." }
                    )
                )
                questionText = ""; optA = ""; optB = ""; optC = ""; optD = ""
            },
            enabled = questionText.isNotBlank() && optA.isNotBlank() && optB.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        Text("Bulk Dataset JSON Import (1,000+ Questions Target)", style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(
            value = jsonImportText,
            onValueChange = { jsonImportText = it },
            label = { Text("Paste JSON Array of Questions") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(6.dp))
        GlowingPurpleButton(
            text = "Import JSON Dataset",
            onClick = {
                onImportJson(jsonImportText)
                jsonImportText = ""
            },
            enabled = jsonImportText.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AdAndBonusConfigCard(
    settings: AppSettings,
    onSaveSettings: (AppSettings) -> Unit
) {
    var appId by remember(settings) { mutableStateOf(settings.adMobAppId) }
    var bannerId by remember(settings) { mutableStateOf(settings.bannerAdUnitId) }
    var rewardedId by remember(settings) { mutableStateOf(settings.rewardedAdUnitId) }
    var interstitialId by remember(settings) { mutableStateOf(settings.interstitialAdUnitId) }
    var useTestAds by remember(settings) { mutableStateOf(settings.useTestAds) }
    var igUrl by remember(settings) { mutableStateOf(settings.instagramUrl) }
    var spinWeights by remember(settings) { mutableStateOf(settings.spinWeightsCsv) }

    GlassCard(borderColor = CyanAccent.copy(alpha = 0.5f)) {
        Text("AdMob & Reward Configuration", style = MaterialTheme.typography.titleMedium)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Use Official Google Test Ads (Dev Mode)")
            Switch(checked = useTestAds, onCheckedChange = { useTestAds = it })
        }
        OutlinedTextField(value = appId, onValueChange = { appId = it }, label = { Text("AdMob App ID") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = bannerId, onValueChange = { bannerId = it }, label = { Text("Banner Ad Unit ID") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = rewardedId, onValueChange = { rewardedId = it }, label = { Text("Rewarded Ad Unit ID") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = interstitialId, onValueChange = { interstitialId = it }, label = { Text("Interstitial Ad Unit ID") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = igUrl, onValueChange = { igUrl = it }, label = { Text("Instagram Page URL") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = spinWeights, onValueChange = { spinWeights = it }, label = { Text("Spin Weights (10 values CSV)") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(10.dp))
        GlowingGoldButton(
            text = "Save Ad & Config Settings",
            onClick = {
                onSaveSettings(
                    settings.copy(
                        adMobAppId = appId,
                        bannerAdUnitId = bannerId,
                        rewardedAdUnitId = rewardedId,
                        interstitialAdUnitId = interstitialId,
                        useTestAds = useTestAds,
                        instagramUrl = igUrl,
                        spinWeightsCsv = spinWeights
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun BroadcastNotificationCard(onBroadcast: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    GlassCard(borderColor = PurpleVibrant.copy(alpha = 0.5f)) {
        Text("Broadcast Notification", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("Message") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        GlowingPurpleButton(
            text = "Send to All Users",
            onClick = {
                onBroadcast(title, body)
                title = ""
                body = ""
            },
            enabled = title.isNotBlank() && body.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
