package com.alpha900i.a9kblanketbattle.data


/**
 * Specific game phase with some data unique only for it
 * [NotStarted] - game not even started yet
 * [InProgress] - game started, normal move expected
 * [AwaitingTripletSelection] - triplet removal action is expected; [triplets] are possible targets for this removal
 * [GameOver] - game is over, player #[winnerIndex] has won
 */
sealed interface GamePhase {
    data object NotStarted : GamePhase
    data object InProgress : GamePhase
    data class AwaitingTripletSelection(val triplets: Set<TripletOnBoard>) : GamePhase
    data class GameOver(val winnerIndex: Int) : GamePhase
}

val GamePhase.deletableTripletsOrEmpty: Set<TripletOnBoard>
    get() = (this as? GamePhase.AwaitingTripletSelection)?.triplets ?: emptySet()
val GamePhase.isPlayable: Boolean
    get() = (this is GamePhase.InProgress || this is GamePhase.AwaitingTripletSelection)

/**
 * Current state of game
 * [oldBoard] - board that was before last turn
 * [board] - current board, that we obtained after last turn
 * [pendingEffects] - series of piece movements that led from [oldBoard] to [board]
 * [hands] - 2-item list, containing player's hands
 * [activePlayerIndex] - number of player whose turn currently is happening
 * [maxPieceAmount] - full amount of pieces one player can have on board; used for one of game over conditions
 * [moveCount] - amount of moves done so far. One move - one action of one player
 * [gamePhase] - current phase of game and data, that additional describes it
 */
data class GameState(
    val oldBoard: Board,
    val board: Board,
    val pendingEffects: List<VisualEffect>,
    val hands: List<Hand>,
    val activePlayerIndex: Int,
    val maxPieceAmount: Int,
    val moveCount: Int,
    val gamePhase: GamePhase
) {
    /**
     * Game is in progress if it
     * - was started
     * - at least one move was done
     * Uses to decide, whether game screen can be left without confirmation
     */
    fun gameInProgress(): Boolean {
        return gamePhase is GamePhase.InProgress && moveCount > 0
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
                maxPieceAmount = kittenMax + catMax,
                moveCount = 0,
                gamePhase = if (gameIsActive) GamePhase.InProgress else GamePhase.NotStarted
            )
        }
    }
}
