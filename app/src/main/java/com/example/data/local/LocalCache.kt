package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.QuizQuestion
import com.example.data.model.UserProfile
import com.example.data.model.Wallet
import com.example.data.model.WalletTransaction
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "cached_profiles")
data class CachedProfileEntity(
    @PrimaryKey val userId: String,
    val displayName: String,
    val email: String,
    val photoUrl: String,
    val phoneNumber: String,
    val phoneVerified: Boolean,
    val language: String,
    val darkMode: Boolean,
    val notificationsEnabled: Boolean,
    val role: String,
    val blocked: Boolean,
    val blockReason: String,
    val welcomeBonusClaimed: Boolean
)

@Entity(tableName = "cached_wallets")
data class CachedWalletEntity(
    @PrimaryKey val userId: String,
    val availableCoins: Long,
    val totalEarnedCoins: Long,
    val welcomeBonusCoins: Long,
    val dailyBonusCoins: Long,
    val scratchCoins: Long,
    val spinCoins: Long,
    val quizCoins: Long,
    val adCoins: Long,
    val redeemedCoins: Long,
    val adRewardsCount: Long,
    val specialSpinsUnlocked: Long,
    val specialSpinsUsed: Long,
    val lastDailyBonusAt: Long,
    val dailyScratchDate: String,
    val dailyScratchUsed: Long,
    val extraScratchAvailable: Long,
    val lastFreeSpinAt: Long,
    val adFreePassExpiresAt: Long
)

@Entity(tableName = "cached_transactions")
data class CachedTransactionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val txType: String,
    val amount: Long,
    val status: String,
    val description: String,
    val referenceId: String,
    val createdAtMillis: Long
)

@Entity(tableName = "cached_questions")
data class CachedQuestionEntity(
    @PrimaryKey val id: String,
    val questionClass: Int,
    val subject: String,
    val chapter: String,
    val questionText: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctOption: String,
    val explanation: String,
    val source: String,
    val examYear: String
)

@Dao
interface StudyWinDao {
    @Query("SELECT * FROM cached_profiles WHERE userId = :userId LIMIT 1")
    fun observeProfile(userId: String): Flow<CachedProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(entity: CachedProfileEntity)

    @Query("SELECT * FROM cached_wallets WHERE userId = :userId LIMIT 1")
    fun observeWallet(userId: String): Flow<CachedWalletEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWallet(entity: CachedWalletEntity)

    @Query("SELECT * FROM cached_transactions WHERE userId = :userId ORDER BY createdAtMillis DESC")
    fun observeTransactions(userId: String): Flow<List<CachedTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransactions(items: List<CachedTransactionEntity>)

    @Query("SELECT * FROM cached_questions ORDER BY questionClass ASC, subject ASC")
    fun observeQuestions(): Flow<List<CachedQuestionEntity>>

