package com.alpha900i.a9kblanketbattle.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.alpha900i.a9kblanketbattle.data.GameState
import com.alpha900i.a9kblanketbattle.domain.BotPlayerA
import com.alpha900i.a9kblanketbattle.domain.BotPlayerB
import com.alpha900i.a9kblanketbattle.domain.Game
import com.alpha900i.a9kblanketbattle.domain.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

public class AppViewModel() : ViewModel() {
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

    init {
        viewModelScope.launch {
            gameState.collect { state ->
                if (state.gameIsActive) {
                    game.makeMove(players, state) { newState ->
                        updateGameState(newState = newState)
                    }
                }
            }
        }
    }


    private val game: Game = Game()
    private val players: List<Player> = listOf(
        BotPlayerA(index = 0),
        BotPlayerB(index = 1)
    )


    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AppViewModel()
            }
        }
    }
}