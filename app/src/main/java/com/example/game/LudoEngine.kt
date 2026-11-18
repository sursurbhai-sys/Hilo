package com.example.game

import com.example.data.PowerUpType
import kotlin.random.Random

enum class PlayerColor(val seatIndex: Int, val startOffset: Int, val label: String) {
    RED(0, 0, "Crimson"),
    GREEN(1, 13, "Emerald"),
    YELLOW(2, 26, "Amber"),
    BLUE(3, 39, "Cobalt")
}

enum class GameMode {
    AI_DUEL,
    AI_ARENA,
    PASS_AND_PLAY,
    ONLINE
}

enum class TurnPhase {
    ROLL,
    MOVE,
    FINISHED
}

data class GridCoord(val row: Float, val col: Float)

data class PlayerSeat(
    val seatIndex: Int,
    val color: PlayerColor,
    val name: String,
    val aiBot: Boolean,
    val uid: String = "",
    val active: Boolean = true
)

data class MatchPowerOrb(
    val globalTile: Int,
    val type: PowerUpType
)

data class LudoGameState(
    val mode: GameMode = GameMode.AI_ARENA,
    val seats: List<PlayerSeat> = emptyList(),
    val tokenPositions: List<Int> = List(16) { -1 },
    val currentSeatIndex: Int = 0,
    val turnPhase: TurnPhase = TurnPhase.ROLL,
    val lastDiceValue: Int = 0,
    val bonusSteps: Int = 0,
    val consecutiveSixes: Int = 0,
    val pendingLuckySix: Boolean = false,
    val pendingTurbo: Boolean = false,
    val shieldTurns: List<Int> = List(4) { 0 },
    val frozenTurns: List<Int> = List(4) { 0 },
    val orbs: List<MatchPowerOrb> = emptyList(),
    val winnerSeatIndex: Int? = null,
    val lastEventText: String = "Roll the dice to launch your first pawn!",
    val roomId: String? = null,
    val localSeatIndex: Int = 0,
    // Match telemetry for Daily Missions & Leaderboard rewards
    val humanCaptures: Int = 0,
    val humanPowerUpsUsed: Int = 0,
    val humanSixesRolled: Int = 0,
    val humanTokensHome: Int = 0,
    val earnedMatchPowerUps: Map<PowerUpType, Int> = emptyMap()
) {
    val currentSeat: PlayerSeat?
        get() = seats.getOrNull(currentSeatIndex)

    val effectiveSteps: Int
        get() = lastDiceValue + bonusSteps
}

data class MoveOutcome(
    val nextState: LudoGameState,
    val capturedCount: Int = 0,
    val reachedHome: Boolean = false,
    val collectedOrb: PowerUpType? = null
)

object LudoEngine {

    val SAFE_GLOBAL_TILES = setOf(0, 8, 13, 21, 26, 34, 39, 47)
    val STAR_ONLY_TILES = setOf(8, 21, 34, 47)

