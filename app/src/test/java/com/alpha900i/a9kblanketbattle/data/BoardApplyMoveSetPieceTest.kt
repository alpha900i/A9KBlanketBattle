package com.alpha900i.a9kblanketbattle.data

import com.alpha900i.a9kblanketbattle.domain.HandChange
import com.alpha900i.a9kblanketbattle.domain.Move
import com.alpha900i.a9kblanketbattle.domain.MoveType
import org.junit.Assert
import kotlin.test.Test
import kotlin.test.assertNull


/**
 * Set a single piece on an empty cell, with no boops, no triplets, no game over.
 */
class BoardApplyMoveSetPieceTest {
    //Well above anything these tests place, so the "catCount == maxPieceAmount"
    //game-over branch can't fire accidentally.
    private val maxPieceAmount = 8



    //-------------------------------------------------------------------------
    //Happy path: piece lands on the board with the right type and owner
    //-------------------------------------------------------------------------
    @Test
    fun `SET_KITTEN places kitten of acting player on the board`() {
        val board = board(
            "...",
            "...",
            "...",
        )
        val move = Move(row = 1, column = 1, moveType = MoveType.SET_KITTEN)

        val result = board.applyMove(move, playerIndex = 0, maxPieceAmount = maxPieceAmount)

        result.board.assertCellPiece(1, 1, PieceType.KITTEN, 0)
    }

    @Test
    fun `SET_CAT places cat of acting player on the board`() {
        val board = board(
            "...",
            "...",
            "...",
        )
        val move = Move(row = 2, column = 2, moveType = MoveType.SET_CAT)

        val result = board.applyMove(move, playerIndex = 0, maxPieceAmount = maxPieceAmount)

        result.board.assertCellPiece(2, 2, PieceType.CAT, 0)
    }



    //-------------------------------------------------------------------------
    //handChanges for the acting player
    //-------------------------------------------------------------------------
    @Test
    fun `SET_KITTEN decrements acting player's kitten count by one and touches nothing else`() {
        val board = board("...")
        val move = Move(0, 0, MoveType.SET_KITTEN)

        val result = board.applyMove(move, playerIndex = 0, maxPieceAmount = maxPieceAmount)

        Assert.assertEquals(
            HandChange(
                deltaCurrentKitten = -1,
                deltaMaxKitten = 0,
                deltaCurrentCat = 0,
                deltaMaxCat = 0
            ),
            result.handChanges[0],
        )
    }

    @Test
    fun `SET_CAT decrements acting player's cat count by one and touches nothing else`() {
        val board = board("...")
        val move = Move(0, 0, MoveType.SET_CAT)

        val result = board.applyMove(move, playerIndex = 0, maxPieceAmount = maxPieceAmount)

        Assert.assertEquals(
            HandChange(
                deltaCurrentKitten = 0,
                deltaMaxKitten = 0,
                deltaCurrentCat = -1,
                deltaMaxCat = 0
            ),
            result.handChanges[0],
        )
    }

    @Test
    fun `SET_KITTEN by second player decrements second player's kitten count`() {
        val board = board("...")
        val move = Move(0, 0, MoveType.SET_KITTEN)

        val result = board.applyMove(move, playerIndex = 1, maxPieceAmount = maxPieceAmount)

        Assert.assertEquals(HandChange.emptyHand(), result.handChanges[0])
        Assert.assertEquals(
            HandChange(-1, 0, 0, 0),
            result.handChanges[1],
        )
        Assert.assertEquals(1, result.board.cells[0][0].ownerOrNull)
    }



    //-------------------------------------------------------------------------
    //The other player's hand must remain empty regardless
    //-------------------------------------------------------------------------
    @Test
    fun `setting a piece does not change the other player's hand`() {
        val board = board("...")
        val move = Move(0, 0, MoveType.SET_KITTEN)

        val result = board.applyMove(move, playerIndex = 0, maxPieceAmount = maxPieceAmount)

        Assert.assertEquals(HandChange.emptyHand(), result.handChanges[1])
    }



