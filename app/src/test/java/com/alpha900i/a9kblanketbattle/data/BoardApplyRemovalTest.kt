package com.alpha900i.a9kblanketbattle.data

import com.alpha900i.a9kblanketbattle.domain.HandChange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Board.applyRemoval.
 *
 * applyRemoval takes a triplet the caller has already decided to remove and:
 *   - clears its three cells
 *   - appends one RemovePiece per cell
 *   - applies per-piece hand deltas:
 *       kitten -> deltaMaxKitten -1, deltaCurrentCat +1, deltaMaxCat +1
 *       cat    -> deltaCurrentCat +1
 *   - returns gameOver = false, winnerIndex = -1, empty deletableTriplets
 *
 * Because applyMove already gated the call (only reached when 2+ triplets
 * were available and the game is not over), none of the following need
 * testing here: triplet detection, boop, game-over, auto-removal. Those
 * live in different tests
 *
 * `getDeletableTriplets` only ever returns single-owner triplets, so the
 * mixed-owner case below is defensive - but applyRemoval is public and the
 * current code handles it correctly, so pinning the behaviour is cheap.
 *
 * Note on the OOB guard: removeTriplet only checks the *positive* direction
 * (rowIndex + 2*dx >= height, columnIndex + 2*dy >= width). It does not
 * check the negative direction, so a hand-constructed triplet with
 * columnShift = -1 and column < 2 would index at -1 and throw. That path
 * is unreachable from real game flow and is not tested here.
 */
class BoardApplyRemovalTest {

    private fun Board.assertCell(row: Int, col: Int, type: CellType, owner: Int) {
        assertEquals(type, getCellType(row, col), "type at ($row,$col)")
        assertEquals(owner, cells[row][col].owner, "owner at ($row,$col)")
    }

    private fun MoveResult.removePieces(): List<VisualEffect.RemovePiece> =
        pendingEffects.filterIsInstance<VisualEffect.RemovePiece>()

    //=========================================================================
    //2.1 — Pure cat triplet
    //=========================================================================

    @Test
    fun `pure cat triplet - cells cleared, three RemovePiece effects, hand gains three cats`() {
        //P0 cats in row 1 at columns 1,2,3; triplet starting at (1,1) with
        //direction (0,1).
        val result = board(
            "....",
            ".ccc",
            "....",
            "....",
        ).applyRemoval(TripletOnBoard(row = 1, column = 1, rowShift = 0, columnShift = 1))

        result.board.assertCell(1, 1, CellType.EMPTY, -1)
        result.board.assertCell(1, 2, CellType.EMPTY, -1)
        result.board.assertCell(1, 3, CellType.EMPTY, -1)

        //Three RemovePiece of type CAT, owner 0.
        val removes = result.removePieces()
        assertEquals(3, removes.size, "expected 3 RemovePiece, got ${result.pendingEffects}")
        assertTrue(removes.all { it.type == CellType.CAT && it.owner == 0 })
        assertEquals(
            setOf(1 to 1, 1 to 2, 1 to 3),
            removes.map { it.row to it.column }.toSet(),
        )

        //Hand: three cats returned, no kitten conversion.
        assertEquals(HandChange(0, 0, 3, 0), result.handChanges[0])
        assertEquals(HandChange.emptyHand(), result.handChanges[1])
    }

    //=========================================================================
    //2.2 — Pure kitten triplet
    //=========================================================================

    @Test
    fun `pure kitten triplet - three kittens convert to three cats`() {
        val result = board(
            "....",
            ".kkk",
            "....",
            "....",
        ).applyRemoval(TripletOnBoard(row = 1, column = 1, rowShift = 0, columnShift = 1))

        result.board.assertCell(1, 1, CellType.EMPTY, -1)
        result.board.assertCell(1, 2, CellType.EMPTY, -1)
        result.board.assertCell(1, 3, CellType.EMPTY, -1)

        val removes = result.removePieces()
        assertEquals(3, removes.size)
        assertTrue(removes.all { it.type == CellType.KITTEN && it.owner == 0 })

        //Hand: max kittens shrink by 3, current + max cats grow by 3.
        assertEquals(HandChange(0, -3, 3, 3), result.handChanges[0])
        assertEquals(HandChange.emptyHand(), result.handChanges[1])
    }

    //=========================================================================
    //2.3 — Mixed triplet
    //=========================================================================

