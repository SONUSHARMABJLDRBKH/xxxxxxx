package com.example.ui.screens

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.ads.findActivity
import com.example.data.model.UserProfile
import com.example.data.model.Wallet
import com.example.ui.components.CoinBalancePill
import com.example.ui.components.GlassCard
import com.example.ui.components.GlowingGoldButton
import com.example.ui.components.formatCoins
import com.example.ui.strings.LocalAppStrings
import com.example.ui.theme.*

data class HomeTile(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color,
    val route: String,
    val badge: String? = null
)

@Composable
fun HomeDashboardScreen(
    profile: UserProfile?,
    wallet: Wallet?,
    unreadNotifications: Int,
    isAdmin: Boolean,
    onNavigate: (String) -> Unit,
    onWatchAdEarn: (Activity?) -> Unit,
    onToggleLanguage: () -> Unit
) {
    val strings = LocalAppStrings.current
    val activity = LocalContext.current.findActivity()
    val coins = wallet?.availableCoins ?: 0L
    val totalEarned = wallet?.totalEarnedCoins ?: 0L

    val tiles = buildList {
        add(HomeTile("daily_bonus", strings.dailyBonus, "1–50 Coins / 24h", Icons.Rounded.CalendarMonth, GoldPrimary, Routes.EARN))
        add(HomeTile("lucky_spin", strings.luckySpin, "Win up to 999 Coins", Icons.Rounded.Casino, PurpleVibrant, Routes.SPIN))
        add(HomeTile("scratch", strings.scratchCards, "3 Free Cards Daily", Icons.Rounded.Style, CyanAccent, Routes.EARN))
        add(HomeTile("watch_ads", strings.watchAdsEarn, "Verified AdMob Coins", Icons.Rounded.PlayCircle, EmeraldSuccess, "ACTION_WATCH_AD"))
        add(HomeTile("play_quiz", strings.playQuiz, "Bihar Board 9–12", Icons.Rounded.School, GoldBright, Routes.QUIZ))
        add(HomeTile("scoreboard", strings.scoreboard, "0.09999 pts & Pass", Icons.Rounded.Assessment, Color(0xFFF472B6), Routes.SCOREBOARD))
        add(HomeTile("store", strings.rewardStore, "Watches, UPI, Table", Icons.Rounded.Storefront, GoldPrimary, Routes.STORE))
        add(HomeTile("monthly", strings.monthlyRewards, "Coins + Monthly Prizes", Icons.Rounded.EmojiEvents, AmberWarning, Routes.MONTHLY))
        add(HomeTile("leaderboard", strings.leaderboard, "Weekly & Monthly", Icons.Rounded.Leaderboard, CyanAccent, Routes.LEADERBOARD))
        add(HomeTile("history", strings.transactionHistory, "Earned & Spent", Icons.Rounded.ReceiptLong, PurpleVibrant, Routes.WALLET))
        add(
            HomeTile(
                "notifications",
                strings.notifications,
                if (unreadNotifications > 0) "$unreadNotifications unread" else "All caught up",
                Icons.Rounded.NotificationsActive,
                EmeraldSuccess,
                Routes.NOTIFICATIONS,
                badge = if (unreadNotifications > 0) unreadNotifications.toString() else null
            )
        )
        add(HomeTile("support", strings.helpSupport, "Privacy, Terms & FAQ", Icons.Rounded.SupportAgent, Color(0xFF94A3B8), Routes.SUPPORT))
        if (isAdmin) {
            add(HomeTile("admin", strings.adminPanel, "Manage App & Ads", Icons.Rounded.AdminPanelSettings, RubyError, Routes.ADMIN))
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 156.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header + Profile + Wallet Balance
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f).clickable { onNavigate(Routes.PROFILE) }
                ) {
                    if (!profile?.photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = profile?.photoUrl,
                            contentDescription = "Profile photo",
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, GoldPrimary, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(PurplePrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Person, contentDescription = "Profile", tint = Color.White)
                        }
                    }
                    Column {
                        Text(
                            text = "Namaste, ${profile?.displayName ?: "Student"}",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = strings.tagline,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onToggleLanguage,
                        modifier = Modifier.testTag("home_lang_button")
                    ) {
                        Icon(Icons.Rounded.Translate, contentDescription = "Switch Language", tint = GoldPrimary)
                    }
                    CoinBalancePill(coins = coins, onClick = { onNavigate(Routes.WALLET) })
                }
            }
        }

        // Hero Welcome + Wallet Card
        item(span = { GridItemSpan(maxLineSpan) }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF28125E), Color(0xFF141D4B), Color(0xFF3E2B00))
                        )
                    )
                    .border(1.5.dp, Brush.horizontalGradient(listOf(GoldPrimary, PurpleVibrant)), RoundedCornerShape(24.dp))
                    .padding(18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_studywin_logo),
                        contentDescription = "StudyWin India Emblem",
                        modifier = Modifier
                            .size(86.dp)
                            .clip(RoundedCornerShape(20.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.availableCoins,
                            style = MaterialTheme.typography.labelMedium,
                            color = GoldBright
                        )
                        Text(
                            text = "${formatCoins(coins)} Coins",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "${strings.totalEarned}: ${formatCoins(totalEarned)} • Ads: ${wallet?.adRewardsCount ?: 0}/100",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD6DCFA)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GlowingGoldButton(
                                text = strings.playQuiz,
                                onClick = { onNavigate(Routes.QUIZ) },
                                modifier = Modifier.weight(1f),
                                testTag = "hero_play_quiz_btn"
                            )
                        }
                    }
                }
            }
        }

        // Feature Grid
        items(tiles, key = { it.id }) { tile ->
            GlassCard(
                onClick = {
                    if (tile.route == "ACTION_WATCH_AD") {
                        onWatchAdEarn(activity)
                    } else {
                        onNavigate(tile.route)
                    }
                },
                borderColor = tile.accent.copy(alpha = 0.45f),
                modifier = Modifier.testTag("home_tile_${tile.id}")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(tile.accent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(tile.icon, contentDescription = tile.title, tint = tile.accent)
                    }
                    if (tile.badge != null) {
                        Badge(containerColor = RubyError) {
                            Text(tile.badge, color = Color.White)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = tile.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = tile.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
