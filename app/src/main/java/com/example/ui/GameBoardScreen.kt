package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.MatchRoom
import com.example.data.PowerUpType
import com.example.data.UserProfile
import com.example.game.GameMode
import com.example.game.LudoEngine
import com.example.game.LudoGameState
import com.example.game.PlayerColor
import com.example.game.PlayerSeat
import com.example.game.TurnPhase
import com.example.ui.theme.ArcaneCyan
import com.example.ui.theme.LudoBlue
import com.example.ui.theme.LudoBlueLight
import com.example.ui.theme.LudoGreen
import com.example.ui.theme.LudoGreenLight
import com.example.ui.theme.LudoRed
import com.example.ui.theme.LudoRedLight
import com.example.ui.theme.LudoYellow
import com.example.ui.theme.LudoYellowLight
import com.example.ui.theme.MidnightArena
import com.example.ui.theme.UnicGold
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun WaitingRoomOverlay(
    room: MatchRoom,
    currentUserId: String,
    onStartMatch: () -> Unit,
    onCancelRoom: () -> Unit
) {
    BackHandler { onCancelRoom() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Online Multiplayer Lobby", style = MaterialTheme.typography.titleLarge)
                Surface(
                    color = UnicGold.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.border(1.5.dp, UnicGold, RoundedCornerShape(14.dp))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "ROOM CODE",
                            style = MaterialTheme.typography.labelSmall,
                            color = UnicGold
                        )
                        Text(
                            text = room.roomCode,
                            style = MaterialTheme.typography.displayLarge,
                            color = UnicGold
                        )
                    }
                }

                Text(
                    text = "Share this code with friends or wait for challengers (${room.playerUids.size}/4)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    room.playerNames.forEachIndexed { idx, name ->
                        val colorTag = room.playerColors.getOrElse(idx) { "RED" }
                        val seatColor = when (colorTag) {
                            "GREEN" -> LudoGreen
                            "YELLOW" -> LudoYellow
                            "BLUE" -> LudoBlue
                            else -> LudoRed
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(seatColor)
                                )
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                if (room.playerUids.getOrNull(idx) == room.hostId) {
                                    Text(
                                        text = "HOST",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = UnicGold
                                    )
                                }
                            }
                        }
                    }
                }

                val isHost = room.hostId == currentUserId
                if (isHost) {
                    Button(
                        onClick = onStartMatch,
                        enabled = room.playerUids.size >= 2,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LudoGreen,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("start_online_match_button")
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (room.playerUids.size >= 2) "Start Match"
                            else "Waiting for 1+ Opponent…"
                        )
                    }
                } else {
                    Text(
                        text = "Waiting for the host to start the match…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ArcaneCyan
                    )
                }

                OutlinedButton(
                    onClick = onCancelRoom,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("leave_waiting_room_button")
                ) {
                    Text(if (isHost) "Close Room" else "Leave Lobby")
                }
            }
        }
    }
}