    //-------------------------------------------------------------------------
    //No game-over, no triplet side effects for a single piece
    //-------------------------------------------------------------------------
    @Test
    fun `setting a single piece does not end the game`() {
        val board = board("...")
        val move = Move(0, 0, MoveType.SET_KITTEN)

        val result = board.applyMove(move, playerIndex = 0, maxPieceAmount = maxPieceAmount)

        assertNull(result.winnerIndex)
    }

    @Test
    fun `setting a single piece produces no deletable triplets`() {
        val board = board("...")
        val move = Move(0, 0, MoveType.SET_KITTEN)

        val result = board.applyMove(move, playerIndex = 0, maxPieceAmount = maxPieceAmount)

        Assert.assertTrue(result.deletableTriplets.isEmpty())
    }



    //-------------------------------------------------------------------------
    //Exactly one AddPiece effect, with the right payload
    //-------------------------------------------------------------------------
    @Test
    fun `setting a kitten produces exactly one AddPiece effect`() {
        val board = board("...")
        val move = Move(0, 2, MoveType.SET_KITTEN)

        val result = board.applyMove(move, playerIndex = 1, maxPieceAmount = maxPieceAmount)

        val effect = result.pendingEffects.single()
        Assert.assertTrue(
            "expected AddPiece, got ${effect::class.simpleName}",
            effect is VisualEffect.AddPiece
        )
        val addPiece = effect as VisualEffect.AddPiece
        Assert.assertEquals(0, addPiece.row)
        Assert.assertEquals(2, addPiece.column)
        Assert.assertEquals(1, addPiece.owner)
        Assert.assertEquals(PieceType.KITTEN, addPiece.type)
    }

    @Test
    fun `setting a cat produces exactly one AddPiece effect`() {
        val board = board("...")
        val move = Move(0, 2, MoveType.SET_CAT)

        val result = board.applyMove(move, playerIndex = 1, maxPieceAmount = maxPieceAmount)

        val effect = result.pendingEffects.single()
        Assert.assertTrue(
            "expected AddPiece, got ${effect::class.simpleName}",
            effect is VisualEffect.AddPiece
        )
        val addPiece = effect as VisualEffect.AddPiece
        Assert.assertEquals(PieceType.CAT, addPiece.type)
        Assert.assertEquals(1, addPiece.owner)
    }



    //-------------------------------------------------------------------------
    //Pre-existing pieces survive the move untouched
    //-------------------------------------------------------------------------
    @Test
    fun `setting a piece leaves unrelated cells of the board untouched`() {
        //Pieces at (0,0) and (5,5); the target (3,3) is Manhattan-distance 6
        //from (0,0) and Manhattan-distance 4 from (5,5), so no boop can fire.
        val board = board(
            "k.....",
            "......",
            "......",
            "......",
            "......",
            ".....C",
        )
        val move = Move(3, 3, MoveType.SET_KITTEN)

        val result = board.applyMove(move, playerIndex = 0, maxPieceAmount = maxPieceAmount)

        //new piece
        result.board.assertCellPiece(3, 3, PieceType.KITTEN, 0)
        //pre-existing pieces preserved, with their original owners
        result.board.assertCellPiece(0, 0, PieceType.KITTEN, 0)
        result.board.assertCellPiece(5, 5, PieceType.CAT, 1)
        //exactly one new piece appeared
        val filled = result.board.cells.sumOf { row -> row.count { it !is Cell.Empty } }
        Assert.assertEquals(3, filled)
    }



    //-------------------------------------------------------------------------
    //Board is a value, not a mutable buffer
    //-------------------------------------------------------------------------
    @Test
    fun `applyMove does not mutate the original board`() {
        val original = board(
            "k...",
            "....",
        )
        original.applyMove(Move(1, 1, MoveType.SET_KITTEN), playerIndex = 1, maxPieceAmount = maxPieceAmount)

        original.assertCellEmpty(1, 1)
        original.assertCellPiece(0, 0, PieceType.KITTEN, 0)
    }
}