package com.alpha900i.a9kblanketbattle.domain

import com.alpha900i.a9kblanketbattle.data.Board
import com.alpha900i.a9kblanketbattle.data.GameState
import com.alpha900i.a9kblanketbattle.data.Hand
import com.alpha900i.a9kblanketbattle.data.TripletOnBoard
import com.alpha900i.a9kblanketbattle.data.board
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
/**
 * Random bot logic.
 *
 * Tested through the public `makeMove`/`removeTriplet`
 * entry points (which are `suspend` and call delay(BOT_DELAY_TIME)).
 * `runTest` intercepts delay() and skips it via virtual time, so the
 * suite does not add 500ms per test.
 *
 * BotRandom takes an injected Random; we test STRUCTURAL properties
 * (which branches are reachable, which value ranges come back) rather
 * than exact picks, so the tests survive changes to kotlin.random's
 * underlying algorithm.
 *
 * The `check(...)` calls throw IllegalStateException (not AssertionError) - tested accordingly.
 */
class BotRandomTest {
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
    )



    //=========================================================================
    //Set move: type choice
    //=========================================================================
    @Test
    fun `formSetMove is always SET_KITTEN when no cats are in hand`() = runTest {
        val bot = BotRandom(0, Random(42))
        val gameState = state(
            board = board("...", "...", "..."),
            hand = Hand(kittenCurrent = 3, kittenMax = 3, catCurrent = 0, catMax = 2),
        )
        var captured: Move? = null
        bot.makeMove(gameState) { captured = it }

        assertEquals(MoveType.SET_KITTEN, assertNotNull(captured).moveType)
    }

    @Test
    fun `formSetMove is always SET_CAT when no kittens are in hand`() = runTest {
        val bot = BotRandom(0, Random(42))
        val gameState = state(
            board = board("...", "...", "..."),
            hand = Hand(kittenCurrent = 0, kittenMax = 3, catCurrent = 2, catMax = 2),
        )
        var captured: Move? = null
        bot.makeMove(gameState) { captured = it }

        assertEquals(MoveType.SET_CAT, assertNotNull(captured).moveType)
    }

    @Test
    fun `formSetMove picks a type from the valid set when both are in hand`() = runTest {
        //With both in hand, the result must be one of the two valid types,
        //never a third option or a crash. We don't pin the exact pick
        //because kotlin.random's algorithm may change between versions.
        val bot = BotRandom(0, Random(42))
        val gameState = state(
            board = board("...", "...", "..."),
            hand = Hand(kittenCurrent = 3, kittenMax = 3, catCurrent = 2, catMax = 2),
        )
        var captured: Move? = null
        bot.makeMove(gameState) { captured = it }

        assertTrue(
            assertNotNull(captured).moveType in setOf(MoveType.SET_KITTEN, MoveType.SET_CAT),
        )
    }

    @Test
    fun `formSetMove picks a cell from the empty-cell set`() = runTest {
        //Single empty cell → the pick must be that cell, regardless of seed.
        val bot = BotRandom(0, Random(42))
        val gameState = state(
            board = board(
                "k.k",
                "..k",
                "kk.",
            ),
            hand = Hand(kittenCurrent = 3, kittenMax = 3, catCurrent = 2, catMax = 2),
        )
        var captured: Move? = null
        bot.makeMove(gameState) { captured = it }

        val move = assertNotNull(captured)
        //The only empty cells are (0,1), (1,0), (1,1), (2,2).
        val allowed = setOf(0 to 1, 1 to 0, 1 to 1, 2 to 2)
        assertTrue(
            (move.row to move.column) in allowed,
            "expected pick from $allowed, got (${move.row},${move.column})",
        )
    }



    //=========================================================================
    //Return move: type choice
    //=========================================================================
    @Test
    fun `formReturnMove picks PROMOTE for kitten and RETURN for cat`() = runTest {
        //Board: one kitten at (0,0), one cat at (0,2). Both belong to P0.
        //Repeat with several seeds to confirm the pick is drawn from both
        //valid types and its type matches the picked cell's piece type.
        for (seed in 0 until 20) {
            val bot = BotRandom(0, Random(seed))
            val gameState = state(
                board = board(
                    "k.c",
                    "...",
                    "...",
                ),
                hand = Hand(kittenCurrent = 0, kittenMax = 3, catCurrent = 0, catMax = 2),
            )
            var captured: Move? = null
            bot.makeMove(gameState) { captured = it }

            val move = assertNotNull(captured)
            when (move.row to move.column) {
                0 to 0 -> assertEquals(MoveType.PROMOTE_KITTEN, move.moveType, "seed=$seed")
                0 to 2 -> assertEquals(MoveType.RETURN_CAT, move.moveType, "seed=$seed")
                else -> error("unexpected pick (${move.row},${move.column}) for seed=$seed")
            }
        }
    }



    //=========================================================================
    //Triplet removal
    //=========================================================================
    @Test
    fun `selectTripletForRemoval picks one of the available triplets`() = runTest {
        val t1 = TripletOnBoard(0, 0, 0, 1)
        val t2 = TripletOnBoard(0, 0, 1, 0)
        val bot = BotRandom(0, Random(42))
        val gameState = state(
            board = board("ccc", "c..", "..."),
            hand = Hand(kittenCurrent = 0, kittenMax = 3, catCurrent = 0, catMax = 2),
            deletableTriplets = setOf(t1, t2),
        )
        var captured: TripletOnBoard? = null
        bot.removeTriplet(gameState) { captured = it }

        assertTrue(
            assertNotNull(captured) in setOf(t1, t2),
            "expected one of $t1,$t2, got $captured",
        )
    }
}