package com.alpha900i.a9kblanketbattle.ui

import androidx.activity.OnBackPressedDispatcher
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.alpha900i.a9kblanketbattle.data.GameState
import com.alpha900i.a9kblanketbattle.data.TripletOnBoard
import com.alpha900i.a9kblanketbattle.domain.Move
import com.alpha900i.a9kblanketbattle.ui.screens.GameScreen
import com.alpha900i.a9kblanketbattle.ui.screens.SettingsScreen
import com.alpha900i.a9kblanketbattle.ui.screens.StartScreen

@Composable
fun AppNavHost(
    startScreenActions: StartScreenActions,
    gameState: GameState,
    uiState: UiState,
    infoSectionMessage: InfoSectionState,
    activator: () -> Unit,
    submitMove: (Move) -> Unit,
    submitRemoval: (TripletOnBoard) -> Unit,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    NavHost(
        navController = navController,
        startDestination = Screen.START.name
    ) {
        composable(route = Screen.START.name) {
            StartScreen(
                actions = startScreenActions
            )
        }
        composable(route = Screen.GAME.name) {
            GameScreen(
                gameState = gameState,
                isHumanTurn = uiState.isHumanTurn,
                infoSectionMessage = infoSectionMessage,
                activator = activator,
                submitMove = submitMove,
                submitRemoval = submitRemoval
            )
        }
        composable(route = Screen.SETTINGS.name) {
            SettingsScreen()
        }
    }
}

class AppNavigationController(
    private val navController: NavHostController,
    private val backDispatcher: OnBackPressedDispatcher?
) {
    fun navigateToStart() {
        navController.navigate(Screen.START.name) {
            popUpTo(Screen.START.name) { inclusive = true }
        }
    }
    fun navigateToGame() {
        navController.navigate(Screen.GAME.name)
    }
    fun navigateToSettings() {
        navController.navigate(Screen.SETTINGS.name)
    }
    fun goBack() {
        backDispatcher?.onBackPressed() ?: navController.popBackStack()
    }
}