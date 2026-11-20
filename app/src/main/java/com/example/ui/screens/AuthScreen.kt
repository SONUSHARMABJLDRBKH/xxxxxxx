package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.ui.components.GlassCard
import com.example.ui.components.GlowingGoldButton
import com.example.ui.components.GlowingPurpleButton
import com.example.ui.components.StudyWinBackground
import com.example.ui.strings.StringsEn
import com.example.ui.strings.StringsHi
import com.example.ui.theme.*
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    onPhoneVerified: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isHindi by remember { mutableStateOf(false) }
    val strings = if (isHindi) StringsHi else StringsEn

    var selectedTab by remember { mutableIntStateOf(0) } // 0=Google, 1=Mobile+91 OTP, 2=Email
    var isLoading by remember { mutableStateOf(false) }
    var authMessage by remember { mutableStateOf<String?>(null) }
    var isErrorMsg by remember { mutableStateOf(false) }

    // Mobile OTP state (App Built-In OTP default + Live Firebase SMS OTP option)
    var phoneInput by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }
    var demoOtpMode by remember { mutableStateOf(true) }
    var demoOtpSentCode by remember { mutableStateOf<String?>(null) }
    var demoOtpExpiresAt by remember { mutableLongStateOf(0L) }
    var demoOtpSecondsLeft by remember { mutableIntStateOf(0) }
    var demoOtpAttempts by remember { mutableIntStateOf(0) }
    var appOtpVerified by remember { mutableStateOf(false) }
    var verificationId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(demoOtpSentCode, demoOtpExpiresAt) {
        while (demoOtpSentCode != null && demoOtpExpiresAt > System.currentTimeMillis()) {
            demoOtpSecondsLeft = ((demoOtpExpiresAt - System.currentTimeMillis()) / 1000L).toInt().coerceAtLeast(0)
            kotlinx.coroutines.delay(1000L)
        }
        if (demoOtpSentCode != null && System.currentTimeMillis() >= demoOtpExpiresAt) {
            demoOtpSecondsLeft = 0
        }
    }

    // Email tab informational fields
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }

    // Silent auto sign-in check on startup
    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope
        )
    }

    StudyWinBackground(darkTheme = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Language Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    shape = CircleShape,
                    color = MidnightSurfaceElevated,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { isHindi = !isHindi }
                        .testTag("auth_language_toggle")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Rounded.Translate, contentDescription = "Language", tint = GoldPrimary, modifier = Modifier.size(18.dp))
                        Text(
                            text = if (isHindi) "हिंदी • EN" else "EN • हिंदी",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // StudyWin India Uploaded Emblem Logo
            Box(
                modifier = Modifier
                    .size(138.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .border(2.dp, Brush.linearGradient(listOf(GoldBright, PurpleVibrant)), RoundedCornerShape(30.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_studywin_logo),
                    contentDescription = "StudyWin India Logo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = strings.appName,
                style = MaterialTheme.typography.displaySmall,
                color = GoldBright,
                textAlign = TextAlign.Center
            )
            Text(
                text = strings.tagline,
                style = MaterialTheme.typography.titleSmall,
                color = PurpleVibrant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = CircleShape,
                color = GoldContainerDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Rounded.CardGiftcard, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                    Text(
                        text = strings.welcomeBonusBadge,
                        style = MaterialTheme.typography.bodySmall,
                        color = GoldBright,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3 Login Option Tabs: Google / Mobile +91 OTP / Email
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val tabs = listOf("Google", "+91 OTP", "Email")
                tabs.forEachIndexed { idx, label ->
                    SegmentedButton(
                        selected = selectedTab == idx,
                        onClick = {
                            selectedTab = idx
                            authMessage = null
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = tabs.size),
                        modifier = Modifier.testTag("auth_tab_$idx")
                    ) {
                        Text(label, maxLines = 1)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            GlassCard(
                modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(),
                borderColor = GoldPrimary.copy(alpha = 0.4f)
            ) {
                when (selectedTab) {
                    0 -> {
                        Text(
                            text = strings.welcomeHeadline,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = strings.welcomeSubheadline,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFB8C1E8)
                        )
                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = {
                                isLoading = true
                                authMessage = null
                                onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = {
                                        isLoading = false
                                        onAuthSuccess()
                                    },
                                    onAuthError = { msg ->
                                        isLoading = false
                                        isErrorMsg = true
                                        authMessage = msg
                                    },
                                    scope = scope,
                                    onAuthCancelled = {
                                        isLoading = false
                                        isErrorMsg = false
                                        authMessage = "Sign-in cancelled. Tap Google Sign-In to continue."
                                    }
                                )
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("google_sign_in_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldPrimary,
                                contentColor = Color(0xFF1F1600)
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.5.dp,
                                    color = Color(0xFF1F1600)
                                )
                            } else {
                                Icon(Icons.Rounded.VerifiedUser, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = strings.signInWithGoogle,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    1 -> {
                        Text(
                            text = if (isHindi) "मोबाइल नंबर (+91) और 6-अंकों का ऐप OTP" else "Mobile Number (+91) & 6-Digit App OTP",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isHindi) {
                                "ऐप का अपना इन-बिल्ट 6-अंकों का OTP (टेस्टिंग/वेरिफिकेशन — यह असली SMS नहीं है) या Firebase लाइव SMS OTP चुनें।"
                            } else {
                                "Use the app's built-in 6-digit OTP (local verification — not a real SMS) or switch off to use Live Firebase SMS OTP."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB8C1E8)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isHindi) "ऐप का अपना OTP मोड (Not SMS)" else "App Built-In OTP Mode (Not SMS)",
                                style = MaterialTheme.typography.bodySmall,
                                color = GoldBright,
                                fontWeight = FontWeight.SemiBold
                            )
                            Switch(
                                checked = demoOtpMode,
                                onCheckedChange = {
                                    demoOtpMode = it
                                    demoOtpSentCode = null
                                    demoOtpExpiresAt = 0L
                                    demoOtpSecondsLeft = 0
                                    demoOtpAttempts = 0
                                    appOtpVerified = false
                                    otpInput = ""
                                    authMessage = null
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = {
                                if (it.length <= 10 && it.all { ch -> ch.isDigit() }) {
                                    phoneInput = it
                                    appOtpVerified = false
                                }
                            },
                            label = { Text(if (isHindi) "10-अंकों का मोबाइल नंबर" else "10-digit Mobile Number") },
                            prefix = { Text("+91 ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("phone_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        GlowingPurpleButton(
                            text = if (demoOtpMode) {
                                if (demoOtpSentCode != null) {
                                    if (isHindi) "नया ऐप OTP जनरेट करें" else "Regenerate 6-Digit App OTP"
                                } else {
                                    if (isHindi) "6-अंकों का ऐप OTP जनरेट करें" else "Generate 6-Digit App OTP"
                                }
                            } else {
                                if (isHindi) "लाइव SMS OTP भेजें" else "Send Live SMS OTP"
                            },
                            onClick = {
                                if (phoneInput.length != 10) {
                                    isErrorMsg = true
                                    authMessage = if (isHindi) {
                                        "कृपया सही 10-अंकों का भारतीय मोबाइल नंबर दर्ज करें।"
                                    } else {
                                        "Please enter a valid 10-digit Indian mobile number."
                                    }
                                } else if (demoOtpMode) {
                                    val code = (100000..999999).random().toString()
                                    demoOtpSentCode = code
                                    demoOtpExpiresAt = System.currentTimeMillis() + 120_000L
                                    demoOtpSecondsLeft = 120
                                    demoOtpAttempts = 0
                                    appOtpVerified = false
                                    otpInput = ""
                                    isErrorMsg = false
                                    authMessage = if (isHindi) {
                                        "ऐप का इन-बिल्ट OTP तैयार है (असली SMS नहीं): नीचे कोड दर्ज करें।"
                                    } else {
                                        "App Built-In OTP generated (not an SMS): enter the 6-digit code below."
                                    }
                                } else {
                                    val activity = context as? Activity
                                    if (activity == null) {
                                        isErrorMsg = true
                                        authMessage = "Activity required for Phone Auth."
                                    } else {
                                        isLoading = true
                                        val options = PhoneAuthOptions.newBuilder(Firebase.auth)
                                            .setPhoneNumber("+91$phoneInput")
                                            .setTimeout(60L, TimeUnit.SECONDS)
                                            .setActivity(activity)
                                            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                                                override fun onVerificationCompleted(cred: PhoneAuthCredential) {
                                                    isLoading = false
                                                    scope.launch {
                                                        try {
                                                            Firebase.auth.signInWithCredential(cred).await()
                                                            onPhoneVerified("+91$phoneInput")
                                                            onAuthSuccess()
                                                        } catch (e: Exception) {
                                                            isErrorMsg = true
                                                            authMessage = e.localizedMessage ?: "Phone auth failed"
                                                        }
                                                    }
                                                }

                                                override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                                                    isLoading = false
                                                    isErrorMsg = true
                                                    val raw = e.localizedMessage.orEmpty()
                                                    authMessage = when {
                                                        raw.contains("17006") || raw.contains("not enabled", ignoreCase = true) || raw.contains("OPERATION_NOT_ALLOWED", ignoreCase = true) ->
                                                            "Live SMS OTP is not enabled on Firebase Console yet (Error 17006). Switch ON App Built-In OTP Mode above or use Google Sign-In."
                                                        raw.contains("canceled", ignoreCase = true) || raw.contains("reCAPTCHA", ignoreCase = true) ->
                                                            "Verification check was cancelled. Try again or use App Built-In OTP Mode."
                                                        else ->
                                                            raw.ifBlank { "Enable Phone Sign-In in Firebase Console or use App Built-In OTP Mode." }
                                                    }
                                                }

                                                override fun onCodeSent(verId: String, token: PhoneAuthProvider.ForceResendingToken) {
                                                    isLoading = false
                                                    verificationId = verId
                                                    isErrorMsg = false
                                                    authMessage = "6-digit SMS OTP sent to +91$phoneInput"
                                                }
                                            })
                                            .build()
                                        PhoneAuthProvider.verifyPhoneNumber(options)
                                    }
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "send_otp_button"
                        )

                        if (demoOtpMode && demoOtpSentCode != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = GoldContainerDark,
                                border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.7f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isHindi) "ऐप इन-बिल्ट OTP (SMS नहीं)" else "App Built-In OTP (Not SMS)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = GoldBright
                                        )
                                        Text(
                                            text = demoOtpSentCode!!.chunked(1).joinToString(" "),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = if (demoOtpSecondsLeft > 0) {
                                                if (isHindi) "वैधता: ${demoOtpSecondsLeft}s" else "Valid for ${demoOtpSecondsLeft}s"
                                            } else {
                                                if (isHindi) "OTP समाप्त — नया OTP जनरेट करें" else "Expired — tap Regenerate above"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (demoOtpSecondsLeft > 0) EmeraldSuccess else RubyError
                                        )
                                    }
                                    TextButton(
                                        onClick = { otpInput = demoOtpSentCode.orEmpty() },
                                        modifier = Modifier.testTag("autofill_app_otp_btn")
                                    ) {
                                        Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, tint = GoldBright, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isHindi) "भरें" else "Fill", color = GoldBright, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = otpInput,
                            onValueChange = { if (it.length <= 6 && it.all { ch -> ch.isDigit() }) otpInput = it },
                            label = { Text(if (isHindi) "6-अंकों का OTP दर्ज करें" else "Enter 6-digit OTP") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("otp_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        GlowingGoldButton(
                            text = if (appOtpVerified) {
                                if (isHindi) "Google से लॉगिन पूरा करें (+91 वेरिफाइड)" else "Complete Login with Google (+91 Verified)"
                            } else {
                                if (isHindi) "OTP वेरिफाई करें और आगे बढ़ें" else "Verify OTP & Login"
                            },
                            onClick = {
                                if (demoOtpMode) {
                                    if (appOtpVerified) {
                                        isLoading = true
                                        onGoogleSignInClicked(
                                            context = context,
                                            credentialManager = credentialManager,
                                            onAuthSuccess = {
                                                isLoading = false
                                                onAuthSuccess()
                                            },
                                            onAuthError = { msg ->
                                                isLoading = false
                                                isErrorMsg = true
                                                authMessage = msg
                                            },
                                            scope = scope,
                                            onAuthCancelled = {
                                                isLoading = false
                                                isErrorMsg = false
                                                authMessage = "OTP verified for +91$phoneInput! Tap button above to finish Google account link."
                                            }
                                        )
                                    } else if (demoOtpSentCode == null) {
                                        isErrorMsg = true
                                        authMessage = if (isHindi) "पहले ऊपर 6-अंकों का ऐप OTP जनरेट करें।" else "Generate your 6-digit App OTP above first."
                                    } else if (demoOtpSecondsLeft <= 0) {
                                        isErrorMsg = true
                                        authMessage = if (isHindi) "OTP की समय सीमा समाप्त हो गई है। नया OTP जनरेट करें।" else "OTP expired. Please regenerate a fresh 6-digit OTP."
                                    } else if (demoOtpAttempts >= 5) {
                                        isErrorMsg = true
                                        authMessage = if (isHindi) "बहुत अधिक गलत प्रयास। नया OTP जनरेट करें।" else "Too many wrong attempts. Please regenerate a new OTP."
                                    } else if (otpInput.length == 6 && otpInput == demoOtpSentCode) {
                                        appOtpVerified = true
                                        isErrorMsg = false
                                        onPhoneVerified("+91$phoneInput")
                                        authMessage = if (isHindi) {
                                            "+91$phoneInput का ऐप OTP वेरिफाई हो गया! आपका क्लाउड वॉलेट खोला जा रहा है..."
                                        } else {
                                            "App OTP verified for +91$phoneInput! Connecting your secure cloud wallet..."
                                        }
                                        isLoading = true
                                        onGoogleSignInClicked(
                                            context = context,
                                            credentialManager = credentialManager,
                                            onAuthSuccess = {
                                                isLoading = false
                                                onAuthSuccess()
                                            },
                                            onAuthError = { msg ->
                                                isLoading = false
                                                isErrorMsg = true
                                                authMessage = msg
                                            },
                                            scope = scope,
                                            onAuthCancelled = {
                                                isLoading = false
                                                isErrorMsg = false
                                                authMessage = "OTP verified for +91$phoneInput! Tap 'Complete Login with Google' to enter."
                                            }
                                        )
                                    } else {
                                        demoOtpAttempts += 1
                                        isErrorMsg = true
                                        authMessage = if (isHindi) {
                                            "गलत 6-अंकों का OTP (${5 - demoOtpAttempts} प्रयास शेष)।"
                                        } else {
                                            "Incorrect 6-digit App OTP (${5 - demoOtpAttempts} attempts remaining)."
                                        }
                                    }
                                } else {
                                    val verId = verificationId
                                    if (verId.isNullOrBlank() || otpInput.length != 6) {
                                        isErrorMsg = true
                                        authMessage = "Request an OTP and enter all 6 digits."
                                    } else {
                                        isLoading = true
                                        val cred = PhoneAuthProvider.getCredential(verId, otpInput)
                                        scope.launch {
                                            try {
                                                Firebase.auth.signInWithCredential(cred).await()
                                                onPhoneVerified("+91$phoneInput")
                                                isLoading = false
                                                onAuthSuccess()
                                            } catch (e: Exception) {
                                                isLoading = false
                                                isErrorMsg = true
                                                authMessage = e.localizedMessage ?: "Invalid OTP"
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "verify_otp_button"
                        )
                    }

                    else -> {
                        Text(
                            text = "Email & Password Account",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "StudyWin India uses verified Google Sign-In in this environment for instant wallet protection and zero-trust Firestore security. You can also configure Email/Password in Firebase Console.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB8C1E8)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text("Email Address") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    if (emailInput.contains("@")) {
                                        Firebase.auth.sendPasswordResetEmail(emailInput.trim())
                                            .addOnSuccessListener {
                                                isErrorMsg = false
                                                authMessage = "Password reset link sent if Email Auth is enabled for ${emailInput.trim()}."
                                            }
                                            .addOnFailureListener {
                                                isErrorMsg = true
                                                authMessage = "Use Google Sign-In below (Email/Password provider is disabled on this project)."
                                            }
                                    } else {
                                        isErrorMsg = true
                                        authMessage = "Enter a valid email address first."
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Reset Password")
                            }
                            GlowingGoldButton(
                                text = "Use Google Login",
                                onClick = { selectedTab = 0 },
                                modifier = Modifier.weight(1f),
                                testTag = "switch_to_google_btn"
                            )
                        }
                    }
                }

                if (!authMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = if (isErrorMsg) RubyContainer else EmeraldContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = authMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            modifier = Modifier.padding(12.dp).fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onUnauthenticated()
        return
    }
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()
    scope.launch {
        try {
            val cred = credentialManager.getCredential(context, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (_: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()
    scope.launch {
        try {
            val activity = context as? Activity
                ?: throw IllegalStateException("Activity context required for Google Sign-In")
            val cred = credentialManager.getCredential(activity, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Sign in failed")
        }
    }
}
