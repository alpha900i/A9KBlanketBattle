package com.alpha900i.a9kblanketbattle.data

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