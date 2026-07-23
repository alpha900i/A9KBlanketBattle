package com.alpha900i.a9kblanketbattle.data

data class GameState(
    val oldBoard: Board,
    val board: Board,
    val pendingEffects: List<VisualEffect>,
    val hands: List<Hand>,
    val activePlayerIndex: Int,
    val gameIsActive: Boolean,
    val winnerIndex: Int,
    val deletableTriplets: Set<TripletOnBoard>
) {
    companion object {
        const val DEFAULT_WIDTH = 6
        const val DEFAULT_HEIGHT = 6
        const val DEFAULT_CAT_START = 0
        const val DEFAULT_CAT_MAX = DEFAULT_CAT_START
        const val DEFAULT_KITTEN_START = 8
        const val DEFAULT_KITTEN_MAX = DEFAULT_KITTEN_START

        fun startingState(
            width: Int,
            height: Int,
            kittenStart: Int,
            catStart: Int,
            gameIsActive: Boolean
        ): GameState {
            return startingState(
                width = width,
                height = height,
                catStart = catStart,
                catMax = catStart,
                kittenStart = kittenStart,
                kittenMax = kittenStart,
                gameIsActive = gameIsActive
            )
        }
//        fun startingState(gameIsActive: Boolean): GameState {
//            return startingState(
//                width = DEFAULT_WIDTH,
//                height = DEFAULT_HEIGHT,
//                catStart = DEFAULT_CAT_START,
//                catMax = DEFAULT_CAT_MAX,
//                kittenStart = DEFAULT_KITTEN_START,
//                kittenMax = DEFAULT_KITTEN_MAX,
//                gameIsActive = gameIsActive
//            )
//        }
        fun startingState(width: Int,
                          height: Int,
                          catStart: Int,
                          catMax: Int,
                          kittenStart: Int,
                          kittenMax:Int,
                          gameIsActive: Boolean
        ): GameState {
            return GameState(
                oldBoard = Board.emptyBoard(width = width, height = height),
                board = Board.emptyBoard(width = width, height = height),
                pendingEffects = listOf(),
                hands = listOf(
                    Hand(
                        kittenCurrent = kittenStart,
                        kittenMax = kittenMax,
                        catCurrent = catStart,
                        catMax = catMax,
                    ),
                    Hand(
                        kittenCurrent = kittenStart,
                        kittenMax = kittenMax,
                        catCurrent = catStart,
                        catMax = catMax,
                    )
                ),
                activePlayerIndex = 0,
                gameIsActive = gameIsActive,
                winnerIndex = -1,
                deletableTriplets = setOf()
            )
        }
    }
}
