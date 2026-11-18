package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.signOutUser
import com.example.data.LudoRepository
import com.example.data.UiState
import com.example.ui.AuthScreen
import com.example.ui.GameBoardScreen
import com.example.ui.GameViewModel
import com.example.ui.HomeLobbyScreen
import com.example.ui.LeaderboardScreen
import com.example.ui.MainViewModel
import com.example.ui.MissionsScreen
import com.example.ui.WaitingRoomOverlay
import com.example.ui.theme.LudoUnicTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LudoUnicTheme {
                LudoUnicAppRoot()
            }
        }
    }
}

private fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun LudoUnicAppRoot(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            LudoMainShell(
                currentUserId = user.uid,
                displayName = user.displayName?.takeIf { it.isNotBlank() } ?: "Unic Player"
            )
        }
    }
}

@Composable
private fun LudoMainShell(
    currentUserId: String,
    displayName: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val mainViewModel: MainViewModel = viewModel(
        key = "main_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val dbId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                MainViewModel(LudoRepository(db), currentUserId, displayName)
            }
        }
    )

    val gameViewModel: GameViewModel = viewModel(
        key = "game_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val dbId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                GameViewModel(LudoRepository(db), currentUserId, displayName)
            }
        }
    )

    val profileState by mainViewModel.profileState.collectAsStateWithLifecycle()
    val missionsState by mainViewModel.missionsState.collectAsStateWithLifecycle()
    val leaderboardState by mainViewModel.leaderboardState.collectAsStateWithLifecycle()
    val openRoomsState by mainViewModel.openRoomsState.collectAsStateWithLifecycle()
    val bannerMessage by mainViewModel.bannerMessage.collectAsStateWithLifecycle()
    val isRoomBusy by mainViewModel.isRoomBusy.collectAsStateWithLifecycle()

    val gameState by gameViewModel.gameState.collectAsStateWithLifecycle()
    val waitingRoom by gameViewModel.waitingRoom.collectAsStateWithLifecycle()
    val isDiceRolling by gameViewModel.isDiceRolling.collectAsStateWithLifecycle()
    val matchError by gameViewModel.matchError.collectAsStateWithLifecycle()
    val outcomeSummary by gameViewModel.outcomeSummary.collectAsStateWithLifecycle()

    val profile = (profileState as? UiState.Success)?.data
    val missions = (missionsState as? UiState.Success)?.data ?: emptyList()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(bannerMessage) {
        val msg = bannerMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        mainViewModel.dismissBanner()
    }

    val activeWaitingRoom = waitingRoom
    val activeGame = gameState

    if (activeWaitingRoom != null) {
        WaitingRoomOverlay(
            room = activeWaitingRoom,
            currentUserId = currentUserId,
            onStartMatch = { gameViewModel.startWaitingRoom() },
            onCancelRoom = { gameViewModel.cancelWaitingRoom {} }
        )
        return
    }

    if (activeGame != null) {
        GameBoardScreen(
            gameState = activeGame,
            profile = profile,
            isDiceRolling = isDiceRolling,
            matchError = matchError,
            outcomeSummary = outcomeSummary,
            currentUserId = currentUserId,
            onRollDice = { gameViewModel.rollDice(profile, missions) },
            onMoveToken = { tokenIdx -> gameViewModel.moveToken(tokenIdx, profile, missions) },
            onActivatePowerUp = { type -> gameViewModel.activatePowerUp(type, profile, missions) },
            onDismissError = { gameViewModel.clearError() },
            onPlayAgain = { gameViewModel.startLocalMatch(activeGame.mode) },
            onExitMatch = { gameViewModel.exitMatch() }
        )
        return
    }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar {
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 0) Icons.Filled.SportsEsports else Icons.Outlined.SportsEsports,
                                    contentDescription = stringResource(id = R.string.nav_arena)
                                )
                            },
                            label = { Text(stringResource(id = R.string.nav_arena)) },
                            modifier = Modifier.testTag("nav_arena")
                        )
                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 1) Icons.Filled.MilitaryTech else Icons.Outlined.MilitaryTech,
                                    contentDescription = stringResource(id = R.string.nav_missions)
                                )
                            },
                            label = { Text(stringResource(id = R.string.nav_missions)) },
                            modifier = Modifier.testTag("nav_missions")
                        )
                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 2) Icons.Filled.EmojiEvents else Icons.Outlined.EmojiEvents,
                                    contentDescription = stringResource(id = R.string.nav_ranks)
                                )
                            },
                            label = { Text(stringResource(id = R.string.nav_ranks)) },
                            modifier = Modifier.testTag("nav_ranks")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail {
                        NavigationRailItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 0) Icons.Filled.SportsEsports else Icons.Outlined.SportsEsports,
                                    contentDescription = stringResource(id = R.string.nav_arena)
                                )
                            },
                            label = { Text(stringResource(id = R.string.nav_arena)) }
                        )
                        NavigationRailItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 1) Icons.Filled.MilitaryTech else Icons.Outlined.MilitaryTech,
                                    contentDescription = stringResource(id = R.string.nav_missions)
                                )
                            },
                            label = { Text(stringResource(id = R.string.nav_missions)) }
                        )
                        NavigationRailItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 2) Icons.Filled.EmojiEvents else Icons.Outlined.EmojiEvents,
                                    contentDescription = stringResource(id = R.string.nav_ranks)
                                )
                            },
                            label = { Text(stringResource(id = R.string.nav_ranks)) }
                        )
                    }
                }

                when (selectedTab) {
                    0 -> HomeLobbyScreen(
                        profileState = profileState,
                        openRoomsState = openRoomsState,
                        isRoomBusy = isRoomBusy,
                        onStartLocalMatch = { mode -> gameViewModel.startLocalMatch(mode) },
                        onCreateOnlineRoom = {
                            mainViewModel.createOnlineRoom { roomCode ->
                                gameViewModel.attachOnlineRoom(roomCode)
                            }
                        },
                        onJoinOnlineRoom = { code ->
                            mainViewModel.joinOnlineRoom(code) { roomCode ->
                                gameViewModel.attachOnlineRoom(roomCode)
                            }
                        },
                        onOpenArmory = { selectedTab = 1 },
                        onSignOut = {
                            gameViewModel.exitMatch()
                            signOutUser(credentialManager, scope)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    1 -> MissionsScreen(
                        profileState = profileState,
                        missionsState = missionsState,
                        onClaimMission = { mission -> mainViewModel.claimMission(mission) },
                        onBuyPowerUp = { powerUp -> mainViewModel.buyPowerUp(powerUp) },
                        modifier = Modifier.weight(1f)
                    )
                    else -> LeaderboardScreen(
                        leaderboardState = leaderboardState,
                        currentUserId = currentUserId,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
