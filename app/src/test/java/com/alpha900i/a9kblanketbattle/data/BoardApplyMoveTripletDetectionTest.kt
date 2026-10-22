package com.alpha900i.a9kblanketbattle.data

import com.alpha900i.a9kblanketbattle.domain.Move
import com.alpha900i.a9kblanketbattle.domain.MoveType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Triplet detection, observed through Board.applyMove's `deletableTriplets` output.
 *
 * IMPORTANT — the auto-removal rule:
 *   In Board.applyMove, if exactly one triplet is detected and the game
 *   is not over, that triplet is removed inline and `deletableTriplets`
 *   comes back empty. So any test that wants to OBSERVE a triplet in
 *   `deletableTriplets` must create >= 2 triplets with the single move.
 *
 *   Two shapes are used below to satisfy this:
 *     "+" (centre + N/S/E/W pieces)  -> horizontal + vertical triplet
 *     "X" (centre + 4 corner pieces) -> both diagonal triplets
 *
 *   Tests that assert the ABSENCE of a triplet don't need this, because
 *   zero triplets means the auto-removal branch can't fire.
 *
 *   Pre-placed pieces are deliberately the wrong type for the move
 *   (CATs for a SET_KITTEN move, kittens for SET_CAT), so `isPushable`
 *   is false for all of them and no boop fires. That keeps the triplet
 *   shape intact through the move.
 *
 * The single-triplet-with-auto-removal case is different test territory and
 * is not covered here.
 */
class BoardApplyMoveTripletDetectionTest {

    private val maxPieceAmount = 8

    private fun triplet(
        row: Int,
        column: Int,
        rowShift: Int,
        columnShift: Int,
    ): TripletOnBoard = TripletOnBoard(row, column, rowShift, columnShift)



    //=========================================================================
    //Positive detection: all four directions
    //=========================================================================
    @Test
    fun `plus shape detects both horizontal and vertical triplets through the placed piece`() {
        //"+" shape: move at (2,2); P0 cats at N/S/E/W, placed kitten at centre.
        //Surrounding pieces are CATs, so SET_KITTEN cannot push them.
        //
        //Expected triplets (both belong to P0, both mixed type):
        //   horizontal: (2,1) -> (2,2) -> (2,3)   direction (0, 1)
        //   vertical:   (1,2) -> (2,2) -> (3,2)   direction (1, 0)
        //
        //Two triplets => auto-removal is suppressed, both observable.
        val result = board(
            ".....",
            "..c..",
            ".c.c.",
            "..c..",
            ".....",
        ).applyMove(Move(2, 2, MoveType.SET_KITTEN), playerIndex = 0, maxPieceAmount = maxPieceAmount)

        val expected = setOf(
            triplet(row = 2, column = 1, rowShift = 0, columnShift = 1),
            triplet(row = 1, column = 2, rowShift = 1, columnShift = 0),
        )
        assertEquals(expected, result.deletableTriplets)

        //Mixed type (kitten + cat + cat) counts as a deletable triplet but
        //is not a winning cat-triplet, so no game over.
        assertFalse(result.gameOver, "mixed triplet must not end the game")
        assertEquals(-1, result.winnerIndex)
    }

    @Test
    fun `x shape detects both diagonal triplets through the placed piece`() {
        //"X" shape: move at (2,2); P0 cats in the four corners, placed kitten
        //at centre. Surrounding pieces are CATs, so SET_KITTEN cannot push them.
        //
        //Expected triplets (both belong to P0, both mixed type):
        //   down-right: (1,1) -> (2,2) -> (3,3)   direction (1,  1)
        //   down-left:  (1,3) -> (2,2) -> (3,1)   direction (1, -1)
        val result = board(
            ".....",
            ".c.c.",
            ".....",
            ".c.c.",
            ".....",
        ).applyMove(Move(2, 2, MoveType.SET_KITTEN), playerIndex = 0, maxPieceAmount = maxPieceAmount)

        val expected = setOf(
            triplet(row = 1, column = 1, rowShift = 1, columnShift = 1),
            triplet(row = 1, column = 3, rowShift = 1, columnShift = -1),
        )
        assertEquals(expected, result.deletableTriplets)

        assertFalse(result.gameOver)
        assertEquals(-1, result.winnerIndex)
    }



    //=========================================================================
    //Negative cases: things that look like a triplet but are not
    //=========================================================================
    @Test
    fun `foreign piece in the middle of a would-be triplet prevents detection`() {
        //(0,0) and (0,2) are P0 kittens; (0,1) is P1's kitten.
        //For P0 this is only two P0 cells with a foreign cell in between.
        //The move at (3,3) is far from row 0 and adjacent cells are empty,
        //so no boop fires and the row-0 shape survives.
        val result = board(
            "kKk.",
            "....",
            "....",
            "....",
        ).applyMove(Move(3, 3, MoveType.SET_KITTEN), playerIndex = 0, maxPieceAmount = maxPieceAmount)

        assertTrue(
            result.deletableTriplets.isEmpty(),
            "foreign middle piece must break the triplet, got ${result.deletableTriplets}",
        )
        assertFalse(result.gameOver)
    }

