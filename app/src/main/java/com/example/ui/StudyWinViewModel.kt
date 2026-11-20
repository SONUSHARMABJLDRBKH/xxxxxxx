package com.example.ui

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ads.AdMobManager
import com.example.data.model.*
import com.example.data.quiz.BiharBoardQuestionBank
import com.example.data.remote.AdminOverviewStats
import com.example.data.remote.QuizAndRewardsRepository
import com.example.data.remote.UserWalletRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class QuizSessionState(
    val selectedClass: Int = 10,
    val selectedSubject: String = "Mathematics",
    val activeQuestion: QuizQuestion? = null,
    val shuffledOptions: List<Pair<String, String>> = emptyList(), // ("A".."D", display text)
    val selectedOptionKey: String? = null,
    val answeredCorrectly: Boolean? = null,
    val lastCoinsEarned: Long = 0L,
    val wrongAnswerLocked: Boolean = false,
    val adsWatchedForUnlock: Int = 0,
    val explanationRevealed: Boolean = false,
    val recentQuestionIds: Set<String> = emptySet(),
    val sessionCorrect: Int = 0,
    val sessionAttempted: Int = 0,
    val show95PercentCelebration: Boolean = false
)

class StudyWinViewModel(
    private val userWalletRepo: UserWalletRepository,
    private val quizRewardsRepo: QuizAndRewardsRepository,
    private val currentUserId: String
) : ViewModel() {

    private val adMob = AdMobManager.getInstance()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _coinRainTrigger = MutableStateFlow(0)
    val coinRainTrigger: StateFlow<Int> = _coinRainTrigger.asStateFlow()

    private val _nowMillis = MutableStateFlow(System.currentTimeMillis())
    val nowMillis: StateFlow<Long> = _nowMillis.asStateFlow()

    private val _quizSession = MutableStateFlow(QuizSessionState())
    val quizSession: StateFlow<QuizSessionState> = _quizSession.asStateFlow()

    private val _adminStats = MutableStateFlow<AdminOverviewStats?>(null)
    val adminStats: StateFlow<AdminOverviewStats?> = _adminStats.asStateFlow()

    val profile: StateFlow<UserProfile?> = userWalletRepo.observeProfile(currentUserId)
        .catch { err ->
            unwrapOrThrow(err)
            emitAll(userWalletRepo.observeCachedProfile(currentUserId))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val wallet: StateFlow<Wallet?> = userWalletRepo.observeWallet(currentUserId)
        .catch { err ->
            unwrapOrThrow(err)
            emitAll(userWalletRepo.observeCachedWallet(currentUserId))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val transactions: StateFlow<List<WalletTransaction>> = userWalletRepo.observeTransactions(currentUserId)
        .catch { err ->
            unwrapOrThrow(err)
            emitAll(userWalletRepo.observeCachedTransactions(currentUserId))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val questions: StateFlow<List<QuizQuestion>> = quizRewardsRepo.observeQuestions()
        .catch { err ->
            unwrapOrThrow(err)
            emitAll(quizRewardsRepo.observeCachedQuestions())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), BiharBoardQuestionBank.starterQuestions())

    val quizScore: StateFlow<QuizScoreSummary> = quizRewardsRepo.observeQuizScore(currentUserId)
        .map { it ?: QuizScoreSummary(userId = currentUserId) }
        .catch { err ->
            unwrapOrThrow(err)
            emit(QuizScoreSummary(userId = currentUserId))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), QuizScoreSummary(userId = currentUserId))

    val rewards: StateFlow<List<RewardCatalogItem>> = quizRewardsRepo.observeRewards()
        .catch { err ->
            unwrapOrThrow(err)
            emit(QuizAndRewardsRepository.defaultRewardCatalog())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), QuizAndRewardsRepository.defaultRewardCatalog())

    val announcements: StateFlow<List<Announcement>> = quizRewardsRepo.observeAnnouncements()
        .catch { err ->
            unwrapOrThrow(err)
            emit(QuizAndRewardsRepository.defaultMonthlyAnnouncements())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), QuizAndRewardsRepository.defaultMonthlyAnnouncements())

    val leaderboard: StateFlow<List<LeaderboardEntry>> = quizRewardsRepo.observeLeaderboard()
        .catch { err ->
            unwrapOrThrow(err)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val notifications: StateFlow<List<AppNotification>> = quizRewardsRepo.observeNotifications(currentUserId)
        .catch { err ->
            unwrapOrThrow(err)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val appSettings: StateFlow<AppSettings> = quizRewardsRepo.observeAppSettings()
        .onEach { cfg ->
            adMob.updateAdConfig(
                useTestAds = cfg.useTestAds,
                bannerId = cfg.bannerAdUnitId,
                rewardedId = cfg.rewardedAdUnitId,
                interstitialId = cfg.interstitialAdUnitId
            )
        }
        .catch { err ->
            unwrapOrThrow(err)
            emit(AppSettings())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AppSettings())

    val isAdminUser: StateFlow<Boolean> = profile
        .map { p ->
            val email = Firebase.auth.currentUser?.email ?: ""
            p?.role == "admin" || email.equals("farginagabhai@gmail.com", ignoreCase = true)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)

    val submissions: StateFlow<List<RewardSubmission>> = isAdminUser
        .flatMapLatest { admin ->
            quizRewardsRepo.observeSubmissions(currentUserId, admin)
                .catch { err ->
                    unwrapOrThrow(err)
                    emit(emptyList())
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    init {
        viewModelScope.launch {
            Firebase.auth.currentUser?.let { user ->
                userWalletRepo.ensureUserInitialized(user)
            }
        }
        viewModelScope.launch {
            while (true) {
                _nowMillis.value = System.currentTimeMillis()
                delay(1000L)
            }
        }
    }

    private fun unwrapOrThrow(error: Throwable) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun showMessage(msg: String) {
        _statusBanner.value = msg
    }

    fun triggerCoinRain() {
        _coinRainTrigger.value += 1
    }

    fun todayKey(): String = userWalletRepo.todayKey()

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            userWalletRepo.updatePreferences(language = lang)
        }
    }

    fun setDarkMode(dark: Boolean) {
        viewModelScope.launch {
            userWalletRepo.updatePreferences(darkMode = dark)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userWalletRepo.updatePreferences(notificationsEnabled = enabled)
        }
    }

    fun linkVerifiedPhone(phone: String) {
        viewModelScope.launch {
            userWalletRepo.updatePreferences(phoneNumber = phone, phoneVerified = true)
                .onSuccess { showMessage("Mobile number $phone verified & saved.") }
                .onFailure { showMessage(it.message ?: "Could not save phone number") }
        }
    }

    fun claimDailyBonus() {
        viewModelScope.launch {
            val cfg = appSettings.value
            userWalletRepo.claimDailyBonus(cfg.dailyBonusMin, cfg.dailyBonusMax)
                .onSuccess { coins ->
                    triggerCoinRain()
                    showMessage("Daily Bonus claimed: +$coins coins!")
                }
                .onFailure { showMessage(it.message ?: "Daily Bonus unavailable right now") }
        }
    }

    fun claimFreeScratchCard() {
        viewModelScope.launch {
            val cfg = appSettings.value
            userWalletRepo.claimScratchCard(false, null, cfg.scratchMin, cfg.scratchMax)
                .onSuccess { coins ->
                    triggerCoinRain()
                    showMessage("Scratch Card won +$coins coins!")
                }
                .onFailure { showMessage(it.message ?: "Could not claim scratch card") }
        }
    }

    fun watchAdForExtraScratch(activity: Activity?) {
        adMob.showRewardedAd(
            activity = activity,
            userId = currentUserId,
            placement = "scratch_extra_unlock",
            onRewardEarned = { token, unitId ->
                viewModelScope.launch {
                    userWalletRepo.recordVerifiedRewardedAd(
                        placement = "scratch_extra_unlock",
                        adUnitId = unitId,
                        rewardToken = token,
                        bonusCoins = 0L,
                        unlockExtraScratch = true
                    ).onSuccess {
                        showMessage("Extra Scratch Card unlocked! Scratch & claim your coins.")
                    }.onFailure { showMessage(it.message ?: "Failed to unlock scratch card") }
                }
            },
            onAdSkippedOrClosed = { showMessage("Ad skipped. Complete the ad to unlock an extra scratch card.") },
            onAdUnavailable = { msg -> showMessage(msg) },
            onAdLoading = { showMessage("Loading rewarded ad, please wait...") }
        )
    }

    fun claimExtraScratchCard(rewardToken: String) {
        viewModelScope.launch {
            val cfg = appSettings.value
            userWalletRepo.claimScratchCard(true, rewardToken, cfg.scratchMin, cfg.scratchMax)
                .onSuccess { coins ->
                    triggerCoinRain()
                    showMessage("Bonus Scratch Card won +$coins coins!")
                }
                .onFailure { showMessage(it.message ?: "Claim failed") }
        }
    }

    fun watchRewardedAdEarnCoins(activity: Activity?) {
        adMob.showRewardedAd(
            activity = activity,
            userId = currentUserId,
            placement = "watch_ads_earn",
            onRewardEarned = { token, unitId ->
                viewModelScope.launch {
                    val coins = Random.nextLong(10L, 26L)
                    userWalletRepo.recordVerifiedRewardedAd(
                        placement = "watch_ads_earn",
                        adUnitId = unitId,
                        rewardToken = token,
                        bonusCoins = coins
                    ).onSuccess {
                        triggerCoinRain()
                        showMessage("Verified Ad completed! +$coins coins added.")
                    }.onFailure { showMessage(it.message ?: "Error crediting ad reward") }
                }
            },
            onAdSkippedOrClosed = { showMessage("Ad skipped before completion — no coins awarded.") },
            onAdUnavailable = { msg -> showMessage(msg) },
            onAdLoading = { showMessage("Loading rewarded ad, please wait...") }
        )
    }

    fun completeFreeOrSpecialSpin(coins: Long, spinType: String, onSaved: () -> Unit) {
        viewModelScope.launch {
            userWalletRepo.recordSpinOutcome(coins, spinType, adVerified = (spinType == "AD_EXTRA"))
                .onSuccess {
                    triggerCoinRain()
                    showMessage("Lucky Spin won +$coins coins!")
                    onSaved()
                }
                .onFailure { showMessage(it.message ?: "Spin failed") }
        }
    }

    fun watchAdForExtraSpin(activity: Activity?, onAdVerifiedToStartSpin: () -> Unit) {
        adMob.showRewardedAd(
            activity = activity,
            userId = currentUserId,
            placement = "lucky_spin_extra",
            onRewardEarned = { token, unitId ->
                viewModelScope.launch {
                    userWalletRepo.recordVerifiedRewardedAd(
                        placement = "lucky_spin_extra",
                        adUnitId = unitId,
                        rewardToken = token,
                        bonusCoins = 0L
                    ).onSuccess {
                        onAdVerifiedToStartSpin()
                    }.onFailure { showMessage(it.message ?: "Ad verification failed") }
                }
            },
            onAdSkippedOrClosed = { showMessage("Ad skipped. Watch the full ad to spin.") },
            onAdUnavailable = { msg -> showMessage(msg) },
            onAdLoading = { showMessage("Loading rewarded ad, please wait...") }
        )
    }

    // --- BIHAR BOARD QUIZ ENGINE ---

    fun selectClassAndSubject(qClass: Int, subject: String) {
        _quizSession.update { it.copy(selectedClass = qClass, selectedSubject = subject) }
        loadNextQuizQuestion()
    }

    fun loadNextQuizQuestion() {
        val state = _quizSession.value
        val pool = questions.value.filter {
            it.questionClass == state.selectedClass && it.subject.equals(state.selectedSubject, ignoreCase = true)
        }.ifEmpty {
            questions.value.filter { it.subject.equals(state.selectedSubject, ignoreCase = true) }
        }.ifEmpty {
            questions.value
        }

        val fresh = pool.filterNot { it.id in state.recentQuestionIds }.ifEmpty { pool }
        val picked = fresh.shuffled().firstOrNull() ?: return
        val options = listOf(
            "A" to picked.optionA,
            "B" to picked.optionB,
            "C" to picked.optionC,
            "D" to picked.optionD
        ).shuffled()

        _quizSession.update {
            it.copy(
                activeQuestion = picked,
                shuffledOptions = options,
                selectedOptionKey = null,
                answeredCorrectly = null,
                lastCoinsEarned = 0L,
                wrongAnswerLocked = false,
                adsWatchedForUnlock = 0,
                explanationRevealed = false,
                recentQuestionIds = (it.recentQuestionIds + picked.id).toList().takeLast(25).toSet()
            )
        }
    }

    fun submitQuizOption(optionKey: String) {
        val state = _quizSession.value
        val question = state.activeQuestion ?: return
        if (state.selectedOptionKey != null) return

        val isCorrect = optionKey.equals(question.correctOption, ignoreCase = true)
        val coins = if (isCorrect) Random.nextLong(1L, 6L) else 0L
        val adFreeActive = (wallet.value?.adFreePassExpiresAt ?: 0L) > _nowMillis.value
        val newAttempted = state.sessionAttempted + 1
        val newCorrect = state.sessionCorrect + if (isCorrect) 1 else 0
        val accuracy = if (newAttempted >= 5) (newCorrect.toDouble() / newAttempted) * 100.0 else 0.0

        _quizSession.update {
            it.copy(
                selectedOptionKey = optionKey,
                answeredCorrectly = isCorrect,
                lastCoinsEarned = coins,
                wrongAnswerLocked = !isCorrect && !adFreeActive,
                explanationRevealed = isCorrect || adFreeActive,
                sessionAttempted = newAttempted,
                sessionCorrect = newCorrect,
                show95PercentCelebration = accuracy >= 95.0
            )
        }

        if (isCorrect) {
            triggerCoinRain()
        }

        viewModelScope.launch {
            quizRewardsRepo.recordQuizAttempt(
                question = question,
                selectedOption = optionKey,
                correct = isCorrect,
                coinsAwarded = coins,
                adsWatchedForUnlock = 0
            )
            // Correct answer: never interrupt with an ad; brief visual highlight then immediately load next question
            if (isCorrect) {
                delay(750L)
                loadNextQuizQuestion()
            }
        }
    }

    fun watchQuizUnlockAd(activity: Activity?) {
        val state = _quizSession.value
        if (!state.wrongAnswerLocked) return
        adMob.showRewardedAd(
            activity = activity,
            userId = currentUserId,
            placement = "quiz_wrong_unlock_${state.adsWatchedForUnlock + 1}",
            onRewardEarned = { token, unitId ->
                viewModelScope.launch {
                    userWalletRepo.recordVerifiedRewardedAd(
                        placement = "quiz_wrong_unlock",
                        adUnitId = unitId,
                        rewardToken = token,
                        bonusCoins = 0L
                    )
                    val nextCount = (_quizSession.value.adsWatchedForUnlock + 1).coerceAtMost(3)
                    _quizSession.update {
                        it.copy(
                            adsWatchedForUnlock = nextCount,
                            wrongAnswerLocked = nextCount < 3,
                            explanationRevealed = nextCount >= 3
                        )
                    }
                    if (nextCount >= 3) {
                        showMessage("3/3 Ads completed! Correct answer & explanation unlocked.")
                    }
                }
            },
            onAdSkippedOrClosed = { showMessage("Ad skipped. Complete 3 ads consecutively to unlock explanation.") },
            onAdUnavailable = { msg -> showMessage(msg) },
            onAdLoading = { showMessage("Loading rewarded ad, please wait...") }
        )
    }

    fun buyAdFreePass() {
        viewModelScope.launch {
            quizRewardsRepo.purchaseQuizAdFreePass()
                .onSuccess { showMessage("24h Quiz Ad-Free Pass activated!") }
                .onFailure { showMessage(it.message ?: "Purchase failed") }
        }
    }

    fun redeemStoreReward(reward: RewardCatalogItem, upiOrAddress: String) {
        viewModelScope.launch {
            val name = profile.value?.displayName ?: "Student"
            quizRewardsRepo.redeemReward(reward, name, upiOrAddress)
                .onSuccess { showMessage("${reward.title} redemption submitted! Track status in History.") }
                .onFailure { showMessage(it.message ?: "Redemption failed") }
        }
    }

    fun submitMonthlyReward(category: String, coinCost: Long, handle: String, screenshotName: String) {
        viewModelScope.launch {
            val name = profile.value?.displayName ?: "Student"
            quizRewardsRepo.submitMonthlyParticipation(category, coinCost, name, handle, screenshotName)
                .onSuccess { showMessage("Monthly Reward submitted ($coinCost coins)! Track status below.") }
                .onFailure { showMessage(it.message ?: "Submission failed") }
        }
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch { quizRewardsRepo.markNotificationRead(id) }
    }

    // --- ADMIN ACTIONS ---
    fun loadAdminOverview() {
        viewModelScope.launch {
            quizRewardsRepo.adminFetchOverviewStats()
                .onSuccess { _adminStats.value = it }
                .onFailure { showMessage(it.message ?: "Could not load admin stats") }
        }
    }

    fun adminSaveQuestion(q: QuizQuestion) {
        viewModelScope.launch {
            quizRewardsRepo.adminAddOrUpdateQuestion(q)
                .onSuccess { showMessage("Question saved to Bihar Board Bank!") }
                .onFailure { showMessage(it.message ?: "Save failed") }
        }
    }

    fun adminImportQuestionsJson(json: String) {
        viewModelScope.launch {
            BiharBoardQuestionBank.parseImportedJson(json, currentUserId)
                .onSuccess { list ->
                    quizRewardsRepo.adminBulkImportQuestions(list)
                        .onSuccess { count -> showMessage("Imported $count verified questions!") }
                        .onFailure { showMessage(it.message ?: "Import failed") }
                }
                .onFailure { showMessage("Invalid JSON: ${it.message}") }
        }
    }

    fun adminDeleteQuestion(id: String) {
        viewModelScope.launch {
            quizRewardsRepo.adminDeleteQuestion(id)
                .onSuccess { showMessage("Question deleted") }
                .onFailure { showMessage(it.message ?: "Delete failed") }
        }
    }

    fun adminReviewSubmission(id: String, status: String, note: String) {
        viewModelScope.launch {
            quizRewardsRepo.adminUpdateSubmissionStatus(id, status, note)
                .onSuccess { showMessage("Submission marked $status") }
                .onFailure { showMessage(it.message ?: "Update failed") }
        }
    }

    fun adminSaveReward(item: RewardCatalogItem) {
        viewModelScope.launch {
            quizRewardsRepo.adminSaveRewardItem(item)
                .onSuccess { showMessage("Reward catalog updated") }
                .onFailure { showMessage(it.message ?: "Save failed") }
        }
    }

    fun adminSaveSettings(settings: AppSettings) {
        viewModelScope.launch {
            quizRewardsRepo.adminUpdateAppSettings(settings)
                .onSuccess { showMessage("Admin settings & AdMob config updated!") }
                .onFailure { showMessage(it.message ?: "Settings update failed") }
        }
    }

    fun adminSendNotification(title: String, body: String) {
        viewModelScope.launch {
            quizRewardsRepo.adminBroadcastNotification(title, body, "ANNOUNCEMENT")
                .onSuccess { showMessage("Notification sent to all users!") }
                .onFailure { showMessage(it.message ?: "Broadcast failed") }
        }
    }

    fun adminSetUserBlocked(targetUid: String, blocked: Boolean, reason: String) {
        viewModelScope.launch {
            quizRewardsRepo.adminBlockUser(targetUid, blocked, reason)
                .onSuccess {
                    showMessage(if (blocked) "User blocked" else "User unblocked")
                    loadAdminOverview()
                }
                .onFailure { showMessage(it.message ?: "Failed to update user") }
        }
    }

    suspend fun deleteAccount(context: Context): Result<Unit> {
        return userWalletRepo.deleteAccountSecurely(currentUserId)
    }

    fun preloadAds(context: Context) {
        adMob.preloadRewardedAd(context, currentUserId, "general")
        adMob.preloadInterstitial(context)
    }
}
