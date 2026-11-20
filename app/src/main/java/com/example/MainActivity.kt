package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ads.AdBannerFooter
import com.example.ads.AdMobManager
import com.example.data.local.StudyWinDatabase
import com.example.data.remote.QuizAndRewardsRepository
import com.example.data.remote.UserWalletRepository
import com.example.ui.StudyWinViewModel
import com.example.ui.components.CoinRainOverlay
import com.example.ui.components.StudyWinBackground
import com.example.ui.screens.*
import com.example.ui.strings.LocalAppStrings
import com.example.ui.strings.StringsEn
import com.example.ui.strings.StringsHi
import com.example.ui.theme.StudyWinTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AppContent()
        }
    }
}

@Composable
fun AppContent() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var pendingVerifiedPhone by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    if (currentUser == null) {
        StudyWinTheme(darkTheme = true) {
            AuthScreen(
                onAuthSuccess = { currentUser = Firebase.auth.currentUser },
                onPhoneVerified = { phone -> pendingVerifiedPhone = phone }
            )
        }
    } else {
        val uid = currentUser!!.uid
        key(uid) {
            AuthenticatedStudyWinShell(
                currentUserId = uid,
                pendingVerifiedPhone = pendingVerifiedPhone,
                onClearPendingPhone = { pendingVerifiedPhone = null }
            )
        }
    }
}

