package com.alpha900i.a9kblanketbattle.data

data class GameState(
    val board: Board,
    val hands: List<Hand>,
    val activePlayerIndex: Int,
    val gameIsActive: Boolean
) {
    companion object {
        const val DEFAULT_WIDTH = 6
        const val DEFAULT_HEIGHT = 6
        const val DEFAULT_CAT_START = 0
        const val DEFAULT_CAT_MAX = 8
        const val DEFAULT_KITTEN_START = 8
        const val DEFAULT_KITTEN_MAX = 8

        fun startingState(): GameState {
            return startingState(
                width = DEFAULT_WIDTH,
                height = DEFAULT_HEIGHT,
                catStart = DEFAULT_CAT_START,
                catMax = DEFAULT_CAT_MAX,
                kittenStart = DEFAULT_KITTEN_START,
                kittenMax = DEFAULT_KITTEN_MAX,
            )
        }
        fun startingState(width: Int, height: Int, catStart: Int, catMax: Int, kittenStart: Int, kittenMax:Int): GameState {
            val cells = List(height) {
                List(width) {
                    Cell(CellType.EMPTY, -1)
                }
            }
            return GameState(
                Board(cells),
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
                gameIsActive = false,
            )
        }
    }
}
