package com.alpha900i.a9kblanketbattle.data

/**
 * Description of possible animation effects
 */
sealed class VisualEffect {
    /**
     * Piece of [type] belonging to player #[owner] is moving from [fromRow]x[fromColumn] to [toRow]x[toColumn]
     */
    data class MovePiece(
        val fromRow: Int,
        val fromColumn: Int,
        val toRow: Int,
        val toColumn: Int,
        val owner: Int,
        val type: CellType
    ) : VisualEffect()
    /**
     * New piece of [type] belonging to player #[owner] appears on [row]x[column]
     */
    data class AddPiece(
        val row: Int,
        val column: Int,
        val owner: Int,
        val type: CellType
    ) : VisualEffect()
    /**
     * Piece of [type] belonging to player #[owner] removed from [row]x[column]
     */
    data class RemovePiece(
        val row: Int,
        val column: Int,
        val owner: Int,
        val type: CellType
    ) : VisualEffect()
}
