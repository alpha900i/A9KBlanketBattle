package com.alpha900i.a9kblanketbattle.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.alpha900i.a9kblanketbattle.R
import com.alpha900i.a9kblanketbattle.data.CellType
import com.alpha900i.a9kblanketbattle.data.GameState
import com.alpha900i.a9kblanketbattle.domain.Move
import com.alpha900i.a9kblanketbattle.domain.MoveType

@Composable
fun GameScreen(
    gameState: GameState,
    isHumanTurn: Boolean,
    activator: () -> Unit,
    submitMove: (Move) -> Unit
) {
    LaunchedEffect(Unit) {
        activator()
    }

    Column(
        modifier = Modifier
            .fillMaxSize(1f)
    ) {
        gameState.board.cells.forEachIndexed { rowIndex, row ->
            Row(modifier = Modifier
                .weight(1f)
                .fillMaxWidth(1f)
            ) {
                row.forEachIndexed { colIndex, cell ->
                    val painter = when (cell.type) {
                        CellType.CAT -> painterResource(R.drawable.ic_cat)
                        CellType.KITTEN -> painterResource(R.drawable.ic_kitten)
                        CellType.EMPTY -> null
                    }
                    val playerColor = when (cell.owner) {
                        0 -> Color.Blue
                        1 -> Color.Red
                        else -> Color.Yellow
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(4.dp, Color.White)
                            .background(Color.Gray)
                            .clickable(
                                enabled = isHumanTurn,
                                onClick = {
                                    submitMove(
                                        Move(rowIndex, colIndex, MoveType.KITTEN)
                                    )
                                }
                            )
                    ) {
                        if (painter != null) {
                            Image(
                                painter = painter,
                                contentDescription = "Icon",
                                modifier = Modifier.fillMaxSize(),
                                colorFilter = ColorFilter.tint(playerColor)
                            )
                        }
                    }
                }
            }
        }
    }
}
