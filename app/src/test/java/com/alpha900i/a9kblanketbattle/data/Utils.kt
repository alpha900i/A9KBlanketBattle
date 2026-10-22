package com.alpha900i.a9kblanketbattle.data

fun board(vararg rows: String): Board = Board(
    rows.map { row ->
        row.map { c ->
            when (c) {
                '.' -> Cell.emptyCell()
                'k' -> Cell(CellType.KITTEN, 0)
                'c' -> Cell(CellType.CAT, 0)
                'K' -> Cell(CellType.KITTEN, 1)
                'C' -> Cell(CellType.CAT, 1)
                else -> error("bad cell $c")
            }
        }
    }
)