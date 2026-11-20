package com.example.data.remote

import android.content.Context
import android.os.SystemClock
import com.example.R
import com.example.data.local.StudyWinDao
import com.example.data.local.toDomain
import com.example.data.local.toEntity
import com.example.data.model.*
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.random.Random

class UserWalletRepository(
    private val db: FirebaseFirestore,
    private val localDao: StudyWinDao,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context, localDao: StudyWinDao) : this(
        FirebaseFirestore.getInstance(context.applicationContext.getString(R.string.firestore_database_id)),
        localDao
    )

    private var lastServerClockAnchorMs: Long = System.currentTimeMillis()
    private var lastElapsedAnchorMs: Long = SystemClock.elapsedRealtime()

    private fun trustedNowMillis(): Long {
        val elapsedDelta = (SystemClock.elapsedRealtime() - lastElapsedAnchorMs).coerceAtLeast(0L)
        return maxOf(System.currentTimeMillis(), lastServerClockAnchorMs + elapsedDelta)
    }

    private fun updateTrustedClockAnchor(serverTs: Timestamp?) {
        serverTs?.toDate()?.time?.let { serverMs ->
            if (serverMs > lastServerClockAnchorMs) {
                lastServerClockAnchorMs = serverMs
                lastElapsedAnchorMs = SystemClock.elapsedRealtime()
            }
        }
    }

    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("User must be signed in with Google.")

    fun todayKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(trustedNowMillis()))

    fun observeProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snap ->
                    val p = snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    p?.also { updateTrustedClockAnchor(it.updatedAt) }
                }
                .onEach { profile -> if (profile != null) localDao.upsertProfile(profile.toEntity()) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(firestoreError ?: error, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeCachedProfile(userId: String): Flow<UserProfile?> =
        localDao.observeProfile(userId).map { it?.toDomain() }

    fun observeWallet(userId: String): Flow<Wallet?> = flow {
        val path = "wallets/$userId"
        emitAll(
            db.collection("wallets").document(userId)
                .snapshots()
                .map { snap ->
                    val w = snap.toObject(Wallet::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    w?.also { updateTrustedClockAnchor(it.updatedAt) }
                }
                .onEach { wallet -> if (wallet != null) localDao.upsertWallet(wallet.toEntity()) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(firestoreError ?: error, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeCachedWallet(userId: String): Flow<Wallet?> =
        localDao.observeWallet(userId).map { it?.toDomain() }

    fun observeTransactions(userId: String): Flow<List<WalletTransaction>> = flow {
        val path = "transactions"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(WalletTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .onEach { txs -> localDao.upsertTransactions(txs.map { it.toEntity() }) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeCachedTransactions(userId: String): Flow<List<WalletTransaction>> =
        localDao.observeTransactions(userId).map { list -> list.map { it.toDomain() } }

    suspend fun ensureUserInitialized(firebaseUser: FirebaseUser): Result<Unit> {
        val uid = firebaseUser.uid
        return try {
            val userRef = db.collection("users").document(uid)
            val walletRef = db.collection("wallets").document(uid)
            val scoreRef = db.collection("quiz_scores").document(uid)
            val lbRef = db.collection("leaderboard").document(uid)

            val existingUser = userRef.get().await()
            val existingWallet = walletRef.get().await()

            if (!existingUser.exists() || !existingWallet.exists()) {
                val batch = db.batch()
                val displayName = (firebaseUser.displayName?.takeIf { it.isNotBlank() } ?: "Student").take(75)
                val email = (firebaseUser.email ?: "").take(150)
                val photoUrl = (firebaseUser.photoUrl?.toString() ?: "").take(500)

                if (!existingUser.exists()) {
                    val userMap = mapOf(
                        "userId" to uid,
                        "displayName" to displayName,
                        "email" to email,
                        "photoUrl" to photoUrl,
                        "phoneNumber" to (firebaseUser.phoneNumber ?: ""),
                        "phoneVerified" to !(firebaseUser.phoneNumber.isNullOrBlank()),
                        "language" to "en",
                        "darkMode" to true,
                        "notificationsEnabled" to true,
                        "role" to "user",
                        "blocked" to false,
                        "welcomeBonusClaimed" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    batch.set(userRef, userMap)
                }

                if (!existingWallet.exists()) {
                    val walletMap = mapOf(
                        "userId" to uid,
                        "availableCoins" to 10L,
                        "totalEarnedCoins" to 10L,
                        "welcomeBonusCoins" to 10L,
                        "dailyBonusCoins" to 0L,
                        "scratchCoins" to 0L,
                        "spinCoins" to 0L,
                        "quizCoins" to 0L,
                        "adCoins" to 0L,
                        "redeemedCoins" to 0L,
                        "adRewardsCount" to 0L,
                        "specialSpinsUnlocked" to 0L,
                        "specialSpinsUsed" to 0L,
                        "lastDailyBonusAt" to 0L,
                        "dailyScratchDate" to todayKey(),
                        "dailyScratchUsed" to 0L,
                        "extraScratchAvailable" to 0L,
                        "lastFreeSpinAt" to 0L,
                        "adFreePassExpiresAt" to 0L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    batch.set(walletRef, walletMap)

                    val txId = "welcome_$uid"
                    batch.set(
                        db.collection("transactions").document(txId),
                        mapOf(
                            "userId" to uid,
                            "txType" to "WELCOME_BONUS",
                            "amount" to 10L,
                            "status" to "COMPLETED",
                            "description" to "Welcome Bonus for new verified StudyWin India account",
                            "referenceId" to txId,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                    batch.set(
                        db.collection("notifications").document("notif_welcome_$uid"),
                        mapOf(
                            "userId" to uid,
                            "title" to "Welcome to StudyWin India!",
                            "titleHi" to "StudyWin India में आपका स्वागत है!",
                            "body" to "You received 10 Welcome Coins! Start a Bihar Board Quiz or claim your Daily Bonus.",
                            "bodyHi" to "आपको 10 वेलकम कॉइन्स मिले हैं! बिहार बोर्ड क्विज़ खेलें या दैनिक बोनस क्लेम करें।",
                            "category" to "WELCOME",
                            "readFlag" to false,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                if (!scoreRef.get().await().exists()) {
                    batch.set(
                        scoreRef,
                        mapOf(
                            "userId" to uid,
                            "mathScore" to 0.0,
                            "physicsScore" to 0.0,
                            "chemistryScore" to 0.0,
                            "biologyScore" to 0.0,
                            "hindiScore" to 0.0,
                            "englishScore" to 0.0,
                            "totalAttempted" to 0L,
                            "correctCount" to 0L,
                            "incorrectCount" to 0L,
                            "overallScore" to 0.0,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                if (!lbRef.get().await().exists()) {
                    batch.set(
                        lbRef,
                        mapOf(
                            "userId" to uid,
                            "username" to displayName,
                            "photoUrl" to photoUrl,
                            "totalQuizScore" to 0.0,
                            "totalCoinsEarned" to 10L,
                            "weeklyScore" to 0.0,
                            "monthlyScore" to 0.0,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun updatePreferences(
        language: String? = null,
        darkMode: Boolean? = null,
        notificationsEnabled: Boolean? = null,
        phoneNumber: String? = null,
        phoneVerified: Boolean? = null,
        displayName: String? = null
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val updates = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
            if (language != null && language in listOf("en", "hi")) updates["language"] = language
            if (darkMode != null) updates["darkMode"] = darkMode
            if (notificationsEnabled != null) updates["notificationsEnabled"] = notificationsEnabled
            if (phoneNumber != null) updates["phoneNumber"] = phoneNumber.take(24)
            if (phoneVerified != null) updates["phoneVerified"] = phoneVerified
            if (!displayName.isNullOrBlank()) updates["displayName"] = displayName.trim().take(80)
            db.collection("users").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun claimDailyBonus(minCoins: Long, maxCoins: Long): Result<Long> {
        val uid = requireUserId()
        return try {
            val now = trustedNowMillis()
            val today = todayKey()
            val walletRef = db.collection("wallets").document(uid)
            val bonusRef = db.collection("daily_bonuses").document("db_${uid}_$today")
            val existingBonus = bonusRef.get().await()
            if (existingBonus.exists()) {
                return Result.failure(IllegalStateException("Daily bonus already claimed for today."))
            }
            val coins = Random.nextLong(minCoins.coerceAtLeast(1L), maxCoins.coerceAtLeast(minCoins) + 1L)
            val txId = "daily_${uid}_${UUID.randomUUID().toString().take(8)}"

            db.runTransaction { tx ->
                val snap = tx.get(walletRef)
                val lastClaimAt = snap.getLong("lastDailyBonusAt") ?: 0L
                val elapsed = now - lastClaimAt
                if (lastClaimAt > 0L && elapsed < 24 * 60 * 60 * 1000L) {
                    throw IllegalStateException("24-hour cooldown is still active.")
                }
                tx.update(
                    walletRef,
                    mapOf(
                        "availableCoins" to FieldValue.increment(coins),
                        "totalEarnedCoins" to FieldValue.increment(coins),
                        "dailyBonusCoins" to FieldValue.increment(coins),
                        "lastDailyBonusAt" to now,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    bonusRef,
                    mapOf(
                        "userId" to uid,
                        "claimDate" to today,
                        "coins" to coins,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    db.collection("transactions").document(txId),
                    mapOf(
                        "userId" to uid,
                        "txType" to "DAILY_BONUS",
                        "amount" to coins,
                        "status" to "COMPLETED",
                        "description" to "Daily Bonus claimed (+$coins coins)",
                        "referenceId" to bonusRef.id,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()

            db.collection("leaderboard").document(uid).update(
                mapOf(
                    "totalCoinsEarned" to FieldValue.increment(coins),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(coins)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "daily_bonuses")
            Result.failure(e)
        }
    }

    suspend fun claimScratchCard(isExtraAdCard: Boolean, rewardToken: String?, minCoins: Long, maxCoins: Long): Result<Long> {
        val uid = requireUserId()
        return try {
            val today = todayKey()
            val walletRef = db.collection("wallets").document(uid)
            val coins = Random.nextLong(minCoins.coerceAtLeast(1L), maxCoins.coerceAtLeast(minCoins) + 1L)
            val cardId = "sc_${uid}_${UUID.randomUUID().toString().take(10)}"
            val txId = "tx_sc_${UUID.randomUUID().toString().take(10)}"

            db.runTransaction { tx ->
                val snap = tx.get(walletRef)
                val savedDate = snap.getString("dailyScratchDate") ?: ""
                val usedToday = if (savedDate == today) (snap.getLong("dailyScratchUsed") ?: 0L) else 0L
                val extraAvail = snap.getLong("extraScratchAvailable") ?: 0L

                if (!isExtraAdCard) {
                    if (usedToday >= 3L) throw IllegalStateException("All 3 free scratch cards for today are used.")
                    tx.update(
                        walletRef,
                        mapOf(
                            "availableCoins" to FieldValue.increment(coins),
                            "totalEarnedCoins" to FieldValue.increment(coins),
                            "scratchCoins" to FieldValue.increment(coins),
                            "dailyScratchDate" to today,
                            "dailyScratchUsed" to (usedToday + 1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    if (rewardToken.isNullOrBlank()) throw IllegalStateException("Rewarded ad verification required.")
                    if (extraAvail <= 0L) throw IllegalStateException("Watch a rewarded ad first to unlock an extra card.")
                    tx.update(
                        walletRef,
                        mapOf(
                            "availableCoins" to FieldValue.increment(coins),
                            "totalEarnedCoins" to FieldValue.increment(coins),
                            "scratchCoins" to FieldValue.increment(coins),
                            "extraScratchAvailable" to FieldValue.increment(-1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                tx.set(
                    db.collection("scratch_cards").document(cardId),
                    mapOf(
                        "userId" to uid,
                        "cardDate" to today,
                        "coins" to coins,
                        "extraAdCard" to isExtraAdCard,
                        "claimed" to true,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    db.collection("transactions").document(txId),
                    mapOf(
                        "userId" to uid,
                        "txType" to "SCRATCH_REWARD",
                        "amount" to coins,
                        "status" to "COMPLETED",
                        "description" to "Scratch Card reward (+$coins coins)",
                        "referenceId" to cardId,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()

            db.collection("leaderboard").document(uid).update(
                mapOf(
                    "totalCoinsEarned" to FieldValue.increment(coins),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(coins)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "scratch_cards")
            Result.failure(e)
        }
    }

    suspend fun recordVerifiedRewardedAd(
        placement: String,
        adUnitId: String,
        rewardToken: String,
        bonusCoins: Long = 0L,
        unlockExtraScratch: Boolean = false
    ): Result<Unit> {
        val uid = requireUserId()
        return try {
            val safeToken = rewardToken.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(80)
            val adRef = db.collection("ad_rewards").document("ad_$safeToken")
            if (adRef.get().await().exists()) {
                return Result.failure(IllegalStateException("Duplicate ad reward callback ignored."))
            }
            val walletRef = db.collection("wallets").document(uid)
            db.runTransaction { tx ->
                val wSnap = tx.get(walletRef)
                val currentCount = (wSnap.getLong("adRewardsCount") ?: 0L) + 1L
                val unlockedMilestones = wSnap.getLong("specialSpinsUnlocked") ?: 0L
                val newMilestones = if (currentCount % 100L == 0L) unlockedMilestones + 5L else unlockedMilestones

                val walletUpdates = mutableMapOf<String, Any>(
                    "adRewardsCount" to currentCount,
                    "specialSpinsUnlocked" to newMilestones,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (bonusCoins > 0L) {
                    walletUpdates["availableCoins"] = FieldValue.increment(bonusCoins)
                    walletUpdates["totalEarnedCoins"] = FieldValue.increment(bonusCoins)
                    walletUpdates["adCoins"] = FieldValue.increment(bonusCoins)
                }
                if (unlockExtraScratch) {
                    walletUpdates["extraScratchAvailable"] = FieldValue.increment(1L)
                }
                tx.update(walletRef, walletUpdates)

                tx.set(
                    adRef,
                    mapOf(
                        "userId" to uid,
                        "adFormat" to "REWARDED",
                        "placement" to placement,
                        "adUnitId" to adUnitId,
                        "coinsAwarded" to bonusCoins,
                        "verified" to true,
                        "rewardToken" to safeToken,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                if (bonusCoins > 0L) {
                    tx.set(
                        db.collection("transactions").document("tx_ad_$safeToken"),
                        mapOf(
                            "userId" to uid,
                            "txType" to "AD_REWARD",
                            "amount" to bonusCoins,
                            "status" to "COMPLETED",
                            "description" to "Verified Rewarded Ad ($placement)",
                            "referenceId" to safeToken,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
            if (bonusCoins > 0L) {
                db.collection("leaderboard").document(uid).update(
                    mapOf(
                        "totalCoinsEarned" to FieldValue.increment(bonusCoins),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "ad_rewards")
            Result.failure(e)
        }
    }

    suspend fun recordSpinOutcome(
        coins: Long,
        spinType: String,
        adVerified: Boolean
    ): Result<Unit> {
        val uid = requireUserId()
        return try {
            val now = trustedNowMillis()
            val walletRef = db.collection("wallets").document(uid)
            val spinId = "spin_${uid}_${UUID.randomUUID().toString().take(10)}"
            val txId = "tx_spin_${UUID.randomUUID().toString().take(10)}"

            db.runTransaction { tx ->
                val snap = tx.get(walletRef)
                when (spinType) {
                    "DAILY_FREE" -> {
                        val lastSpin = snap.getLong("lastFreeSpinAt") ?: 0L
                        if (lastSpin > 0L && (now - lastSpin) < 24 * 60 * 60 * 1000L) {
                            throw IllegalStateException("Free daily spin cooldown is active.")
                        }
                    }
                    "SPECIAL_999" -> {
                        val unlocked = snap.getLong("specialSpinsUnlocked") ?: 0L
                        val used = snap.getLong("specialSpinsUsed") ?: 0L
                        if (used >= unlocked) throw IllegalStateException("No special spins available.")
                    }
                    "AD_EXTRA" -> {
                        if (!adVerified) throw IllegalStateException("Rewarded ad verification required.")
                    }
                }

                val updates = mutableMapOf<String, Any>(
                    "availableCoins" to FieldValue.increment(coins),
                    "totalEarnedCoins" to FieldValue.increment(coins),
                    "spinCoins" to FieldValue.increment(coins),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (spinType == "DAILY_FREE") updates["lastFreeSpinAt"] = now
                if (spinType == "SPECIAL_999") updates["specialSpinsUsed"] = FieldValue.increment(1L)

                tx.update(walletRef, updates)
                tx.set(
                    db.collection("spin_history").document(spinId),
                    mapOf(
                        "userId" to uid,
                        "coins" to coins,
                        "spinType" to spinType,
                        "adVerified" to adVerified,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    db.collection("transactions").document(txId),
                    mapOf(
                        "userId" to uid,
                        "txType" to "SPIN_REWARD",
                        "amount" to coins,
                        "status" to "COMPLETED",
                        "description" to "Lucky Spin ($spinType) won +$coins coins",
                        "referenceId" to spinId,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()

            db.collection("leaderboard").document(uid).update(
                mapOf(
                    "totalCoinsEarned" to FieldValue.increment(coins),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "spin_history")
            Result.failure(e)
        }
    }

    suspend fun deleteAccountSecurely(userId: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Refusing cross-user deletion" }
            db.collection("wallets").document(uid).delete().await()
            db.collection("quiz_scores").document(uid).delete().await()
            db.collection("leaderboard").document(uid).delete().await()
            db.collection("users").document(uid).delete().await()
            localDao.clearProfile(uid)
            localDao.clearWallet(uid)
            localDao.clearTransactions(uid)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$userId")
            Result.failure(e)
        }
    }
}
