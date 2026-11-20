package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Student",
    val email: String = "",
    val photoUrl: String = "",
    val phoneNumber: String = "",
    val phoneVerified: Boolean = false,
    val language: String = "en",
    val darkMode: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val role: String = "user",
    val blocked: Boolean = false,
    val blockReason: String = "",
    val welcomeBonusClaimed: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Wallet(
    @DocumentId val id: String = "",
    val userId: String = "",
    val availableCoins: Long = 0L,
    val totalEarnedCoins: Long = 0L,
    val welcomeBonusCoins: Long = 0L,
    val dailyBonusCoins: Long = 0L,
    val scratchCoins: Long = 0L,
    val spinCoins: Long = 0L,
    val quizCoins: Long = 0L,
    val adCoins: Long = 0L,
    val redeemedCoins: Long = 0L,
    val adRewardsCount: Long = 0L,
    val specialSpinsUnlocked: Long = 0L,
    val specialSpinsUsed: Long = 0L,
    val lastDailyBonusAt: Long = 0L,
    val dailyScratchDate: String = "",
    val dailyScratchUsed: Long = 0L,
    val extraScratchAvailable: Long = 0L,
    val lastFreeSpinAt: Long = 0L,
    val adFreePassExpiresAt: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WalletTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val txType: String = "",
    val amount: Long = 0L,
    val status: String = "COMPLETED",
    val description: String = "",
    val referenceId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DailyBonusRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val claimDate: String = "",
    val coins: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ScratchCardRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val cardDate: String = "",
    val coins: Long = 0L,
    val extraAdCard: Boolean = false,
    val claimed: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SpinHistoryRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val coins: Long = 0L,
    val spinType: String = "DAILY_FREE",
    val adVerified: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class QuizQuestion(
    @DocumentId val id: String = "",
    val questionClass: Int = 10,
    val subject: String = "Mathematics",
    val chapter: String = "",
    val questionText: String = "",
    val optionA: String = "",
    val optionB: String = "",
    val optionC: String = "",
    val optionD: String = "",
    val correctOption: String = "A",
    val explanation: String = "",
    val source: String = "Bihar Board Syllabus",
    val examYear: String = "",
    val createdBy: String = "system",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class QuizAttempt(
    @DocumentId val id: String = "",
    val userId: String = "",
    val questionId: String = "",
    val questionClass: Int = 10,
    val subject: String = "",
    val selectedOption: String = "A",
    val correct: Boolean = false,
    val coinsAwarded: Long = 0L,
    val pointsAwarded: Double = 0.0,
    val adsWatchedForUnlock: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class QuizScoreSummary(
    @DocumentId val id: String = "",
    val userId: String = "",
    val mathScore: Double = 0.0,
    val physicsScore: Double = 0.0,
    val chemistryScore: Double = 0.0,
    val biologyScore: Double = 0.0,
    val hindiScore: Double = 0.0,
    val englishScore: Double = 0.0,
    val totalAttempted: Long = 0L,
    val correctCount: Long = 0L,
    val incorrectCount: Long = 0L,
    val overallScore: Double = 0.0,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val accuracyPercent: Double
        get() = if (totalAttempted == 0L) 0.0 else (correctCount.toDouble() / totalAttempted.toDouble()) * 100.0
}

@Keep
@IgnoreExtraProperties
data class AdRewardLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val adFormat: String = "REWARDED",
    val placement: String = "",
    val adUnitId: String = "",
    val coinsAwarded: Long = 0L,
    val verified: Boolean = true,
    val rewardToken: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AdFreePassRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val costCoins: Long = 6999L,
    val durationHours: Long = 24L,
    val active: Boolean = true,
    val expiresAt: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RewardCatalogItem(
    @DocumentId val id: String = "",
    val title: String = "",
    val titleHi: String = "",
    val category: String = "",
    val coinCost: Long = 0L,
    val inventoryCount: Long = 0L,
    val deliveryTerms: String = "",
    val imageKey: String = "watch",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RewardSubmission(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val rewardId: String = "",
    val rewardTitle: String = "",
    val submissionType: String = "STORE_REDEMPTION",
    val coinCost: Long = 0L,
    val upiOrAddress: String = "",
    val instagramHandle: String = "",
    val screenshotName: String = "",
    val status: String = "PENDING",
    val adminNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Announcement(
    @DocumentId val id: String = "",
    val title: String = "",
    val titleHi: String = "",
    val message: String = "",
    val messageHi: String = "",
    val category: String = "MONTHLY_REWARD",
    val winnerNames: String = "",
    val instagramUrl: String = "https://www.instagram.com/study__win__india?evlk=MWdod2o0azVubjRzbA%3D%3D",
    val closingDate: String = "30 Nov 2026",
    val resultDate: String = "05 Dec 2026",
    val availableSlots: Long = 50L,
    val participantCount: Long = 0L,
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val titleHi: String = "",
    val body: String = "",
    val bodyHi: String = "",
    val category: String = "SYSTEM",
    val readFlag: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LeaderboardEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val photoUrl: String = "",
    val totalQuizScore: Double = 0.0,
    val totalCoinsEarned: Long = 0L,
    val weeklyScore: Double = 0.0,
    val monthlyScore: Double = 0.0,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppSettings(
    @DocumentId val id: String = "settings",
    val adMobAppId: String = "ca-app-pub-3940256099942544~3347511713",
    val bannerAdUnitId: String = "ca-app-pub-6545218519929279/1965994726",
    val rewardedAdUnitId: String = "ca-app-pub-6545218519929279/9899043803",
    val interstitialAdUnitId: String = "ca-app-pub-3940256099942544/1033173712",
    val useTestAds: Boolean = true,
    val instagramUrl: String = "https://www.instagram.com/study__win__india?evlk=MWdod2o0azVubjRzbA%3D%3D",
    val dailyBonusMin: Long = 1L,
    val dailyBonusMax: Long = 50L,
    val scratchMin: Long = 10L,
    val scratchMax: Long = 50L,
    val spinWeightsCsv: String = "25,25,16,12,8,5,4,3,1,1",
    @ServerTimestamp val updatedAt: Timestamp? = null
)
