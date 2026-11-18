package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DailyMission
import com.example.data.LeaderboardEntry
import com.example.data.LudoRepository
import com.example.data.MatchRoom
import com.example.data.PowerUpType
import com.example.data.UiState
import com.example.data.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: LudoRepository,
    private val currentUserId: String,
    private val initialDisplayName: String
) : ViewModel() {

    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    private val _isRoomBusy = MutableStateFlow(false)
    val isRoomBusy: StateFlow<Boolean> = _isRoomBusy.asStateFlow()

    val profileState: StateFlow<UiState<UserProfile?>> = repository.observeProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { error -> emit(UiState.Error(unwrapFirestoreMessage(error, "Failed to load profile"))) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UiState.Loading
        )

    val missionsState: StateFlow<UiState<List<DailyMission>>> =
        repository.observeDailyMissions(currentUserId, repository.todayDateKey())
            .map<List<DailyMission>, UiState<List<DailyMission>>> { UiState.Success(it) }
            .catch { error -> emit(UiState.Error(unwrapFirestoreMessage(error, "Failed to load daily missions"))) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = UiState.Loading
            )

    val leaderboardState: StateFlow<UiState<List<LeaderboardEntry>>> = repository.observeLeaderboard()
        .map<List<LeaderboardEntry>, UiState<List<LeaderboardEntry>>> { UiState.Success(it) }
        .catch { error -> emit(UiState.Error(unwrapFirestoreMessage(error, "Failed to load leaderboard"))) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UiState.Loading
        )

    val openRoomsState: StateFlow<UiState<List<MatchRoom>>> = repository.observeOpenRooms()
        .map<List<MatchRoom>, UiState<List<MatchRoom>>> { UiState.Success(it) }
        .catch { error -> emit(UiState.Error(unwrapFirestoreMessage(error, "Failed to load open rooms"))) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UiState.Loading
        )

    init {
        viewModelScope.launch {
            repository.ensurePlayerInitialized(initialDisplayName).onFailure { err ->
                _bannerMessage.value = err.localizedMessage ?: "Failed to initialize player data"
            }
        }
    }

    fun claimMission(mission: DailyMission) {
        val profile = (profileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.claimMission(profile, mission)
                .onSuccess {
                    _bannerMessage.value = "Claimed +${mission.rewardXp} XP & +${mission.rewardCoins} Coins!"
                }
                .onFailure { err ->
                    _bannerMessage.value = err.localizedMessage ?: "Could not claim mission"
                }
        }
    }

    fun buyPowerUp(type: PowerUpType) {
        val profile = (profileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.buyPowerUp(profile, type)
                .onSuccess {
                    _bannerMessage.value = "Added +1 ${type.title} charge!"
                }
                .onFailure { err ->
                    _bannerMessage.value = err.localizedMessage ?: "Purchase failed"
                }
        }
    }

    fun createOnlineRoom(onRoomReady: (String) -> Unit) {
        if (_isRoomBusy.value) return
        val name = (profileState.value as? UiState.Success)?.data?.displayName ?: initialDisplayName
        viewModelScope.launch {
            _isRoomBusy.value = true
            repository.createRoom(name)
                .onSuccess { roomCode -> onRoomReady(roomCode) }
                .onFailure { err ->
                    _bannerMessage.value = err.localizedMessage ?: "Unable to create room"
                }
            _isRoomBusy.value = false
        }
    }

    fun joinOnlineRoom(roomCode: String, onRoomReady: (String) -> Unit) {
        if (_isRoomBusy.value) return
        val name = (profileState.value as? UiState.Success)?.data?.displayName ?: initialDisplayName
        viewModelScope.launch {
            _isRoomBusy.value = true
            repository.joinRoom(roomCode, name)
                .onSuccess { code -> onRoomReady(code) }
                .onFailure { err ->
                    _bannerMessage.value = err.localizedMessage ?: "Unable to join room"
                }
            _isRoomBusy.value = false
        }
    }

    fun dismissBanner() {
        _bannerMessage.value = null
    }

    private fun unwrapFirestoreMessage(error: Throwable, fallback: String): String {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, fallback, rootError)
        return rootError.message ?: fallback
    }

    private companion object {
        const val TAG = "MainVM"
        const val STOP_TIMEOUT_MS = 5000L
    }
}
