package com.alpha900i.a9kblanketbattle.data.datastore

import com.alpha900i.a9kblanketbattle.domain.PlayerType
import com.alpha900i.a9kblanketbattle.ui.Constants

sealed class InvalidSettingsException(val formatArgs: List<Any>) : IllegalArgumentException() {
    data class WidthOutOfRange(val minWidth: Int, val maxWidth: Int, val width: Int):
        InvalidSettingsException(listOf(minWidth, maxWidth, width))
    data class HeightOutOfRange(val minHeight: Int, val maxHeight: Int, val height: Int):
        InvalidSettingsException(listOf(minHeight, maxHeight, height))
    data class KittenOutOfRange(val minKittens: Int, val kitten: Int):
        InvalidSettingsException(listOf(minKittens, kitten))
    data class CatOutOfRange(val minCat: Int, val cat: Int):
        InvalidSettingsException(listOf(minCat, cat))
    class NoStartingPieces: InvalidSettingsException(listOf())
    data class NotEnoughCells(val piecePerPlayer: Int, val width: Int, val height: Int):
        InvalidSettingsException(listOf(piecePerPlayer, 2*piecePerPlayer, width, height, width*height))
}

data class Settings(
    val width: Int,
    val height: Int,
    val kittenStart: Int,
    val catStart: Int,
    val firstPlayerType: PlayerType,
    val secondPlayerType: PlayerType
) {
    init {
        //range checks
        checkRule(
            width in Constants.MIN_WIDTH..Constants.MAX_WIDTH,
            InvalidSettingsException.WidthOutOfRange(
                Constants.MIN_WIDTH,
                Constants.MAX_WIDTH,
                width
            )
        )
        checkRule(
            height in Constants.MIN_HEIGHT..Constants.MAX_HEIGHT,
            InvalidSettingsException.HeightOutOfRange(
                Constants.MIN_HEIGHT,
                Constants.MAX_HEIGHT,
                height
            )
        )
        checkRule(
            kittenStart >= Constants.MIN_KITTEN_START,
            InvalidSettingsException.KittenOutOfRange(
                Constants.MIN_KITTEN_START,
                kittenStart
            )
        )
        checkRule(
            catStart >= Constants.MIN_CAT_START,
            InvalidSettingsException.CatOutOfRange(
                Constants.MIN_CAT_START,
                catStart
            )
        )

        //cross-field invariants
        checkRule(
            kittenStart + catStart > 0,
            InvalidSettingsException.NoStartingPieces()
        )
        checkRule(
            2 * (kittenStart + catStart) <= width * height,
            InvalidSettingsException.NotEnoughCells(kittenStart + catStart, width, height)
        )
    }

    private fun checkRule(rule: Boolean, e: InvalidSettingsException) {
        if (!rule) {
            throw e
        }
    }

    companion object {
        val DEFAULT: Settings = Settings(
            width = Constants.DEFAULT_WIDTH,
            height = Constants.DEFAULT_HEIGHT,
            kittenStart = Constants.DEFAULT_KITTEN_START,
            catStart = Constants.DEFAULT_CAT_START,
            firstPlayerType = Constants.DEFAULT_FIRST_PLAYER_TYPE,
            secondPlayerType = Constants.DEFAULT_SECOND_PLAYER_TYPE
        )
    }
}