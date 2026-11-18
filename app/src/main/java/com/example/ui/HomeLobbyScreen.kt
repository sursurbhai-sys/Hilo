package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.MatchRoom
import com.example.data.PowerUpType
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.game.GameMode
import com.example.ui.theme.ArcaneCyan
import com.example.ui.theme.LudoBlue
import com.example.ui.theme.LudoGreen
import com.example.ui.theme.LudoRed
import com.example.ui.theme.LudoYellow
import com.example.ui.theme.MidnightArena
import com.example.ui.theme.RoyalViolet
import com.example.ui.theme.UnicGold

@Composable
fun HomeLobbyScreen(
    profileState: UiState<UserProfile?>,
    openRoomsState: UiState<List<MatchRoom>>,
    isRoomBusy: Boolean,
    onStartLocalMatch: (GameMode) -> Unit,
    onCreateOnlineRoom: () -> Unit,
    onJoinOnlineRoom: (String) -> Unit,
    onOpenArmory: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile = (profileState as? UiState.Success)?.data
    var roomCodeInput by remember { mutableStateOf("") }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Player Top Profile & Wallet Bar
            item {
                PlayerHeaderCard(
                    profile = profile,
                    onSignOut = onSignOut
                )
            }

            // 2. Hero Arena Banner
            item {
                HeroBannerCard(
                    onQuickPlay = { onStartLocalMatch(GameMode.AI_ARENA) }
                )
            }

            // 3. Game Modes Grid
            item {
                Text(
                    text = "Battle Modes",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ModeActionCard(
                            title = stringResource(id = R.string.play_ai_duel),
                            subtitle = "Fast 1v1 vs Nova Bot",
                            icon = Icons.Filled.SportsEsports,
                            accent = LudoRed,
                            tag = "play_duel_button",
                            onClick = { onStartLocalMatch(GameMode.AI_DUEL) },
                            modifier = Modifier.weight(1f)
                        )
                        ModeActionCard(
                            title = stringResource(id = R.string.play_ai_arena),
                            subtitle = "4-Player Tactical AI",
                            icon = Icons.Filled.SmartToy,
                            accent = LudoGreen,
                            tag = "play_arena_button",
                            onClick = { onStartLocalMatch(GameMode.AI_ARENA) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    ModeActionCard(
                        title = stringResource(id = R.string.play_pass_play),
                        subtitle = "4 players on one screen with Power-Ups",
                        icon = Icons.Filled.Groups,
                        accent = LudoBlue,
                        tag = "play_pass_play_button",
                        onClick = { onStartLocalMatch(GameMode.PASS_AND_PLAY) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 4. Power-Up Loadout Strip
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your Power-Up Loadout",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    FilledTonalButton(
                        onClick = onOpenArmory,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("open_armory_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = "Open Armory",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Armory", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(PowerUpType.entries) { powerUp ->
                        val charges = profile?.chargeCount(powerUp) ?: 0L
                        PowerUpMiniChip(powerUp = powerUp, charges = charges)
                    }
                }
            }

            // 5. Online Multiplayer Room Hub
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            RoundedCornerShape(20.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Public,
                                contentDescription = "Online Multiplayer",
                                tint = ArcaneCyan
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Online Multiplayer Rooms",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = "Create a private code or join an open live match",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = onCreateOnlineRoom,
                            enabled = !isRoomBusy,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RoyalViolet,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("create_room_button")
                        ) {
                            if (isRoomBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.AddCircle,
                                    contentDescription = "Create Room"
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(id = R.string.create_online_room),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = roomCodeInput,
                                onValueChange = { roomCodeInput = it.uppercase().take(12) },
                                placeholder = { Text(stringResource(id = R.string.join_room_hint)) },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("room_code_input")
                            )
                            OutlinedButton(
                                onClick = {
                                    if (roomCodeInput.isNotBlank()) {
                                        onJoinOnlineRoom(roomCodeInput)
                                    }
                                },
                                enabled = roomCodeInput.trim().length >= 4 && !isRoomBusy,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .height(54.dp)
                                    .testTag("join_room_button")
                            ) {
                                Text(stringResource(id = R.string.join_room))
                            }
                        }

                        when (openRoomsState) {
                            is UiState.Loading -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Text(
                                        text = "Scanning live rooms…",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            is UiState.Error -> {
                                Text(
                                    text = openRoomsState.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            is UiState.Success -> {
                                val rooms = openRoomsState.data
                                if (rooms.isEmpty()) {
                                    Text(
                                        text = "No open rooms waiting right now — create one above!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Text(
                                        text = "Open Lobbies (${rooms.size})",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = ArcaneCyan
                                    )
                                    rooms.forEach { room ->
                                        OpenRoomRow(
                                            room = room,
                                            onJoin = { onJoinOnlineRoom(room.roomCode) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerHeaderCard(
    profile: UserProfile?,
    onSignOut: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(UnicGold, LudoRed))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (profile?.displayName?.firstOrNull() ?: 'U').uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                }
                Column {
                    Text(
                        text = profile?.displayName ?: "Loading player…",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatBadge(
                            icon = Icons.Filled.EmojiEvents,
                            value = "${profile?.trophies ?: 100}",
                            tint = UnicGold
                        )
                        StatBadge(
                            icon = Icons.Filled.MonetizationOn,
                            value = "${profile?.coins ?: 200}",
                            tint = LudoYellow
                        )
                        Surface(
                            color = RoyalViolet.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = profile?.rankTier() ?: "Bronze",
                                style = MaterialTheme.typography.labelSmall,
                                color = RoyalViolet,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            IconButton(
                onClick = onSignOut,
                modifier = Modifier.testTag("sign_out_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = stringResource(id = R.string.sign_out),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatBadge(icon: ImageVector, value: String, tint: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun HeroBannerCard(onQuickPlay: () -> Unit) {
    Card(
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(176.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_banner),
                contentDescription = "Ludo Unic Arena Hero Art",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MidnightArena.copy(alpha = 0.92f),
                                MidnightArena.copy(alpha = 0.55f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    color = UnicGold.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "SEASON 1 • POWER-UP ROYALE",
                        style = MaterialTheme.typography.labelSmall,
                        color = UnicGold,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Column {
                    Text(
                        text = "Outsmart Every Roll",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    Text(
                        text = "Collect board orbs, unleash shields & race to Home.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFD2DCF2)
                    )
                }
                Button(
                    onClick = onQuickPlay,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = UnicGold,
                        contentColor = MidnightArena
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("hero_quick_play_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Quick Play",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Quick Arena", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun ModeActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accent,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun PowerUpMiniChip(powerUp: PowerUpType, charges: Long) {
    val accent = powerUpColor(powerUp)
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = powerUp.badgeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    fontWeight = FontWeight.Bold
                )
            }
            Column {
                Text(powerUp.title, style = MaterialTheme.typography.labelLarge)
                Text(
                    text = "x$charges ready",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun OpenRoomRow(room: MatchRoom, onJoin: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Room ${room.roomCode}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${room.playerNames.firstOrNull() ?: "Host"} • ${room.playerUids.size}/4 Players",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onJoin,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.testTag("join_open_room_${room.roomCode}")
            ) {
                Text("Join")
            }
        }
    }
}

fun powerUpColor(type: PowerUpType): Color = when (type) {
    PowerUpType.TURBO_BOOST -> UnicGold
    PowerUpType.SHIELD -> ArcaneCyan
    PowerUpType.SWAP -> RoyalViolet
    PowerUpType.LUCKY_SIX -> LudoRed
    PowerUpType.FREEZE -> LudoBlue
    PowerUpType.MAGNET -> LudoGreen
}
