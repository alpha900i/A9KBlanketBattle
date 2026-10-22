package com.alpha900i.a9kblanketbattle.data

import com.alpha900i.a9kblanketbattle.domain.HandChange
import com.alpha900i.a9kblanketbattle.domain.Move
import com.alpha900i.a9kblanketbattle.domain.MoveType

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Boop mechanics of Board.applyMove.
 *
 * Every board here uses width == height, so any failure in this file is a
 * genuine boop-logic bug, not the width/height swap from setPieceAndBoop
 * (that swap is covered separately in BoardApplyMoveBoopNonSquareTest).
 */
class BoardApplyMoveBoopTest {

    private val maxPieceAmount = 8

    private fun MoveResult.movePieces(): List<VisualEffect.MovePiece> =
        pendingEffects.filterIsInstance<VisualEffect.MovePiece>()

    private fun MoveResult.removePieces(): List<VisualEffect.RemovePiece> =
        pendingEffects.filterIsInstance<VisualEffect.RemovePiece>()

    private fun MoveResult.addPieces(): List<VisualEffect.AddPiece> =
        pendingEffects.filterIsInstance<VisualEffect.AddPiece>()

    private fun MoveResult.singleMovePiece(): VisualEffect.MovePiece {
        val pieces = movePieces()
        assertEquals(1, pieces.size, "expected exactly one MovePiece, got $pendingEffects")
        return pieces[0]
    }

    private fun MoveResult.singleRemovePiece(): VisualEffect.RemovePiece {
        val pieces = removePieces()
        assertEquals(1, pieces.size, "expected exactly one RemovePiece, got $pendingEffects")
        return pieces[0]
    }

    private fun MoveResult.assertOnlyAddPiece() {
        assertTrue(movePieces().isEmpty(), "expected no MovePiece, got ${movePieces()}")
        assertTrue(removePieces().isEmpty(), "expected no RemovePiece, got ${removePieces()}")
        assertEquals(1, addPieces().size, "expected exactly one AddPiece, got ${addPieces()}")
    }



    //=========================================================================
    //Boop that lands on the board (kitten pushed to a destination cell)
    //=========================================================================
    private data class OnBoardCase(
        val label: String,
        val rows: List<String>,
        val move: Move,
        val from: Pair<Int, Int>,
        val to: Pair<Int, Int>,
    )