    @Test
    fun `mixed triplet - only the kitten contributes kitten-to-cat conversion deltas`() {
        //cat, kitten, cat in row 1. Same coordinate triplet direction.
        val result = board(
            "....",
            ".ckc",
            "....",
            "....",
        ).applyRemoval(TripletOnBoard(row = 1, column = 1, rowShift = 0, columnShift = 1))

        result.board.assertCell(1, 1, CellType.EMPTY, -1)
        result.board.assertCell(1, 2, CellType.EMPTY, -1)
        result.board.assertCell(1, 3, CellType.EMPTY, -1)

        val removes = result.removePieces()
        assertEquals(3, removes.size)
        assertEquals(
            setOf(CellType.CAT to (1 to 1), CellType.KITTEN to (1 to 2), CellType.CAT to (1 to 3)),
            removes.map { it.type to (it.row to it.column) }.toSet(),
        )

        //Hand: 3 current cats returned; one of them was a kitten pre-removal,
        //so max kittens -1 and max cats +1.
        assertEquals(HandChange(0, -1, 3, 1), result.handChanges[0])
        assertEquals(HandChange.emptyHand(), result.handChanges[1])
    }

    //=========================================================================
    //2.4 — Mixed-owner triplet (defensive)
    //=========================================================================

    @Test
    fun `mixed-owner triplet - each owner receives their own removed pieces`() {
        //Row 1: P0 cat, P1 cat, P0 kitten. Owner attribution is per-cell,
        //not per-triplet.
        val result = board(
            "....",
            ".cCk",
            "....",
            "....",
        ).applyRemoval(TripletOnBoard(row = 1, column = 1, rowShift = 0, columnShift = 1))

        val removes = result.removePieces()
        assertEquals(3, removes.size)
        assertEquals(
            setOf(
                Triple(1, 1, 0),  //P0 cat at (1,1)
                Triple(1, 2, 1),  //P1 cat at (1,2)
                Triple(1, 3, 0),  //P0 kitten at (1,3)
            ),
            removes.map { Triple(it.row, it.column, it.owner) }.toSet(),
        )

        //P0: cat + kitten returned -> +2 currentCat, -1 maxKitten, +1 maxCat
        assertEquals(HandChange(0, -1, 2, 1), result.handChanges[0])
        //P1: cat returned -> +1 currentCat only
        assertEquals(HandChange(0, 0, 1, 0), result.handChanges[1])
    }

    //=========================================================================
    //2.5 — Down-left diagonal
    //=========================================================================

    @Test
    fun `down-left diagonal triplet - columnShift -1 coordinates are correct`() {
        //Board: 3x3 with cats at (0,2), (1,1), (2,0). Triplet starts at
        //top-right with rowShift=1, columnShift=-1.
        //
        //This is the only triplet direction where the OOB guard in
        //removeTriplet doesn't apply (its check is on the *positive* shift),
        //so a small off-by-one in column arithmetic here would slip through
        //any test that only exercises columnShift = 0 or +1.
        val result = board(
            "..c",
            ".c.",
            "c..",
        ).applyRemoval(TripletOnBoard(row = 0, column = 2, rowShift = 1, columnShift = -1))

        result.board.assertCell(0, 2, CellType.EMPTY, -1)
        result.board.assertCell(1, 1, CellType.EMPTY, -1)
        result.board.assertCell(2, 0, CellType.EMPTY, -1)

        assertEquals(
            setOf(0 to 2, 1 to 1, 2 to 0),
            result.removePieces().map { it.row to it.column }.toSet(),
        )
        assertEquals(HandChange(0, 0, 3, 0), result.handChanges[0])
        assertEquals(HandChange.emptyHand(), result.handChanges[1])
    }

    //=========================================================================
    //2.6 — Out-of-bounds triplet (defensive)
    //=========================================================================

    @Test
    fun `triplet extending past the board is a silent no-op`() {
        //3x3 board; triplet starting at column 1 with columnShift=1 would
        //need cells at columns 1,2,3 - the last is off the right edge.
        //removeTriplet returns early with an empty effect list and no
        //hand changes.
        val result = board(
            "...",
            ".cc",
            "...",
        ).applyRemoval(TripletOnBoard(row = 1, column = 1, rowShift = 0, columnShift = 1))

        //Board unchanged - pieces at (1,1), (1,2) still present.
        result.board.assertCell(1, 1, CellType.CAT, 0)
        result.board.assertCell(1, 2, CellType.CAT, 0)

        //No effects, no hand changes.
        assertTrue(result.pendingEffects.isEmpty())
        assertEquals(HandChange.emptyHand(), result.handChanges[0])
        assertEquals(HandChange.emptyHand(), result.handChanges[1])
    }

    //=========================================================================
    //2.7 — MoveResult invariants
    //=========================================================================

    @Test
    fun `applyRemoval always reports no game over, no winner, no deletable triplets`() {
        //Any successful removal. Using the pure-cat fixture as a driver.
        val result = board(
            "....",
            ".ccc",
            "....",
            "....",
        ).applyRemoval(TripletOnBoard(row = 1, column = 1, rowShift = 0, columnShift = 1))

        assertFalse(result.gameOver)
        assertEquals(-1, result.winnerIndex)
        assertTrue(result.deletableTriplets.isEmpty())
    }
}