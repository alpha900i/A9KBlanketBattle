package com.alpha900i.a9kblanketbattle.ui.screens

import android.util.Log
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
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alpha900i.a9kblanketbattle.R
import com.alpha900i.a9kblanketbattle.data.CellType
import com.alpha900i.a9kblanketbattle.data.GameState
import com.alpha900i.a9kblanketbattle.data.Hand
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
    var moveType by remember { mutableStateOf<MoveType?>(null) }
    LaunchedEffect(gameState.activePlayerIndex) {
        moveType = null
    }
    Column(
        modifier = Modifier
            .fillMaxSize(1f)
    ) {
        BoardSection(
            gameState = gameState,
            isHumanTurn = isHumanTurn,
            submitMove = submitMove,
            moveType = moveType,
            modifier = Modifier.weight(4f)
        )
        HandsSection(
            gameState = gameState,
            isHumanTurn = isHumanTurn,
            setKittenMove = {
                moveType = MoveType.KITTEN
            },
            setCatMove = {
                moveType = MoveType.CAT
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun BoardSection(
    gameState: GameState,
    isHumanTurn: Boolean,
    submitMove: (Move) -> Unit,
    moveType: MoveType?,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth(1f)
    ) {
        gameState.board.cells.forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier
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
                                enabled = isHumanTurn && moveType != null,
                                onClick = {
                                    submitMove(
                                        Move(rowIndex, colIndex, moveType!!)
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

@Composable
fun HandsSection(
    gameState: GameState,
    isHumanTurn: Boolean,
    setKittenMove: () -> Unit,
    setCatMove: () -> Unit,
    modifier: Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(1f)
    ) {
        gameState.hands.forEachIndexed { index, hand ->
            HandBlock(
                hand = hand,
                isActiveHand = isHumanTurn && gameState.activePlayerIndex == index,
                handIndex = index,
                setKittenMove = setKittenMove,
                setCatMove = setCatMove,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun HandBlock(
    hand: Hand,
    isActiveHand: Boolean,
    handIndex: Int,
    setKittenMove: () -> Unit,
    setCatMove: () -> Unit,
    modifier: Modifier
) {
    var kittenActive by remember(isActiveHand) { mutableStateOf(false) }
    var catActive by remember(isActiveHand) { mutableStateOf(false) }

    Column(
        modifier = modifier
            .border(1.dp, Color.Gray)
            .background(Color.White)
    ) {
        Text(
            text = "Player #$handIndex",
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth(1f)
        )
        Row(
            modifier = Modifier.weight(1f)
        ) {
            PieceBlock(
                "Kitten",
                hand.kittenCurrent,
                hand.kittenMax,
                isActiveHand = isActiveHand,
                isActivePiece = kittenActive,
                onClick = {
                    kittenActive = true
                    catActive = false
                    setKittenMove()
                },
                modifier = Modifier.weight(1f)
            )
            PieceBlock(
                "Cat",
                hand.catCurrent,
                hand.catMax,
                isActiveHand = isActiveHand,
                isActivePiece = catActive,
                onClick = {
                    kittenActive = false
                    catActive = true
                    setCatMove()
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun PieceBlock(
    title: String,
    current: Int,
    max: Int,
    isActiveHand: Boolean,
    isActivePiece: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {

    Log.d("TEST", "PieceBlock $title build $isActivePiece")
    val pieceColor = if (isActivePiece) Color.Blue else Color.White
    Column(
        modifier = modifier
            .fillMaxSize(1f)
            .border(1.dp, Color.Gray)
            .background(pieceColor)
            .clickable(
                enabled = isActiveHand && current > 0,
                onClick = onClick
            )
    ) {
        Text(
            text = title,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth(1f)
                .border(1.dp, Color.Gray)
        )
        BasicText(
            text = "$current/$max",
            autoSize = TextAutoSize.StepBased(
                minFontSize = 12.sp,
                maxFontSize = 64.sp,   // default is 112.sp if not set
                stepSize = 1.sp        // granularity for scaling steps
            ),
            modifier = Modifier
                .fillMaxWidth(1f)
                .border(1.dp, Color.Gray)
        )
    }
}
