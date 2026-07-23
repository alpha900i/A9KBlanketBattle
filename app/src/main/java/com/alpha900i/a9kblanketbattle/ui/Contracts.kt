package com.alpha900i.a9kblanketbattle.ui

import kotlinx.coroutines.flow.Flow

interface StartScreenActions {
    fun startGame()
    fun openSettings()
    fun exitGame()
}

interface SettingsAction {
    fun getWidth(): Flow<Int>
    fun getHeight(): Flow<Int>
    fun getKittenStart(): Flow<Int>
    fun getCatStart(): Flow<Int>

    fun setWidth(width: Int)
    fun setHeight(height: Int)
    fun setKittenStart(kittenStart: Int)
    fun setCatStart(catStart: Int)
}