    // 52 clockwise main-track grid coordinates (row 0..14, col 0..14)
    val MAIN_TRACK_COORDS: List<GridCoord> = listOf(
        // 0..4: Bottom-Left up along col 6 (RED start at 0 = (13,6))
        GridCoord(13f, 6f), GridCoord(12f, 6f), GridCoord(11f, 6f), GridCoord(10f, 6f), GridCoord(9f, 6f),
        // 5..10: Left along row 8
        GridCoord(8f, 5f), GridCoord(8f, 4f), GridCoord(8f, 3f), GridCoord(8f, 2f), GridCoord(8f, 1f), GridCoord(8f, 0f),
        // 11..12: Turn up on col 0
        GridCoord(7f, 0f), GridCoord(6f, 0f),
        // 13..17: Right along row 6 (GREEN start at 13 = (6,1))
        GridCoord(6f, 1f), GridCoord(6f, 2f), GridCoord(6f, 3f), GridCoord(6f, 4f), GridCoord(6f, 5f),
        // 18..23: Up along col 6
        GridCoord(5f, 6f), GridCoord(4f, 6f), GridCoord(3f, 6f), GridCoord(2f, 6f), GridCoord(1f, 6f), GridCoord(0f, 6f),
        // 24..25: Turn right on row 0
        GridCoord(0f, 7f), GridCoord(0f, 8f),
        // 26..30: Down along col 8 (YELLOW start at 26 = (1,8))
        GridCoord(1f, 8f), GridCoord(2f, 8f), GridCoord(3f, 8f), GridCoord(4f, 8f), GridCoord(5f, 8f),
        // 31..36: Right along row 6
        GridCoord(6f, 9f), GridCoord(6f, 10f), GridCoord(6f, 11f), GridCoord(6f, 12f), GridCoord(6f, 13f), GridCoord(6f, 14f),
        // 37..38: Turn down on col 14
        GridCoord(7f, 14f), GridCoord(8f, 14f),
        // 39..43: Left along row 8 (BLUE start at 39 = (8,13))
        GridCoord(8f, 13f), GridCoord(8f, 12f), GridCoord(8f, 11f), GridCoord(8f, 10f), GridCoord(8f, 9f),
        // 44..49: Down along col 8
        GridCoord(9f, 8f), GridCoord(10f, 8f), GridCoord(11f, 8f), GridCoord(12f, 8f), GridCoord(13f, 8f), GridCoord(14f, 8f),
        // 50..51: Turn left on row 14
        GridCoord(14f, 7f), GridCoord(14f, 6f)
    )

    private val HOME_STRETCH_COORDS: Map<PlayerColor, List<GridCoord>> = mapOf(
        PlayerColor.RED to listOf(
            GridCoord(13f, 7f), GridCoord(12f, 7f), GridCoord(11f, 7f), GridCoord(10f, 7f), GridCoord(9f, 7f), GridCoord(7.85f, 7f)
        ),
        PlayerColor.GREEN to listOf(
            GridCoord(7f, 1f), GridCoord(7f, 2f), GridCoord(7f, 3f), GridCoord(7f, 4f), GridCoord(7f, 5f), GridCoord(7f, 6.15f)
        ),
        PlayerColor.YELLOW to listOf(
            GridCoord(1f, 7f), GridCoord(2f, 7f), GridCoord(3f, 7f), GridCoord(4f, 7f), GridCoord(5f, 7f), GridCoord(6.15f, 7f)
        ),
        PlayerColor.BLUE to listOf(
            GridCoord(7f, 13f), GridCoord(7f, 12f), GridCoord(7f, 11f), GridCoord(7f, 10f), GridCoord(7f, 9f), GridCoord(7f, 7.85f)
        )
    )

    private val YARD_COORDS: Map<PlayerColor, List<GridCoord>> = mapOf(
        PlayerColor.RED to listOf(
            GridCoord(10.8f, 1.8f), GridCoord(10.8f, 3.2f), GridCoord(12.2f, 1.8f), GridCoord(12.2f, 3.2f)
        ),
        PlayerColor.GREEN to listOf(
            GridCoord(1.8f, 1.8f), GridCoord(1.8f, 3.2f), GridCoord(3.2f, 1.8f), GridCoord(3.2f, 3.2f)
        ),
        PlayerColor.YELLOW to listOf(
            GridCoord(1.8f, 10.8f), GridCoord(1.8f, 12.2f), GridCoord(3.2f, 10.8f), GridCoord(3.2f, 12.2f)
        ),
        PlayerColor.BLUE to listOf(
            GridCoord(10.8f, 10.8f), GridCoord(10.8f, 12.2f), GridCoord(12.2f, 10.8f), GridCoord(12.2f, 12.2f)
        )
    )

    fun toGlobalTrackIndex(color: PlayerColor, step: Int): Int? {
        if (step !in 0..50) return null
        return (color.startOffset + step) % 52
    }

    fun toStepFromGlobal(color: PlayerColor, globalIndex: Int): Int {
        return (globalIndex - color.startOffset + 52) % 52
    }