@Composable
private fun AuthenticatedStudyWinShell(
    currentUserId: String,
    pendingVerifiedPhone: String? = null,
    onClearPendingPhone: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: StudyWinViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val dao = StudyWinDatabase.getInstance(app).studyWinDao()
                val userWalletRepo = UserWalletRepository(app, dao)
                val quizRewardsRepo = QuizAndRewardsRepository(app, dao)
                StudyWinViewModel(userWalletRepo, quizRewardsRepo, currentUserId)
            }
        }
    )

    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val wallet by viewModel.wallet.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val questions by viewModel.questions.collectAsStateWithLifecycle()
    val quizScore by viewModel.quizScore.collectAsStateWithLifecycle()
    val rewards by viewModel.rewards.collectAsStateWithLifecycle()
    val announcements by viewModel.announcements.collectAsStateWithLifecycle()
    val submissions by viewModel.submissions.collectAsStateWithLifecycle()
    val leaderboard by viewModel.leaderboard.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdminUser.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val coinRainTrigger by viewModel.coinRainTrigger.collectAsStateWithLifecycle()
    val nowMillis by viewModel.nowMillis.collectAsStateWithLifecycle()
    val quizSession by viewModel.quizSession.collectAsStateWithLifecycle()
    val adminStats by viewModel.adminStats.collectAsStateWithLifecycle()

    val adMob = remember { AdMobManager.getInstance() }
    val useTestAds by adMob.useTestAds.collectAsStateWithLifecycle()

    // Save verified +91 mobile number from OTP flow once Firestore profile exists
    LaunchedEffect(currentUserId, pendingVerifiedPhone, profile != null) {
        val phone = pendingVerifiedPhone
        if (!phone.isNullOrBlank() && profile != null) {
            viewModel.linkVerifiedPhone(phone)
            onClearPendingPhone()
        }
    }

    // Preload Rewarded & Interstitial Ads once authenticated
    LaunchedEffect(currentUserId, useTestAds) {
        viewModel.preloadAds(context)
    }

    // Android 13+ POST_NOTIFICATIONS runtime request
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {}
    )
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val darkTheme = profile?.darkMode ?: true
    val isHindi = profile?.language == "hi"
    val strings = if (isHindi) StringsHi else StringsEn

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusBanner) {
        statusBanner?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusBanner()
        }
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Routes.HOME

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val unreadCount = remember(notifications) { notifications.count { !it.readFlag } }
    val bottomItems = listOf(
        Triple(Routes.HOME, strings.navHome, Icons.Rounded.Home),
        Triple(Routes.QUIZ, strings.navQuiz, Icons.Rounded.School),
        Triple(Routes.EARN, strings.navEarn, Icons.Rounded.Stars),
        Triple(Routes.STORE, strings.navStore, Icons.Rounded.Storefront),
        Triple(Routes.PROFILE, strings.navProfile, Icons.Rounded.Person)
    )

    CompositionLocalProvider(LocalAppStrings provides strings) {
        StudyWinTheme(darkTheme = darkTheme) {
            StudyWinBackground(darkTheme = darkTheme) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val useNavRail = maxWidth >= 600.dp

                    Row(modifier = Modifier.fillMaxSize()) {
                        if (useNavRail) {
                            NavigationRail(
                                modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
                            ) {
                                bottomItems.forEach { (route, label, icon) ->
                                    NavigationRailItem(
                                        selected = currentRoute == route,
                                        onClick = { navigateTopLevel(route) },
                                        icon = { Icon(icon, contentDescription = label) },
                                        label = { Text(label) }
                                    )
                                }
                            }
                        }

                        Scaffold(
                            containerColor = androidx.compose.ui.graphics.Color.Transparent,
                            snackbarHost = { SnackbarHost(snackbarHostState) },
                            bottomBar = {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .windowInsetsPadding(WindowInsets.navigationBars)
                                ) {
                                    // Always-visible AdMob Banner placed safely above bottom navigation
                                    AdBannerFooter(
                                        adUnitId = adMob.activeBannerUnitId(),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    if (!useNavRail) {
                                        NavigationBar {
                                            bottomItems.forEach { (route, label, icon) ->
                                                NavigationBarItem(
                                                    selected = currentRoute == route,
                                                    onClick = { navigateTopLevel(route) },
                                                    icon = { Icon(icon, contentDescription = label) },
                                                    label = { Text(label) },
                                                    modifier = Modifier.testTag("nav_$route")
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        ) { innerPadding ->
                            NavHost(
                                navController = navController,
                                startDestination = Routes.HOME,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                                    .windowInsetsPadding(WindowInsets.statusBars)
                            ) {
                                composable(Routes.HOME) {
                                    HomeDashboardScreen(
                                        profile = profile,
                                        wallet = wallet,
                                        unreadNotifications = unreadCount,
                                        isAdmin = isAdmin,
                                        onNavigate = { route -> navController.navigate(route) },
                                        onWatchAdEarn = { act -> viewModel.watchRewardedAdEarnCoins(act) },
                                        onToggleLanguage = {
                                            viewModel.setLanguage(if (isHindi) "en" else "hi")
                                        }
                                    )
                                }
                                composable(Routes.QUIZ) {
                                    QuizEngineScreen(
                                        session = quizSession,
                                        allQuestions = questions,
                                        wallet = wallet,
                                        nowMillis = nowMillis,
                                        onSelectClassSubject = viewModel::selectClassAndSubject,
                                        onSubmitOption = viewModel::submitQuizOption,
                                        onWatchUnlockAd = viewModel::watchQuizUnlockAd,
                                        onNextQuestion = viewModel::loadNextQuizQuestion,
                                        onOpenScoreboard = { navController.navigate(Routes.SCOREBOARD) }
                                    )
                                }
                                composable(Routes.SCOREBOARD) {
                                    QuizScoreboardScreen(
                                        score = quizScore,
                                        wallet = wallet,
                                        allQuestions = questions,
                                        nowMillis = nowMillis,
                                        onBuyAdFreePass = viewModel::buyAdFreePass
                                    )
                                }
                                composable(Routes.EARN) {
                                    DailyBonusAndScratchScreen(
                                        wallet = wallet,
                                        settings = appSettings,
                                        todayKey = viewModel.todayKey(),
                                        nowMillis = nowMillis,
                                        transactions = transactions,
                                        onClaimDailyBonus = viewModel::claimDailyBonus,
                                        onClaimFreeScratch = viewModel::claimFreeScratchCard,
                                        onWatchAdForExtraScratch = viewModel::watchAdForExtraScratch,
                                        onClaimExtraScratch = viewModel::claimExtraScratchCard,
                                        onWatchAdEarn = viewModel::watchRewardedAdEarnCoins,
                                        onNavigateSpin = { navController.navigate(Routes.SPIN) }
                                    )
                                }
                                composable(Routes.SPIN) {
                                    LuckySpinScreen(
                                        wallet = wallet,
                                        settings = appSettings,
                                        nowMillis = nowMillis,
                                        onSpinCompleted = viewModel::completeFreeOrSpecialSpin,
                                        onWatchAdForExtraSpin = viewModel::watchAdForExtraSpin
                                    )
                                }
                                composable(Routes.STORE) {
                                    RewardStoreScreen(
                                        rewards = rewards,
                                        wallet = wallet,
                                        onRedeemReward = viewModel::redeemStoreReward,
                                        onOpenMonthlyRewards = { navController.navigate(Routes.MONTHLY) }
                                    )
                                }
                                composable(Routes.MONTHLY) {
                                    MonthlyRewardsScreen(
                                        announcements = announcements,
                                        submissions = submissions,
                                        settings = appSettings,
                                        wallet = wallet,
                                        onSubmitParticipation = viewModel::submitMonthlyReward
                                    )
                                }
                                composable(Routes.WALLET) {
                                    WalletAndHistoryScreen(wallet = wallet, transactions = transactions)
                                }
                                composable(Routes.LEADERBOARD) {
                                    LeaderboardScreen(entries = leaderboard)
                                }
                                composable(Routes.NOTIFICATIONS) {
                                    NotificationsScreen(
                                        notifications = notifications,
                                        onMarkRead = viewModel::markNotificationRead
                                    )
                                }
                                composable(Routes.SUPPORT) {
                                    SupportAndLegalScreen()
                                }
                                composable(Routes.PROFILE) {
                                    ProfileScreen(
                                        profile = profile,
                                        wallet = wallet,
                                        score = quizScore,
                                        isAdmin = isAdmin,
                                        onLanguageChange = viewModel::setLanguage,
                                        onDarkModeChange = viewModel::setDarkMode,
                                        onNotificationsChange = viewModel::setNotificationsEnabled,
                                        onLinkPhone = viewModel::linkVerifiedPhone,
                                        onOpenSupport = { navController.navigate(Routes.SUPPORT) },
                                        onOpenAdmin = { navController.navigate(Routes.ADMIN) },
                                        onDeleteAccount = { viewModel.deleteAccount(context) }
                                    )
                                }
                                composable(Routes.ADMIN) {
                                    AdminPanelScreen(
                                        isAdmin = isAdmin,
                                        stats = adminStats,
                                        questions = questions,
                                        rewards = rewards,
                                        submissions = submissions,
                                        settings = appSettings,
                                        onLoadStats = viewModel::loadAdminOverview,
                                        onSaveQuestion = viewModel::adminSaveQuestion,
                                        onImportJson = viewModel::adminImportQuestionsJson,
                                        onDeleteQuestion = viewModel::adminDeleteQuestion,
                                        onReviewSubmission = viewModel::adminReviewSubmission,
                                        onSaveReward = viewModel::adminSaveReward,
                                        onSaveSettings = viewModel::adminSaveSettings,
                                        onBroadcastNotification = viewModel::adminSendNotification,
                                        onBlockUser = viewModel::adminSetUserBlocked
                                    )
                                }
                            }
                        }
                    }

                    CoinRainOverlay(trigger = coinRainTrigger)
                }
            }
        }
    }
}
