package com.alpha900i.a9kblanketbattle.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import com.alpha900i.a9kblanketbattle.data.CellType

sealed class AnimatedPiece(
    open val pos: Pair<Int, Int>,
    open val owner: Int,
    open val type: CellType,
    open val offset: Animatable<Float, AnimationVector1D>
) {
    data class MovingPiece(
        val startPos: Pair<Int, Int>,
        val endPos: Pair<Int, Int>,
        override val owner: Int,
        override val type: CellType,
        override val offset: Animatable<Float, AnimationVector1D>
    ) : AnimatedPiece(startPos, owner, type, offset)

    data class AppearingPiece(
        override val pos: Pair<Int, Int>,
        override val owner: Int,
        override val type: CellType,
        override val offset: Animatable<Float, AnimationVector1D>
    ) : AnimatedPiece(pos, owner, type, offset)

    data class DisappearingPiece(
        override val pos: Pair<Int, Int>,
        override val owner: Int,
        override val type: CellType,
        override val offset: Animatable<Float, AnimationVector1D>
    ) : AnimatedPiece(pos, owner, type, offset)
}