    @Test
    fun `empty cell in the middle of a would-be triplet prevents detection`() {
        //(0,0) and (0,2) are P0 kittens, (0,1) is empty.
        val result = board(
            "k.k.",
            "....",
            "....",
            "....",
        ).applyMove(Move(3, 3, MoveType.SET_KITTEN), playerIndex = 0, maxPieceAmount = maxPieceAmount)

        assertTrue(
            result.deletableTriplets.isEmpty(),
            "empty middle cell must break the triplet, got ${result.deletableTriplets}",
        )
    }

    @Test
    fun `piece at board edge is not falsely detected as a triplet via an off-edge direction`() {
        //3x3 board with P0 kittens at (0,2) and (2,0); SET_KITTEN at (0,0).
        //
        //Multiple isTriplet calls from these edge pieces have a furthest
        //cell past the board:
        //   - (0,2) dir (0,1)  -> furthest col = 4, past the right edge
        //   - (0,2) dir (1,1)  -> furthest col = 4, past the right edge
        //   - (2,0) dir (1,0)  -> furthest row = 4, past the bottom edge
        //   - (2,0) dir (1,1)  -> furthest row = 4, past the bottom edge
        //   - (2,0) dir (1,-1) -> furthest row = 4, past the bottom edge
        //
        //The guard in isTriplet must return false for all of these before
        //indexing past the array bounds. If it doesn't, this test throws
        //IndexOutOfBoundsException instead of reaching the assertion.
        //
        //The move cell (0,0) is not adjacent to (0,2) or (2,0), so no boop
        //fires and no other triplet can form.
        val result = board(
            "..k",
            "...",
            "k..",
        ).applyMove(Move(0, 0, MoveType.SET_KITTEN), playerIndex = 0, maxPieceAmount = maxPieceAmount)

        assertTrue(result.deletableTriplets.isEmpty())
    }



    //=========================================================================
    //Triplet ownership vs game-over interaction
    //=========================================================================
    @Test
    fun `cat triplet for the acting player ends the game`() {
        //P0 has two P0 cats in row 2; SET_CAT at (2,3) completes the triplet.
        //The cat at (2,2) is pushable by SET_CAT, but its receiver (2,1) is
        //also a cat, so the boop is blocked and the shape survives.
        val result = board(
            "....",
            "....",
            ".cc.",
            "....",
        ).applyMove(Move(2, 3, MoveType.SET_CAT), playerIndex = 0, maxPieceAmount = maxPieceAmount)

        assertTrue(result.gameOver, "cat triplet must end the game")
        assertEquals(0, result.winnerIndex)

        //Game-over suppresses auto-removal, so the winning triplet is still
        //reported in Board's MoveResult. Game.applyMove is responsible for
        //dropping it when it constructs the final GameState; that is
        //test in different place.
        assertEquals(
            setOf(triplet(row = 2, column = 1, rowShift = 0, columnShift = 1)),
            result.deletableTriplets,
        )

        //Board still contains the three cats, all P0.
        assertEquals(CellType.CAT, result.board.getCellType(2, 1))
        assertEquals(CellType.CAT, result.board.getCellType(2, 2))
        assertEquals(CellType.CAT, result.board.getCellType(2, 3))
        assertEquals(0, result.board.cells[2][1].owner)
        assertEquals(0, result.board.cells[2][2].owner)
        assertEquals(0, result.board.cells[2][3].owner)
    }

    @Test
    fun `cat triplet for the other player does not end the acting player's game`() {
        //P1 owns a full cat triplet in row 0; P0 makes an unrelated move.
        //
        //checkForGameOver only inspects cells owned by the *acting* player,
        //so P1's triplet cannot end the game. getDeletableTriplets likewise
        //filters by acting player, so P1's triplet is not reported for
        //P0's removal.
        val result = board(
            "CCC.",
            "....",
            "....",
            "....",
        ).applyMove(Move(3, 3, MoveType.SET_KITTEN), playerIndex = 0, maxPieceAmount = maxPieceAmount)

        assertFalse(result.gameOver, "other player's cat triplet must not end the game")
        assertEquals(-1, result.winnerIndex)
        assertTrue(
            result.deletableTriplets.isEmpty(),
            "other player's triplet must not appear in deletableTriplets, got ${result.deletableTriplets}",
        )

        //P1's triplet remains intact on the board.
        assertEquals(CellType.CAT, result.board.getCellType(0, 0))
        assertEquals(CellType.CAT, result.board.getCellType(0, 1))
        assertEquals(CellType.CAT, result.board.getCellType(0, 2))
        assertEquals(1, result.board.cells[0][0].owner)
        assertEquals(1, result.board.cells[0][1].owner)
        assertEquals(1, result.board.cells[0][2].owner)
    }
}