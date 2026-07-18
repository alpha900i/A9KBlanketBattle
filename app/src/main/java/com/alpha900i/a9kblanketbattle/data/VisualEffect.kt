package com.alpha900i.a9kblanketbattle.data

sealed class VisualEffect {
    data class MovePiece(
        val fromRow: Int,
        val fromColumn: Int,
        val toRow: Int,
        val toColumn: Int,
        val owner: Int,
        val type: CellType
    ) : VisualEffect()
    data class RemovePiece(
        val row: Int,
        val column: Int
    ) : VisualEffect()

    companion object {
        fun emptyList() : List<VisualEffect>
                = listOf()
    }
}
