package com.alpha900i.a9kblanketbattle.data

import com.alpha900i.a9kblanketbattle.domain.HandChange
import com.alpha900i.a9kblanketbattle.domain.Move
import com.alpha900i.a9kblanketbattle.domain.MoveType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Two "remove from board" move types.
 *
 *   PROMOTE_KITTEN -> clears the kitten, converts it into a cat *in hand*
 *                     (max kitten -1, current cat +1, max cat +1)
 *   RETURN_CAT     -> clears the cat, moves it back into the hand
 *                     (current cat +1, max cat unchanged)
 *
 * Both go through Board.applyMove and therefore still run
 *   - checkForGameOver    (removing pieces cannot end the game)
 *   - getDeletableTriplets (removing pieces cannot create a triplet)
 * so every happy path asserts `gameOver == false`, `winnerIndex == -1`,
 * and `deletableTriplets.isEmpty()`.
 *
 * Neither move type boops. setPieceAndBoop is only called for
 * SET_KITTEN / SET_CAT, and even if it were, isPushable returns false
 * for both of these move types. So no scaffolding for non-pushable
 * neighboring pieces is needed here.
 */
class BoardApplyMoveRemoveFromBoardTest {

    private val maxPieceAmount = 8

    private fun MoveResult.removePieces(): List<VisualEffect.RemovePiece> =
        pendingEffects.filterIsInstance<VisualEffect.RemovePiece>()

    private fun MoveResult.addPieces(): List<VisualEffect.AddPiece> =
        pendingEffects.filterIsInstance<VisualEffect.AddPiece>()

    private fun MoveResult.movePieces(): List<VisualEffect.MovePiece> =
        pendingEffects.filterIsInstance<VisualEffect.MovePiece>()



    //=========================================================================
    //PROMOTE_KITTEN
    //=========================================================================
    @Test
    fun `PROMOTE_KITTEN clears the kitten, adds cat to hand, keeps other cells intact`() {
        //P0's kitten at (1,1), P1's cat at (0,0), P1's kitten at (2,2).
        //Because neither promote nor setPieceAndBoop touches neighbouring
        //cells, all three positions are unrelated - this test verifies
        //that the promote operation touches only its own cell.
        val result = board(
            "C..",
            ".k.",
            "..K",
        ).applyMove(Move(1, 1, MoveType.PROMOTE_KITTEN), playerIndex = 0, maxPieceAmount = maxPieceAmount)

        //Promoted cell is now empty.
        assertEquals(CellType.EMPTY, result.board.getCellType(1, 1))

        //Hand deltas: kitten leaves the game, cat appears in hand.
        assertEquals(
            HandChange(
                deltaCurrentKitten = 0,  //kitten was not in hand
                deltaMaxKitten = -1,     //kitten no longer in play
                deltaCurrentCat = 1,     //new cat in hand
                deltaMaxCat = 1,         //counts toward max
            ),
            result.handChanges[0],
        )
        assertEquals(HandChange.emptyHand(), result.handChanges[1])

        //Exactly one RemovePiece of type KITTEN, at the promoted cell.
        val removes = result.removePieces()
        assertEquals(1, removes.size, "expected one RemovePiece, got ${result.pendingEffects}")
        val remove = removes.single()
        assertEquals(1, remove.row)
        assertEquals(1, remove.column)
        assertEquals(CellType.KITTEN, remove.type)
        assertEquals(0, remove.owner)

        //No AddPiece or MovePiece accompanies a promotion.
        assertTrue(result.addPieces().isEmpty(), "promotion should not emit AddPiece, got ${result.addPieces()}")
        assertTrue(result.movePieces().isEmpty(), "promotion should not emit MovePiece, got ${result.movePieces()}")

        //Other cells untouched.
        assertEquals(CellType.CAT, result.board.getCellType(0, 0))
        assertEquals(1, result.board.cells[0][0].owner)
        assertEquals(CellType.KITTEN, result.board.getCellType(2, 2))
        assertEquals(1, result.board.cells[2][2].owner)

        //Removing a piece cannot end the game or form a triplet.
        assertFalse(result.gameOver)
        assertEquals(-1, result.winnerIndex)
        assertTrue(result.deletableTriplets.isEmpty())
    }

    @Test
    fun `PROMOTE_KITTEN can be performed on any board position without side effects`() {
        //Corner position, near-only-one-neighbour. Same as the centre case
        //in terms of behaviour, but pins that the operation is position-
        //agnostic and doesn't accidentally interact with edges.
        val result = board(
            "k..",
            "...",
            "...",
        ).applyMove(Move(0, 0, MoveType.PROMOTE_KITTEN), playerIndex = 0, maxPieceAmount = maxPieceAmount)

        assertEquals(CellType.EMPTY, result.board.getCellType(0, 0))
        assertEquals(
            HandChange(0, -1, 1, 1),
            result.handChanges[0],
        )
        assertEquals(CellType.KITTEN, result.removePieces().single().type)
    }

