package com.alpha900i.a9kblanketbattle.data

import com.alpha900i.a9kblanketbattle.domain.Game
import com.alpha900i.a9kblanketbattle.domain.HandChange
import com.alpha900i.a9kblanketbattle.domain.Move
import com.alpha900i.a9kblanketbattle.domain.MoveType
import com.alpha900i.a9kblanketbattle.domain.Player
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class GameMoveCountTest {
    private class FixedPlayer(
        override val index: Int,
        private val move: Move,
        private val triplet: TripletOnBoard? = null,
    ) : Player {
        override suspend fun makeMove(gameState: GameState, applier: (Move) -> Unit) = applier(move)
        override suspend fun removeTriplet(gameState: GameState, applier: (TripletOnBoard) -> Unit) =
            applier(checkNotNull(triplet) { "FixedPlayer was not configured with a triplet" })
    }

    @Test
    fun `startingState has moveCount zero`() {
        assertEquals(0, GameState.startingState(3, 3, 5, 2, gameIsActive = true).moveCount)
    }

    @Test
    fun `normal move increments moveCount`() = runBlocking {
        val before = GameState.startingState(3, 3, 5, 2, gameIsActive = true)
        val player = FixedPlayer(0, Move(0, 0, MoveType.SET_KITTEN))
        var after: GameState? = null
        Game().makeMove(listOf(player, player), before) { after = it }
        assertEquals(1, after!!.moveCount)
    }

    @Test
    fun `triplet removal increments moveCount`() = runBlocking {
        val kitten0 = Cell(CellType.KITTEN, owner = 0)
        val empty = Cell.emptyCell()
        val boardWithTriplet = Board(
            listOf(
                listOf(kitten0, kitten0, kitten0),
                listOf(empty, empty, empty),
                listOf(empty, empty, empty),
            )
        )

        // Horizontal triplet starting at (0, 0), moving right
        val triplet = TripletOnBoard(row = 0, column = 0, rowShift = 0, columnShift = 1)

        val base = GameState.startingState(3, 3, kittenStart = 3, catStart = 0, gameIsActive = true)
        val state = base.copy(
            board = boardWithTriplet,
            hands = listOf(
                // P0 has all 3 kittens on the board, 0 in hand
                base.hands[0].applyChange(HandChange(-3, 0, 0, 0)),
                base.hands[1],
            ),
            deletableTriplets = setOf(triplet),
        )

        // Sanity: the state must be in the "removal required" branch.
        // If HandChange's argument order differs from what's assumed here,
        // these fail before the assertion of interest and point you at the cause.
        assertEquals(1, state.deletableTriplets.size)
        assertEquals(0, state.hands[0].kittenCurrent)
        assertEquals(3, state.hands[0].kittenMax)
        assertEquals(0, state.moveCount)

        val player0 = FixedPlayer(0, move = Move(0, 0, MoveType.SET_KITTEN), triplet = triplet)
        val player1 = FixedPlayer(1, move = Move(0, 0, MoveType.SET_KITTEN))  // unused

        var after: GameState? = null
        Game().makeMove(listOf(player0, player1), state) { after = it }

        val result = after!!

        // The assertion this test exists for:
        assertEquals(1, result.moveCount)

        // Sanity that we actually went through applyRemoval's path,
        // not the applyMove one — otherwise the increment could be
        // in the wrong place and this test wouldn't notice.
        assertEquals(0, result.deletableTriplets.size)
        assertEquals(1, result.activePlayerIndex)  // turn passed to P1
    }

    @Test
    fun `promote-only move increments moveCount`() = runBlocking {
        val empty = Cell.emptyCell()
        val kitten0 = Cell(CellType.KITTEN, owner = 0)
        val boardWithKitten = Board(
            listOf(
                listOf(kitten0, empty, empty),
                listOf(empty, empty, empty),
                listOf(empty, empty, empty),
            )
        )

        // Hand starts with 1 kitten. Emptying it means "all pieces are on the board."
        val base = GameState.startingState(3, 3, kittenStart = 1, catStart = 0, gameIsActive = true)
        val state = base.copy(
            board = boardWithKitten,
            hands = listOf(
                base.hands[0].applyChange(HandChange(-1, 0, 0, 0)),
                base.hands[1].applyChange(HandChange(-1, 0, 0, 0)),
            ),
        )

        // Sanity: the setup must actually be "empty hand, one piece on board."
        assertEquals(0, state.hands[0].kittenCurrent)
        assertEquals(1, state.hands[0].kittenMax)
        assertEquals(0, state.moveCount)

        val player0 = FixedPlayer(0, Move(0, 0, MoveType.PROMOTE_KITTEN))
        val player1 = FixedPlayer(1, Move(0, 0, MoveType.SET_KITTEN))  // unused

        var after: GameState? = null
        Game().makeMove(listOf(player0, player1), state) { after = it }

        assertEquals(1, after!!.moveCount)
    }

    @Test
    fun `boop-return-to-hand increments moveCount`() = runBlocking {
        val empty = Cell.emptyCell()
        val kitten1 = Cell(CellType.KITTEN, owner = 1)
        val boardWithKitten = Board(
            listOf(
                listOf(kitten1, empty, empty),
                listOf(empty, empty, empty),
                listOf(empty, empty, empty),
            )
        )

        val base = GameState.startingState(3, 3, kittenStart = 1, catStart = 0, gameIsActive = true)
        val state = base.copy(
            board = boardWithKitten,
            hands = listOf(
                base.hands[0],                                       // player 0: 1 kitten in hand
                base.hands[1].applyChange(HandChange(-1, 0, 0, 0)),  // player 1: 0 in hand, piece on board
            ),
        )

        // Player 0 places a kitten at (0, 1). The neighbour at (0, 0) is booped
        // toward (0, -1), which is off-board → returns to player 1's hand.
        val player0 = FixedPlayer(0, Move(0, 1, MoveType.SET_KITTEN))
        val player1 = FixedPlayer(1, Move(0, 0, MoveType.SET_KITTEN))  // unused

        var after: GameState? = null
        Game().makeMove(listOf(player0, player1), state) { after = it }

        val result = after!!

        assertEquals(1, result.moveCount)

        // The whole point: verify the boop-return actually happened.
        assertEquals(CellType.EMPTY, result.board.getCellType(0, 0))
        assertEquals(CellType.KITTEN, result.board.getCellType(0, 1))
        assertEquals(1, result.hands[1].kittenCurrent)  // returned to hand
        assertEquals(0, result.hands[0].kittenCurrent)  // placed one
    }

    @Test
    fun `two sequential moves increment moveCount twice`() = runBlocking {
        val state0 = GameState.startingState(3, 3, kittenStart = 5, catStart = 0, gameIsActive = true)
        assertEquals(0, state0.moveCount)

        val players = listOf(
            FixedPlayer(0, Move(0, 0, MoveType.SET_KITTEN)),
            FixedPlayer(1, Move(0, 1, MoveType.SET_KITTEN)),
        )

        var state1: GameState? = null
        Game().makeMove(players, state0) { state1 = it }
        assertEquals(1, state1!!.moveCount)

        var state2: GameState? = null
        Game().makeMove(players, state1) { state2 = it }
        assertEquals(2, state2!!.moveCount)
    }
}