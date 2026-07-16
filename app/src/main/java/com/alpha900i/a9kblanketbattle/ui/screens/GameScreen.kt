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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alpha900i.a9kblanketbattle.R
import com.alpha900i.a9kblanketbattle.data.CellType
import com.alpha900i.a9kblanketbattle.data.GameState
import com.alpha900i.a9kblanketbattle.data.Hand
import com.alpha900i.a9kblanketbattle.data.TripletOnBoard
import com.alpha900i.a9kblanketbattle.domain.Move
import com.alpha900i.a9kblanketbattle.domain.MoveType
import com.alpha900i.a9kblanketbattle.ui.InfoSectionState

@Composable
fun GameScreen(
    gameState: GameState,
    isHumanTurn: Boolean,
    infoSectionMessage: InfoSectionState,
    activator: () -> Unit,
    submitMove: (Move) -> Unit,
    submitRemoval: (TripletOnBoard) -> Unit,
) {
    LaunchedEffect(Unit) {
        activator()
    }
    var moveType by remember(gameState.activePlayerIndex) { mutableStateOf<MoveType?>(null) }
    var highlightedTriplet by remember { mutableStateOf<TripletOnBoard?>(null) }
    Column(
        modifier = Modifier
            .fillMaxSize(1f)
    ) {
        InfoSection(
            infoSectionMessage = infoSectionMessage
        )
        BoardSection(
            gameState = gameState,
            highlightedTriplet = highlightedTriplet,
            isHumanTurn = isHumanTurn,
            submitMove = submitMove,
            moveType = moveType,
            modifier = Modifier.weight(4f)
        )
        if (gameState.deletableTriplets.isEmpty()) {
            HandsSection(
                gameState = gameState,
                isHumanTurn = isHumanTurn,
                setKittenMove = {
                    Log.d("HandBlock", "Set kitten move")
                    moveType = MoveType.KITTEN
                },
                setCatMove = {
                    Log.d("HandBlock", "Set cat move")
                    moveType = MoveType.CAT
                },
                modifier = Modifier.weight(1f)
            )
        } else {
            TripletRemovalSection(
                gameState = gameState,
                isHumanTurn = isHumanTurn,
                highlightTriplet = { triplet ->
                    highlightedTriplet = triplet
                },
                submitRemoval = submitRemoval,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun InfoSection(infoSectionMessage: InfoSectionState) {
    Text(
        text = stringResource(infoSectionMessage.resourceId, *infoSectionMessage.formatArgs),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(1f)
    )
}

@Composable
fun BoardSection(
    gameState: GameState,
    highlightedTriplet: TripletOnBoard?,
    isHumanTurn: Boolean,
    submitMove: (Move) -> Unit,
    moveType: MoveType?,
    modifier: Modifier
) {
    val superHighlightedCells: Set<Pair<Int, Int>> = remember (highlightedTriplet) {
        getHighlights(setOf(highlightedTriplet))
    }
    val highlightedCells: Set<Pair<Int, Int>>  = remember (gameState.deletableTriplets) {
        getHighlights(gameState.deletableTriplets)
    }
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
                    val backgroundColor =
                        if (superHighlightedCells.contains(Pair(rowIndex, colIndex))) {
                            Color.Green
                        } else if (highlightedCells.contains(Pair(rowIndex, colIndex))) {
                            Color.Magenta
                        } else {
                            Color.Gray
                        }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(4.dp, Color.White)
                            .background(backgroundColor)
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

fun getHighlights(triplets: Set<TripletOnBoard?>): Set<Pair<Int, Int>> =
    triplets
        .filterNotNull()
        .flatMap { triplet ->
        triplet.getCells()
    }.toSet()


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
fun TripletRemovalSection(
    gameState: GameState,
    isHumanTurn: Boolean,
    highlightTriplet: (TripletOnBoard?) -> Unit,
    submitRemoval: (TripletOnBoard) -> Unit,
    modifier: Modifier
) {
    var selectedTriplet: TripletOnBoard? by remember{mutableStateOf(null)}
    Column(modifier = modifier.fillMaxWidth(1f)) {
        Row(
            modifier = modifier
                .fillMaxWidth(1f)
                .weight(1f)
        ) {
            gameState.deletableTriplets.forEach { tripletOnBoard ->
                val buttonColor = if (tripletOnBoard == selectedTriplet) {
                    Color.Green
                } else {
                    Color.Gray
                }
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonColor
                    ),
                    onClick = {
                        highlightTriplet(tripletOnBoard)
                        selectedTriplet = tripletOnBoard
                    },
                    enabled = isHumanTurn,
                    modifier = modifier
                        .fillMaxHeight(1f)
                ) {
                    Text(
                        text = "Row ${tripletOnBoard.row} Column ${tripletOnBoard.column}"
                    )
                }
            }

        }
        Button(
            onClick = {
                submitRemoval(selectedTriplet!!)
                highlightTriplet(null)
            },
            enabled = (selectedTriplet != null) && isHumanTurn,
            modifier = modifier.fillMaxWidth(1f)
        ) {
            Text(
                text = "Remove selected"
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