    @Test
    fun `PROMOTE_KITTEN on a non-kitten cell fails an assertion`() {
        //Cell at (1,1) is a cat, not a kitten. ascendKitten's leading
        //assert must fire before any state is corrupted.
        //
        //Requires Gradle's test task to have enableAssertions = true
        //(the default).
        assertFailsWith<AssertionError> {
            board(
                "...",
                ".c.",
                "...",
            ).applyMove(Move(1, 1, MoveType.PROMOTE_KITTEN), playerIndex = 0, maxPieceAmount = maxPieceAmount)
        }
    }

    @Test
    fun `PROMOTE_KITTEN on another player's kitten fails an assertion`() {
        //Cell at (1,1) is P1's kitten; the move belongs to P0.
        assertFailsWith<AssertionError> {
            board(
                "...",
                ".K.",
                "...",
            ).applyMove(Move(1, 1, MoveType.PROMOTE_KITTEN), playerIndex = 0, maxPieceAmount = maxPieceAmount)
        }
    }



    //=========================================================================
    //RETURN_CAT
    //=========================================================================
    @Test
    fun `RETURN_CAT clears the cat, returns it to hand, keeps other cells intact`() {
        //P0's cat at (1,1); other pieces unrelated.
        val result = board(
            "K..",
            ".c.",
            "..k",
        ).applyMove(Move(1, 1, MoveType.RETURN_CAT), playerIndex = 0, maxPieceAmount = maxPieceAmount)

        //Returned cell is now empty.
        assertEquals(CellType.EMPTY, result.board.getCellType(1, 1))

        //Hand deltas: cat returns from board to hand, max unchanged.
        assertEquals(
            HandChange(
                deltaCurrentKitten = 0,
                deltaMaxKitten = 0,
                deltaCurrentCat = 1,   //cat back in hand
                deltaMaxCat = 0,       //was already counted toward max
            ),
            result.handChanges[0],
        )
        assertEquals(HandChange.emptyHand(), result.handChanges[1])

        //Exactly one RemovePiece of type CAT, at the returned cell.
        val removes = result.removePieces()
        assertEquals(1, removes.size, "expected one RemovePiece, got ${result.pendingEffects}")
        val remove = removes.single()
        assertEquals(1, remove.row)
        assertEquals(1, remove.column)
        assertEquals(CellType.CAT, remove.type)
        assertEquals(0, remove.owner)

        //Other cells untouched.
        assertEquals(CellType.KITTEN, result.board.getCellType(0, 0))
        assertEquals(1, result.board.cells[0][0].owner)
        assertEquals(CellType.KITTEN, result.board.getCellType(2, 2))
        assertEquals(0, result.board.cells[2][2].owner)

        assertFalse(result.gameOver)
        assertEquals(-1, result.winnerIndex)
        assertTrue(result.deletableTriplets.isEmpty())
    }

    @Test
    fun `RETURN_CAT can be performed on any board position without side effects`() {
        //Corner position; behaviour is position-agnostic.
        val result = board(
            "c..",
            "...",
            "...",
        ).applyMove(Move(0, 0, MoveType.RETURN_CAT), playerIndex = 0, maxPieceAmount = maxPieceAmount)

        assertEquals(CellType.EMPTY, result.board.getCellType(0, 0))
        assertEquals(
            HandChange(0, 0, 1, 0),
            result.handChanges[0],
        )
        assertEquals(CellType.CAT, result.removePieces().single().type)
    }

    @Test
    fun `RETURN_CAT on a non-cat cell fails an assertion`() {
        //Cell at (1,1) is a kitten, not a cat.
        assertFailsWith<AssertionError> {
            board(
                "...",
                ".k.",
                "...",
            ).applyMove(Move(1, 1, MoveType.RETURN_CAT), playerIndex = 0, maxPieceAmount = maxPieceAmount)
        }
    }

    @Test
    fun `RETURN_CAT on another player's cat fails an assertion`() {
        //Cell at (1,1) is P1's cat; the move belongs to P0.
        assertFailsWith<AssertionError> {
            board(
                "...",
                ".C.",
                "...",
            ).applyMove(Move(1, 1, MoveType.RETURN_CAT), playerIndex = 0, maxPieceAmount = maxPieceAmount)
        }
    }
}