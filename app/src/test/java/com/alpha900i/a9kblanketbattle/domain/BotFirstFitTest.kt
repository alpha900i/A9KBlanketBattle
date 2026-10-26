package com.alpha900i.a9kblanketbattle.domain

import com.alpha900i.a9kblanketbattle.data.Board
import com.alpha900i.a9kblanketbattle.data.GameState
import com.alpha900i.a9kblanketbattle.data.Hand
import com.alpha900i.a9kblanketbattle.data.TripletOnBoard
import com.alpha900i.a9kblanketbattle.data.board
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

/**
 * First-fit bot logic.
 *
 * Tested through the public `makeMove`/`removeTriplet`
 * entry points (which are `suspend` and call delay(BOT_DELAY_TIME)).
 * `runTest` intercepts delay() and skips it via virtual time, so the
 * suite does not add 500ms per test.
 *
 * BotFirstFit is fully deterministic and needs no injection.
 * BotRandom takes an injected Random; we test STRUCTURAL properties
 * (which branches are reachable, which value ranges come back) rather
 * than exact picks, so the tests survive changes to kotlin.random's
 * underlying algorithm.
 *
 * The `check(...)` calls in both bots throw IllegalStateException (not AssertionError) - tested accordingly.
 */
class BotFirstFitTest {
    private fun state(
        board: Board,
        hand: Hand = Hand(kittenCurrent = 3, kittenMax = 3, catCurrent = 2, catMax = 2),
        deletableTriplets: Set<TripletOnBoard> = emptySet(),
    ): GameState = GameState(
        oldBoard = board,
        board = board,
        pendingEffects = emptyList(),
        hands = listOf(
            hand,
            Hand(kittenCurrent = 3, kittenMax = 3, catCurrent = 2, catMax = 2),
        ),
        activePlayerIndex = 0,
        gameIsActive = true,
        winnerIndex = -1,
        deletableTriplets = deletableTriplets,
        maxPieceAmount = 8,
        moveCount = 0
    )



    //=========================================================================
    //formSetMove
    //=========================================================================
    @Test
    fun `formSetMove chooses SET_CAT when cats are in hand`() = runTest {
        val bot = BotFirstFit(0)
        val gameState = state(
            board = board(
                "...",
                "...",
                "..."),
            hand = Hand(kittenCurrent = 3, kittenMax = 3, catCurrent = 2, catMax = 2),
        )
        var captured: Move? = null
        bot.makeMove(gameState) { captured = it }

        val move = assertNotNull(captured)
        assertEquals(MoveType.SET_CAT, move.moveType)
    }

    @Test
    fun `formSetMove chooses SET_KITTEN when no cats are in hand`() = runTest {
        val bot = BotFirstFit(0)
        val gameState = state(
            board = board(
                "...",
                "...",
                "..."
            ),
            hand = Hand(kittenCurrent = 3, kittenMax = 3, catCurrent = 0, catMax = 2),
        )
        var captured: Move? = null
        bot.makeMove(gameState) { captured = it }

        val move = assertNotNull(captured)
        assertEquals(MoveType.SET_KITTEN, move.moveType)
    }

    @Test
    fun `formSetMove picks the first empty cell in row-major order`() = runTest {
        //(0,1) is the first empty cell when scanning row-by-row.
        val bot = BotFirstFit(0)
        val gameState = state(
            board = board(
                "k..",
                "...",
                "...",
            ),
            hand = Hand(kittenCurrent = 3, kittenMax = 3, catCurrent = 2, catMax = 2),
        )
        var captured: Move? = null
        bot.makeMove(gameState) { captured = it }

        val move = assertNotNull(captured)
        assertEquals(0, move.row, "expected row 0")
        assertEquals(1, move.column, "expected column 1")
    }

    @Test
    fun `formSetMove throws when board has no empty cells`() = runTest {
        //1x1 board, its only cell occupied; hand still has pieces so
        //formMove routes to formSetMove, then the check fails.
        val bot = BotFirstFit(0)
        val gameState = state(
            board = board("k"),
            hand = Hand(kittenCurrent = 3, kittenMax = 3, catCurrent = 2, catMax = 2),
        )
        assertFailsWith<IllegalStateException> {
            bot.makeMove(gameState) { error("move should not be produced") }
        }
    }



    //=========================================================================
    //formReturnMove
    //=========================================================================
    @Test
    fun `formReturnMove chooses PROMOTE_KITTEN when own piece is a kitten`() = runTest {
        val bot = BotFirstFit(0)
        val gameState = state(
            board = board(
                "...",
                "...",
                ".k.",
            ),
            //empty hand → formMove routes to formReturnMove.
            hand = Hand(kittenCurrent = 0, kittenMax = 3, catCurrent = 0, catMax = 2),
        )
        var captured: Move? = null
        bot.makeMove(gameState) { captured = it }

        val move = assertNotNull(captured)
        assertEquals(MoveType.PROMOTE_KITTEN, move.moveType)
        assertEquals(2, move.row)
        assertEquals(1, move.column)
    }

    @Test
    fun `formReturnMove chooses RETURN_CAT when own piece is a cat`() = runTest {
        val bot = BotFirstFit(0)
        val gameState = state(
            board = board(
                "...",
                "..c",
                "...",
            ),
            hand = Hand(kittenCurrent = 0, kittenMax = 3, catCurrent = 0, catMax = 2),
        )
        var captured: Move? = null
        bot.makeMove(gameState) { captured = it }

        val move = assertNotNull(captured)
        assertEquals(MoveType.RETURN_CAT, move.moveType)
        assertEquals(1, move.row)
        assertEquals(2, move.column)
    }

    @Test
    fun `formReturnMove picks the first own piece in row-major order`() = runTest {
        //P0's kitten at (1,0) precedes P0's cat at (2,2).
        val bot = BotFirstFit(0)
        val gameState = state(
            board = board(
                "...",
                "k..",
                "..c",
            ),
            hand = Hand(kittenCurrent = 0, kittenMax = 3, catCurrent = 0, catMax = 2),
        )
        var captured: Move? = null
        bot.makeMove(gameState) { captured = it }

        val move = assertNotNull(captured)
        assertEquals(1, move.row)
        assertEquals(0, move.column)
        assertEquals(MoveType.PROMOTE_KITTEN, move.moveType)
    }

    @Test
    fun `formReturnMove throws when player has no pieces on board`() = runTest {
        //Empty hand + only other player's piece on board → getPlayerCellCoordinates(0) is empty.
        val bot = BotFirstFit(0)
        val gameState = state(
            board = board("C"),
            hand = Hand(kittenCurrent = 0, kittenMax = 3, catCurrent = 0, catMax = 2),
        )
        assertFailsWith<IllegalStateException> {
            bot.makeMove(gameState) { error("move should not be produced") }
        }
    }



    //=========================================================================
    //Triplet removal
    //=========================================================================
    @Test
    fun `selectTripletForRemoval returns the first triplet`() = runTest {
        //single triplet in the set → returns it. Multi-triplet ordering is
        //a Board implementation detail (LinkedHashSet, row-major insertion)
        //and pinning it here would couple this test to Board.getDeletableTriplets.
        val triplet = TripletOnBoard(0, 0, 0, 1)
        val bot = BotFirstFit(0)
        val gameState = state(
            board = board("ccc", "...", "..."),
            hand = Hand(kittenCurrent = 0, kittenMax = 3, catCurrent = 0, catMax = 2),
            deletableTriplets = setOf(triplet),
        )
        var captured: TripletOnBoard? = null
        bot.removeTriplet(gameState) { captured = it }

        assertEquals(triplet, captured)
    }
}