    fun tokenBoardCoord(seatIndex: Int, tokenLocalIndex: Int, step: Int): GridCoord {
        val color = PlayerColor.entries[seatIndex]
        return when {
            step < 0 -> YARD_COORDS.getValue(color)[tokenLocalIndex]
            step in 0..50 -> MAIN_TRACK_COORDS[toGlobalTrackIndex(color, step)!!]
            step in 51..56 -> HOME_STRETCH_COORDS.getValue(color)[step - 51]
            else -> HOME_STRETCH_COORDS.getValue(color).last()
        }
    }

    fun createLocalMatch(mode: GameMode, humanName: String, humanUid: String): LudoGameState {
        val seats = when (mode) {
            GameMode.AI_DUEL -> listOf(
                PlayerSeat(0, PlayerColor.RED, humanName, aiBot = false, uid = humanUid, active = true),
                PlayerSeat(1, PlayerColor.GREEN, "Inactive", aiBot = true, active = false),
                PlayerSeat(2, PlayerColor.YELLOW, "Nova Bot", aiBot = true, active = true),
                PlayerSeat(3, PlayerColor.BLUE, "Inactive", aiBot = true, active = false)
            )
            GameMode.AI_ARENA -> listOf(
                PlayerSeat(0, PlayerColor.RED, humanName, aiBot = false, uid = humanUid, active = true),
                PlayerSeat(1, PlayerColor.GREEN, "Viper Bot", aiBot = true, active = true),
                PlayerSeat(2, PlayerColor.YELLOW, "Nova Bot", aiBot = true, active = true),
                PlayerSeat(3, PlayerColor.BLUE, "Titan Bot", aiBot = true, active = true)
            )
            GameMode.PASS_AND_PLAY -> listOf(
                PlayerSeat(0, PlayerColor.RED, humanName, aiBot = false, uid = humanUid, active = true),
                PlayerSeat(1, PlayerColor.GREEN, "Player 2", aiBot = false, active = true),
                PlayerSeat(2, PlayerColor.YELLOW, "Player 3", aiBot = false, active = true),
                PlayerSeat(3, PlayerColor.BLUE, "Player 4", aiBot = false, active = true)
            )
            GameMode.ONLINE -> listOf(
                PlayerSeat(0, PlayerColor.RED, humanName, aiBot = false, uid = humanUid, active = true),
                PlayerSeat(1, PlayerColor.GREEN, "Player 2", aiBot = false, active = true)
            )
        }
        return LudoGameState(
            mode = mode,
            seats = seats,
            orbs = generateInitialOrbs(),
            localSeatIndex = 0,
            lastEventText = "Match ready! ${seats.first().name} rolls first."
        )
    }

    fun generateInitialOrbs(count: Int = 4): List<MatchPowerOrb> {
        val candidates = (0 until 52).filter { it !in SAFE_GLOBAL_TILES }.shuffled()
        val types = PowerUpType.entries
        return candidates.take(count).mapIndexed { idx, tile ->
            MatchPowerOrb(tile, types[Random.nextInt(types.size)])
        }
    }

    fun legalTokenIndices(state: LudoGameState, seatIndex: Int = state.currentSeatIndex): List<Int> {
        if (state.turnPhase != TurnPhase.MOVE) return emptyList()
        val roll = state.lastDiceValue
        val steps = state.effectiveSteps
        val base = seatIndex * 4
        return (0..3).filter { localIdx ->
            val pos = state.tokenPositions[base + localIdx]
            when {
                pos == -1 -> roll == 6
                pos in 0..55 -> pos + steps <= 56
                else -> false
            }
        }
    }

