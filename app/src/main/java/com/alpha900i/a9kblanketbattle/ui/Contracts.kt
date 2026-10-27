package com.alpha900i.a9kblanketbattle.ui

import com.alpha900i.a9kblanketbattle.domain.MoveType
import com.alpha900i.a9kblanketbattle.domain.PlayerType

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
 * each existing setting should have setter
 * [restoreDefaults] sets all settings to their default values
 */
interface SettingsActions {
    fun setWidth(width: Int): SettingsResult
    fun setHeight(height: Int): SettingsResult
    fun setKittenStart(kittenStart: Int): SettingsResult
    fun setCatStart(catStart: Int): SettingsResult
    fun setFirstPlayerType(playerType: PlayerType): SettingsResult
    fun setSecondPlayerType(playerType: PlayerType): SettingsResult

    fun restoreDefaults()
}