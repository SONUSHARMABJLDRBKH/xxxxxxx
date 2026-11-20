package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import coil.compose.AsyncImage
import com.example.data.model.QuizScoreSummary
import com.example.data.model.UserProfile
import com.example.data.model.Wallet
import com.example.ui.components.GlassCard
import com.example.ui.components.GlowingGoldButton
import com.example.ui.components.formatCoins
import com.example.ui.strings.LocalAppStrings
import com.example.ui.theme.*
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ProfileScreen(
    profile: UserProfile?,
    wallet: Wallet?,
    score: QuizScoreSummary,
    isAdmin: Boolean,
    onLanguageChange: (String) -> Unit,
    onDarkModeChange: (Boolean) -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    onLinkPhone: (String) -> Unit,
    onOpenSupport: () -> Unit,
    onOpenAdmin: () -> Unit,
    onDeleteAccount: suspend () -> Result<Unit>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strings = LocalAppStrings.current
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPhoneDialog by remember { mutableStateOf(false) }
    var phoneInput by remember { mutableStateOf("") }
    var deleteError by remember { mutableStateOf<String?>(null) }

    val regDate = remember(profile?.createdAt) {
        profile?.createdAt?.toDate()?.let {
            SimpleDateFormat("dd MMM yyyy", Locale.US).format(it)
        } ?: "Active"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        GlassCard(borderColor = GoldPrimary.copy(alpha = 0.6f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (!profile?.photoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = profile?.photoUrl,
                        contentDescription = "Profile avatar",
                        modifier = Modifier.size(64.dp).clip(CircleShape).border(2.dp, GoldPrimary, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.size(64.dp).clip(CircleShape).background(PurplePrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(profile?.displayName ?: "Student", style = MaterialTheme.typography.titleLarge)
                    Text(profile?.email ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("UID: ${profile?.userId?.take(12) ?: "-"} • Joined: $regDate", style = MaterialTheme.typography.bodySmall, color = CyanAccent)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Wallet: ${formatCoins(wallet?.availableCoins ?: 0L)} Coins", color = GoldPrimary, style = MaterialTheme.typography.labelLarge)
                Text("Quiz Score: ${String.format(Locale.US, "%.2f", score.overallScore)}", color = EmeraldSuccess, style = MaterialTheme.typography.labelLarge)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (profile?.phoneVerified == true) "Verified Mobile: ${profile.phoneNumber}" else "Mobile (+91): Not linked",
                    style = MaterialTheme.typography.bodySmall
                )
                TextButton(onClick = { showPhoneDialog = true }) {
                    Text(if (profile?.phoneVerified == true) "Update" else "Verify +91")
                }
            }
        }

        // Settings
        GlassCard(borderColor = PurpleVibrant.copy(alpha = 0.5f)) {
            Text("Preferences & Settings", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(strings.languageLabel)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = (profile?.language ?: "en") == "en",
                        onClick = { onLanguageChange("en") },
                        label = { Text("English") }
                    )
                    FilterChip(
                        selected = profile?.language == "hi",
                        onClick = { onLanguageChange("hi") },
                        label = { Text("हिंदी") }
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(strings.themeLabel)
                Switch(
                    checked = profile?.darkMode ?: true,
                    onCheckedChange = onDarkModeChange,
                    modifier = Modifier.testTag("dark_mode_switch")
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(strings.notifications)
                Switch(
                    checked = profile?.notificationsEnabled ?: true,
                    onCheckedChange = onNotificationsChange
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            TextButton(onClick = onOpenSupport, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Policy, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("${strings.privacyPolicy} • ${strings.termsConditions} • ${strings.helpSupport}")
            }

            if (isAdmin) {
                GlowingGoldButton(
                    text = strings.adminPanel,
                    onClick = onOpenAdmin,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "profile_admin_btn"
                )
            }
        }

        // Logout & Delete Account
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        withContext(NonCancellable) {
                            try {
                                credentialManager.clearCredentialState(ClearCredentialStateRequest())
                            } catch (_: Exception) { }
                            Firebase.auth.signOut()
                        }
                    }
                },
                modifier = Modifier.weight(1f).height(50.dp).testTag("logout_button")
            ) {
                Icon(Icons.Rounded.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.logout)
            }

            Button(
                onClick = { showDeleteDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = RubyError),
                modifier = Modifier.weight(1f).height(50.dp).testTag("delete_account_button")
            ) {
                Icon(Icons.Rounded.DeleteForever, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.deleteAccount, maxLines = 1)
            }
        }

        if (!deleteError.isNullOrBlank()) {
            Text(deleteError!!, color = RubyError, style = MaterialTheme.typography.bodySmall)
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirm Account Deletion") },
            text = {
                Text("This permanently erases your StudyWin India profile, wallet balance, and quiz scoreboard. Logout preserves your data; Delete Account cannot be undone.")
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = RubyError),
                    onClick = {
                        showDeleteDialog = false
                        scope.launch {
                            onDeleteAccount()
                                .onSuccess {
                                    withContext(NonCancellable) {
                                        try { credentialManager.clearCredentialState(ClearCredentialStateRequest()) } catch (_: Exception) {}
                                        Firebase.auth.signOut()
                                    }
                                }
                                .onFailure { deleteError = it.message ?: "Could not delete account" }
                        }
                    }
                ) { Text("Delete Permanently") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    if (showPhoneDialog) {
        AlertDialog(
            onDismissRequest = { showPhoneDialog = false },
            title = { Text("Link Mobile Number (+91)") },
            text = {
                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = { if (it.length <= 10 && it.all { ch -> ch.isDigit() }) phoneInput = it },
                    label = { Text("10-digit Mobile Number") },
                    prefix = { Text("+91 ") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onLinkPhone("+91$phoneInput")
                        showPhoneDialog = false
                    },
                    enabled = phoneInput.length == 10
                ) { Text("Save Number") }
            },
            dismissButton = {
                TextButton(onClick = { showPhoneDialog = false }) { Text(strings.cancel) }
            }
        )
    }
}
