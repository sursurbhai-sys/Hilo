package com.example.ui

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.LeaderboardEntry
import com.example.data.UiState
import com.example.ui.theme.ArcaneCyan
import com.example.ui.theme.LudoGreen
import com.example.ui.theme.RoyalViolet
import com.example.ui.theme.UnicGold

@Composable
fun LeaderboardScreen(
    leaderboardState: UiState<List<LeaderboardEntry>>,
    currentUserId: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_trophy_crest),
                            contentDescription = "Championship Crest",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .border(2.dp, UnicGold, CircleShape)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = R.string.leaderboard_title),
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Text(
                                text = "Live global standings updated after every match",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            when (leaderboardState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
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
                                text = leaderboardState.message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
                is UiState.Success -> {
                    val entries = leaderboardState.data
                    if (entries.isEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.EmojiEvents,
                                        contentDescription = null,
                                        tint = UnicGold,
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No rankings yet", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "Play your first match in the Arena to claim #1!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        // Top 3 Podium Cards
                        item {
                            val topThree = entries.take(3)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                topThree.forEachIndexed { idx, entry ->
                                    PodiumCard(
                                        rank = idx + 1,
                                        entry = entry,
                                        isCurrentUser = entry.userId == currentUserId,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        itemsIndexed(entries, key = { _, item -> item.userId }) { index, entry ->
                            LeaderboardRow(
                                rank = index + 1,
                                entry = entry,
                                isCurrentUser = entry.userId == currentUserId
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PodiumCard(
    rank: Int,
    entry: LeaderboardEntry,
    isCurrentUser: Boolean,
    modifier: Modifier = Modifier
) {
    val medalColor = when (rank) {
        1 -> UnicGold
        2 -> Color(0xFFD0D8E8)
        else -> Color(0xFFCD7F32)
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.border(
            width = if (isCurrentUser) 2.dp else 1.dp,
            color = if (isCurrentUser) ArcaneCyan else medalColor.copy(alpha = 0.6f),
            shape = RoundedCornerShape(16.dp)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(medalColor.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#$rank",
                    style = MaterialTheme.typography.titleMedium,
                    color = medalColor,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = entry.displayName,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${entry.trophies} 🏆",
                style = MaterialTheme.typography.labelLarge,
                color = medalColor
            )
            Text(
                text = entry.tier,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LeaderboardRow(
    rank: Int,
    entry: LeaderboardEntry,
    isCurrentUser: Boolean
) {
    val rankColor = when (rank) {
        1 -> UnicGold
        2 -> Color(0xFFD0D8E8)
        3 -> Color(0xFFCD7F32)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentUser) ArcaneCyan.copy(alpha = 0.12f)
            else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isCurrentUser) 1.5.dp else 0.5.dp,
                color = if (isCurrentUser) ArcaneCyan else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "$rank",
                style = MaterialTheme.typography.titleLarge,
                color = rankColor,
                modifier = Modifier.width(28.dp)
            )

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(RoyalViolet, ArcaneCyan))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (entry.displayName.firstOrNull() ?: 'P').uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = entry.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isCurrentUser) {
                        Surface(
                            color = ArcaneCyan.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "YOU",
                                style = MaterialTheme.typography.labelSmall,
                                color = ArcaneCyan,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "${entry.wins}W / ${entry.gamesPlayed}G • ${entry.captures} Captures • ${entry.tier}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.EmojiEvents,
                    contentDescription = "Trophies",
                    tint = LudoGreen,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "${entry.trophies}",
                    style = MaterialTheme.typography.titleMedium,
                    color = UnicGold
                )
            }
        }
    }
}