    @Query("SELECT COUNT(*) FROM cached_questions")
    suspend fun countQuestions(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuestions(items: List<CachedQuestionEntity>)

    @Query("DELETE FROM cached_questions WHERE id = :id")
    suspend fun deleteQuestion(id: String)

    @Query("DELETE FROM cached_profiles WHERE userId = :userId")
    suspend fun clearProfile(userId: String)

    @Query("DELETE FROM cached_wallets WHERE userId = :userId")
    suspend fun clearWallet(userId: String)

    @Query("DELETE FROM cached_transactions WHERE userId = :userId")
    suspend fun clearTransactions(userId: String)
}

@Database(
    entities = [
        CachedProfileEntity::class,
        CachedWalletEntity::class,
        CachedTransactionEntity::class,
        CachedQuestionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class StudyWinDatabase : RoomDatabase() {
    abstract fun studyWinDao(): StudyWinDao

    companion object {
        @Volatile
        private var INSTANCE: StudyWinDatabase? = null

        fun getInstance(context: Context): StudyWinDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    StudyWinDatabase::class.java,
                    "studywin_offline_cache.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}

fun UserProfile.toEntity() = CachedProfileEntity(
    userId = userId,
    displayName = displayName,
    email = email,
    photoUrl = photoUrl,
    phoneNumber = phoneNumber,
    phoneVerified = phoneVerified,
    language = language,
    darkMode = darkMode,
    notificationsEnabled = notificationsEnabled,
    role = role,
    blocked = blocked,
    blockReason = blockReason,
    welcomeBonusClaimed = welcomeBonusClaimed
)

fun CachedProfileEntity.toDomain() = UserProfile(
    id = userId,
    userId = userId,
    displayName = displayName,
    email = email,
    photoUrl = photoUrl,
    phoneNumber = phoneNumber,
    phoneVerified = phoneVerified,
    language = language,
    darkMode = darkMode,
    notificationsEnabled = notificationsEnabled,
    role = role,
    blocked = blocked,
    blockReason = blockReason,
    welcomeBonusClaimed = welcomeBonusClaimed
)

fun Wallet.toEntity() = CachedWalletEntity(
    userId = userId,
    availableCoins = availableCoins,
    totalEarnedCoins = totalEarnedCoins,
    welcomeBonusCoins = welcomeBonusCoins,
    dailyBonusCoins = dailyBonusCoins,
    scratchCoins = scratchCoins,
    spinCoins = spinCoins,
    quizCoins = quizCoins,
    adCoins = adCoins,
    redeemedCoins = redeemedCoins,
    adRewardsCount = adRewardsCount,
    specialSpinsUnlocked = specialSpinsUnlocked,
    specialSpinsUsed = specialSpinsUsed,
    lastDailyBonusAt = lastDailyBonusAt,
    dailyScratchDate = dailyScratchDate,
    dailyScratchUsed = dailyScratchUsed,
    extraScratchAvailable = extraScratchAvailable,
    lastFreeSpinAt = lastFreeSpinAt,
    adFreePassExpiresAt = adFreePassExpiresAt
)

fun CachedWalletEntity.toDomain() = Wallet(
    id = userId,
    userId = userId,
    availableCoins = availableCoins,
    totalEarnedCoins = totalEarnedCoins,
    welcomeBonusCoins = welcomeBonusCoins,
    dailyBonusCoins = dailyBonusCoins,
    scratchCoins = scratchCoins,
    spinCoins = spinCoins,
    quizCoins = quizCoins,
    adCoins = adCoins,
    redeemedCoins = redeemedCoins,
    adRewardsCount = adRewardsCount,
    specialSpinsUnlocked = specialSpinsUnlocked,
    specialSpinsUsed = specialSpinsUsed,
    lastDailyBonusAt = lastDailyBonusAt,
    dailyScratchDate = dailyScratchDate,
    dailyScratchUsed = dailyScratchUsed,
    extraScratchAvailable = extraScratchAvailable,
    lastFreeSpinAt = lastFreeSpinAt,
    adFreePassExpiresAt = adFreePassExpiresAt
)

fun WalletTransaction.toEntity() = CachedTransactionEntity(
    id = id,
    userId = userId,
    txType = txType,
    amount = amount,
    status = status,
    description = description,
    referenceId = referenceId,
    createdAtMillis = createdAt?.toDate()?.time ?: System.currentTimeMillis()
)

fun CachedTransactionEntity.toDomain() = WalletTransaction(
    id = id,
    userId = userId,
    txType = txType,
    amount = amount,
    status = status,
    description = description,
    referenceId = referenceId
)

fun QuizQuestion.toEntity() = CachedQuestionEntity(
    id = id,
    questionClass = questionClass,
    subject = subject,
    chapter = chapter,
    questionText = questionText,
    optionA = optionA,
    optionB = optionB,
    optionC = optionC,
    optionD = optionD,
    correctOption = correctOption,
    explanation = explanation,
    source = source,
    examYear = examYear
)

fun CachedQuestionEntity.toDomain() = QuizQuestion(
    id = id,
    questionClass = questionClass,
    subject = subject,
    chapter = chapter,
    questionText = questionText,
    optionA = optionA,
    optionB = optionB,
    optionC = optionC,
    optionD = optionD,
    correctOption = correctOption,
    explanation = explanation,
    source = source,
    examYear = examYear
)
