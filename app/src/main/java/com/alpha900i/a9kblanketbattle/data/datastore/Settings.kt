package com.alpha900i.a9kblanketbattle.data.datastore

import com.alpha900i.a9kblanketbattle.domain.PlayerType
import com.alpha900i.a9kblanketbattle.ui.Constants

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
        require(width in Constants.MIN_WIDTH..Constants.MAX_WIDTH)   {
            "Width must be between ${Constants.MIN_WIDTH} and ${Constants.MAX_WIDTH}, got $width"
        }
        require(height in Constants.MIN_HEIGHT..Constants.MAX_HEIGHT)  {
            "Height must be between ${Constants.MIN_HEIGHT} and ${Constants.MAX_HEIGHT}, got $height"
        }
        require(kittenStart >= Constants.MIN_KITTEN_START) {
            "Starting kittens can't be less than ${Constants.MIN_KITTEN_START}, got $kittenStart"
        }
        require(catStart >= Constants.MIN_CAT_START)    {
            "Starting cats can't be less than ${Constants.MIN_CAT_START}, got $catStart"
        }

        //cross-field invariants
        require(kittenStart + catStart > 0) {
            "Each player must start with at least one piece"
        }
        require(2 * (kittenStart + catStart) <= width * height) {
            "Not enough cells: 2 players × ${kittenStart + catStart} pieces " +
                    "= ${2 * (kittenStart + catStart)} pieces, " +
                    "but board is ${width}×${height} = ${width * height} cells"
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