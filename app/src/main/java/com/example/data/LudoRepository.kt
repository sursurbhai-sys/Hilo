package com.example.data

import android.content.Context
import com.example.R
import com.example.game.LudoEngine
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LudoRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun todayDateKey(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return fmt.format(Date())
    }

    fun observeProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exToLog = firestoreError ?: (error as? Exception)
                    if (exToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeDailyMissions(userId: String, dateKey: String): Flow<List<DailyMission>> = flow {
        val path = "users/$userId/missions"
        emitAll(
            db.collection("users").document(userId).collection("missions")
                .whereEqualTo("missionDate", dateKey)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        DailyMission::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.safeType().ordinal }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exToLog = firestoreError ?: (error as? Exception)
                    if (exToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeLeaderboard(): Flow<List<LeaderboardEntry>> = flow {
        val path = "leaderboard"
        emitAll(
            db.collection("leaderboard")
                .whereGreaterThanOrEqualTo("trophies", 0)
                .orderBy("trophies", Query.Direction.DESCENDING)
                .limit(50)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        LeaderboardEntry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exToLog = firestoreError ?: (error as? Exception)
                    if (exToLog != null && auth.currentUser != null) {
                        handleFirestoreError(exToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeOpenRooms(): Flow<List<MatchRoom>> = flow {
        val path = "rooms"
        emitAll(
            db.collection("rooms")
                .whereEqualTo("status", "WAITING")
                .limit(20)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MatchRoom::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exToLog = firestoreError ?: (error as? Exception)
                    if (exToLog != null && auth.currentUser != null) {
                        handleFirestoreError(exToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeRoom(roomId: String): Flow<MatchRoom?> = flow {
        val path = "rooms/$roomId"
        emitAll(
            db.collection("rooms").document(roomId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        MatchRoom::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exToLog = firestoreError ?: (error as? Exception)
                    if (exToLog != null && auth.currentUser != null) {
                        handleFirestoreError(exToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensurePlayerInitialized(displayName: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val safeName = displayName.trim().ifBlank { "Unic Player" }.take(40)
        return try {
            val userRef = db.collection("users").document(uid)
            val userSnap = userRef.get().await()
            if (!userSnap.exists()) {
                val profilePayload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "trophies" to 100L,
                    "xp" to 0L,
                    "coins" to 200L,
                    "wins" to 0L,
                    "gamesPlayed" to 0L,
                    "captures" to 0L,
                    "powerUpsUsed" to 0L,
                    "sixesRolled" to 0L,
                    "tokensHome" to 0L,
                    "turboCount" to 2L,
                    "shieldCount" to 2L,
                    "swapCount" to 1L,
                    "luckySixCount" to 1L,
                    "freezeCount" to 1L,
                    "magnetCount" to 1L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userRef.set(profilePayload).await()
            }

            val lbRef = db.collection("leaderboard").document(uid)
            val lbSnap = lbRef.get().await()
            if (!lbSnap.exists()) {
                val lbPayload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "trophies" to 100L,
                    "wins" to 0L,
                    "gamesPlayed" to 0L,
                    "captures" to 0L,
                    "tier" to "Bronze",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                lbRef.set(lbPayload).await()
            }

            val dateKey = todayDateKey()
            for (missionType in MissionType.entries) {
                val docId = "${dateKey}_${missionType.name}"
                val missionRef = userRef.collection("missions").document(docId)
                val snap = missionRef.get().await()
                if (!snap.exists()) {
                    val missionPayload = mapOf(
                        "missionType" to missionType.name,
                        "title" to missionType.title,
                        "description" to missionType.description,
                        "missionDate" to dateKey,
                        "progress" to 0L,
                        "target" to missionType.target,
                        "rewardXp" to missionType.rewardXp,
                        "rewardCoins" to missionType.rewardCoins,
                        "claimed" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    missionRef.set(missionPayload).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun spendPowerUpCharge(profile: UserProfile, type: PowerUpType): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid"
        return try {
            val current = profile.chargeCount(type)
            if (current <= 0L) return Result.failure(IllegalStateException("No ${type.title} charges left"))
            val field = powerUpFieldName(type)
            db.collection("users").document(uid).update(
                mapOf(
                    field to (current - 1L).coerceAtLeast(0L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun buyPowerUp(profile: UserProfile, type: PowerUpType): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid"
        return try {
            if (profile.coins < type.coinCost) {
                return Result.failure(IllegalStateException("Need ${type.coinCost} coins to buy ${type.title}"))
            }
            val field = powerUpFieldName(type)
            val newCharges = (profile.chargeCount(type) + 1L).coerceAtMost(999L)
            db.collection("users").document(uid).update(
                mapOf(
                    "coins" to (profile.coins - type.coinCost).coerceAtLeast(0L),
                    field to newCharges,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun claimMission(profile: UserProfile, mission: DailyMission): Result<Unit> {
        val uid = requireUserId()
        val missionPath = "users/$uid/missions/${mission.id}"
        return try {
            if (mission.claimed || mission.progress < mission.target) {
                return Result.failure(IllegalStateException("Mission not ready to claim"))
            }
            db.collection("users").document(uid)
                .collection("missions").document(mission.id)
                .update(
                    mapOf(
                        "claimed" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

            db.collection("users").document(uid).update(
                mapOf(
                    "xp" to (profile.xp + mission.rewardXp).coerceAtMost(10_000_000L),
                    "coins" to (profile.coins + mission.rewardCoins).coerceAtMost(10_000_000L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, missionPath)
            Result.failure(e)
        }
    }

    suspend fun recordMatchOutcome(
        profile: UserProfile,
        missions: List<DailyMission>,
        didWin: Boolean,
        captures: Int,
        powerUpsUsed: Int,
        sixesRolled: Int,
        tokensHome: Int,
        earnedPowerUps: Map<PowerUpType, Int>
    ): Result<Unit> {
        val uid = requireUserId()
        val userPath = "users/$uid"
        return try {
            val trophyDelta = if (didWin) 35L else 10L
            val coinReward = if (didWin) 90L else 30L
            val xpReward = if (didWin) 120L else 45L

            val newTrophies = (profile.trophies + trophyDelta).coerceAtMost(1_000_000L)
            val newWins = (profile.wins + if (didWin) 1L else 0L).coerceAtMost(100_000L)
            val newGames = (profile.gamesPlayed + 1L).coerceAtMost(100_000L)
            val newCaptures = (profile.captures + captures.toLong()).coerceAtMost(1_000_000L)

            val userUpdates = mutableMapOf<String, Any>(
                "trophies" to newTrophies,
                "xp" to (profile.xp + xpReward).coerceAtMost(10_000_000L),
                "coins" to (profile.coins + coinReward).coerceAtMost(10_000_000L),
                "wins" to newWins,
                "gamesPlayed" to newGames,
                "captures" to newCaptures,
                "powerUpsUsed" to (profile.powerUpsUsed + powerUpsUsed.toLong()).coerceAtMost(1_000_000L),
                "sixesRolled" to (profile.sixesRolled + sixesRolled.toLong()).coerceAtMost(1_000_000L),
                "tokensHome" to (profile.tokensHome + tokensHome.toLong()).coerceAtMost(1_000_000L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            for ((type, count) in earnedPowerUps) {
                if (count > 0) {
                    val field = powerUpFieldName(type)
                    userUpdates[field] = (profile.chargeCount(type) + count.toLong()).coerceAtMost(999L)
                }
            }

            db.collection("users").document(uid).update(userUpdates).await()

            val updatedProfile = profile.copy(trophies = newTrophies)
            db.collection("leaderboard").document(uid).update(
                mapOf(
                    "displayName" to profile.displayName,
                    "trophies" to newTrophies,
                    "wins" to newWins,
                    "gamesPlayed" to newGames,
                    "captures" to newCaptures,
                    "tier" to updatedProfile.rankTier(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // Update active unclaimed daily missions
            val deltas = mapOf(
                MissionType.PLAY_MATCHES to 1L,
                MissionType.CAPTURE_TOKENS to captures.toLong(),
                MissionType.USE_POWERUPS to powerUpsUsed.toLong(),
                MissionType.TOKENS_HOME to tokensHome.toLong(),
                MissionType.ROLL_SIXES to sixesRolled.toLong()
            )
            for (mission in missions) {
                if (mission.claimed) continue
                val delta = deltas[mission.safeType()] ?: 0L
                if (delta <= 0L) continue
                val nextProgress = (mission.progress + delta).coerceAtMost(1000L)
                if (nextProgress != mission.progress) {
                    db.collection("users").document(uid)
                        .collection("missions").document(mission.id)
                        .update(
                            mapOf(
                                "progress" to nextProgress,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                }
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userPath)
            Result.failure(e)
        }
    }

    suspend fun createRoom(displayName: String): Result<String> {
        val uid = requireUserId()
        val safeName = displayName.trim().ifBlank { "Host" }.take(30)
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val code = (1..6).map { chars.random() }.joinToString("")
        val path = "rooms/$code"
        return try {
            val initialOrbs = LudoEngine.generateInitialOrbs()
            val payload = mapOf(
                "hostId" to uid,
                "roomCode" to code,
                "status" to "WAITING",
                "playerUids" to listOf(uid),
                "playerNames" to listOf(safeName),
                "playerColors" to listOf("RED"),
                "currentTurnIndex" to 0L,
                "lastDiceValue" to 0L,
                "turnPhase" to "ROLL",
                "winnerUid" to "",
                "tokenPositions" to List(16) { -1L },
                "shieldTurns" to List(4) { 0L },
                "frozenTurns" to List(4) { 0L },
                "orbTiles" to initialOrbs.map { it.globalTile.toLong() },
                "orbTypes" to initialOrbs.map { it.type.name },
                "lastEventText" to "$safeName opened Room $code.",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("rooms").document(code).set(payload).await()
            Result.success(code)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun joinRoom(roomCode: String, displayName: String): Result<String> {
        val uid = requireUserId()
        val code = roomCode.trim().uppercase(Locale.US)
        if (!code.matches(Regex("^[A-Z0-9_\\-]{4,12}$"))) {
            return Result.failure(IllegalArgumentException("Enter a valid 4-12 character room code"))
        }
        val path = "rooms/$code"
        return try {
            val docRef = db.collection("rooms").document(code)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                return Result.failure(IllegalStateException("Room $code not found"))
            }
            val room = snap.toObject(MatchRoom::class.java)
                ?: return Result.failure(IllegalStateException("Invalid room data"))
            if (uid in room.playerUids) {
                return Result.success(code)
            }
            if (room.status != "WAITING") {
                return Result.failure(IllegalStateException("Room $code has already started"))
            }
            if (room.playerUids.size >= 4) {
                return Result.failure(IllegalStateException("Room $code is full"))
            }
            val colors = listOf("RED", "YELLOW", "GREEN", "BLUE")
            val safeName = displayName.trim().ifBlank { "Player" }.take(30)
            val updatedUids = room.playerUids + uid
            val updatedNames = room.playerNames + safeName
            val updatedColors = room.playerColors + colors[room.playerUids.size]
            docRef.update(
                mapOf(
                    "playerUids" to updatedUids,
                    "playerNames" to updatedNames,
                    "playerColors" to updatedColors,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(code)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun startRoom(roomId: String): Result<Unit> {
        val uid = requireUserId()
        val path = "rooms/$roomId"
        return try {
            val orbs = LudoEngine.generateInitialOrbs()
            db.collection("rooms").document(roomId).update(
                mapOf(
                    "status" to "ACTIVE",
                    "currentTurnIndex" to 0L,
                    "turnPhase" to "ROLL",
                    "orbTiles" to orbs.map { it.globalTile.toLong() },
                    "orbTypes" to orbs.map { it.type.name },
                    "lastEventText" to "Online match started! Seat 1 rolls first.",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun pushRoomTurnState(
        roomId: String,
        status: String,
        currentTurnIndex: Int,
        lastDiceValue: Int,
        turnPhase: String,
        winnerUid: String,
        tokenPositions: List<Int>,
        shieldTurns: List<Int>,
        frozenTurns: List<Int>,
        orbTiles: List<Int>,
        orbTypes: List<String>,
        lastEventText: String
    ): Result<Unit> {
        requireUserId()
        val path = "rooms/$roomId"
        return try {
            db.collection("rooms").document(roomId).update(
                mapOf(
                    "status" to status,
                    "currentTurnIndex" to currentTurnIndex.toLong(),
                    "lastDiceValue" to lastDiceValue.toLong(),
                    "turnPhase" to turnPhase,
                    "winnerUid" to winnerUid,
                    "tokenPositions" to tokenPositions.map { it.toLong() },
                    "shieldTurns" to shieldTurns.map { it.toLong() },
                    "frozenTurns" to frozenTurns.map { it.toLong() },
                    "orbTiles" to orbTiles.map { it.toLong() },
                    "orbTypes" to orbTypes,
                    "lastEventText" to lastEventText.take(180),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteWaitingRoom(roomId: String): Result<Unit> {
        requireUserId()
        val path = "rooms/$roomId"
        return try {
            db.collection("rooms").document(roomId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    private fun powerUpFieldName(type: PowerUpType): String = when (type) {
        PowerUpType.TURBO_BOOST -> "turboCount"
        PowerUpType.SHIELD -> "shieldCount"
        PowerUpType.SWAP -> "swapCount"
        PowerUpType.LUCKY_SIX -> "luckySixCount"
        PowerUpType.FREEZE -> "freezeCount"
        PowerUpType.MAGNET -> "magnetCount"
    }
}