    @Test
    fun `SET_KITTEN boops adjacent kitten onto the board in all 8 directions`() {
        val cases = listOf(
            //Down (1,0): move at (1,1), kitten at (2,1), lands at (3,1)
            OnBoardCase(
                "down",
                listOf("....", "....", ".k..", "...."),
                Move(1, 1, MoveType.SET_KITTEN), from = 2 to 1, to = 3 to 1,
            ),
            //Up (-1,0): move at (2,1), kitten at (1,1), lands at (0,1)
            OnBoardCase(
                "up",
                listOf("....", ".k..", "....", "...."),
                Move(2, 1, MoveType.SET_KITTEN), from = 1 to 1, to = 0 to 1,
            ),
            //Right (0,1): move at (1,1), kitten at (1,2), lands at (1,3)
            OnBoardCase(
                "right",
                listOf("....", "..k.", "....", "...."),
                Move(1, 1, MoveType.SET_KITTEN), from = 1 to 2, to = 1 to 3,
            ),
            //Left (0,-1): move at (1,2), kitten at (1,1), lands at (1,0)
            OnBoardCase(
                "left",
                listOf("....", ".k..", "....", "...."),
                Move(1, 2, MoveType.SET_KITTEN), from = 1 to 1, to = 1 to 0,
            ),
            //Down-right (1,1): move at (1,1), kitten at (2,2), lands at (3,3)
            OnBoardCase(
                "down-right",
                listOf("....", "....", "..k.", "...."),
                Move(1, 1, MoveType.SET_KITTEN), from = 2 to 2, to = 3 to 3,
            ),
            //Up-left (-1,-1): move at (2,2), kitten at (1,1), lands at (0,0)
            OnBoardCase(
                "up-left",
                listOf("....", ".k..", "....", "...."),
                Move(2, 2, MoveType.SET_KITTEN), from = 1 to 1, to = 0 to 0,
            ),
            //Down-left (1,-1): move at (1,2), kitten at (2,1), lands at (3,0)
            OnBoardCase(
                "down-left",
                listOf("....", "....", ".k..", "...."),
                Move(1, 2, MoveType.SET_KITTEN), from = 2 to 1, to = 3 to 0,
            ),
            //Up-right (-1,1): move at (2,1), kitten at (1,2), lands at (0,3)
            OnBoardCase(
                "up-right",
                listOf("....", "..k.", "....", "...."),
                Move(2, 1, MoveType.SET_KITTEN), from = 1 to 2, to = 0 to 3,
            ),
        )

        for (case in cases) {
            val result = board(*case.rows.toTypedArray())
                .applyMove(case.move, playerIndex = 0, maxPieceAmount = maxPieceAmount)

            val movePiece = result.singleMovePiece()
            assertEquals(case.from.first, movePiece.fromRow, "${case.label}: fromRow")
            assertEquals(case.from.second, movePiece.fromColumn, "${case.label}: fromCol")
            assertEquals(case.to.first, movePiece.toRow, "${case.label}: toRow")
            assertEquals(case.to.second, movePiece.toColumn, "${case.label}: toCol")
            assertEquals(0, movePiece.owner, "${case.label}: owner preserved")
            assertEquals(CellType.KITTEN, movePiece.type, "${case.label}: type preserved")

            //Source emptied, destination occupied, placed piece at move cell.
            assertEquals(CellType.EMPTY, result.board.getCellType(case.from.first, case.from.second), "${case.label}: source empty")
            assertEquals(CellType.KITTEN, result.board.getCellType(case.to.first, case.to.second), "${case.label}: destination filled")
            assertEquals(CellType.KITTEN, result.board.getCellType(case.move.row, case.move.column), "${case.label}: placed piece")

            //Nothing went to hand because nothing fell off.
            assertEquals(-1, result.handChanges[0].deltaCurrentKitten, "${case.label}: place delta")
            assertEquals(HandChange.emptyHand(), result.handChanges[1])
        }
    }



    //=========================================================================
    //Boop that falls off the board (piece returns to owner's hand)
    //=========================================================================

    private data class OffBoardCase(
        val label: String,
        val rows: List<String>,
        val move: Move,
        val pushedFrom: Pair<Int, Int>,
    )

    @Test
    fun `SET_KITTEN boops kitten off the board in all 8 directions, returns to owner's hand`() {
        //Move is always at (1,1) on a 3x3 board. The kitten must sit at
        //(1+dx, 1+dy) so that its receiver (1+2dx, 1+2dy) falls off the edge.
        val cases = listOf(
            OffBoardCase("down",       listOf("...", "...", ".k."), Move(1, 1, MoveType.SET_KITTEN), 2 to 1),
            OffBoardCase("up",         listOf(".k.", "...", "..."), Move(1, 1, MoveType.SET_KITTEN), 0 to 1),
            OffBoardCase("right",      listOf("...", "..k", "..."), Move(1, 1, MoveType.SET_KITTEN), 1 to 2),
            OffBoardCase("left",       listOf("...", "k..", "..."), Move(1, 1, MoveType.SET_KITTEN), 1 to 0),
            OffBoardCase("down-right", listOf("...", "...", "..k"), Move(1, 1, MoveType.SET_KITTEN), 2 to 2),
            OffBoardCase("up-left",    listOf("k..", "...", "..."), Move(1, 1, MoveType.SET_KITTEN), 0 to 0),
            OffBoardCase("down-left",  listOf("...", "...", "k.."), Move(1, 1, MoveType.SET_KITTEN), 2 to 0),
            OffBoardCase("up-right",   listOf("..k", "...", "..."), Move(1, 1, MoveType.SET_KITTEN), 0 to 2),
        )

        for (case in cases) {
            val result = board(*case.rows.toTypedArray())
                .applyMove(case.move, playerIndex = 0, maxPieceAmount = maxPieceAmount)

            val removePiece = result.singleRemovePiece()
            assertEquals(case.pushedFrom.first, removePiece.row, "${case.label}: removeRow")
            assertEquals(case.pushedFrom.second, removePiece.column, "${case.label}: removeCol")
            assertEquals(CellType.KITTEN, removePiece.type, "${case.label}: type")
            assertEquals(0, removePiece.owner, "${case.label}: owner")

            //Pushed cell emptied, placed piece present, destination gone.
            assertEquals(CellType.EMPTY, result.board.getCellType(case.pushedFrom.first, case.pushedFrom.second), "${case.label}: source empty")
            assertEquals(CellType.KITTEN, result.board.getCellType(case.move.row, case.move.column), "${case.label}: placed piece")

            //Place + return = net 0 on current kitten for the acting player.
            assertEquals(0, result.handChanges[0].deltaCurrentKitten, "${case.label}: current delta")
            assertEquals(HandChange.emptyHand(), result.handChanges[1])
        }
    }

