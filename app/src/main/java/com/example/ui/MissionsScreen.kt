package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.DailyMission
import com.example.data.PowerUpType
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.ui.theme.LudoGreen
import com.example.ui.theme.LudoYellow
import com.example.ui.theme.MidnightArena
import com.example.ui.theme.RoyalViolet
import com.example.ui.theme.UnicGold

@Composable
fun MissionsScreen(
    profileState: UiState<UserProfile?>,
    missionsState: UiState<List<DailyMission>>,
    onClaimMission: (DailyMission) -> Unit,
    onBuyPowerUp: (PowerUpType) -> Unit,
    modifier: Modifier = Modifier
) {
    val profile = (profileState as? UiState.Success)?.data

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(id = R.string.daily_missions_title),
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "Complete quests to earn XP & Coins",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        color = UnicGold.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MonetizationOn,
                                contentDescription = "Coins",
                                tint = LudoYellow,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "${profile?.coins ?: 0}",
                                style = MaterialTheme.typography.titleMedium,
                                color = UnicGold
                            )
                        }
                    }
                }
            }

            when (missionsState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                is UiState.Error -> {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = missionsState.message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
                is UiState.Success -> {
                    val missions = missionsState.data
                    if (missions.isEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.MilitaryTech,
                                        contentDescription = null,
                                        tint = UnicGold,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Today's missions are syncing…", style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                    } else {
                        items(missions, key = { it.id }) { mission ->
                            DailyMissionCard(
                                mission = mission,
                                onClaim = { onClaimMission(mission) }
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(id = R.string.powerup_armory_title),
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Stock up on tactical charges before entering the Arena",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(PowerUpType.entries) { powerUp ->
                val charges = profile?.chargeCount(powerUp) ?: 0L
                val canAfford = (profile?.coins ?: 0L) >= powerUp.coinCost
                PowerUpShopCard(
                    powerUp = powerUp,
                    charges = charges,
                    canAfford = canAfford,
                    onBuy = { onBuyPowerUp(powerUp) }
                )
            }
        }
    }
}

@Composable
private fun DailyMissionCard(
    mission: DailyMission,
    onClaim: () -> Unit
) {
    val clampedProgress = mission.progress.coerceAtMost(mission.target)
    val ratio = if (mission.target > 0) clampedProgress.toFloat() / mission.target.toFloat() else 0f
    val readyToClaim = !mission.claimed && mission.progress >= mission.target

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (readyToClaim) LudoGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                RoundedCornerShape(18.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(mission.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = mission.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                when {
                    mission.claimed -> {
                        Surface(
                            color = LudoGreen.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Claimed",
                                    tint = LudoGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = stringResource(id = R.string.claimed_label),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = LudoGreen
                                )
                            }
                        }
                    }
                    readyToClaim -> {
                        Button(
                            onClick = onClaim,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LudoGreen,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.testTag("claim_mission_${mission.missionType}")
                        ) {
                            Text(stringResource(id = R.string.claim_reward))
                        }
                    }
                    else -> {
                        Text(
                            text = "$clampedProgress / ${mission.target}",
                            style = MaterialTheme.typography.titleMedium,
                            color = UnicGold
                        )
                    }
                }
            }

            LinearProgressIndicator(
                progress = { ratio },
                color = if (mission.claimed || readyToClaim) LudoGreen else UnicGold,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "+${mission.rewardXp} XP",
                    style = MaterialTheme.typography.labelSmall,
                    color = RoyalViolet,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "+${mission.rewardCoins} Coins",
                    style = MaterialTheme.typography.labelSmall,
                    color = UnicGold,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PowerUpShopCard(
    powerUp: PowerUpType,
    charges: Long,
    canAfford: Boolean,
    onBuy: () -> Unit
) {
    val accent = powerUpColor(powerUp)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Bolt,
                    contentDescription = powerUp.title,
                    tint = accent,
                    modifier = Modifier.size(26.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(powerUp.title, style = MaterialTheme.typography.titleMedium)
                    Surface(
                        color = accent.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "x$charges",
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = powerUp.shortDesc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = onBuy,
                enabled = canAfford,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.testTag("buy_powerup_${powerUp.name}")
            ) {
                Text("${powerUp.coinCost}c", color = if (canAfford) UnicGold else Color.Gray)
            }
        }
    }
}