    fun rollDice(state: LudoGameState, forcedRoll: Int? = null): LudoGameState {
        if (state.turnPhase != TurnPhase.ROLL || state.winnerSeatIndex != null) return state
        val seat = state.currentSeat ?: return state

        // Check if current seat is frozen
        if (state.frozenTurns[seat.seatIndex] > 0) {
            val nextFrozen = state.frozenTurns.toMutableList()
            nextFrozen[seat.seatIndex] = (nextFrozen[seat.seatIndex] - 1).coerceAtLeast(0)
            val nextSeat = nextActiveSeat(state, seat.seatIndex)
            return state.copy(
                frozenTurns = nextFrozen,
                currentSeatIndex = nextSeat,
                turnPhase = TurnPhase.ROLL,
                lastDiceValue = 0,
                bonusSteps = 0,
                pendingTurbo = false,
                pendingLuckySix = false,
                consecutiveSixes = 0,
                lastEventText = "${seat.name} was frozen! Turn skipped."
            )
        }

        val rolled = forcedRoll ?: if (state.pendingLuckySix) 6 else Random.nextInt(1, 7)
        val bonus = if (state.pendingTurbo) 3 else 0
        val newSixStreak = if (rolled == 6) state.consecutiveSixes + 1 else 0
        val humanSixDelta = if (!seat.aiBot && rolled == 6 && seat.seatIndex == state.localSeatIndex) 1 else 0

        if (newSixStreak >= 3) {
            val nextSeat = nextActiveSeat(state, seat.seatIndex)
            return advanceShieldsAndTurn(
                state.copy(
                    lastDiceValue = rolled,
                    bonusSteps = 0,
                    pendingLuckySix = false,
                    pendingTurbo = false,
                    consecutiveSixes = 0,
                    humanSixesRolled = state.humanSixesRolled + humanSixDelta,
                    lastEventText = "${seat.name} rolled three 6s in a row! Turn forfeited."
                ),
                nextSeat
            )
        }

        val candidateState = state.copy(
            turnPhase = TurnPhase.MOVE,
            lastDiceValue = rolled,
            bonusSteps = bonus,
            pendingLuckySix = false,
            pendingTurbo = false,
            consecutiveSixes = newSixStreak,
            humanSixesRolled = state.humanSixesRolled + humanSixDelta
        )

        val legalMoves = legalTokenIndices(candidateState, seat.seatIndex)
        if (legalMoves.isEmpty()) {
            val nextSeat = if (rolled == 6) seat.seatIndex else nextActiveSeat(candidateState, seat.seatIndex)
            val msg = if (rolled == 6) "${seat.name} rolled 6 but has no valid moves."
            else "${seat.name} rolled $rolled — no legal moves."
            return advanceShieldsAndTurn(candidateState.copy(lastEventText = msg), nextSeat)
        }

        val bonusText = if (bonus > 0) " (+3 Turbo!)" else ""
        return candidateState.copy(
            lastEventText = "${seat.name} rolled $rolled$bonusText — tap a highlighted pawn!"
        )
    }

