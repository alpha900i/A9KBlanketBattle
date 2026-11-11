package com.alpha900i.a9kblanketbattle.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

object PlayerColors {
    val P1 = Color.Blue
    val P2 = Color.Red
    val None = Color.Yellow

    fun of(owner: Int?): Color = when (owner) {
        0 -> P1
        1 -> P2
        else -> None
    }
}
//some of the colors below can eventually go to the theme.kt
object ButtonColors {
    val ButtonBlocked = Color.Red
    val ButtonActive = Color.Green
    val DefaultTint = Color.Black

    fun of(enabled: Boolean): Color {
        return if (enabled) {
            ButtonActive
        } else {
            ButtonBlocked
        }
    }
}
object PieceColors {
    val Border = Color.White
    val DefaultBackground = Color.Gray
    val HighlightBackground = Color.Magenta
    val SuperHighlightBackground = Color.Green
}
object PieceButtonColors {
    val Active = Color.Blue
    val Inactive = Color.White

    fun of(active: Boolean): Color {
        return if (active) {
            Active
        } else {
            Inactive
        }
    }
}
object UIColors {
    val Background = Color.White
    val Border = Color.Gray
}
object TripletHighlightColors {
    val Selected = Color.Green
    val NotSelected = Color.Gray

    fun of(selected: Boolean): Color {
        return if (selected) {
            Selected
        } else {
            NotSelected
        }
    }
}