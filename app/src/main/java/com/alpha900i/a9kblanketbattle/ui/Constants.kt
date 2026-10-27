package com.alpha900i.a9kblanketbattle.ui

import com.alpha900i.a9kblanketbattle.domain.PlayerType

object Constants {
    //default settings values for width, height, starting amount of cats and kittens
    const val DEFAULT_WIDTH = 6
    const val DEFAULT_HEIGHT = 6
    const val DEFAULT_CAT_START = 0
    const val DEFAULT_KITTEN_START = 8

    //minimal possible values for width, height, starting amount of cats and kittens
    const val MIN_WIDTH = 1
    const val MIN_HEIGHT = 1
    const val MIN_CAT_START = 0
    const val MIN_KITTEN_START = 0

    //maximum possible values for width, height
    //starting amount of cats and kittens is controlled by width and height indirectly - see Settings.init
    const val MAX_WIDTH = 20
    const val MAX_HEIGHT = 20

    //default type of players
    val DEFAULT_FIRST_PLAYER_TYPE = PlayerType.HUMAN
    val DEFAULT_SECOND_PLAYER_TYPE = PlayerType.BOT_FIRST_FIT

    //type for animation cycles
    const val ANIMATION_TIME = 300
    //delay before bot movement - needed to make sure that bot-vs-bot match actually has time to show events
    const val BOT_DELAY_TIME = 500L
}
