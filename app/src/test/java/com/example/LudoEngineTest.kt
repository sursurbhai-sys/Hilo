package com.example

import com.example.data.PowerUpType
import com.example.game.GameMode
import com.example.game.LudoEngine
import com.example.game.TurnPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LudoEngineTest {

    @Test
    fun deployFromYardRequiresSixAndPlacesAtZero() {
        val start = LudoEngine.createLocalMatch(GameMode.AI_DUEL, "Tester", "u1")
        val rolledFive = LudoEngine.rollDice(start, forcedRoll = 5)
        // All tokens in yard, rolling 5 auto-advances turn
        assertEquals(TurnPhase.ROLL, rolledFive.turnPhase)
        assertEquals(2, rolledFive.currentSeatIndex)

        val rolledSix = LudoEngine.rollDice(start, forcedRoll = 6)
        assertEquals(TurnPhase.MOVE, rolledSix.turnPhase)
        assertEquals(listOf(0, 1, 2, 3), LudoEngine.legalTokenIndices(rolledSix, 0))

        val moved = LudoEngine.moveToken(rolledSix, 0)
        assertEquals(0, moved.nextState.tokenPositions[0])
        // Rolling 6 grants bonus roll on same seat
        assertEquals(0, moved.nextState.currentSeatIndex)
        assertEquals(TurnPhase.ROLL, moved.nextState.turnPhase)
    }

    @Test
    fun shieldAuraProtectsFromCapture() {
        val base = LudoEngine.createLocalMatch(GameMode.AI_DUEL, "Tester", "u1")
        // Put Red token 0 at step 15 (global 15, non-safe) and Yellow token 0 at step 40 (global (26+40)%52 = 14)
        val positions = base.tokenPositions.toMutableList()
        positions[0] = 15
        positions[8] = 40 // Yellow is seat 2 -> base index 8
        val shielded = base.copy(
            tokenPositions = positions,
            currentSeatIndex = 2,
            shieldTurns = listOf(2, 0, 0, 0)
        )
        val rolledOne = LudoEngine.rollDice(shielded, forcedRoll = 1)
        val outcome = LudoEngine.moveToken(rolledOne, 0)
        assertEquals(0, outcome.capturedCount)
        assertEquals(15, outcome.nextState.tokenPositions[0])
    }

    @Test
    fun powerUpsTurboAndLuckySixAndMagnetWork() {
        val base = LudoEngine.createLocalMatch(GameMode.AI_DUEL, "Tester", "u1")
        val positions = base.tokenPositions.toMutableList()
        positions[0] = 2
        val primed = LudoEngine.applyPowerUp(base.copy(tokenPositions = positions), PowerUpType.TURBO_BOOST)
        assertNotNull(primed)
        val rolled = LudoEngine.rollDice(primed!!, forcedRoll = 4)
        assertEquals(7, rolled.effectiveSteps) // 4 + 3 turbo

        val moved = LudoEngine.moveToken(rolled, 0)
        assertEquals(9, moved.nextState.tokenPositions[0])

        val magnetized = LudoEngine.applyPowerUp(moved.nextState.copy(currentSeatIndex = 0), PowerUpType.MAGNET)
        assertNotNull(magnetized)
        // Next safe star after step 9 (global 9) is global 13 (step 13)
        assertEquals(13, magnetized!!.tokenPositions[0])

        val luckySix = LudoEngine.applyPowerUp(
            magnetized.copy(currentSeatIndex = 0, turnPhase = TurnPhase.ROLL),
            PowerUpType.LUCKY_SIX
        )
        assertNotNull(luckySix)
        assertEquals(6, luckySix!!.lastDiceValue)
        assertTrue(luckySix.humanSixesRolled >= 1)
    }
}