    @Test
    fun `SET_CAT boops cat off the board, cat returns to owner's hand`() {
        //3x3 board; cat at (2,1), move at (1,1) with SET_CAT pushes it off the bottom edge.
        val result = board(
            "...",
            "...",
            ".c.",
        ).applyMove(Move(1, 1, MoveType.SET_CAT), playerIndex = 0, maxPieceAmount = maxPieceAmount)

        val removePiece = result.singleRemovePiece()
        assertEquals(2, removePiece.row)
        assertEquals(1, removePiece.column)
        assertEquals(CellType.CAT, removePiece.type)
        assertEquals(0, removePiece.owner)

        //Placed cat (-1) + returned cat (+1) cancel on currentCat.
        //But returned cat is a CAT, so no deltaMaxCat change either.
        assertEquals(
            HandChange(deltaCurrentKitten = 0, deltaMaxKitten = 0, deltaCurrentCat = 0, deltaMaxCat = 0),
            result.handChanges[0],
        )
    }



    //=========================================================================
    //Which pieces get pushed by which move
    //=========================================================================
    @Test
    fun `SET_CAT pushes a kitten`() {
        val result = board(
            "....",
            "....",
            ".k..",
            "....",
        ).applyMove(Move(1, 1, MoveType.SET_CAT), playerIndex = 0, maxPieceAmount = maxPieceAmount)

        val movePiece = result.singleMovePiece()
        assertEquals(2, movePiece.fromRow)
        assertEquals(1, movePiece.fromColumn)
        assertEquals(3, movePiece.toRow)
        assertEquals(1, movePiece.toColumn)
        assertEquals(CellType.KITTEN, movePiece.type)
    }

    @Test
    fun `SET_CAT pushes a cat`() {
        val result = board(
            "....",
            "....",
            ".c..",
            "....",
        ).applyMove(Move(1, 1, MoveType.SET_CAT), playerIndex = 1, maxPieceAmount = maxPieceAmount)

        val movePiece = result.singleMovePiece()
        assertEquals(2, movePiece.fromRow)
        assertEquals(1, movePiece.fromColumn)
        assertEquals(3, movePiece.toRow)
        assertEquals(1, movePiece.toColumn)
        assertEquals(CellType.CAT, movePiece.type)
        assertEquals(0, movePiece.owner) //owner of the pushed cat, not the mover
    }

    @Test
    fun `SET_KITTEN does not push a cat`() {
        val result = board(
            "....",
            "....",
            ".c..",
            "....",
        ).applyMove(Move(1, 1, MoveType.SET_KITTEN), playerIndex = 0, maxPieceAmount = maxPieceAmount)

        result.assertOnlyAddPiece()
        //Cat stays where it was.
        assertEquals(CellType.CAT, result.board.getCellType(2, 1))
        assertEquals(0, result.board.cells[2][1].owner)
    }



    //=========================================================================
    //Blocked boop
    //=========================================================================
    @Test
    fun `boop is skipped when receiver cell is occupied`() {
        //Move at (1,1). P1 kitten at (2,1) is pushable, but its receiver
        //(3,1) is occupied by a P1 cat, so the boop is skipped.
        //
        //Ownership is deliberately split across players so the placed P0
        //piece plus the two P1 pieces do not form a triplet — otherwise the
        //auto-removal branch would fire and the "no RemovePiece" assertion
        //would fail for the wrong reason.
        val result = board(
            "....",
            "....",
            ".K..",
            ".C..",
        ).applyMove(Move(1, 1, MoveType.SET_KITTEN), playerIndex = 0, maxPieceAmount = maxPieceAmount)

        result.assertOnlyAddPiece()
        assertEquals(CellType.KITTEN, result.board.getCellType(1, 1))
        assertEquals(CellType.KITTEN, result.board.getCellType(2, 1))
        assertEquals(CellType.CAT, result.board.getCellType(3, 1))
        assertEquals(1, result.board.cells[2][1].owner)
        assertEquals(1, result.board.cells[3][1].owner)
    }



