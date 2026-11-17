package com.alpha900i.a9kblanketbattle.data

import kotlin.test.assertEquals
import kotlin.test.assertTrue

fun board(vararg rows: String): Board = Board(
    rows.map { row ->
        row.map { c ->
            when (c) {
                '.' -> Cell.Empty
                'k' -> Cell.Piece(PieceType.KITTEN, 0)
                'c' -> Cell.Piece(PieceType.CAT, 0)
                'K' -> Cell.Piece(PieceType.KITTEN, 1)
                'C' -> Cell.Piece(PieceType.CAT, 1)
                else -> error("bad cell $c")
            }
        }
    }
)

fun Board.assertCellEmpty(row: Int, col: Int) {
    assertTrue(cells[row][col] is Cell.Empty, "Not empty cell at ($row,$col)")
}
fun Board.assertCellPiece(row: Int, col: Int, type: PieceType, owner: Int) {
    assertTrue(cells[row][col] is Cell.Piece, "Not piece cell at ($row,$col)")
    assertEquals(type, getCellType(row, col), "type at ($row,$col)")
    assertEquals(owner, cells[row][col].ownerOrNull, "owner at ($row,$col)")
}
fun MoveResult.removePieces(): List<VisualEffect.RemovePiece> =
    pendingEffects.filterIsInstance<VisualEffect.RemovePiece>()