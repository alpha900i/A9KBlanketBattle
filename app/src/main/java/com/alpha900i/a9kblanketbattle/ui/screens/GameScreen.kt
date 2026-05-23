package com.alpha900i.a9kblanketbattle.ui.screens

import android.media.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.alpha900i.a9kblanketbattle.data.Board
import com.alpha900i.a9kblanketbattle.data.Cell

@Composable
fun GameScreen(
    board: Board
) {
    Column(
        modifier = Modifier
            .fillMaxSize(1f)
    ) {
        for (row in board.cells) {
            Row(modifier = Modifier
                .weight(1f)
                .fillMaxWidth(1f)
            ) {
                for (cell in row) {
                    val color = when (cell) {
                        Cell.EMPTY -> Color.Gray
                        Cell.KITTEN -> Color.Yellow
                        Cell.CAT -> Color.Red
                    }
                    Box(Modifier
                        .background(color = color)
                        .weight(1f)
                        .fillMaxHeight(1f)
                        .border(width = 4.dp, color = Color.White)
                    )
                }
            }
        }
    }
}
