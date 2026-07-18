package com.alpha900i.a9kblanketbattle.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.alpha900i.a9kblanketbattle.R
import com.alpha900i.a9kblanketbattle.data.GameState
import com.alpha900i.a9kblanketbattle.data.TripletOnBoard
import com.alpha900i.a9kblanketbattle.domain.BotPlayerA
import com.alpha900i.a9kblanketbattle.domain.Game
import com.alpha900i.a9kblanketbattle.domain.HumanPlayer
import com.alpha900i.a9kblanketbattle.domain.Move
import com.alpha900i.a9kblanketbattle.domain.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val isHumanTurn: Boolean
)
sealed class InfoSectionState {
    data class PlayerTurn(val playerIndex: Int): InfoSectionState() {
        override val resourceId: Int = R.string.players_turn
        override val formatArgs = arrayOf(playerIndex + 1)
    }
    data class PlayerRemoval(val playerIndex: Int): InfoSectionState() {
        override val resourceId = R.string.players_removal
        override val formatArgs = arrayOf(playerIndex + 1)
    }
    data class GameOver(val playerIndex: Int): InfoSectionState() {
        override val resourceId = R.string.game_over
        override val formatArgs = arrayOf(playerIndex + 1)
    }
    object WaitingForGame: InfoSectionState() {
        override val resourceId = R.string.waiting_for_game
        override val formatArgs = emptyArray<Any>()
    }
    abstract val resourceId: Int
    abstract val formatArgs: Array<out Any>
}

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
    fun onAnimationComplete() {
        _gameState.update { currentState ->
            currentState.copy(pendingEffects = listOf())
        }
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


    val infoMessage: StateFlow<InfoSectionState> = gameState.map { state ->
        when {
            !state.gameIsActive -> InfoSectionState.GameOver(state.winnerIndex)
            state.deletableTriplets.size > 1 -> InfoSectionState.PlayerRemoval(state.activePlayerIndex)
            else ->  InfoSectionState.PlayerTurn(state.activePlayerIndex)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, InfoSectionState.WaitingForGame)

    fun submitMove(activePlayerIndex: Int, move: Move) {
        players[activePlayerIndex].submitMove(move)
    }
    fun submitRemoval(activePlayerIndex: Int, tripletOnBoard: TripletOnBoard) {
        players[activePlayerIndex].submitRemoval(tripletOnBoard)
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