package com.alpha900i.a9kblanketbattle.ui

import androidx.annotation.StringRes
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
import com.alpha900i.a9kblanketbattle.data.datastore.InvalidSettingsException
import com.alpha900i.a9kblanketbattle.data.datastore.Settings
import com.alpha900i.a9kblanketbattle.data.repository.DataStoreRepository
import com.alpha900i.a9kblanketbattle.domain.Game
import com.alpha900i.a9kblanketbattle.domain.HumanPlayer
import com.alpha900i.a9kblanketbattle.domain.Move
import com.alpha900i.a9kblanketbattle.domain.MoveType
import com.alpha900i.a9kblanketbattle.domain.Player
import com.alpha900i.a9kblanketbattle.domain.PlayerType
import com.alpha900i.a9kblanketbattle.util.CustomLog
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val isHumanTurn: Boolean,
    val selectedMoveType: MoveType?
)

enum class TempMessageType {
    CANT_SET_KITTEN,
    CANT_SET_CAT,
    CANT_PROMOTE_KITTEN,
    CANT_RETURN_CAT
}
sealed class InfoSectionTempState {
    object NoHint: InfoSectionTempState() {
        override val resourceId: Int = 0
        override val formatArgs = emptyArray<Any>()
    }
    data class CantSetKitten(val playerIndex: Int): InfoSectionTempState() {
        override val resourceId: Int = R.string.cant_set_kitten_hint
        override val formatArgs = arrayOf(playerIndex + 1)
    }
    data class CantSetCat(val playerIndex: Int): InfoSectionTempState() {
        override val resourceId: Int = R.string.cant_set_cat_hint
        override val formatArgs = arrayOf(playerIndex + 1)
    }
    data class CantPromoteKitten(val playerIndex: Int): InfoSectionTempState() {
        override val resourceId: Int = R.string.cant_promote_kitten_hint
        override val formatArgs = arrayOf(playerIndex + 1)
    }
    data class CantReturnCat(val playerIndex: Int): InfoSectionTempState() {
        override val resourceId: Int = R.string.cant_return_cat_hint
        override val formatArgs = arrayOf(playerIndex + 1)
    }

    abstract val resourceId: Int
    abstract val formatArgs: Array<out Any>
}
sealed class InfoSectionState {
    data class GeneralTurn(val playerIndex: Int) : InfoSectionState() {
        override val resourceId: Int = R.string.player_turn
        override val formatArgs = arrayOf(playerIndex + 1)
    }
    data class SetKitten(val playerIndex: Int) : InfoSectionState() {
        override val resourceId: Int = R.string.player_set_kitten_turn
        override val formatArgs = arrayOf(playerIndex + 1)
    }
    data class SetCat(val playerIndex: Int) : InfoSectionState() {
        override val resourceId: Int = R.string.player_set_cat_turn
        override val formatArgs = arrayOf(playerIndex + 1)
    }
    data class PromoteKitten(val playerIndex: Int) : InfoSectionState() {
        override val resourceId: Int = R.string.player_promote_kitten_turn
        override val formatArgs = arrayOf(playerIndex + 1)
    }
    data class ReturnCat(val playerIndex: Int) : InfoSectionState() {
        override val resourceId: Int = R.string.player_return_cat_turn
        override val formatArgs = arrayOf(playerIndex + 1)
    }

    data class TripletRemoval(val playerIndex: Int) : InfoSectionState() {
        override val resourceId = R.string.players_removal
        override val formatArgs = arrayOf(playerIndex + 1)
    }

    data class GameOver(val playerIndex: Int) : InfoSectionState() {
        override val resourceId = R.string.game_over
        override val formatArgs = arrayOf(playerIndex + 1)
    }

    data class TempMessage(val temp: InfoSectionTempState) : InfoSectionState() {
        override val resourceId = temp.resourceId
        override val formatArgs = temp.formatArgs
    }

    object WaitingForGame : InfoSectionState() {
        override val resourceId = R.string.waiting_for_game
        override val formatArgs = emptyArray<Any>()
    }

    abstract val resourceId: Int
    abstract val formatArgs: Array<out Any>
}


/**
 * Result of attempt to save settings
 * [Success] is obvious, [Failure] is result of init's consistency check
 */
sealed interface SettingsResult {
    data object Success : SettingsResult
    data class Failure(
        @StringRes val messageRes: Int,
        val formatArgs: List<Any> = emptyList(),
    ) : SettingsResult
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
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    fun startNewGame() {
        viewModelScope.launch {
            gameLoopJob?.cancel()
            players = listOf(
                initialSettings.first().firstPlayerType.makePlayer(0),
                initialSettings.first().secondPlayerType.makePlayer(1)
            )
            _gameState.update {
                GameState.startingState(
                    width = initialSettings.first().width,
                    height = initialSettings.first().height,
                    kittenStart = initialSettings.first().kittenStart,
                    catStart = initialSettings.first().catStart,
                    gameIsActive = true
                )
            }

            startGameLoop()
        }
    }

    fun updateGameState(newState: GameState) {
        CustomLog.d("Move ${newState.moveCount}")
        _gameState.update { newState }
    }

    fun onAnimationComplete() {
        _gameState.update { currentState ->
            currentState.copy(pendingEffects = listOf())
        }
    }


