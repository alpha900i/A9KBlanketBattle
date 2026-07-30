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
