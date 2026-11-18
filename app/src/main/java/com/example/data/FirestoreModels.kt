package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class PowerUpType(
    val title: String,
    val shortDesc: String,
    val coinCost: Long,
    val badgeLabel: String
) {
    TURBO_BOOST(
        title = "Turbo Boost",
        shortDesc = "Adds +3 bonus steps to your next dice roll.",
        coinCost = 40L,
        badgeLabel = "+3"
    ),
    SHIELD(
        title = "Shield Aura",
        shortDesc = "Protects your pawns from captures for 2 turns.",
        coinCost = 50L,
        badgeLabel = "SHD"
    ),
    SWAP(
        title = "Swap Warp",
        shortDesc = "Swaps your lead pawn with an unshielded rival pawn.",
        coinCost = 70L,
        badgeLabel = "SWP"
    ),
    LUCKY_SIX(
        title = "Precision 6",
        shortDesc = "Guarantees your next roll lands on 6.",
        coinCost = 65L,
        badgeLabel = "6!"
    ),
    FREEZE(
        title = "Freeze Trap",
        shortDesc = "Freezes the next opponent so they skip 1 turn.",
        coinCost = 55L,
        badgeLabel = "ICE"
    ),
    MAGNET(
        title = "Magnet Pull",
        shortDesc = "Warps your lead pawn to the next safe star tile.",
        coinCost = 60L,
        badgeLabel = "MAG"
    );

    companion object {
        fun fromName(raw: String?): PowerUpType? =
            entries.firstOrNull { it.name == raw }
    }
}

enum class MissionType(
    val title: String,
    val description: String,
    val target: Long,
    val rewardXp: Long,
    val rewardCoins: Long
) {
    PLAY_MATCHES(
        title = "Arena Contender",
        description = "Complete 2 Ludo Unic matches today",
        target = 2L,
        rewardXp = 150L,
        rewardCoins = 80L
    ),
    CAPTURE_TOKENS(
        title = "Hunter Strike",
        description = "Capture 3 rival tokens on the board",
        target = 3L,
        rewardXp = 200L,
        rewardCoins = 120L
    ),
    USE_POWERUPS(
        title = "Power Tactician",
        description = "Activate 4 Power-Ups during matches",
        target = 4L,
        rewardXp = 180L,
        rewardCoins = 100L
    ),
    TOKENS_HOME(
        title = "Homeward Bound",
        description = "Guide 4 pawns safely into the center goal",
        target = 4L,
        rewardXp = 250L,
        rewardCoins = 150L
    ),
    ROLL_SIXES(
        title = "Six Shooter",
        description = "Roll 5 sixes across any game mode",
        target = 5L,
        rewardXp = 120L,
        rewardCoins = 60L
    )
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Player",
    val trophies: Long = 100L,
    val xp: Long = 0L,
    val coins: Long = 200L,
    val wins: Long = 0L,
    val gamesPlayed: Long = 0L,
    val captures: Long = 0L,
    val powerUpsUsed: Long = 0L,
    val sixesRolled: Long = 0L,
    val tokensHome: Long = 0L,
    val turboCount: Long = 2L,
    val shieldCount: Long = 2L,
    val swapCount: Long = 1L,
    val luckySixCount: Long = 1L,
    val freezeCount: Long = 1L,
    val magnetCount: Long = 1L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun chargeCount(type: PowerUpType): Long = when (type) {
        PowerUpType.TURBO_BOOST -> turboCount
        PowerUpType.SHIELD -> shieldCount
        PowerUpType.SWAP -> swapCount
        PowerUpType.LUCKY_SIX -> luckySixCount
        PowerUpType.FREEZE -> freezeCount
        PowerUpType.MAGNET -> magnetCount
    }

    fun rankTier(): String = when {
        trophies >= 2000L -> "Unic Master"
        trophies >= 1200L -> "Platinum"
        trophies >= 700L -> "Gold"
        trophies >= 350L -> "Silver"
        else -> "Bronze"
    }
}

@Keep
@IgnoreExtraProperties
data class DailyMission(
    @DocumentId val id: String = "",
    val missionType: String = MissionType.PLAY_MATCHES.name,
    val title: String = "",
    val description: String = "",
    val missionDate: String = "",
    val progress: Long = 0L,
    val target: Long = 1L,
    val rewardXp: Long = 100L,
    val rewardCoins: Long = 50L,
    val claimed: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun safeType(): MissionType =
        MissionType.entries.firstOrNull { it.name == missionType } ?: MissionType.PLAY_MATCHES
}

@Keep
@IgnoreExtraProperties
data class LeaderboardEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Player",
    val trophies: Long = 0L,
    val wins: Long = 0L,
    val gamesPlayed: Long = 0L,
    val captures: Long = 0L,
    val tier: String = "Bronze",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MatchRoom(
    @DocumentId val id: String = "",
    val hostId: String = "",
    val roomCode: String = "",
    val status: String = "WAITING",
    val playerUids: List<String> = emptyList(),
    val playerNames: List<String> = emptyList(),
    val playerColors: List<String> = emptyList(),
    val currentTurnIndex: Long = 0L,
    val lastDiceValue: Long = 0L,
    val turnPhase: String = "ROLL",
    val winnerUid: String = "",
    val tokenPositions: List<Long> = List(16) { -1L },
    val shieldTurns: List<Long> = List(4) { 0L },
    val frozenTurns: List<Long> = List(4) { 0L },
    val orbTiles: List<Long> = emptyList(),
    val orbTypes: List<String> = emptyList(),
    val lastEventText: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
