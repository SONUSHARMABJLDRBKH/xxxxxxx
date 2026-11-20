package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.local.StudyWinDao
import com.example.data.local.toDomain
import com.example.data.local.toEntity
import com.example.data.model.*
import com.example.data.quiz.BiharBoardQuestionBank
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.tasks.await
import java.util.UUID

class QuizAndRewardsRepository(
    private val db: FirebaseFirestore,
    private val localDao: StudyWinDao,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context, localDao: StudyWinDao) : this(
        FirebaseFirestore.getInstance(context.applicationContext.getString(R.string.firestore_database_id)),
        localDao
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("User must be signed in.")

    fun observeQuestions(): Flow<List<QuizQuestion>> = flow {
        val starter = BiharBoardQuestionBank.starterQuestions()
        if (localDao.countQuestions() == 0) {
            localDao.upsertQuestions(starter.map { it.toEntity() })
        }
        val path = "quiz_questions"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snap ->
                    val remote = snap.toObjects(QuizQuestion::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    val merged = (starter + remote).associateBy { it.id }.values.toList()
                    merged
                }
                .onEach { items -> localDao.upsertQuestions(items.map { it.toEntity() }) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeCachedQuestions(): Flow<List<QuizQuestion>> =
        localDao.observeQuestions().map { list ->
            if (list.isEmpty()) BiharBoardQuestionBank.starterQuestions()
            else list.map { it.toDomain() }
        }

    fun observeQuizScore(userId: String): Flow<QuizScoreSummary?> = flow {
        val path = "quiz_scores/$userId"
        emitAll(
            db.collection("quiz_scores").document(userId)
                .snapshots()
                .map { it.toObject(QuizScoreSummary::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
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

    suspend fun recordQuizAttempt(
        question: QuizQuestion,
        selectedOption: String,
        correct: Boolean,
        coinsAwarded: Long,
        adsWatchedForUnlock: Int
    ): Result<Unit> {
        val uid = requireUserId()
        return try {
            val points = if (correct) 0.09999 else 0.0
            val attemptId = "qa_${uid}_${UUID.randomUUID().toString().take(10)}"
            val scoreRef = db.collection("quiz_scores").document(uid)
            val walletRef = db.collection("wallets").document(uid)
            val lbRef = db.collection("leaderboard").document(uid)

            db.runTransaction { tx ->
                val scoreSnap = tx.get(scoreRef)
                val subjectField = when (question.subject.lowercase()) {
                    "mathematics" -> "mathScore"
                    "physics" -> "physicsScore"
                    "chemistry" -> "chemistryScore"
                    "biology" -> "biologyScore"
                    "hindi" -> "hindiScore"
                    else -> "englishScore"
                }
                val currentSubj = scoreSnap.getDouble(subjectField) ?: 0.0
                val currentOverall = scoreSnap.getDouble("overallScore") ?: 0.0

                val scoreUpdates = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "totalAttempted" to FieldValue.increment(1L),
                    "correctCount" to FieldValue.increment(if (correct) 1L else 0L),
                    "incorrectCount" to FieldValue.increment(if (correct) 0L else 1L),
                    subjectField to (currentSubj + points).coerceAtMost(100.0),
                    "overallScore" to (currentOverall + points).coerceAtMost(100.0),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                tx.set(scoreRef, scoreUpdates, com.google.firebase.firestore.SetOptions.merge())

                if (correct && coinsAwarded > 0L) {
                    tx.update(
                        walletRef,
                        mapOf(
                            "availableCoins" to FieldValue.increment(coinsAwarded),
                            "totalEarnedCoins" to FieldValue.increment(coinsAwarded),
                            "quizCoins" to FieldValue.increment(coinsAwarded),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    tx.set(
                        db.collection("transactions").document("tx_q_${attemptId}"),
                        mapOf(
                            "userId" to uid,
                            "txType" to "QUIZ_REWARD",
                            "amount" to coinsAwarded,
                            "status" to "COMPLETED",
                            "description" to "Bihar Board Class ${question.questionClass} ${question.subject} (+$coinsAwarded coins)",
                            "referenceId" to attemptId,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                tx.set(
                    db.collection("quiz_attempts").document(attemptId),
                    mapOf(
                        "userId" to uid,
                        "questionId" to question.id,
                        "questionClass" to question.questionClass,
                        "subject" to question.subject,
                        "selectedOption" to selectedOption,
                        "correct" to correct,
                        "coinsAwarded" to coinsAwarded,
                        "pointsAwarded" to points,
                        "adsWatchedForUnlock" to adsWatchedForUnlock,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()

            if (correct) {
                lbRef.update(
                    mapOf(
                        "totalQuizScore" to FieldValue.increment(points),
                        "weeklyScore" to FieldValue.increment(points),
                        "monthlyScore" to FieldValue.increment(points),
                        "totalCoinsEarned" to FieldValue.increment(coinsAwarded),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "quiz_attempts")
            Result.failure(e)
        }
    }

    suspend fun purchaseQuizAdFreePass(): Result<Long> {
        val uid = requireUserId()
        return try {
            val now = System.currentTimeMillis()
            val expiresAt = now + 24L * 60L * 60L * 1000L
            val walletRef = db.collection("wallets").document(uid)
            val passId = "pass_${uid}_${UUID.randomUUID().toString().take(8)}"

            db.runTransaction { tx ->
                val snap = tx.get(walletRef)
                val balance = snap.getLong("availableCoins") ?: 0L
                val existingExpiry = snap.getLong("adFreePassExpiresAt") ?: 0L
                if (existingExpiry > now) {
                    throw IllegalStateException("Quiz Ad-Free Pass is already active.")
                }
                if (balance < 6999L) {
                    throw IllegalStateException("Insufficient coins. 6,999 coins required.")
                }
                tx.update(
                    walletRef,
                    mapOf(
                        "availableCoins" to (balance - 6999L),
                        "redeemedCoins" to FieldValue.increment(6999L),
                        "adFreePassExpiresAt" to expiresAt,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    db.collection("ad_free_passes").document(passId),
                    mapOf(
                        "userId" to uid,
                        "costCoins" to 6999L,
                        "durationHours" to 24L,
                        "active" to true,
                        "expiresAt" to expiresAt,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    db.collection("transactions").document("tx_$passId"),
                    mapOf(
                        "userId" to uid,
                        "txType" to "AD_FREE_PASS",
                        "amount" to -6999L,
                        "status" to "COMPLETED",
                        "description" to "Purchased 24h Quiz Ad-Free Pass (-6,999 coins)",
                        "referenceId" to passId,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(expiresAt)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "ad_free_passes")
            Result.failure(e)
        }
    }

    fun observeRewards(): Flow<List<RewardCatalogItem>> = flow {
        val path = "rewards"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snap ->
                    val items = snap.toObjects(RewardCatalogItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    if (items.isEmpty()) defaultRewardCatalog() else items
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeAnnouncements(): Flow<List<Announcement>> = flow {
        val path = "announcements"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snap ->
                    val items = snap.toObjects(Announcement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    if (items.isEmpty()) defaultMonthlyAnnouncements() else items
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeSubmissions(userId: String, isAdmin: Boolean): Flow<List<RewardSubmission>> = flow {
        val path = "reward_submissions"
        val query = if (isAdmin) {
            db.collection(path)
        } else {
            db.collection(path).whereEqualTo("userId", userId)
        }
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(RewardSubmission::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeLeaderboard(): Flow<List<LeaderboardEntry>> = flow {
        val path = "leaderboard"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snap ->
                    snap.toObjects(LeaderboardEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.totalQuizScore }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeNotifications(userId: String): Flow<List<AppNotification>> = flow {
        val path = "notifications"
        emitAll(
            db.collection(path)
                .whereIn("userId", listOf(userId, "ALL"))
                .snapshots()
                .map { snap ->
                    snap.toObjects(AppNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeAppSettings(): Flow<AppSettings> = flow {
        val path = "app_config/settings"
        emitAll(
            db.document(path)
                .snapshots()
                .map { snap ->
                    snap.toObject(AppSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        ?: AppSettings()
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    handleFirestoreError(firestoreError ?: error, OperationType.GET, path)
                    throw error
                }
        )
    }

    suspend fun redeemReward(
        reward: RewardCatalogItem,
        username: String,
        upiOrAddress: String
    ): Result<Unit> {
        val uid = requireUserId()
        return try {
            require(upiOrAddress.trim().length >= 5) { "Enter valid UPI ID or delivery address." }
            val subId = "sub_${uid}_${UUID.randomUUID().toString().take(8)}"
            val txId = "tx_$subId"
            val walletRef = db.collection("wallets").document(uid)

            db.runTransaction { tx ->
                val wSnap = tx.get(walletRef)
                val available = wSnap.getLong("availableCoins") ?: 0L
                if (available < reward.coinCost) {
                    throw IllegalStateException("Not enough coins for ${reward.title}.")
                }
                tx.update(
                    walletRef,
                    mapOf(
                        "availableCoins" to (available - reward.coinCost),
                        "redeemedCoins" to FieldValue.increment(reward.coinCost),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    db.collection("reward_submissions").document(subId),
                    mapOf(
                        "userId" to uid,
                        "username" to username.take(75),
                        "rewardId" to reward.id,
                        "rewardTitle" to reward.title.take(110),
                        "submissionType" to "STORE_REDEMPTION",
                        "coinCost" to reward.coinCost,
                        "upiOrAddress" to upiOrAddress.trim().take(280),
                        "status" to "PENDING",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    db.collection("transactions").document(txId),
                    mapOf(
                        "userId" to uid,
                        "txType" to "REWARD_REDEMPTION",
                        "amount" to -reward.coinCost,
                        "status" to "PENDING",
                        "description" to "Requested ${reward.title} (${reward.coinCost} coins)",
                        "referenceId" to subId,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "reward_submissions")
            Result.failure(e)
        }
    }

    suspend fun submitMonthlyParticipation(
        category: String,
        coinCost: Long,
        username: String,
        instagramHandle: String,
        screenshotName: String
    ): Result<Unit> {
        val uid = requireUserId()
        return try {
            require(coinCost > 0L) { "A valid coin requirement is needed for $category." }
            require(instagramHandle.trim().isNotBlank()) { "Enter your Instagram handle." }
            require(screenshotName.trim().isNotBlank()) { "Select a confirmation screenshot." }
            val subId = "m_${uid}_${UUID.randomUUID().toString().take(8)}"
            val txId = "tx_$subId"
            val walletRef = db.collection("wallets").document(uid)

            db.runTransaction { tx ->
                val wSnap = tx.get(walletRef)
                val available = wSnap.getLong("availableCoins") ?: 0L
                if (available < coinCost) {
                    throw IllegalStateException("Not enough coins! You need $coinCost coins for $category (available: $available).")
                }
                tx.update(
                    walletRef,
                    mapOf(
                        "availableCoins" to (available - coinCost),
                        "redeemedCoins" to FieldValue.increment(coinCost),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    db.collection("reward_submissions").document(subId),
                    mapOf(
                        "userId" to uid,
                        "username" to username.take(75),
                        "rewardId" to "monthly_${category.lowercase().replace(Regex("[^a-z0-9]"), "_")}",
                        "rewardTitle" to "Monthly: $category".take(110),
                        "submissionType" to "MONTHLY_PARTICIPATION",
                        "coinCost" to coinCost,
                        "instagramHandle" to instagramHandle.trim().take(90),
                        "screenshotName" to screenshotName.take(180),
                        "status" to "PENDING",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    db.collection("transactions").document(txId),
                    mapOf(
                        "userId" to uid,
                        "txType" to "MONTHLY_SUBMISSION",
                        "amount" to -coinCost,
                        "status" to "PENDING",
                        "description" to "Monthly Reward entry: $category ($coinCost coins)",
                        "referenceId" to subId,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "reward_submissions")
            Result.failure(e)
        }
    }

    suspend fun markNotificationRead(notificationId: String): Result<Unit> {
        return try {
            db.collection("notifications").document(notificationId)
                .update("readFlag", true).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- ADMIN OPERATIONS (Guarded by Firestore Security Rules isAdmin()) ---

    suspend fun adminAddOrUpdateQuestion(q: QuizQuestion): Result<Unit> {
        val uid = requireUserId()
        return try {
            val docId = q.id.ifBlank { "q_${System.currentTimeMillis()}" }
            val ref = db.collection("quiz_questions").document(docId)
            val exists = ref.get().await().exists()
            val payload = mutableMapOf<String, Any>(
                "questionClass" to q.questionClass,
                "subject" to q.subject,
                "chapter" to q.chapter,
                "questionText" to q.questionText,
                "optionA" to q.optionA,
                "optionB" to q.optionB,
                "optionC" to q.optionC,
                "optionD" to q.optionD,
                "correctOption" to q.correctOption,
                "explanation" to q.explanation,
                "source" to q.source,
                "examYear" to q.examYear,
                "createdBy" to uid,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!exists) payload["createdAt"] = FieldValue.serverTimestamp()
            if (exists) ref.update(payload).await() else ref.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "quiz_questions")
            Result.failure(e)
        }
    }

    suspend fun adminBulkImportQuestions(questions: List<QuizQuestion>): Result<Int> {
        val uid = requireUserId()
        return try {
            var imported = 0
            questions.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { q ->
                    val ref = db.collection("quiz_questions").document(q.id)
                    batch.set(
                        ref,
                        mapOf(
                            "questionClass" to q.questionClass,
                            "subject" to q.subject,
                            "chapter" to q.chapter,
                            "questionText" to q.questionText,
                            "optionA" to q.optionA,
                            "optionB" to q.optionB,
                            "optionC" to q.optionC,
                            "optionD" to q.optionD,
                            "correctOption" to q.correctOption,
                            "explanation" to q.explanation,
                            "source" to q.source,
                            "examYear" to q.examYear,
                            "createdBy" to uid,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    imported++
                }
                batch.commit().await()
            }
            Result.success(imported)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "quiz_questions")
            Result.failure(e)
        }
    }

    suspend fun adminDeleteQuestion(questionId: String): Result<Unit> {
        return try {
            db.collection("quiz_questions").document(questionId).delete().await()
            localDao.deleteQuestion(questionId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminUpdateSubmissionStatus(submissionId: String, status: String, note: String): Result<Unit> {
        return try {
            db.collection("reward_submissions").document(submissionId).update(
                mapOf(
                    "status" to status,
                    "adminNote" to note.take(280),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "reward_submissions/$submissionId")
            Result.failure(e)
        }
    }

    suspend fun adminSaveRewardItem(item: RewardCatalogItem): Result<Unit> {
        return try {
            val id = item.id.ifBlank { "rew_${System.currentTimeMillis()}" }
            val ref = db.collection("rewards").document(id)
            val exists = ref.get().await().exists()
            val payload = mutableMapOf<String, Any>(
                "title" to item.title,
                "titleHi" to item.titleHi,
                "category" to item.category,
                "coinCost" to item.coinCost,
                "inventoryCount" to item.inventoryCount,
                "deliveryTerms" to item.deliveryTerms,
                "imageKey" to item.imageKey,
                "active" to item.active,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!exists) payload["createdAt"] = FieldValue.serverTimestamp()
            if (exists) ref.update(payload).await() else ref.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminUpdateAppSettings(settings: AppSettings): Result<Unit> {
        return try {
            db.document("app_config/settings").set(
                mapOf(
                    "adMobAppId" to settings.adMobAppId,
                    "bannerAdUnitId" to settings.bannerAdUnitId,
                    "rewardedAdUnitId" to settings.rewardedAdUnitId,
                    "interstitialAdUnitId" to settings.interstitialAdUnitId,
                    "useTestAds" to settings.useTestAds,
                    "instagramUrl" to settings.instagramUrl,
                    "dailyBonusMin" to settings.dailyBonusMin,
                    "dailyBonusMax" to settings.dailyBonusMax,
                    "scratchMin" to settings.scratchMin,
                    "scratchMax" to settings.scratchMax,
                    "spinWeightsCsv" to settings.spinWeightsCsv,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "app_config/settings")
            Result.failure(e)
        }
    }

    suspend fun adminBroadcastNotification(title: String, body: String, category: String): Result<Unit> {
        return try {
            val id = "notif_${System.currentTimeMillis()}"
            db.collection("notifications").document(id).set(
                mapOf(
                    "userId" to "ALL",
                    "title" to title.take(110),
                    "titleHi" to title.take(140),
                    "body" to body.take(480),
                    "bodyHi" to body.take(480),
                    "category" to category,
                    "readFlag" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminFetchOverviewStats(): Result<AdminOverviewStats> {
        return try {
            val usersSnap = db.collection("users").get().await()
            val walletsSnap = db.collection("wallets").get().await()
            val adsSnap = db.collection("ad_rewards").get().await()
            val usersList = usersSnap.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            val totalCoins = walletsSnap.documents.sumOf { it.getLong("totalEarnedCoins") ?: 0L }
            Result.success(
                AdminOverviewStats(
                    totalUsers = usersList.size,
                    activeUsers = usersList.count { !it.blocked },
                    totalCoinsDistributed = totalCoins,
                    totalAdRewardsVerified = adsSnap.size(),
                    users = usersList
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "users")
            Result.failure(e)
        }
    }

    suspend fun adminBlockUser(targetUserId: String, blocked: Boolean, reason: String): Result<Unit> {
        return try {
            db.collection("users").document(targetUserId).update(
                mapOf(
                    "blocked" to blocked,
                    "blockReason" to reason.take(280),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        fun defaultRewardCatalog(): List<RewardCatalogItem> = listOf(
            RewardCatalogItem(
                id = "rew_normal_watch",
                title = "Normal Watch",
                titleHi = "क्लासिक रिस्ट वॉच",
                category = "Watches",
                coinCost = 30000L,
                inventoryCount = 15L,
                deliveryTerms = "Dispatched within 7–10 working days across India after KYC & address verification.",
                imageKey = "watch",
                active = true
            ),
            RewardCatalogItem(
                id = "rew_smartwatch",
                title = "Smartwatch",
                titleHi = "AMOLED स्मार्टवॉच",
                category = "Wearables",
                coinCost = 50000L,
                inventoryCount = 10L,
                deliveryTerms = "Subject to stock & verification. Delivered via tracked courier in 7–14 days.",
                imageKey = "smartwatch",
                active = true
            ),
            RewardCatalogItem(
                id = "rew_upi_2000",
                title = "₹2000 UPI Reward",
                titleHi = "₹2000 UPI रिवॉर्ड",
                category = "UPI",
                coinCost = 120000L,
                inventoryCount = 25L,
                deliveryTerms = "Processed only after verified wallet audit & compliance check (3–5 working days).",
                imageKey = "upi",
                active = true
            ),
            RewardCatalogItem(
                id = "rew_study_table",
                title = "Study Table",
                titleHi = "स्टडी टेबल",
                category = "Study Gear",
                coinCost = 85000L,
                inventoryCount = 5L,
                deliveryTerms = "Delivered to serviceable Indian PIN codes after order confirmation.",
                imageKey = "study_table",
                active = true
            ),
            RewardCatalogItem(
                id = "rew_smartphone",
                title = "Smartphone",
                titleHi = "स्मार्टफोन",
                category = "Electronics",
                coinCost = 250000L,
                inventoryCount = 3L,
                deliveryTerms = "Requires full student verification & stock availability confirmation.",
                imageKey = "smartphone",
                active = true
            ),
            RewardCatalogItem(
                id = "rew_charger",
                title = "65W Fast Mobile Charger",
                titleHi = "65W फास्ट मोबाइल चार्जर",
                category = "Accessories",
                coinCost = 18000L,
                inventoryCount = 20L,
                deliveryTerms = "Delivered within 7 working days across serviceable PIN codes.",
                imageKey = "charger",
                active = true
            )
        )

        fun defaultMonthlyAnnouncements(): List<Announcement> = listOf(
            Announcement(
                id = "monthly_nov_2026",
                title = "Monthly Student Excellence Rewards",
                titleHi = "मासिक छात्र पुरस्कार कार्यक्रम",
                message = "Earn coins through Bihar Board Quizzes, Daily Bonus & Spin to unlock Monthly Rewards across ₹200 UPI, Normal Watch, Smartwatch, Study Table, and Smartphone.",
                messageHi = "क्विज़, डेली बोनस और स्पिन से कॉइन्स कमाएं और ₹200 UPI, नॉर्मल वॉच, स्मार्टवॉच, स्टडी टेबल व स्मार्टफोन के मासिक रिवॉर्ड के लिए सबमिट करें।",
                category = "MONTHLY_REWARD",
                winnerNames = "Verified winners are published here after official review.",
                instagramUrl = "https://www.instagram.com/study__win__india?evlk=MWdod2o0azVubjRzbA%3D%3D",
                closingDate = "30 Nov 2026",
                resultDate = "05 Dec 2026",
                availableSlots = 100L,
                participantCount = 0L,
                active = true
            )
        )
    }
}

data class AdminOverviewStats(
    val totalUsers: Int = 0,
    val activeUsers: Int = 0,
    val totalCoinsDistributed: Long = 0L,
    val totalAdRewardsVerified: Int = 0,
    val users: List<UserProfile> = emptyList()
)
