package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.ads.AdMobManager

class StudyWinApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AdMobManager.getInstance().initializeOnce(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_REWARDS,
                "StudyWin Rewards & Quizzes",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily bonuses, quiz milestones, spin availability, and reward updates"
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_REWARDS = "studywin_rewards"
    }
}
