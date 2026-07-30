package com.alpha900i.a9kblanketbattle.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.alpha900i.a9kblanketbattle.MainApplication
import com.alpha900i.a9kblanketbattle.R
import com.alpha900i.a9kblanketbattle.data.GameState
import com.alpha900i.a9kblanketbattle.data.TripletOnBoard
import com.alpha900i.a9kblanketbattle.data.repository.DataStoreRepository
import com.alpha900i.a9kblanketbattle.domain.Game
import com.alpha900i.a9kblanketbattle.domain.HumanPlayer
import com.alpha900i.a9kblanketbattle.domain.Move
import com.alpha900i.a9kblanketbattle.domain.Player
import com.alpha900i.a9kblanketbattle.domain.PlayerType
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val isHumanTurn: Boolean
)

sealed class InfoSectionState {
    data class PlayerTurn(val playerIndex: Int) : InfoSectionState() {
        override val resourceId: Int = R.string.players_turn
        override val formatArgs = arrayOf(playerIndex + 1)
    }

    data class PlayerRemoval(val playerIndex: Int) : InfoSectionState() {
        override val resourceId = R.string.players_removal
        override val formatArgs = arrayOf(playerIndex + 1)
    }

    data class GameOver(val playerIndex: Int) : InfoSectionState() {
        override val resourceId = R.string.game_over
        override val formatArgs = arrayOf(playerIndex + 1)
    }

    object WaitingForGame : InfoSectionState() {
        override val resourceId = R.string.waiting_for_game
        override val formatArgs = emptyArray<Any>()
    }

    abstract val resourceId: Int
    abstract val formatArgs: Array<out Any>
}

class AppViewModel(
    val dataStoreRepository: DataStoreRepository
) : ViewModel() {
    private val _gameState = MutableStateFlow(
        GameState.startingState(
            gameIsActive = false,
            width = Constants.DEFAULT_WIDTH,
            height = Constants.DEFAULT_HEIGHT,
            kittenStart = Constants.DEFAULT_KITTEN_START,
            catStart = Constants.DEFAULT_CAT_START
        )
    )
    val gameState: StateFlow<GameState> = _gameState.asStateFlow();
    fun startNewGame() {
        viewModelScope.launch {
            gameLoopJob?.cancel()
            players = listOf(
                firstPlayerType.first().makePlayer(0),
                secondPlayerType.first().makePlayer(0)
            )
            _gameState.update {
                GameState.startingState(
                    width = width.first(),
                    height = height.first(),
                    kittenStart = kittenStart.first(),
                    catStart = catStart.first(),
                    gameIsActive = true
                )
            }

            startGameLoop()
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
            else -> InfoSectionState.PlayerTurn(state.activePlayerIndex)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, InfoSectionState.WaitingForGame)

    fun submitMove(activePlayerIndex: Int, move: Move) {
        players[activePlayerIndex].submitMove(move)
    }

    fun submitRemoval(activePlayerIndex: Int, tripletOnBoard: TripletOnBoard) {
        players[activePlayerIndex].submitRemoval(tripletOnBoard)
    }


    //data store section
    val width: Flow<Int> = dataStoreRepository.width
    fun setWidth(width: Int) {
        viewModelScope.launch {
            dataStoreRepository.setWidth(width = width)
        }
    }

    val height: Flow<Int> = dataStoreRepository.height
    fun setHeight(height: Int) {
        viewModelScope.launch {
            dataStoreRepository.setHeight(height = height)
        }
    }

    val kittenStart: Flow<Int> = dataStoreRepository.kittenStart
    fun setKittenStart(kittenStart: Int) {
        viewModelScope.launch {
            dataStoreRepository.setKittenStart(kittenStart = kittenStart)
        }
    }

    val catStart: Flow<Int> = dataStoreRepository.catStart
    fun setCatStart(catStart: Int) {
        viewModelScope.launch {
            dataStoreRepository.setCatStart(catStart = catStart)
        }
    }

    val firstPlayerType: Flow<PlayerType> = dataStoreRepository.firstPlayerType
    fun setFirstPlayerType(playerType: PlayerType) {
        viewModelScope.launch {
            dataStoreRepository.setFirstPlayerType(playerType = playerType)
        }
    }

    val secondPlayerType: Flow<PlayerType> = dataStoreRepository.secondPlayerType
    fun setSecondPlayerType(playerType: PlayerType) {
        viewModelScope.launch {
            dataStoreRepository.setSecondPlayerType(playerType = playerType)
        }
    }



    init {
        startGameLoop()
    }


    private var gameLoopJob: Job? = null

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            gameState.collect { state ->
                try {
                    if (state.gameIsActive) {
                        setHumanTurn(players[state.activePlayerIndex] is HumanPlayer)
                        game.makeMove(players, state) { newState ->
                            updateGameState(newState = newState)
                        }
                    }
                } catch (e: Exception) {
                    Log.d("viewModel", "Exception: ${e.message}")
                }
            }
        }
    }


    private val game: Game = Game()
    private var players: List<Player> = listOf(
        HumanPlayer(index = 0),
        HumanPlayer(index = 1)
    )


    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as MainApplication)
                val dataStoreRepository = application.container.dataStoreRepository
                AppViewModel(
                    dataStoreRepository = dataStoreRepository
                )
            }
        }
    }
}