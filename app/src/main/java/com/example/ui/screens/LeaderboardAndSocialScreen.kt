package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.AppNotification
import com.example.data.model.LeaderboardEntry
import com.example.ui.components.GlassCard
import com.example.ui.components.formatCoins
import com.example.ui.strings.LocalAppStrings
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun LeaderboardScreen(entries: List<LeaderboardEntry>) {
    val strings = LocalAppStrings.current
    var isMonthly by remember { mutableStateOf(false) }

    val sorted = remember(entries, isMonthly) {
        if (isMonthly) entries.sortedByDescending { it.monthlyScore }
        else entries.sortedByDescending { it.weeklyScore }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(strings.leaderboard, style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !isMonthly,
                    onClick = { isMonthly = false },
                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                ) { Text("Weekly Ranking") }
                SegmentedButton(
                    selected = isMonthly,
                    onClick = { isMonthly = true },
                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                ) { Text("Monthly Ranking") }
            }
        }

        if (sorted.isEmpty()) {
            item {
                GlassCard {
                    Text("No rankings yet. Answer Bihar Board quiz questions to appear on the live leaderboard!")
                }
            }
        } else {
            itemsIndexed(sorted, key = { _, item -> item.userId }) { idx, entry ->
                val rank = idx + 1
                GlassCard(
                    borderColor = if (rank <= 3) GoldPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "#$rank",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (rank <= 3) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.ExtraBold
                        )
                        if (entry.photoUrl.isNotBlank()) {
                            AsyncImage(
                                model = entry.photoUrl,
                                contentDescription = entry.username,
                                modifier = Modifier.size(40.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier.size(40.dp).clip(CircleShape).background(PurplePrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Person, contentDescription = null, tint = Color.White)
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.username, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "Coins: ${formatCoins(entry.totalCoinsEarned)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val scoreVal = if (isMonthly) entry.monthlyScore else entry.weeklyScore
                        Text(
                            text = String.format(Locale.US, "%.2f pts", scoreVal),
                            style = MaterialTheme.typography.labelLarge,
                            color = GoldBright
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationsScreen(
    notifications: List<AppNotification>,
    onMarkRead: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(strings.notifications, style = MaterialTheme.typography.headlineMedium)
        }
        if (notifications.isEmpty()) {
            item {
                GlassCard {
                    Text("No notifications yet.")
                }
            }
        } else {
            items(notifications, key = { it.id }) { n ->
                GlassCard(
                    onClick = { if (!n.readFlag) onMarkRead(n.id) },
                    borderColor = if (!n.readFlag) GoldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(n.title, style = MaterialTheme.typography.titleSmall, color = if (!n.readFlag) GoldBright else MaterialTheme.colorScheme.onSurface)
                        if (!n.readFlag) {
                            Badge(containerColor = GoldPrimary) { Text("NEW", color = Color.Black) }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(n.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun SupportAndLegalScreen() {
    var showHindi by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Help, Privacy & Terms", style = MaterialTheme.typography.headlineMedium)
            FilterChip(
                selected = showHindi,
                onClick = { showHindi = !showHindi },
                label = { Text(if (showHindi) "हिंदी" else "English") }
            )
        }

        GlassCard(borderColor = GoldPrimary.copy(alpha = 0.5f)) {
            Text(
                text = if (showHindi) "गोपनीयता नीति (Privacy Policy)" else "Privacy Policy",
                style = MaterialTheme.typography.titleLarge,
                color = GoldPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (showHindi) {
                    "1. डेटा संग्रह: हम केवल Google/फ़ोन प्रमाणीकरण प्रोफ़ाइल, क्विज़ प्रगति, वॉलेट लेन-देन और इनाम सबमिशन एकत्र करते हैं।\n" +
                    "2. खाता सुरक्षा: आपका डेटा Firebase Firestore में सुरक्षित है और लॉगआउट करने पर कभी नहीं हटाया जाता।\n" +
                    "3. विज्ञापन पुरस्कार: कॉइन्स केवल AdMob द्वारा पूर्ण विज्ञापन सत्यापन के बाद दिए जाते हैं।\n" +
                    "4. Instagram स्क्रीनशॉट: केवल मासिक इनाम भागीदारी सत्यापन के लिए उपयोग किए जाते हैं; हम कभी आपका पासवर्ड नहीं मांगते।\n" +
                    "5. डेटा हटाना: आप प्रोफ़ाइल से कभी भी अपना खाता और डेटा स्थायी रूप से हटा सकते हैं।"
                } else {
                    "1. Data Collection: We collect only your verified authentication profile, Bihar Board quiz progress, wallet ledger, and reward submissions.\n" +
                    "2. Account Security: All records persist securely in Firebase Firestore and are never erased on logout.\n" +
                    "3. Advertisement Rewards: Coins are credited strictly after Google AdMob confirms completed rewarded ad playback.\n" +
                    "4. Instagram Screenshots: Screenshots submitted for Monthly Rewards are reviewed solely for participation verification. We never request Instagram passwords.\n" +
                    "5. Data Retention & Deletion: You may permanently delete your account and cloud records at any time from Profile -> Delete Account."
                },
                style = MaterialTheme.typography.bodySmall
            )
        }

        GlassCard(borderColor = PurpleVibrant.copy(alpha = 0.5f)) {
            Text(
                text = if (showHindi) "नियम एवं शर्तें (Terms & Conditions)" else "Terms & Conditions",
                style = MaterialTheme.typography.titleLarge,
                color = PurpleVibrant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (showHindi) {
                    "• वेलकम बोनस (10 कॉइन्स) प्रति सत्यापित खाते केवल एक बार मिलता है।\n" +
                    "• दैनिक बोनस और फ्री स्पिन 24 घंटे में एक बार उपलब्ध हैं; डिवाइस समय बदलने पर भी नियम लागू रहते हैं।\n" +
                    "• भौतिक/UPI पुरस्कार स्टॉक, पात्रता और एडमिन सत्यापन के अधीन हैं। कोई गारंटीकृत नकद वादा नहीं किया जाता।\n" +
                    "• मासिक पुरस्कार सबमिशन के लिए क्विज़, डेली बोनस या स्पिन से अर्जित आवश्यक कॉइन्स होने जरूरी हैं।\n" +
                    "• धोखाधड़ी या डुप्लिकेट खातों को कारण सहित ब्लॉक किया जा सकता है।"
                } else {
                    "• Welcome Bonus (10 coins) is awarded strictly once per verified account.\n" +
                    "• Daily Bonus & Free Lucky Spin follow strict 24-hour cooldowns.\n" +
                    "• Store & Monthly rewards (Watches, Study Table, Smartphone, UPI) require earning the listed StudyWin coins and are subject to stock availability and verification. No real-money deposits are collected.\n" +
                    "• Fraudulent activity, ad manipulation, or duplicate accounts may result in documented suspension."
                },
                style = MaterialTheme.typography.bodySmall
            )
        }

        GlassCard(borderColor = CyanAccent.copy(alpha = 0.5f)) {
            Text("Contact Support & FAQ", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Support Email: studywin.india.support@example.com\nInstagram: @study__win__india\nResponse SLA: 24–48 Business Hours",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
