package com.alpha900i.a9kblanketbattle.data

import com.alpha900i.a9kblanketbattle.domain.Move
import com.alpha900i.a9kblanketbattle.domain.MoveType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * Regression tests for the setPieceAndBoop bounds-check swap:
 *
 *   if (checkX in 0 until mutableCells[0].size &&   //BUG: width, should be height
 *       checkY in 0 until mutableCells.size) {      //BUG: height, should be width
 *
 * The swap is invisible on square boards. On rectangular boards it can
 *   - throw IndexOutOfBoundsException when the buggy check accepts an
 *     out-of-bounds row/column, or
 *   - silently skip a valid boop when the buggy check rejects an
 *     in-bounds row/column.
 *
 * Every test below is written to catch exactly one of those two cases.
 * If any of these fail with IndexOutOfBoundsException, the bug is not yet
 * fixed. If a test fails with a board-state assertion, the bug may have
 * been "fixed" by swapping only one of the two checks.
 */
class BoardApplyMoveBoopNonSquareTest {

    private val maxPieceAmount = 8

    private fun MoveResult.movePieces(): List<VisualEffect.MovePiece> =
        pendingEffects.filterIsInstance<VisualEffect.MovePiece>()



    //-------------------------------------------------------------------------
    //WIDE board (height < width)
    //-------------------------------------------------------------------------
    @Test
    fun `wide board - bottom-row move does not crash when checking the row below the board`() {
        //3 rows x 5 cols. Move at (2, 2), which is the bottom row.
        //Direction (1, 0) computes checkX = 3, which is off the bottom edge.
        //   - Buggy: 3 in 0 until 5 (width) is true -> indexes mutableCells[3] -> crash.
        //   - Fixed: 3 in 0 until 3 (height) is false -> direction skipped.
        //
        //A kitten at (1, 2) verifies the fixed code still fires the valid 'up' boop.
        val result = board(
            ".....",
            "..K..",
            ".....",
        ).applyMove(Move(2, 2, MoveType.SET_KITTEN), playerIndex = 0, maxPieceAmount = maxPieceAmount)

        //No crash -> piece placed.
        assertEquals(CellType.KITTEN, result.board.getCellType(2, 2))
        assertEquals(0, result.board.cells[2][2].owner)

        //Upward boop (direction (-1,0)) still works: (1,2) -> (0,2).
        val movePiece = result.movePieces().singleOrNull { it.fromRow == 1 && it.fromColumn == 2 }
            ?: fail("expected upward boop from (1,2), got ${result.movePieces()}")
        assertEquals(0, movePiece.toRow)
        assertEquals(2, movePiece.toColumn)
    }

    @Test
    fun `wide board - rightward boop not silently dropped when checked column equals height`() {
        //3 rows x 5 cols (height 3, width 5). Move at (1, 2), kitten at (1, 3).
        //The adjacent-cell column is 3, so checkY = 3 at the bounds check.
        //   - Buggy:  3 in 0 until 3 (height) is false -> boop silently skipped.
        //   - Fixed:  3 in 0 until 5 (width)  is true  -> boop fires to (1, 4).
        //
        //Column 3 is a *valid* column; only the wrongly-used `height` bound rejects it.
        val result = board(
            ".....",
            "...K.",
            ".....",
        ).applyMove(Move(1, 2, MoveType.SET_KITTEN), playerIndex = 0, maxPieceAmount = maxPieceAmount)

        val movePiece = result.movePieces().singleOrNull { it.fromRow == 1 && it.fromColumn == 3 }
            ?: fail("expected rightward boop from (1,3), got ${result.movePieces()}")
        assertEquals(1, movePiece.toRow)
        assertEquals(4, movePiece.toColumn)

        assertEquals(CellType.EMPTY, result.board.getCellType(1, 3))
        assertEquals(CellType.KITTEN, result.board.getCellType(1, 4))
    }



    //-------------------------------------------------------------------------
    //TALL board (height > width)
    //-------------------------------------------------------------------------
    @Test
    fun `tall board - rightmost-column move does not crash when checking the column past the board`() {
        //5 rows x 3 cols. Move at (2, 2), which is the rightmost column.
        //Direction (0, 1) computes checkY = 3, which is off the right edge.
        //   - Buggy: 3 in 0 until 5 (height) is true -> indexes mutableCells[2][3] -> crash.
        //   - Fixed: 3 in 0 until 3 (width) is false -> direction skipped.
        //
        //A kitten at (2, 1) verifies the valid 'left' boop still fires.
        val result = board(
            "...",
            "...",
            ".K.",
            "...",
            "...",
        ).applyMove(Move(2, 2, MoveType.SET_KITTEN), playerIndex = 0, maxPieceAmount = maxPieceAmount)

        assertEquals(CellType.KITTEN, result.board.getCellType(2, 2))
        assertEquals(0, result.board.cells[2][2].owner)

        val movePiece = result.movePieces().singleOrNull { it.fromRow == 2 && it.fromColumn == 1 }
            ?: fail("expected leftward boop from (2,1), got ${result.movePieces()}")
        assertEquals(2, movePiece.toRow)
        assertEquals(0, movePiece.toColumn)
    }

    @Test
    fun `tall board - downward boop not silently dropped when checked row equals width`() {
        //5 rows x 3 cols (height 5, width 3). Move at (2, 1), kitten at (3, 1).
        //The adjacent-cell row is 3, so checkX = 3 at the bounds check.
        //   - Buggy:  3 in 0 until 3 (width)  is false -> boop silently skipped.
        //   - Fixed:  3 in 0 until 5 (height) is true  -> boop fires to (4, 1).
        //
        //Row 3 is a *valid* row; only the wrongly-used `width` bound rejects it.
        val result = board(
            "...",
            "...",
            "...",
            ".K.",
            "...",
        ).applyMove(Move(2, 1, MoveType.SET_KITTEN), playerIndex = 0, maxPieceAmount = maxPieceAmount)

        val movePiece = result.movePieces().singleOrNull { it.fromRow == 3 && it.fromColumn == 1 }
            ?: fail("expected downward boop from (3,1), got ${result.movePieces()}")
        assertEquals(4, movePiece.toRow)
        assertEquals(1, movePiece.toColumn)

        assertEquals(CellType.EMPTY, result.board.getCellType(3, 1))
        assertEquals(CellType.KITTEN, result.board.getCellType(4, 1))
    }
}