@Composable
fun GameBoardScreen(
    gameState: LudoGameState,
    profile: UserProfile?,
    isDiceRolling: Boolean,
    matchError: String?,
    outcomeSummary: String?,
    currentUserId: String,
    onRollDice: () -> Unit,
    onMoveToken: (Int) -> Unit,
    onActivatePowerUp: (PowerUpType) -> Unit,
    onDismissError: () -> Unit,
    onPlayAgain: () -> Unit,
    onExitMatch: () -> Unit
) {
    BackHandler { onExitMatch() }

    val activeSeat = gameState.currentSeat
    val canLocalControl = activeSeat != null && !activeSeat.aiBot &&
        (gameState.mode != GameMode.ONLINE || activeSeat.uid == currentUserId)
    val legalTokens = if (canLocalControl && gameState.turnPhase == TurnPhase.MOVE) {
        LudoEngine.legalTokenIndices(gameState, activeSeat!!.seatIndex)
    } else emptyList()

    val pulseTransition = rememberInfiniteTransition(label = "token_pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(650),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onExitMatch,
                    modifier = Modifier.testTag("exit_match_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Exit Match"
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (gameState.roomId != null) "Online Room ${gameState.roomId}" else "Ludo Unic Arena",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = gameState.lastEventText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = UnicGold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (activeSeat != null) "${activeSeat.color.label} Turn" else "Match",
                        style = MaterialTheme.typography.labelLarge,
                        color = activeSeat?.let { seatAccentColor(it.color) } ?: UnicGold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Top Seats Row (Green & Yellow)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                gameState.seats.getOrNull(1)?.takeIf { it.active }?.let { seat ->
                    SeatStatusPill(
                        seat = seat,
                        isTurn = gameState.currentSeatIndex == seat.seatIndex,
                        homeCount = countTokensHome(gameState, seat.seatIndex),
                        shieldTurns = gameState.shieldTurns[seat.seatIndex],
                        frozenTurns = gameState.frozenTurns[seat.seatIndex],
                        modifier = Modifier.weight(1f)
                    )
                }
                gameState.seats.getOrNull(2)?.takeIf { it.active }?.let { seat ->
                    SeatStatusPill(
                        seat = seat,
                        isTurn = gameState.currentSeatIndex == seat.seatIndex,
                        homeCount = countTokensHome(gameState, seat.seatIndex),
                        shieldTurns = gameState.shieldTurns[seat.seatIndex],
                        frozenTurns = gameState.frozenTurns[seat.seatIndex],
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 15x15 Ludo Unic Board Canvas
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .widthIn(max = 540.dp)
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                val boardModifier = if (maxWidth <= maxHeight) {
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                } else {
                    Modifier
                        .height(maxHeight)
                        .aspectRatio(1f)
                }

                Card(
                    shape = RoundedCornerShape(18.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = boardModifier.border(
                        2.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                        RoundedCornerShape(18.dp)
                    )
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("ludo_board_canvas")
                            .pointerInput(gameState, legalTokens) {
                                detectTapGestures { offset ->
                                    if (legalTokens.isEmpty() || activeSeat == null) return@detectTapGestures
                                    val cellSize = size.width / 15f
                                    val seatIdx = activeSeat.seatIndex
                                    val base = seatIdx * 4
                                    var bestToken: Int? = null
                                    var bestDist = cellSize * 1.45f
                                    for (localIdx in legalTokens) {
                                        val step = gameState.tokenPositions[base + localIdx]
                                        val coord = LudoEngine.tokenBoardCoord(seatIdx, localIdx, step)
                                        val cx = (coord.col + 0.5f) * cellSize
                                        val cy = (coord.row + 0.5f) * cellSize
                                        val dist = hypot(offset.x - cx, offset.y - cy)
                                        if (dist < bestDist) {
                                            bestDist = dist
                                            bestToken = localIdx
                                        }
                                    }
                                    if (bestToken != null) {
                                        onMoveToken(bestToken)
                                    }
                                }
                            }
                    ) {
                        drawLudoUnicBoard(
                            state = gameState,
                            legalTokens = legalTokens,
                            pulseScale = pulseScale
                        )
                    }
                }
            }

            // Bottom Seats Row (Red & Blue)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                gameState.seats.getOrNull(0)?.takeIf { it.active }?.let { seat ->
                    SeatStatusPill(
                        seat = seat,
                        isTurn = gameState.currentSeatIndex == seat.seatIndex,
                        homeCount = countTokensHome(gameState, seat.seatIndex),
                        shieldTurns = gameState.shieldTurns[seat.seatIndex],
                        frozenTurns = gameState.frozenTurns[seat.seatIndex],
                        modifier = Modifier.weight(1f)
                    )
                }
                gameState.seats.getOrNull(3)?.takeIf { it.active }?.let { seat ->
                    SeatStatusPill(
                        seat = seat,
                        isTurn = gameState.currentSeatIndex == seat.seatIndex,
                        homeCount = countTokensHome(gameState, seat.seatIndex),
                        shieldTurns = gameState.shieldTurns[seat.seatIndex],
                        frozenTurns = gameState.frozenTurns[seat.seatIndex],
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Power-Ups Action Bar + Dice Control
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (matchError != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onDismissError)
                    ) {
                        Text(
                            text = matchError,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(PowerUpType.entries) { powerUp ->
                        val earned = gameState.earnedMatchPowerUps[powerUp] ?: 0
                        val stored = (profile?.chargeCount(powerUp) ?: 0L).toInt()
                        val total = earned + stored
                        val accent = powerUpColor(powerUp)
                        Surface(
                            color = if (canLocalControl && total > 0) accent.copy(alpha = 0.16f)
                            else MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    1.dp,
                                    if (canLocalControl && total > 0) accent else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable(enabled = canLocalControl && total > 0) {
                                    onActivatePowerUp(powerUp)
                                }
                                .testTag("use_powerup_${powerUp.name}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = powerUp.badgeLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = accent,
                                    fontWeight = FontWeight.Bold
                                )
                                Column {
                                    Text(
                                        text = powerUp.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "x$total",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DiceFaceBadge(
                        value = gameState.lastDiceValue,
                        isRolling = isDiceRolling,
                        turboPrimed = gameState.pendingTurbo || gameState.bonusSteps > 0,
                        accent = activeSeat?.let { seatAccentColor(it.color) } ?: UnicGold
                    )

                    Button(
                        onClick = onRollDice,
                        enabled = canLocalControl && gameState.turnPhase == TurnPhase.ROLL && !isDiceRolling,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = activeSeat?.let { seatAccentColor(it.color) } ?: UnicGold,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .testTag("roll_dice_button")
                    ) {
                        Icon(Icons.Filled.Casino, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                isDiceRolling -> "Rolling…"
                                gameState.turnPhase == TurnPhase.MOVE && canLocalControl -> "Tap a Glowing Pawn!"
                                canLocalControl -> "Roll Dice"
                                activeSeat?.aiBot == true -> "${activeSeat.name} Thinking…"
                                else -> "Waiting for ${activeSeat?.name ?: "Opponent"}…"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }

        // Victory Podium Overlay
        val winnerIdx = gameState.winnerSeatIndex
        if (winnerIdx != null) {
            val winnerSeat = gameState.seats.getOrNull(winnerIdx)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MidnightArena.copy(alpha = 0.82f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp)
                        .border(2.dp, UnicGold, RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EmojiEvents,
                            contentDescription = "Winner",
                            tint = UnicGold,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "${winnerSeat?.name ?: "Champion"} Wins!",
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center
                        )
                        if (outcomeSummary != null) {
                            Surface(
                                color = UnicGold.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = outcomeSummary,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = UnicGold,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                        Text(
                            text = "Captures: ${gameState.humanCaptures} • Power-Ups: ${gameState.humanPowerUpsUsed} • 6s: ${gameState.humanSixesRolled}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        if (gameState.mode != GameMode.ONLINE) {
                            Button(
                                onClick = onPlayAgain,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("play_again_button")
                            ) {
                                Text("Play Again")
                            }
                        }
                        OutlinedButton(
                            onClick = onExitMatch,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("return_lobby_button")
                        ) {
                            Text("Back to Arena")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeatStatusPill(
    seat: PlayerSeat,
    isTurn: Boolean,
    homeCount: Int,
    shieldTurns: Int,
    frozenTurns: Int,
    modifier: Modifier = Modifier
) {
    val accent = seatAccentColor(seat.color)
    Surface(
        color = if (isTurn) accent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.border(
            width = if (isTurn) 2.dp else 1.dp,
            color = if (isTurn) accent else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
            shape = RoundedCornerShape(12.dp)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Text(
                text = seat.name,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (shieldTurns > 0) {
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = "Shielded",
                    tint = ArcaneCyan,
                    modifier = Modifier.size(15.dp)
                )
            }
            if (frozenTurns > 0) {
                Icon(
                    imageVector = Icons.Filled.AcUnit,
                    contentDescription = "Frozen",
                    tint = LudoBlue,
                    modifier = Modifier.size(15.dp)
                )
            }
            Text(
                text = "$homeCount/4",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DiceFaceBadge(
    value: Int,
    isRolling: Boolean,
    turboPrimed: Boolean,
    accent: Color
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(2.5.dp, if (turboPrimed) UnicGold else accent, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (isRolling) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = "Rolling dice",
                tint = accent,
                modifier = Modifier.size(28.dp)
            )
        } else if (value in 1..6) {
            Canvas(modifier = Modifier.size(40.dp)) {
                val r = size.minDimension * 0.11f
                val c = size.minDimension / 2f
                val q1 = size.minDimension * 0.24f
                val q3 = size.minDimension * 0.76f
                val pipColor = Color(0xFF141829)
                val pips = when (value) {
                    1 -> listOf(Offset(c, c))
                    2 -> listOf(Offset(q1, q1), Offset(q3, q3))
                    3 -> listOf(Offset(q1, q1), Offset(c, c), Offset(q3, q3))
                    4 -> listOf(Offset(q1, q1), Offset(q3, q1), Offset(q1, q3), Offset(q3, q3))
                    5 -> listOf(Offset(q1, q1), Offset(q3, q1), Offset(c, c), Offset(q1, q3), Offset(q3, q3))
                    else -> listOf(
                        Offset(q1, q1), Offset(q3, q1),
                        Offset(q1, c), Offset(q3, c),
                        Offset(q1, q3), Offset(q3, q3)
                    )
                }
                pips.forEach { drawCircle(pipColor, radius = r, center = it) }
            }
        } else {
            Icon(
                imageVector = Icons.Filled.Casino,
                contentDescription = "Ready to roll",
                tint = Color(0xFF5A637E),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

private fun DrawScope.drawLudoUnicBoard(
    state: LudoGameState,
    legalTokens: List<Int>,
    pulseScale: Float
) {
    val cell = size.width / 15f
    val boardBg = Color(0xFFFAF8F2)
    val gridStroke = Color(0xFFD5CFC0)

    drawRect(color = boardBg, size = size)

    // Draw 52 outer track tiles
    for (idx in LudoEngine.MAIN_TRACK_COORDS.indices) {
        val coord = LudoEngine.MAIN_TRACK_COORDS[idx]
        val tileColor = when (idx) {
            0 -> LudoRed
            13 -> LudoGreen
            26 -> LudoYellow
            39 -> LudoBlue
            in LudoEngine.STAR_ONLY_TILES -> Color(0xFFECE6D4)
            else -> Color.White
        }
        val topLeft = Offset(coord.col * cell, coord.row * cell)
        drawRect(color = tileColor, topLeft = topLeft, size = Size(cell, cell))
        drawRect(color = gridStroke, topLeft = topLeft, size = Size(cell, cell), style = Stroke(width = 1.2f))

        if (idx in LudoEngine.SAFE_GLOBAL_TILES) {
            val starTint = if (idx in LudoEngine.STAR_ONLY_TILES) Color(0xFFB48600) else Color.White.copy(alpha = 0.9f)
            drawStar(
                center = Offset(topLeft.x + cell / 2f, topLeft.y + cell / 2f),
                outerRadius = cell * 0.32f,
                innerRadius = cell * 0.14f,
                color = starTint
            )
        }
    }

    // Draw 4 Home Stretch columns (5 squares each)
    drawHomeStretchColumn(cell, gridStroke, LudoRed, LudoRedLight, (9..13).map { it to 7 })
    drawHomeStretchColumn(cell, gridStroke, LudoGreen, LudoGreenLight, (1..5).map { 7 to it })
    drawHomeStretchColumn(cell, gridStroke, LudoYellow, LudoYellowLight, (1..5).map { it to 7 })
    drawHomeStretchColumn(cell, gridStroke, LudoBlue, LudoBlueLight, (9..13).map { 7 to it })

    // Draw 4 Corner Yards
    drawYard(cell, 0, 9, LudoRed, LudoRedLight)
    drawYard(cell, 0, 0, LudoGreen, LudoGreenLight)
    drawYard(cell, 9, 0, LudoYellow, LudoYellowLight)
    drawYard(cell, 9, 9, LudoBlue, LudoBlueLight)

    // Draw Center Goal (3x3 at row 6..8, col 6..8)
    val cLeft = 6f * cell
    val cTop = 6f * cell
    val cRight = 9f * cell
    val cBottom = 9f * cell
    val cMidX = 7.5f * cell
    val cMidY = 7.5f * cell

    drawTriangle(Offset(cLeft, cBottom), Offset(cRight, cBottom), Offset(cMidX, cMidY), LudoRed)
    drawTriangle(Offset(cLeft, cTop), Offset(cLeft, cBottom), Offset(cMidX, cMidY), LudoGreen)
    drawTriangle(Offset(cLeft, cTop), Offset(cRight, cTop), Offset(cMidX, cMidY), LudoYellow)
    drawTriangle(Offset(cRight, cTop), Offset(cRight, cBottom), Offset(cMidX, cMidY), LudoBlue)
    drawRect(color = Color(0xFF1E2640), topLeft = Offset(cLeft, cTop), size = Size(3f * cell, 3f * cell), style = Stroke(width = 2.5f))

    // Draw Power-Up Orbs on main track
    for (orb in state.orbs) {
        val coord = LudoEngine.MAIN_TRACK_COORDS.getOrNull(orb.globalTile) ?: continue
        val center = Offset((coord.col + 0.5f) * cell, (coord.row + 0.5f) * cell)
        val orbColor = powerUpColor(orb.type)
        drawCircle(color = orbColor.copy(alpha = 0.28f), radius = cell * 0.42f * pulseScale, center = center)
        drawCircle(color = orbColor, radius = cell * 0.25f, center = center)
        drawCircle(color = Color.White, radius = cell * 0.09f, center = center)
    }

    // Group pawns by coordinate key to apply clean radial offsets when stacked
    data class RenderPawn(
        val seatIndex: Int,
        val localIndex: Int,
        val center: Offset,
        val isLegal: Boolean
    )

    val allPawns = mutableListOf<RenderPawn>()
    for (seat in state.seats) {
        if (!seat.active) continue
        val base = seat.seatIndex * 4
        for (localIdx in 0..3) {
            val step = state.tokenPositions[base + localIdx]
            val coord = LudoEngine.tokenBoardCoord(seat.seatIndex, localIdx, step)
            val isLegal = seat.seatIndex == state.currentSeatIndex && localIdx in legalTokens
            allPawns.add(
                RenderPawn(
                    seatIndex = seat.seatIndex,
                    localIndex = localIdx,
                    center = Offset((coord.col + 0.5f) * cell, (coord.row + 0.5f) * cell),
                    isLegal = isLegal
                )
            )
        }
    }

    val grouped = allPawns.groupBy {
        "${(it.center.x / (cell * 0.25f)).toInt()}_${(it.center.y / (cell * 0.25f)).toInt()}"
    }

    for ((_, stack) in grouped) {
        stack.forEachIndexed { idxInStack, pawn ->
            val offsetAngle = if (stack.size > 1) (idxInStack * 2f * Math.PI / stack.size).toFloat() else 0f
            val stackShift = if (stack.size > 1) cell * 0.16f else 0f
            val pos = Offset(
                pawn.center.x + cos(offsetAngle) * stackShift,
                pawn.center.y + sin(offsetAngle) * stackShift
            )
            val seatColor = seatAccentColor(PlayerColor.entries[pawn.seatIndex])
            val pawnRadius = if (stack.size > 1) cell * 0.26f else cell * 0.34f

            if (pawn.isLegal) {
                drawCircle(
                    color = Color.White,
                    radius = pawnRadius * 1.38f * pulseScale,
                    center = pos,
                    style = Stroke(width = 4f)
                )
                drawCircle(
                    color = UnicGold.copy(alpha = 0.5f),
                    radius = pawnRadius * 1.25f * pulseScale,
                    center = pos
                )
            }

            if (state.shieldTurns[pawn.seatIndex] > 0) {
                drawCircle(
                    color = ArcaneCyan,
                    radius = pawnRadius * 1.2f,
                    center = pos,
                    style = Stroke(width = 3f)
                )
            }

            // Shadow + body + inner glossy cap
            drawCircle(
                color = Color.Black.copy(alpha = 0.28f),
                radius = pawnRadius,
                center = pos + Offset(1.5f, 3f)
            )
            drawCircle(
                color = seatColor,
                radius = pawnRadius,
                center = pos
            )
            drawCircle(
                color = Color.White,
                radius = pawnRadius,
                center = pos,
                style = Stroke(width = 2.4f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.45f),
                radius = pawnRadius * 0.42f,
                center = pos - Offset(pawnRadius * 0.18f, pawnRadius * 0.18f)
            )
        }
    }
}

private fun DrawScope.drawHomeStretchColumn(
    cell: Float,
    gridStroke: Color,
    color: Color,
    lightColor: Color,
    coords: List<Pair<Int, Int>>
) {
    coords.forEach { (r, c) ->
        val tl = Offset(c * cell, r * cell)
        drawRect(color = color.copy(alpha = 0.85f), topLeft = tl, size = Size(cell, cell))
        drawRect(color = lightColor.copy(alpha = 0.4f), topLeft = tl + Offset(3f, 3f), size = Size(cell - 6f, cell - 6f))
        drawRect(color = gridStroke, topLeft = tl, size = Size(cell, cell), style = Stroke(width = 1.2f))
    }
}

private fun DrawScope.drawYard(
    cell: Float,
    startCol: Int,
    startRow: Int,
    color: Color,
    lightColor: Color
) {
    val topLeft = Offset(startCol * cell, startRow * cell)
    val yardSize = Size(6f * cell, 6f * cell)
    drawRect(color = color, topLeft = topLeft, size = yardSize)
    val innerMargin = cell * 0.85f
    drawRoundRect(
        color = Color.White,
        topLeft = topLeft + Offset(innerMargin, innerMargin),
        size = Size(yardSize.width - innerMargin * 2f, yardSize.height - innerMargin * 2f),
        cornerRadius = CornerRadius(cell * 0.6f, cell * 0.6f)
    )
    drawRoundRect(
        color = lightColor.copy(alpha = 0.4f),
        topLeft = topLeft + Offset(innerMargin, innerMargin),
        size = Size(yardSize.width - innerMargin * 2f, yardSize.height - innerMargin * 2f),
        cornerRadius = CornerRadius(cell * 0.6f, cell * 0.6f)
    )
    val slotCenters = listOf(
        Offset((startCol + 2.3f) * cell, (startRow + 2.3f) * cell),
        Offset((startCol + 3.7f) * cell, (startRow + 2.3f) * cell),
        Offset((startCol + 2.3f) * cell, (startRow + 3.7f) * cell),
        Offset((startCol + 3.7f) * cell, (startRow + 3.7f) * cell)
    )
    slotCenters.forEach { c ->
        drawCircle(color = color.copy(alpha = 0.25f), radius = cell * 0.48f, center = c)
        drawCircle(color = color, radius = cell * 0.48f, center = c, style = Stroke(width = 2f))
    }
}

private fun DrawScope.drawTriangle(p1: Offset, p2: Offset, p3: Offset, color: Color) {
    val path = Path().apply {
        moveTo(p1.x, p1.y)
        lineTo(p2.x, p2.y)
        lineTo(p3.x, p3.y)
        close()
    }
    drawPath(path = path, color = color)
}

private fun DrawScope.drawStar(center: Offset, outerRadius: Float, innerRadius: Float, color: Color) {
    val path = Path()
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) outerRadius else innerRadius
        val angle = Math.toRadians((i * 36.0) - 90.0)
        val x = center.x + (cos(angle) * r).toFloat()
        val y = center.y + (sin(angle) * r).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path = path, color = color)
}

private fun countTokensHome(state: LudoGameState, seatIndex: Int): Int {
    val base = seatIndex * 4
    return (0..3).count { state.tokenPositions[base + it] == 56 }
}

fun seatAccentColor(color: PlayerColor): Color = when (color) {
    PlayerColor.RED -> LudoRed
    PlayerColor.GREEN -> LudoGreen
    PlayerColor.YELLOW -> LudoYellow
    PlayerColor.BLUE -> LudoBlue
}