    private val _uiState = MutableStateFlow(
        UiState(
            isHumanTurn = false,
            selectedMoveType = null
        )
    )
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    fun setHumanTurn(isHumanTurn: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(isHumanTurn = isHumanTurn)
        }
    }
    fun selectMoveType(moveType: MoveType?) {
        _uiState.update { currentState ->
            currentState.copy(selectedMoveType = moveType)
        }
    }


    private var _tempMessageJob: Job? = null
    private val _infoTempMessage: MutableStateFlow<InfoSectionTempState> = MutableStateFlow(InfoSectionTempState.NoHint)
    val infoTempMessage: StateFlow<InfoSectionTempState> = _infoTempMessage.asStateFlow()
    fun setTempMessage(playerIndex: Int, tempMessageType: TempMessageType) {
        val tempMessage = when (tempMessageType) {
            TempMessageType.CANT_SET_KITTEN -> InfoSectionTempState.CantSetKitten(playerIndex = playerIndex)
            TempMessageType.CANT_SET_CAT -> InfoSectionTempState.CantSetCat(playerIndex = playerIndex)
            TempMessageType.CANT_PROMOTE_KITTEN -> InfoSectionTempState.CantPromoteKitten(playerIndex = playerIndex)
            TempMessageType.CANT_RETURN_CAT -> InfoSectionTempState.CantReturnCat(playerIndex = playerIndex)
        }
        setTempMessage(tempMessage)
    }
    fun setTempMessage(tempMessage: InfoSectionTempState) {
        _tempMessageJob?.cancel()
        _infoTempMessage.value = tempMessage
        _infoTempMessage.update { tempMessage }
        _tempMessageJob = viewModelScope.launch {
            delay(1000)
            // Only clear if this hint is still the current one
            if (_infoTempMessage.value == tempMessage) _infoTempMessage.value = InfoSectionTempState.NoHint
        }
    }


    val infoPermanentMessage: StateFlow<InfoSectionState> = combine(gameState, uiState, infoTempMessage) {
        gameState, uiState, tempMessage ->
        when {
            (tempMessage != InfoSectionTempState.NoHint) -> InfoSectionState.TempMessage(tempMessage)
            !gameState.gameIsActive -> InfoSectionState.GameOver(gameState.winnerIndex)
            gameState.deletableTriplets.size > 1 -> InfoSectionState.TripletRemoval(gameState.activePlayerIndex)
            uiState.selectedMoveType == MoveType.SET_KITTEN -> InfoSectionState.SetKitten(gameState.activePlayerIndex)
            uiState.selectedMoveType == MoveType.SET_CAT -> InfoSectionState.SetCat(gameState.activePlayerIndex)
            uiState.selectedMoveType == MoveType.PROMOTE_KITTEN -> InfoSectionState.PromoteKitten(gameState.activePlayerIndex)
            uiState.selectedMoveType == MoveType.RETURN_CAT -> InfoSectionState.ReturnCat(gameState.activePlayerIndex)
            else -> InfoSectionState.GeneralTurn(gameState.activePlayerIndex)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, InfoSectionState.WaitingForGame)

    fun submitMove(activePlayerIndex: Int, move: Move) {
        val player = players[activePlayerIndex]
        check(player is HumanPlayer) {
            "submitMove called for non-human player #$activePlayerIndex (${player::class.simpleName})"
        }
        player.submitMove(move)
    }

    fun submitRemoval(activePlayerIndex: Int, tripletOnBoard: TripletOnBoard) {
        val player = players[activePlayerIndex]
        check(player is HumanPlayer) {
            "submitRemoval called for non-human player #$activePlayerIndex (${player::class.simpleName})"
        }
        player.submitRemoval(tripletOnBoard)
    }


    //data store section
    //initialSettings are needed for first-time access on creation, which StateFlow may not have yet
    val initialSettings: Flow<Settings> = dataStoreRepository.settings
    val settings: StateFlow<Settings> = dataStoreRepository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = Settings.DEFAULT,
        )
    fun setSettings(transform: (Settings) -> Settings): SettingsResult {
        val candidate = runCatching { transform(settings.value) }
            .getOrElse { e ->
                return when (e) {
                    is InvalidSettingsException -> formSettingsFailure(e)
                    else -> SettingsResult.Failure(R.string.settings_generic_error)
                }
            }
        viewModelScope.launch { dataStoreRepository.setSettings(candidate) }
        return SettingsResult.Success
    }

    private fun formSettingsFailure(e: InvalidSettingsException): SettingsResult {
        return SettingsResult.Failure(
            when (e) {
                is InvalidSettingsException.WidthOutOfRange -> R.string.settings_width_out_of_range_error
                is InvalidSettingsException.HeightOutOfRange -> R.string.settings_height_out_of_range_error
                is InvalidSettingsException.KittenOutOfRange -> R.string.settings_kitten_out_of_range_error
                is InvalidSettingsException.CatOutOfRange -> R.string.settings_cat_out_of_range_error
                is InvalidSettingsException.NoStartingPieces -> R.string.settings_no_starting_pieces_error
                is InvalidSettingsException.NotEnoughCells -> R.string.settings_not_enough_cells_error
            },
            e.formatArgs
        )
    }

    fun setWidth(width: Int): SettingsResult = setSettings { it.copy(width = width) }
    fun setHeight(height: Int): SettingsResult = setSettings { it.copy(height = height) }
    fun setKittenStart(kittenStart: Int): SettingsResult = setSettings { it.copy(kittenStart = kittenStart) }
    fun setCatStart(catStart: Int): SettingsResult = setSettings { it.copy(catStart = catStart) }
    fun setFirstPlayerType(playerType: PlayerType): SettingsResult = setSettings { it.copy(firstPlayerType = playerType) }
    fun setSecondPlayerType(playerType: PlayerType): SettingsResult = setSettings { it.copy(secondPlayerType = playerType) }
    fun restoreDefaults() {
        setSettings { Settings.DEFAULT }
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
                    CustomLog.d("viewModel", "Exception: ${e.message}")
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