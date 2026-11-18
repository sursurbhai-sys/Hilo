package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DailyMission
import com.example.data.LudoRepository
import com.example.data.MatchRoom
import com.example.data.PowerUpType
import com.example.data.UserProfile
import com.example.game.GameMode
import com.example.game.LudoEngine
import com.example.game.LudoGameState
import com.example.game.MatchPowerOrb
import com.example.game.PlayerColor
import com.example.game.PlayerSeat
import com.example.game.TurnPhase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class GameViewModel(
    private val repository: LudoRepository,
    private val currentUserId: String,
    private val playerName: String
) : ViewModel() {

    private val _gameState = MutableStateFlow<LudoGameState?>(null)
    val gameState: StateFlow<LudoGameState?> = _gameState.asStateFlow()

    private val _waitingRoom = MutableStateFlow<MatchRoom?>(null)
    val waitingRoom: StateFlow<MatchRoom?> = _waitingRoom.asStateFlow()

    private val _isDiceRolling = MutableStateFlow(false)
    val isDiceRolling: StateFlow<Boolean> = _isDiceRolling.asStateFlow()

    private val _matchError = MutableStateFlow<String?>(null)
    val matchError: StateFlow<String?> = _matchError.asStateFlow()

    private val _outcomeSummary = MutableStateFlow<String?>(null)
    val outcomeSummary: StateFlow<String?> = _outcomeSummary.asStateFlow()

    private var aiTurnJob: Job? = null
    private var roomObserverJob: Job? = null
    private var outcomeRecorded = false

    fun startLocalMatch(mode: GameMode) {
        stopActiveJobs()
        _waitingRoom.value = null
        _outcomeSummary.value = null
        outcomeRecorded = false
        val initial = LudoEngine.createLocalMatch(mode, playerName, currentUserId)
        _gameState.value = initial
    }

    fun attachOnlineRoom(roomId: String) {
        stopActiveJobs()
        _gameState.value = null
        _outcomeSummary.value = null
        outcomeRecorded = false

        roomObserverJob = viewModelScope.launch {
            repository.observeRoom(roomId)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    Log.w("GameVM", "Error observing room $roomId", firestoreError ?: error)
                    _matchError.value = (firestoreError ?: error).localizedMessage ?: "Room sync failed"
                }
                .collect { room ->
                    if (room == null) {
                        _waitingRoom.value = null
                        _matchError.value = "Room was closed by host."
                        return@collect
                    }
                    if (room.status == "WAITING") {
                        _waitingRoom.value = room
                        _gameState.value = null
                    } else {
                        _waitingRoom.value = null
                        _gameState.value = mapRoomToGameState(room)
                    }
                }
        }
    }

    fun startWaitingRoom() {
        val room = _waitingRoom.value ?: return
        if (room.hostId != currentUserId || room.playerUids.size < 2) return
        viewModelScope.launch {
            repository.startRoom(room.roomCode).onFailure { err ->
                _matchError.value = err.localizedMessage ?: "Failed to start match"
            }
        }
    }

    fun cancelWaitingRoom(onClosed: () -> Unit) {
        val room = _waitingRoom.value
        if (room != null && room.hostId == currentUserId && room.status == "WAITING") {
            viewModelScope.launch {
                repository.deleteWaitingRoom(room.roomCode)
                stopActiveJobs()
                _waitingRoom.value = null
                onClosed()
            }
        } else {
            stopActiveJobs()
            _waitingRoom.value = null
            onClosed()
        }
    }

    fun rollDice(profile: UserProfile?, missions: List<DailyMission>) {
        val current = _gameState.value ?: return
        if (_isDiceRolling.value || current.turnPhase != TurnPhase.ROLL || current.winnerSeatIndex != null) return
        val seat = current.currentSeat ?: return
        if (seat.aiBot) return
        if (current.mode == GameMode.ONLINE && seat.uid != currentUserId) return

        viewModelScope.launch {
            _isDiceRolling.value = true
            delay(360L)
            val next = LudoEngine.rollDice(current)
            _isDiceRolling.value = false
            applyAndSyncState(next, profile, missions)
        }
    }

    fun moveToken(localTokenIndex: Int, profile: UserProfile?, missions: List<DailyMission>) {
        val current = _gameState.value ?: return
        if (current.turnPhase != TurnPhase.MOVE || current.winnerSeatIndex != null) return
        val seat = current.currentSeat ?: return
        if (seat.aiBot) return
        if (current.mode == GameMode.ONLINE && seat.uid != currentUserId) return

        val outcome = LudoEngine.moveToken(current, localTokenIndex)
        if (outcome.nextState != current) {
            applyAndSyncState(outcome.nextState, profile, missions)
        }
    }

    fun activatePowerUp(
        type: PowerUpType,
        profile: UserProfile?,
        missions: List<DailyMission>
    ) {
        val current = _gameState.value ?: return
        if (current.winnerSeatIndex != null) return
        val seat = current.currentSeat ?: return
        if (seat.aiBot) return
        if (current.mode == GameMode.ONLINE && seat.uid != currentUserId) return

        val matchEarnedCount = current.earnedMatchPowerUps[type] ?: 0
        val profileCharges = profile?.chargeCount(type) ?: 0L
        if (matchEarnedCount <= 0 && profileCharges <= 0L) {
            _matchError.value = "No ${type.title} charges left! Grab orbs on the board or visit the Armory."
            return
        }

        val updated = LudoEngine.applyPowerUp(current, type)
        if (updated == null) {
            _matchError.value = "${type.title} can't be used right now."
            return
        }

        val finalState = if (matchEarnedCount > 0) {
            val map = updated.earnedMatchPowerUps.toMutableMap()
            map[type] = (matchEarnedCount - 1).coerceAtLeast(0)
            updated.copy(earnedMatchPowerUps = map)
        } else {
            if (profile != null) {
                viewModelScope.launch {
                    repository.spendPowerUpCharge(profile, type)
                }
            }
            updated
        }
        applyAndSyncState(finalState, profile, missions)
    }

    fun exitMatch() {
        stopActiveJobs()
        _gameState.value = null
        _waitingRoom.value = null
        _outcomeSummary.value = null
    }

    fun clearError() {
        _matchError.value = null
    }

    private fun applyAndSyncState(
        next: LudoGameState,
        profile: UserProfile?,
        missions: List<DailyMission>
    ) {
        _gameState.value = next

        if (next.mode == GameMode.ONLINE && next.roomId != null) {
            val winnerUid = next.winnerSeatIndex?.let { next.seats.getOrNull(it)?.uid } ?: ""
            val roomStatus = if (next.winnerSeatIndex != null) "FINISHED" else "ACTIVE"
            val roomPhase = when (next.turnPhase) {
                TurnPhase.ROLL -> "ROLL"
                TurnPhase.MOVE -> "MOVE"
                TurnPhase.FINISHED -> "FINISHED"
            }
            viewModelScope.launch {
                repository.pushRoomTurnState(
                    roomId = next.roomId,
                    status = roomStatus,
                    currentTurnIndex = next.currentSeatIndex,
                    lastDiceValue = next.lastDiceValue,
                    turnPhase = roomPhase,
                    winnerUid = winnerUid,
                    tokenPositions = next.tokenPositions,
                    shieldTurns = next.shieldTurns,
                    frozenTurns = next.frozenTurns,
                    orbTiles = next.orbs.map { it.globalTile },
                    orbTypes = next.orbs.map { it.type.name },
                    lastEventText = next.lastEventText
                ).onFailure { err ->
                    _matchError.value = err.localizedMessage ?: "Failed to sync turn"
                }
            }
        }

        if (next.winnerSeatIndex != null && !outcomeRecorded) {
            outcomeRecorded = true
            val didHumanWin = next.winnerSeatIndex == next.localSeatIndex
            val trophyDelta = if (didHumanWin) "+35 Trophies" else "+10 Trophies"
            val coinDelta = if (didHumanWin) "+90 Coins" else "+30 Coins"
            _outcomeSummary.value = "$trophyDelta • $coinDelta"
            if (profile != null) {
                viewModelScope.launch {
                    repository.recordMatchOutcome(
                        profile = profile,
                        missions = missions,
                        didWin = didHumanWin,
                        captures = next.humanCaptures,
                        powerUpsUsed = next.humanPowerUpsUsed,
                        sixesRolled = next.humanSixesRolled,
                        tokensHome = next.humanTokensHome,
                        earnedPowerUps = next.earnedMatchPowerUps
                    )
                }
            }
            return
        }

        scheduleAiTurnIfNeeded(next, profile, missions)
    }

    private fun scheduleAiTurnIfNeeded(
        state: LudoGameState,
        profile: UserProfile?,
        missions: List<DailyMission>
    ) {
        if (state.mode == GameMode.ONLINE || state.winnerSeatIndex != null) return
        val seat = state.currentSeat ?: return
        if (!seat.aiBot) return

        aiTurnJob?.cancel()
        aiTurnJob = viewModelScope.launch {
            delay(550L)
            var current = _gameState.value ?: return@launch
            if (current.currentSeat?.aiBot != true || current.winnerSeatIndex != null) return@launch

            if (current.turnPhase == TurnPhase.ROLL) {
                _isDiceRolling.value = true
                delay(320L)
                _isDiceRolling.value = false
                current = LudoEngine.rollDice(current)
                applyAndSyncState(current, profile, missions)
                return@launch
            }

            if (current.turnPhase == TurnPhase.MOVE) {
                val chosen = LudoEngine.chooseSmartAiToken(current)
                if (chosen != null) {
                    val outcome = LudoEngine.moveToken(current, chosen)
                    applyAndSyncState(outcome.nextState, profile, missions)
                }
            }
        }
    }

    private fun mapRoomToGameState(room: MatchRoom): LudoGameState {
        val colorAssignments = when (room.playerUids.size) {
            2 -> listOf(PlayerColor.RED, PlayerColor.YELLOW)
            else -> listOf(PlayerColor.RED, PlayerColor.GREEN, PlayerColor.YELLOW, PlayerColor.BLUE)
        }
        val seatByIndex = mutableMapOf<Int, PlayerSeat>()
        room.playerUids.forEachIndexed { idx, uid ->
            val color = colorAssignments.getOrElse(idx) { PlayerColor.entries[idx] }
            val name = room.playerNames.getOrElse(idx) { "Player ${idx + 1}" }
            seatByIndex[color.seatIndex] = PlayerSeat(
                seatIndex = color.seatIndex,
                color = color,
                name = name,
                aiBot = false,
                uid = uid,
                active = true
            )
        }
        val fullSeats = (0..3).map { sIdx ->
            seatByIndex[sIdx] ?: PlayerSeat(
                seatIndex = sIdx,
                color = PlayerColor.entries[sIdx],
                name = "Empty",
                aiBot = false,
                active = false
            )
        }
        val localSeat = fullSeats.firstOrNull { it.uid == currentUserId }?.seatIndex ?: 0
        val orbs = room.orbTiles.mapIndexedNotNull { idx, tile ->
            val type = PowerUpType.fromName(room.orbTypes.getOrNull(idx)) ?: return@mapIndexedNotNull null
            MatchPowerOrb(tile.toInt(), type)
        }
        val phase = when (room.turnPhase) {
            "MOVE" -> TurnPhase.MOVE
            "FINISHED" -> TurnPhase.FINISHED
            else -> TurnPhase.ROLL
        }
        val winnerSeat = if (room.winnerUid.isNotBlank()) {
            fullSeats.firstOrNull { it.uid == room.winnerUid }?.seatIndex
        } else null

        val previous = _gameState.value
        return LudoGameState(
            mode = GameMode.ONLINE,
            seats = fullSeats,
            tokenPositions = room.tokenPositions.map { it.toInt() }.let { if (it.size == 16) it else List(16) { -1 } },
            currentSeatIndex = room.currentTurnIndex.toInt().coerceIn(0, 3),
            turnPhase = phase,
            lastDiceValue = room.lastDiceValue.toInt().coerceIn(0, 6),
            shieldTurns = room.shieldTurns.map { it.toInt() }.let { if (it.size == 4) it else List(4) { 0 } },
            frozenTurns = room.frozenTurns.map { it.toInt() }.let { if (it.size == 4) it else List(4) { 0 } },
            orbs = orbs,
            winnerSeatIndex = winnerSeat,
            lastEventText = room.lastEventText,
            roomId = room.roomCode,
            localSeatIndex = localSeat,
            humanCaptures = previous?.humanCaptures ?: 0,
            humanPowerUpsUsed = previous?.humanPowerUpsUsed ?: 0,
            humanSixesRolled = previous?.humanSixesRolled ?: 0,
            humanTokensHome = previous?.humanTokensHome ?: 0,
            earnedMatchPowerUps = previous?.earnedMatchPowerUps ?: emptyMap()
        )
    }

    private fun stopActiveJobs() {
        aiTurnJob?.cancel()
        aiTurnJob = null
        roomObserverJob?.cancel()
        roomObserverJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopActiveJobs()
    }
}
