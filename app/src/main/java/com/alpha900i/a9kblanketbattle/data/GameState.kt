package com.alpha900i.a9kblanketbattle.data


/**
 * Current state of game
 * [oldBoard] - board that was before last turn
 * [board] - current board, that we obtained after last turn
 * [pendingEffects] - series of piece movements that led from [oldBoard] to [board]
 * [hands] - 2-item list, containing player's hands
 * [activePlayerIndex] - number of player whose turn currently is happening
 * [gameIsActive] - flag that shows, whether game is started and not finished yet
 * [winnerIndex] - number of player that already won game (if game is over)
 * [deletableTriplets] - list of triplets that, after last turn, appeared on a board and can be deleted. Player would need to select one of them for deletion
 * [maxPieceAmount] - full amount of pieces one player can have on board; used for one of game over conditions
 * [moveCount] - amount of moves done so far. One move - one action of one player
 */
data class GameState(
    val oldBoard: Board,
    val board: Board,
    val pendingEffects: List<VisualEffect>,
    val hands: List<Hand>,
    val activePlayerIndex: Int,
    val gameIsActive: Boolean,
    val winnerIndex: Int,
    val deletableTriplets: Set<TripletOnBoard>,
    val maxPieceAmount: Int,
    val moveCount: Int
) {
    /**
     * Game is in progress if it
     * - was started
     * - at least one move was done
     * Uses to decide, whether game screen can be left without confirmation
     */
    fun gameInProgress(): Boolean {
        return gameIsActive && moveCount > 0
    }

    companion object {
        /**
         * Public factory method - takes game settings and flag of "activeness"
         * Sets "max values" to "start values", calls private factory
         */
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

        /**
         * Private factory method
         * Actually forms object
         * - both [oldBoard] and [board] created empty with given size
         * - no [pendingEffects] at start
         * - starting [hands] according to current/max amounts of pieces
         * - [activePlayerIndex] is 0 - first player makes first move
         * - [gameIsActive] takes value from outside
         * - [winnerIndex] is -1, there is no winners yet
         * - no [deletableTriplets] yet
         * - [maxPieceAmount] defined by sum max amount of pieces at start - while those values can change, sum will not
         */
        private fun startingState(width: Int,
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
                deletableTriplets = setOf(),
                maxPieceAmount = kittenMax + catMax,
                moveCount = 0
            )
        }
    }
}
