package com.alpha900i.a9kblanketbattle.ui

import com.alpha900i.a9kblanketbattle.domain.MoveType
import com.alpha900i.a9kblanketbattle.domain.PlayerType
import kotlinx.coroutines.flow.Flow

/**
 * Callbacks for startScreen
 * [startGame] and [openSettings] open respective screens
 * [exitGame] closes app
 */
interface StartScreenActions {
    fun startGame()
    fun openSettings()
    fun exitGame()
}

/**
 * Callbacks for main game screen
 * [selectMoveType] changes type of next move player attempts - this modifies some UI-highlights and defines next possible actions
 * [setTempMessage] passes temp (non-permanent) message to info section
 */
interface GameScreenActions {
    fun selectMoveType(moveType: MoveType)
    fun setTempMessage(playerIndex: Int, tempMessageType: TempMessageType)
}


/**
 * Callbacks for "settings" screen
 * each existing setting should have getter and setter
 * [restoreDefaults] sets all settings to their default values
 */
interface SettingsActions {
    fun getWidth(): Flow<Int>
    fun getHeight(): Flow<Int>
    fun getKittenStart(): Flow<Int>
    fun getCatStart(): Flow<Int>
    fun getFirstPlayerType(): Flow<PlayerType>
    fun getSecondPlayerType(): Flow<PlayerType>

    fun setWidth(width: Int)
    fun setHeight(height: Int)
    fun setKittenStart(kittenStart: Int)
    fun setCatStart(catStart: Int)
    fun setFirstPlayerType(playerType: PlayerType)
    fun setSecondPlayerType(playerType: PlayerType)

    fun restoreDefaults()
}