    fun moveToken(state: LudoGameState, localTokenIndex: Int): MoveOutcome {
        if (state.turnPhase != TurnPhase.MOVE || state.winnerSeatIndex != null) {
            return MoveOutcome(state)
        }
        val seat = state.currentSeat ?: return MoveOutcome(state)
        val legal = legalTokenIndices(state, seat.seatIndex)
        if (localTokenIndex !in legal) return MoveOutcome(state)

        val positions = state.tokenPositions.toMutableList()
        val globalTokenIdx = seat.seatIndex * 4 + localTokenIndex
        val currentStep = positions[globalTokenIdx]
        val newStep = if (currentStep == -1) 0 else currentStep + state.effectiveSteps
        positions[globalTokenIdx] = newStep

        var capturedCount = 0
        var collectedOrb: PowerUpType? = null
        val updatedOrbs = state.orbs.toMutableList()
        val earnedMap = state.earnedMatchPowerUps.toMutableMap()

        // Check capture & orb pickup on main track (0..50)
        if (newStep in 0..50) {
            val destGlobal = toGlobalTrackIndex(seat.color, newStep)!!

            // Power-Up Orb pickup
            val orbIdx = updatedOrbs.indexOfFirst { it.globalTile == destGlobal }
            if (orbIdx >= 0) {
                collectedOrb = updatedOrbs[orbIdx].type
                updatedOrbs.removeAt(orbIdx)
                // Spawn a replacement orb
                val occupiedTiles = updatedOrbs.map { it.globalTile }.toSet()
                val freeTile = (0 until 52)
                    .filter { it !in SAFE_GLOBAL_TILES && it !in occupiedTiles && it != destGlobal }
                    .randomOrNull()
                if (freeTile != null) {
                    val randomType = PowerUpType.entries[Random.nextInt(PowerUpType.entries.size)]
                    updatedOrbs.add(MatchPowerOrb(freeTile, randomType))
                }
                if (seat.seatIndex == state.localSeatIndex && !seat.aiBot) {
                    earnedMap[collectedOrb] = (earnedMap[collectedOrb] ?: 0) + 1
                }
            }

            // Captures (only on non-safe tiles and unshielded rivals)
            if (destGlobal !in SAFE_GLOBAL_TILES) {
                for (otherSeat in state.seats) {
                    if (!otherSeat.active || otherSeat.seatIndex == seat.seatIndex) continue
                    if (state.shieldTurns[otherSeat.seatIndex] > 0) continue
                    for (t in 0..3) {
                        val otherGlobalIdx = otherSeat.seatIndex * 4 + t
                        val otherStep = positions[otherGlobalIdx]
                        if (otherStep in 0..50) {
                            val otherTrackTile = toGlobalTrackIndex(otherSeat.color, otherStep)
                            if (otherTrackTile == destGlobal) {
                                positions[otherGlobalIdx] = -1
                                capturedCount++
                            }
                        }
                    }
                }
            }
        }

        val reachedHome = newStep == 56
        val allHome = (0..3).all { positions[seat.seatIndex * 4 + it] == 56 }

        val isLocalHuman = seat.seatIndex == state.localSeatIndex && !seat.aiBot
        val newHumanCaptures = state.humanCaptures + if (isLocalHuman) capturedCount else 0
        val newHumanHome = state.humanTokensHome + if (isLocalHuman && reachedHome) 1 else 0

        if (allHome) {
            val finishedState = state.copy(
                tokenPositions = positions,
                orbs = updatedOrbs,
                turnPhase = TurnPhase.FINISHED,
                winnerSeatIndex = seat.seatIndex,
                bonusSteps = 0,
                humanCaptures = newHumanCaptures,
                humanTokensHome = newHumanHome,
                earnedMatchPowerUps = earnedMap,
                lastEventText = "${seat.name} claims VICTORY in Ludo Unic!"
            )
            return MoveOutcome(finishedState, capturedCount, reachedHome, collectedOrb)
        }

        val bonusRoll = state.lastDiceValue == 6 || capturedCount > 0 || reachedHome
        val nextSeat = if (bonusRoll) seat.seatIndex else nextActiveSeat(state, seat.seatIndex)

        val eventParts = mutableListOf<String>()
        if (currentStep == -1) eventParts.add("${seat.name} deployed a pawn!")
        else if (reachedHome) eventParts.add("${seat.name} scored a Home Goal!")
        else eventParts.add("${seat.name} moved ${state.effectiveSteps} steps.")
        if (capturedCount > 0) eventParts.add("Captured $capturedCount rival!")
        if (collectedOrb != null) eventParts.add("Picked up ${collectedOrb.title}!")
        if (bonusRoll) eventParts.add("Bonus roll!")

        val updatedState = state.copy(
            tokenPositions = positions,
            orbs = updatedOrbs,
            bonusSteps = 0,
            humanCaptures = newHumanCaptures,
            humanTokensHome = newHumanHome,
            earnedMatchPowerUps = earnedMap,
            lastEventText = eventParts.joinToString(" ")
        )

        val finalState = if (bonusRoll) {
            updatedState.copy(
                currentSeatIndex = seat.seatIndex,
                turnPhase = TurnPhase.ROLL,
                lastDiceValue = 0
            )
        } else {
            advanceShieldsAndTurn(updatedState.copy(consecutiveSixes = 0), nextSeat)
        }
        return MoveOutcome(finalState, capturedCount, reachedHome, collectedOrb)
    }

