package com.alpha900i.a9kblanketbattle.data

import com.alpha900i.a9kblanketbattle.domain.HandChange
import com.alpha900i.a9kblanketbattle.domain.Move
import com.alpha900i.a9kblanketbattle.domain.MoveType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The auto-removal rule inside Board.applyMove.
 *
 * The rule (Board.kt):
 *
 *     if (deletableTriplets.size == 1 && !gameOver) {
 *         removeTriplet(...)        //effects + handChanges appended
 *         deletableTriplets.clear() //so the result reports no pending choice
 *     }
 *
 * Three observable branches:
 *
 *   1) size == 1 and !gameOver  -> triplet removed inline; result.deletableTriplets empty
 *   2) size >= 2                -> no removal regardless of gameOver
 *   3) size == 1 and gameOver   -> no removal (the "don't delete the winning triplet" rule)
 *
 * Setup choices:
 *   - Pre-placed pieces are CATs; the move is SET_KITTEN. SET_KITTEN can't
 *     push CATs (CellType.isPushable), so nothing gets displaced and the
 *     triplet shape survives the move unchanged.
 *   - `maxPieceAmount` is per player. Passing a small value lets a game-over
 *     via `catCount == maxPieceAmount` be forced on demand, even when no cat
 *     triplet exists.
 *
 * Branch 3 via the *winning* cat triplet is covered in different test
 * (`cat triplet for the acting player ends the game`). This file adds the
 * less obvious Branch 3 variant via cat count.
 */
class BoardApplyMoveAutoRemovalTest {

    private fun triplet(
        row: Int,
        column: Int,
        rowShift: Int,
        columnShift: Int,
    ): TripletOnBoard = TripletOnBoard(row, column, rowShift, columnShift)



    //=========================================================================
    //Branch 1 — single triplet, no game over -> removed inline
    //=========================================================================
    @Test
    fun `single mixed triplet is auto-removed - board cleared, effects recorded, deletableTriplets empty`() {
        //Pre-move: P0 cats at (1,1) and (1,2). SET_KITTEN at (1,3) makes
        //the mixed triplet (1,1)cat (1,2)cat (1,3)kitten.
        //
        //No boops: (1,2) is a CAT, which SET_KITTEN cannot push; all other
        //neighbours are empty or off the edge.
        val maxPieceAmount = 8
        val result = board(
            "....",
            ".cc.",
            "....",
            "....",
        ).applyMove(Move(1, 3, MoveType.SET_KITTEN), playerIndex = 0, maxPieceAmount = maxPieceAmount)

        //Auto-removal fired -> no triplets left to choose from.
        assertTrue(
            result.deletableTriplets.isEmpty(),
            "auto-removal must clear deletableTriplets, got ${result.deletableTriplets}",
        )
        assertFalse(result.gameOver)

        //Board: all three cells of the triplet are empty now.
        assertEquals(CellType.EMPTY, result.board.getCellType(1, 1))
        assertEquals(CellType.EMPTY, result.board.getCellType(1, 2))
        assertEquals(CellType.EMPTY, result.board.getCellType(1, 3))

        //Effects: exactly one AddPiece and three RemovePieces.
        val addPieces = result.pendingEffects.filterIsInstance<VisualEffect.AddPiece>()
        val removePieces = result.pendingEffects.filterIsInstance<VisualEffect.RemovePiece>()
        assertEquals(1, addPieces.size, "expected one AddPiece, got ${result.pendingEffects}")
        assertEquals(3, removePieces.size, "expected three RemovePieces, got ${result.pendingEffects}")
        assertTrue(addPieces.all { it.owner == 0 && it.type == CellType.KITTEN })
        assertTrue(removePieces.all { it.owner == 0 })
        assertEquals(
            setOf(
                CellType.CAT to (1 to 1),
                CellType.CAT to (1 to 2),
                CellType.KITTEN to (1 to 3),
            ),
            removePieces.map { it.type to (it.row to it.column) }.toSet(),
        )
    }

    @Test
    fun `single mixed triplet - handChanges reflect placement and kitten-to-cat conversion`() {
        //Same setup as above. Hand change maths:
        //   placement: -1 currentKitten
        //   removal of two cats:    +2 currentCat, 0 maxCat change
        //   removal of one kitten:  -1 maxKitten, +1 currentCat, +1 maxCat
        //Net: (-1, -1, +3, +1)
        val maxPieceAmount = 8
        val result = board(
            "....",
            ".cc.",
            "....",
            "....",
        ).applyMove(Move(1, 3, MoveType.SET_KITTEN), playerIndex = 0, maxPieceAmount = maxPieceAmount)

        assertEquals(
            HandChange(
                deltaCurrentKitten = -1,
                deltaMaxKitten = -1,
                deltaCurrentCat = 3,
                deltaMaxCat = 1,
            ),
            result.handChanges[0],
        )
        assertEquals(HandChange.emptyHand(), result.handChanges[1])
    }