    //=========================================================================
    //Multiple simultaneous boops
    //=========================================================================
    @Test
    fun `setting in the middle of 8 kittens boops all 8 onto the board`() {
        //'K' = player 1's kittens, so we can verify ownership is preserved.
        val result = board(
            ".....",
            ".KKK.",
            ".K.K.",
            ".KKK.",
            ".....",
        ).applyMove(Move(2, 2, MoveType.SET_KITTEN), playerIndex = 0, maxPieceAmount = maxPieceAmount)

        val recorded = result.movePieces()
            .map { (it.fromRow to it.fromColumn) to (it.toRow to it.toColumn) }
            .toSet()

        val expectedSet = setOf(
            (1 to 1) to (0 to 0),
            (1 to 2) to (0 to 2),
            (1 to 3) to (0 to 4),
            (2 to 1) to (2 to 0),
            (2 to 3) to (2 to 4),
            (3 to 1) to (4 to 0),
            (3 to 2) to (4 to 2),
            (3 to 3) to (4 to 4),
        )

        assertEquals(8, recorded.size, "expected 8 boops, got ${recorded.size}: $recorded")
        assertEquals(expectedSet, recorded, "boop set mismatch")

        assertTrue(
            result.movePieces().all { it.owner == 1 && it.type == CellType.KITTEN },
            "all moved pieces should belong to player 1",
        )

        //Center has the new piece, all 8 neighbors are empty.
        assertEquals(CellType.KITTEN, result.board.getCellType(2, 2))
        assertEquals(0, result.board.cells[2][2].owner)
        for (r in 1..3) for (c in 1..3) {
            if (r == 2 && c == 2) continue
            assertEquals(CellType.EMPTY, result.board.getCellType(r, c), "neighbor ($r,$c) should be empty")
        }

        //Nothing fell off: only the placed piece leaves the mover's hand.
        assertEquals(-1, result.handChanges[0].deltaCurrentKitten)
        assertEquals(HandChange.emptyHand(), result.handChanges[1])
    }

    @Test
    fun `single move can both boop on-board and push off-board`() {
        //Player 0 sets at (1,1):
        //   - P1's kitten at (0,1) is above → booped off the top edge (returns to P1 hand)
        //   - P1's kitten at (1,2) is right → booped onto the board to (1,3)
        //
        //Move cell (1,1) is empty; only the two P1 kittens are on the board.
        val result = board(
            ".K..",
            "..K.",
            "....",
            "....",
        ).applyMove(Move(1, 1, MoveType.SET_KITTEN), playerIndex = 0, maxPieceAmount = maxPieceAmount)

        //On-board boop.
        val onBoard = result.movePieces().singleOrNull { it.fromRow == 1 && it.fromColumn == 2 }
            ?: fail("expected boop from (1,2), got ${result.movePieces()}")
        assertEquals(1, onBoard.toRow)
        assertEquals(3, onBoard.toColumn)

        //Off-board boop.
        val offBoard = result.removePieces().singleOrNull { it.row == 0 && it.column == 1 }
            ?: fail("expected removal of (0,1), got ${result.removePieces()}")
        assertEquals(CellType.KITTEN, offBoard.type)
        assertEquals(1, offBoard.owner)

        //Board state.
        assertEquals(CellType.EMPTY, result.board.getCellType(0, 1))
        assertEquals(CellType.EMPTY, result.board.getCellType(1, 2))
        assertEquals(CellType.KITTEN, result.board.getCellType(1, 3))
        assertEquals(CellType.KITTEN, result.board.getCellType(1, 1))
        assertEquals(0, result.board.cells[1][1].owner)

        //Hand changes: P0 placed one (-1), P1 got one back from the edge (+1).
        assertEquals(-1, result.handChanges[0].deltaCurrentKitten)
        assertEquals(1, result.handChanges[1].deltaCurrentKitten)
    }
}