    fun applyPowerUp(state: LudoGameState, type: PowerUpType): LudoGameState? {
        if (state.winnerSeatIndex != null) return null
        val seat = state.currentSeat ?: return null
        val base = seat.seatIndex * 4
        val isLocalHuman = seat.seatIndex == state.localSeatIndex && !seat.aiBot
        val powerUpDelta = if (isLocalHuman) 1 else 0

        return when (type) {
            PowerUpType.TURBO_BOOST -> {
                if (state.turnPhase == TurnPhase.ROLL) {
                    if (state.pendingTurbo) return null
                    state.copy(
                        pendingTurbo = true,
                        humanPowerUpsUsed = state.humanPowerUpsUsed + powerUpDelta,
                        lastEventText = "${seat.name} primed Turbo Boost (+3 steps on next roll)!"
                    )
                } else {
                    if (state.bonusSteps > 0) return null
                    state.copy(
                        bonusSteps = 3,
                        humanPowerUpsUsed = state.humanPowerUpsUsed + powerUpDelta,
                        lastEventText = "${seat.name} activated Turbo Boost (+3 steps)!"
                    )
                }
            }
            PowerUpType.LUCKY_SIX -> {
                if (state.turnPhase != TurnPhase.ROLL) return null
                rollDice(
                    state.copy(
                        pendingLuckySix = true,
                        humanPowerUpsUsed = state.humanPowerUpsUsed + powerUpDelta
                    ),
                    forcedRoll = 6
                ).copy(
                    lastEventText = "${seat.name} triggered Precision 6 — guaranteed 6!"
                )
            }
            PowerUpType.SHIELD -> {
                val shields = state.shieldTurns.toMutableList()
                shields[seat.seatIndex] = 2
                state.copy(
                    shieldTurns = shields,
                    humanPowerUpsUsed = state.humanPowerUpsUsed + powerUpDelta,
                    lastEventText = "${seat.name} raised Shield Aura for 2 turns!"
                )
            }
            PowerUpType.FREEZE -> {
                val targetSeat = nextActiveSeat(state, seat.seatIndex)
                if (targetSeat == seat.seatIndex) return null
                val frozen = state.frozenTurns.toMutableList()
                frozen[targetSeat] = 1
                val targetName = state.seats.getOrNull(targetSeat)?.name ?: "Opponent"
                state.copy(
                    frozenTurns = frozen,
                    humanPowerUpsUsed = state.humanPowerUpsUsed + powerUpDelta,
                    lastEventText = "${seat.name} froze $targetName for 1 turn!"
                )
            }
            PowerUpType.SWAP -> {
                // Find player's furthest active main-track pawn and closest unshielded rival main-track pawn
                val myBestLocal = (0..3)
                    .filter { state.tokenPositions[base + it] in 0..50 }
                    .maxByOrNull { state.tokenPositions[base + it] } ?: return null
                val myStep = state.tokenPositions[base + myBestLocal]
                val myGlobal = toGlobalTrackIndex(seat.color, myStep)!!

                var bestEnemyGlobalIdx = -1
                var bestEnemyStep = -1
                var bestEnemyGlobalTile = -1
                for (other in state.seats) {
                    if (!other.active || other.seatIndex == seat.seatIndex) continue
                    if (state.shieldTurns[other.seatIndex] > 0) continue
                    for (t in 0..3) {
                        val idx = other.seatIndex * 4 + t
                        val step = state.tokenPositions[idx]
                        if (step in 0..50) {
                            val tile = toGlobalTrackIndex(other.color, step)!!
                            if (tile !in SAFE_GLOBAL_TILES && step > bestEnemyStep) {
                                bestEnemyStep = step
                                bestEnemyGlobalIdx = idx
                                bestEnemyGlobalTile = tile
                            }
                        }
                    }
                }
                if (bestEnemyGlobalIdx < 0) return null

                val enemySeatColor = PlayerColor.entries[bestEnemyGlobalIdx / 4]
                val positions = state.tokenPositions.toMutableList()
                positions[base + myBestLocal] = toStepFromGlobal(seat.color, bestEnemyGlobalTile)
                positions[bestEnemyGlobalIdx] = toStepFromGlobal(enemySeatColor, myGlobal)

                state.copy(
                    tokenPositions = positions,
                    humanPowerUpsUsed = state.humanPowerUpsUsed + powerUpDelta,
                    lastEventText = "${seat.name} warped & swapped positions with ${enemySeatColor.label}!"
                )
            }
            PowerUpType.MAGNET -> {
                val candidateLocal = (0..3)
                    .filter { state.tokenPositions[base + it] in 0..49 }
                    .maxByOrNull { state.tokenPositions[base + it] } ?: return null
                val currentStep = state.tokenPositions[base + candidateLocal]
                val targetStep = (currentStep + 1..50).firstOrNull { step ->
                    val globalTile = toGlobalTrackIndex(seat.color, step)!!
                    globalTile in SAFE_GLOBAL_TILES
                } ?: return null
                val positions = state.tokenPositions.toMutableList()
                positions[base + candidateLocal] = targetStep
                state.copy(
                    tokenPositions = positions,
                    humanPowerUpsUsed = state.humanPowerUpsUsed + powerUpDelta,
                    lastEventText = "${seat.name} used Magnet Pull to snap onto a Safe Star!"
                )
            }
        }
    }