    //=========================================================================
    //Branch 1 with pure-kitten triplet - different handChanges shape
    //=========================================================================
    @Test
    fun `single pure kitten triplet is auto-removed - all three kittens convert to cats`() {
        //Pre-move: P0 kittens at (1,1), (1,2). SET_KITTEN at (1,3) makes
        //the pure-kitten triplet (1,1) (1,2) (1,3).
        //
        //Boop check: (1,2) is a KITTEN, pushable by SET_KITTEN. Its receiver
        //(1,1) is another KITTEN, so the boop is blocked. No piece moves.
        val maxPieceAmount = 8
        val result = board(
            "....",
            ".kk.",
            "....",
            "....",
        ).applyMove(Move(1, 3, MoveType.SET_KITTEN), playerIndex = 0, maxPieceAmount = maxPieceAmount)

        assertTrue(
            result.deletableTriplets.isEmpty(),
            "auto-removal must clear deletableTriplets, got ${result.deletableTriplets}",
        )

        //Hand change maths:
        //   placement: -1 currentKitten
        //   removal of three kittens: -3 maxKitten, +3 currentCat, +3 maxCat
        //Net: (-1, -3, +3, +3)
        assertEquals(
            HandChange(
                deltaCurrentKitten = -1,
                deltaMaxKitten = -3,
                deltaCurrentCat = 3,
                deltaMaxCat = 3,
            ),
            result.handChanges[0],
        )
        assertEquals(HandChange.emptyHand(), result.handChanges[1])
    }



    //=========================================================================
    //Branch 2 — two triplets suppress auto-removal
    //=========================================================================
    @Test
    fun `two triplets suppress auto-removal - board intact, both triplets reported`() {
        //"+" shape around (2,2): P0 cats at N/S/E/W, placed kitten at centre.
        //SET_KITTEN can't push CATs, so nothing gets displaced.
        //
        //Expected triplets:
        //   horizontal: (2,1) (2,2) (2,3)
        //   vertical:   (1,2) (2,2) (3,2)
        //Both belong to P0, both are mixed type.
        val maxPieceAmount = 8
        val result = board(
            ".....",
            "..c..",
            ".c.c.",
            "..c..",
            ".....",
        ).applyMove(Move(2, 2, MoveType.SET_KITTEN), playerIndex = 0, maxPieceAmount = maxPieceAmount)

        val expected = setOf(
            triplet(2, 1, 0, 1),
            triplet(1, 2, 1, 0),
        )
        assertEquals(expected, result.deletableTriplets)

        //Board intact: all 5 pieces still in place.
        assertEquals(CellType.CAT, result.board.getCellType(1, 2))
        assertEquals(CellType.CAT, result.board.getCellType(2, 1))
        assertEquals(CellType.KITTEN, result.board.getCellType(2, 2))
        assertEquals(CellType.CAT, result.board.getCellType(2, 3))
        assertEquals(CellType.CAT, result.board.getCellType(3, 2))
        assertEquals(0, result.board.cells[2][2].owner)

        //No RemovePiece in the effects because no removal happened.
        val removePieces = result.pendingEffects.filterIsInstance<VisualEffect.RemovePiece>()
        assertTrue(
            removePieces.isEmpty(),
            "no removal should have fired, got ${result.pendingEffects}",
        )

        //Only the placement affects hands.
        assertEquals(
            HandChange(deltaCurrentKitten = -1, deltaMaxKitten = 0, deltaCurrentCat = 0, deltaMaxCat = 0),
            result.handChanges[0],
        )
        assertEquals(HandChange.emptyHand(), result.handChanges[1])
    }



    //=========================================================================
    //Branch 3 — game over (via cat count) suppresses auto-removal
    //=========================================================================
    @Test
    fun `game over via cat count suppresses auto-removal of a coexisting non-winning triplet`() {
        //P0 has four cats: a pair at (0,0)+(0,1) and another pair at (2,2)+(2,3).
        //No triplet exists pre-move (pairs only, no third piece in line).
        //SET_KITTEN at (0,2) completes a mixed triplet (0,0)-(0,1)-(0,2).
        //
        //Meanwhile catCount post-move = 4, and maxPieceAmount = 4, so
        //checkForGameOver fires via the `catCount == maxPieceAmount` branch
        //(there is no cat triplet - only cats and kittens, and no three
        //cats line up).
        //
        //Board.applyMove's auto-removal condition is
        //     size == 1 && !gameOver
        //which is false here because gameOver is true. So the triplet is
        //NOT removed even though it is the only one and it is not itself
        //a winning triplet.
        //
        //No boops: (0,1) is a CAT, not pushable by SET_KITTEN.
        val maxPieceAmount = 4
        val result = board(
            "cc...",
            ".....",
            "..cc.",
            ".....",
            ".....",
        ).applyMove(Move(0, 2, MoveType.SET_KITTEN), playerIndex = 0, maxPieceAmount = maxPieceAmount)

        assertTrue(result.gameOver, "catCount == maxPieceAmount must end the game")
        assertEquals(0, result.winnerIndex)

        //The mixed triplet is still reported - Board's MoveResult keeps it,
        //and it is Game.applyMove's job to drop it for the final GameState.
        assertEquals(
            setOf(triplet(0, 0, 0, 1)),
            result.deletableTriplets,
        )

        //Board still has all five pieces - the triplet was not removed.
        assertEquals(CellType.CAT, result.board.getCellType(0, 0))
        assertEquals(CellType.CAT, result.board.getCellType(0, 1))
        assertEquals(CellType.KITTEN, result.board.getCellType(0, 2))
        assertEquals(CellType.CAT, result.board.getCellType(2, 2))
        assertEquals(CellType.CAT, result.board.getCellType(2, 3))

        //No RemovePiece effects: nothing was removed.
        val removePieces = result.pendingEffects.filterIsInstance<VisualEffect.RemovePiece>()
        assertTrue(removePieces.isEmpty())

        //Only the placement affects hands - no conversion deltas leaked
        //through from a phantom removal.
        assertEquals(
            HandChange(deltaCurrentKitten = -1, deltaMaxKitten = 0, deltaCurrentCat = 0, deltaMaxCat = 0),
            result.handChanges[0],
        )
        assertEquals(HandChange.emptyHand(), result.handChanges[1])
    }
}