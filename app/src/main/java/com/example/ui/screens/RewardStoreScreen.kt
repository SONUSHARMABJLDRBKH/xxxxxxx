package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CurrencyRupee
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.RewardCatalogItem
import com.example.data.model.Wallet
import com.example.ui.components.GlassCard
import com.example.ui.components.GlowingGoldButton
import com.example.ui.components.GlowingPurpleButton
import com.example.ui.components.formatCoins
import com.example.ui.strings.LocalAppStrings
import com.example.ui.theme.*

@Composable
fun RewardStoreScreen(
    rewards: List<RewardCatalogItem>,
    wallet: Wallet?,
    onRedeemReward: (RewardCatalogItem, String) -> Unit,
    onOpenMonthlyRewards: () -> Unit
) {
    val strings = LocalAppStrings.current
    val availableCoins = wallet?.availableCoins ?: 0L
    var selectedReward by remember { mutableStateOf<RewardCatalogItem?>(null) }
    var upiOrAddress by remember { mutableStateOf("") }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 170.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(strings.rewardStore, style = MaterialTheme.typography.headlineMedium)
                    Text(
                        text = "Balance: ${formatCoins(availableCoins)} Coins",
                        style = MaterialTheme.typography.bodySmall,
                        color = GoldPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                GlowingPurpleButton(
                    text = strings.monthlyRewards,
                    onClick = onOpenMonthlyRewards,
                    testTag = "store_monthly_btn"
                )
            }
        }

        items(rewards.filter { it.active }, key = { it.id }) { item ->
            val canAfford = availableCoins >= item.coinCost && item.inventoryCount > 0L
            GlassCard(borderColor = GoldPrimary.copy(alpha = 0.45f)) {
                RewardProductArtwork(imageKey = item.imageKey, title = item.title)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${formatCoins(item.coinCost)} Coins • Stock: ${item.inventoryCount}",
                    style = MaterialTheme.typography.labelMedium,
                    color = GoldPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.deliveryTerms,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(10.dp))
                GlowingGoldButton(
                    text = if (item.inventoryCount <= 0L) "Out of Stock" else if (canAfford) "Redeem Now" else "Need ${formatCoins((item.coinCost - availableCoins).coerceAtLeast(0L))} more",
                    onClick = {
                        upiOrAddress = ""
                        selectedReward = item
                    },
                    enabled = canAfford,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "redeem_${item.id}"
                )
            }
        }
    }

    selectedReward?.let { reward ->
        AlertDialog(
            onDismissRequest = { selectedReward = null },
            title = { Text("Redeem ${reward.title}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Cost: ${formatCoins(reward.coinCost)} Coins")
                    Text(reward.deliveryTerms, style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = upiOrAddress,
                        onValueChange = { upiOrAddress = it },
                        label = {
                            Text(if (reward.category.equals("UPI", ignoreCase = true)) "Enter UPI ID (e.g. name@upi)" else "Full Delivery Address & PIN")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRedeemReward(reward, upiOrAddress)
                        selectedReward = null
                    },
                    enabled = upiOrAddress.trim().length >= 5
                ) { Text(strings.confirm) }
            },
            dismissButton = {
                TextButton(onClick = { selectedReward = null }) { Text(strings.cancel) }
            }
        )
    }
}

@Composable
fun RewardProductArtwork(imageKey: String, title: String, modifier: Modifier = Modifier) {
    val resId = when (imageKey.lowercase()) {
        "watch" -> R.drawable.img_product_watch
        "smartwatch" -> R.drawable.img_product_smartwatch
        "study_table" -> R.drawable.img_product_study_table
        "smartphone" -> R.drawable.img_product_smartphone
        "charger" -> R.drawable.img_product_charger
        else -> null
    }
    if (resId != null) {
        Image(
            painter = painterResource(id = resId),
            contentDescription = title,
            modifier = modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(14.dp)),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF1F3B2C), Color(0xFF065F46)))),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.CurrencyRupee, contentDescription = title, tint = GoldBright, modifier = Modifier.size(44.dp))
                Text("UPI Reward", style = MaterialTheme.typography.labelLarge, color = Color.White)
            }
        }
    }
}