    fun chooseSmartAiToken(state: LudoGameState): Int? {
        val seat = state.currentSeat ?: return null
        val legal = legalTokenIndices(state, seat.seatIndex)
        if (legal.isEmpty()) return null
        if (legal.size == 1) return legal.first()

        val base = seat.seatIndex * 4
        return legal.maxByOrNull { localIdx ->
            val currentStep = state.tokenPositions[base + localIdx]
            val destStep = if (currentStep == -1) 0 else currentStep + state.effectiveSteps
            var score = 0
            if (destStep == 56) score += 1000
            if (currentStep == -1) score += 450
            if (destStep in 51..55) score += 350
            if (destStep in 0..50) {
                val destGlobal = toGlobalTrackIndex(seat.color, destStep)!!
                if (destGlobal in SAFE_GLOBAL_TILES) score += 220
                if (state.orbs.any { it.globalTile == destGlobal }) score += 180
                if (destGlobal !in SAFE_GLOBAL_TILES) {
                    val canCapture = state.seats.any { other ->
                        other.active && other.seatIndex != seat.seatIndex &&
                            state.shieldTurns[other.seatIndex] == 0 &&
                            (0..3).any { t ->
                                val oStep = state.tokenPositions[other.seatIndex * 4 + t]
                                oStep in 0..50 && toGlobalTrackIndex(other.color, oStep) == destGlobal
                            }
                    }
                    if (canCapture) score += 700
                }
            }
            score + destStep
        }
    }

    private fun nextActiveSeat(state: LudoGameState, fromSeat: Int): Int {
        if (state.seats.isEmpty()) return 0
        for (offset in 1..4) {
            val idx = (fromSeat + offset) % 4
            if (state.seats.getOrNull(idx)?.active == true) return idx
        }
        return fromSeat
    }

    private fun advanceShieldsAndTurn(state: LudoGameState, nextSeat: Int): LudoGameState {
        val shields = state.shieldTurns.toMutableList()
        if (shields[nextSeat] > 0) {
            shields[nextSeat] = (shields[nextSeat] - 1).coerceAtLeast(0)
        }
        return state.copy(
            currentSeatIndex = nextSeat,
            turnPhase = TurnPhase.ROLL,
            lastDiceValue = 0,
            bonusSteps = 0,
            shieldTurns = shields
        )
    }
}
