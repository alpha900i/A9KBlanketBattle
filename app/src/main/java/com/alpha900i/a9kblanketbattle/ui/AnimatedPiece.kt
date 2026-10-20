package com.alpha900i.a9kblanketbattle.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import com.alpha900i.a9kblanketbattle.data.CellType

/**
 * Basic sealed class for animated piece
 * [pos] is starting position (in board terms, not in coordinates)
 * [owner] is number of player, that owns this piece
 * [type] is type of animated piece
 * [offset] is Animatable, keeping current progress in animation
 */
sealed class AnimatedPiece(
    open val pos: Pair<Int, Int>,
    open val owner: Int,
    open val type: CellType,
    open val offset: Animatable<Float, AnimationVector1D>
) {
    /**
     * Piece that moves after being booped
     */
    data class MovingPiece(
        val startPos: Pair<Int, Int>,
        val endPos: Pair<Int, Int>,
        override val owner: Int,
        override val type: CellType,
        override val offset: Animatable<Float, AnimationVector1D>
    ) : AnimatedPiece(startPos, owner, type, offset)

    /**
     * Newly-added piece, that just appears on the screen
     */
    data class AppearingPiece(
        override val pos: Pair<Int, Int>,
        override val owner: Int,
        override val type: CellType,
        override val offset: Animatable<Float, AnimationVector1D>
    ) : AnimatedPiece(pos, owner, type, offset)

    /**
     * Piece that removed or booped of the board
     */
    data class DisappearingPiece(
        override val pos: Pair<Int, Int>,
        override val owner: Int,
        override val type: CellType,
        override val offset: Animatable<Float, AnimationVector1D>
    ) : AnimatedPiece(pos, owner, type, offset)
}
