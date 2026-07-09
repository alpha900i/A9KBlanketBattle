package com.alpha900i.a9kblanketbattle.ui.screens

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.alpha900i.a9kblanketbattle.R
import com.alpha900i.a9kblanketbattle.data.GameState
import com.alpha900i.a9kblanketbattle.data.TripletOnBoard
import com.alpha900i.a9kblanketbattle.domain.Move
import com.alpha900i.a9kblanketbattle.ui.AppNavHost
import com.alpha900i.a9kblanketbattle.ui.AppNavigationController
import com.alpha900i.a9kblanketbattle.ui.AppViewModel
import com.alpha900i.a9kblanketbattle.ui.StartScreenActions
import com.alpha900i.a9kblanketbattle.ui.UiState
import com.alpha900i.a9kblanketbattle.ui.theme.A9KBlanketBattleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            A9KBlanketBattleTheme {
                MainContent()
            }
        }
    }
}

@Composable
fun MainContent(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val appNavigationController = remember(navController) {
        AppNavigationController(
            navController = navController,
            backDispatcher = backDispatcher
        )
    }
    val viewModel: AppViewModel = viewModel(factory = AppViewModel.Companion.Factory)
    val gameState by viewModel.gameState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val startScreenActions = object: StartScreenActions{
        override fun startGame() {
            appNavigationController.navigateToGame()
        }

        override fun openSettings() {
            appNavigationController.navigateToSettings()
        }

        override fun exitGame() {
            (context as? Activity)?.finishAffinity()
        }

    }

    Scaffold(
        topBar = {
            AppTopBar(
                stringResource(R.string.main_title)
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        MainScreen(
            gameState = gameState,
            uiState = uiState,
            startScreenActions = startScreenActions,
            activator = viewModel::activateGame,
            submitMove = { move ->
                viewModel.submitMove(gameState.activePlayerIndex, move)
            },
            submitRemoval = { tripletToRemove ->
                viewModel.submitRemoval(gameState.activePlayerIndex, tripletToRemove)
            },
            navController = navController,
            contentPadding = innerPadding
        )
    }
}

@Composable
fun MainScreen(
    gameState: GameState,
    uiState: UiState,
    startScreenActions: StartScreenActions,
    activator: () -> Unit,
    submitMove: (Move) -> Unit,
    submitRemoval: (TripletOnBoard) -> Unit,
    navController: NavHostController,
    contentPadding: PaddingValues
) {
    Box(modifier = Modifier.padding(contentPadding)) {
        AppNavHost(
            startScreenActions = startScreenActions,
            gameState = gameState,
            uiState = uiState,
            activator = activator,
            submitMove = submitMove,
            submitRemoval = submitRemoval,
            navController = navController
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Text(
                text = title
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = modifier,
        navigationIcon = {},
        actions = {}
    )
}