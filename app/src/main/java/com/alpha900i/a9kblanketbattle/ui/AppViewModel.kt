package com.alpha900i.a9kblanketbattle.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.alpha900i.a9kblanketbattle.data.GameState
import com.alpha900i.a9kblanketbattle.domain.BotPlayerA
import com.alpha900i.a9kblanketbattle.domain.Game
import com.alpha900i.a9kblanketbattle.domain.HumanPlayer
import com.alpha900i.a9kblanketbattle.domain.Move
import com.alpha900i.a9kblanketbattle.domain.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val isHumanTurn: Boolean
)

class AppViewModel() : ViewModel() {
    private val _gameState = MutableStateFlow(
        GameState.startingState()
    )
    val gameState: StateFlow<GameState> = _gameState.asStateFlow();
    fun activateGame() {
        _gameState.update { currentState ->
            currentState.copy(gameIsActive = true)
        }
    }
    fun updateGameState(newState: GameState) {
        _gameState.update { newState }
    }

    private val _uiState = MutableStateFlow(
        UiState(
            isHumanTurn = false
        )
    )
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    fun setHumanTurn(isHumanTurn: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(isHumanTurn = isHumanTurn)
        }
    }


    fun submitMove(activePlayerIndex: Int, move: Move) {
        players[activePlayerIndex].submitMove(move)
    }


    init {
        viewModelScope.launch {
            gameState.collect { state ->
                if (state.gameIsActive) {
                    setHumanTurn(players[state.activePlayerIndex] is HumanPlayer)
                    game.makeMove(players, state) { newState ->
                        updateGameState(newState = newState)
                    }
                }
            }
        }
    }


    private val game: Game = Game()
    private val players: List<Player> = listOf(
        HumanPlayer(index = 0),
        HumanPlayer(index = 1)
    )


    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AppViewModel()
            }
        }
    }
}