package com.alpha900i.a9kblanketbattle.ui.screens

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alpha900i.a9kblanketbattle.R
import com.alpha900i.a9kblanketbattle.data.Board
import com.alpha900i.a9kblanketbattle.data.Cell
import com.alpha900i.a9kblanketbattle.data.CellType
import com.alpha900i.a9kblanketbattle.data.GameState
import com.alpha900i.a9kblanketbattle.data.Hand
import com.alpha900i.a9kblanketbattle.data.TripletOnBoard
import com.alpha900i.a9kblanketbattle.data.VisualEffect
import com.alpha900i.a9kblanketbattle.domain.Move
import com.alpha900i.a9kblanketbattle.domain.MoveType
import com.alpha900i.a9kblanketbattle.ui.AnimatedPiece
import com.alpha900i.a9kblanketbattle.ui.Constants
import com.alpha900i.a9kblanketbattle.ui.InfoSectionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun GameScreen(
    gameState: GameState,
    isHumanTurn: Boolean,
    infoSectionMessage: InfoSectionState,
    startNewGame: () -> Unit,
    submitMove: (Move) -> Unit,
    submitRemoval: (TripletOnBoard) -> Unit,
    onAnimationComplete: () -> Unit
) {
    LaunchedEffect(Unit) {
        startNewGame()
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
        AnimatedBoardSection(
            gameState = gameState,
            highlightedTriplet = highlightedTriplet,
            isHumanTurn = isHumanTurn,
            submitMove = submitMove,
            onAnimationComplete = onAnimationComplete,
            moveType = moveType,
            modifier = Modifier.weight(4f)
        )
        if (gameState.deletableTriplets.isEmpty()) {
            HandsSection(
                gameState = gameState,
                isHumanTurn = isHumanTurn,
                setKittenMove = {
                    Log.d("HandBlock", "Set kitten move")
                    moveType = MoveType.SET_KITTEN
                },
                setCatMove = {
                    Log.d("HandBlock", "Set cat move")
                    moveType = MoveType.SET_CAT
                },
                setPromoteKittenMove = {
                    Log.d("HandBlock", "Set cat move")
                    moveType = MoveType.PROMOTE_KITTEN
                },
                setReturnCatMove = {
                    Log.d("HandBlock", "Set cat move")
                    moveType = MoveType.RETURN_CAT
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
fun AnimatedBoardSection(
    gameState: GameState,
    highlightedTriplet: TripletOnBoard?,
    isHumanTurn: Boolean,
    submitMove: (Move) -> Unit,
    onAnimationComplete: () -> Unit,
    moveType: MoveType?,
    modifier: Modifier
) {
    if (gameState.pendingEffects.isEmpty()) {
        BoardSection(
            gameState = gameState,
            board = gameState.board,
            hiddenCells = setOf(),
            highlightedTriplet = highlightedTriplet,
            isHumanTurn = isHumanTurn,
            submitMove = submitMove,
            moveType = moveType,
            modifier = modifier
        )
    } else {
        // State for ongoing animations
        var animatedPieces by remember(gameState.pendingEffects) { mutableStateOf(listOf<AnimatedPiece>()) }
        var containerSize by remember { mutableStateOf(IntSize.Zero) }

        // When effects change, start animations
        LaunchedEffect(gameState.pendingEffects) {
            animatedPieces = gameState.pendingEffects.map { effect ->
                when (effect) {
                    is VisualEffect.MovePiece ->
                        AnimatedPiece.MovingPiece(
                            startPos = (effect.fromRow to effect.fromColumn),
                            endPos = (effect.toRow to effect.toColumn),
                            owner = effect.owner,
                            type = effect.type,
                            offset = Animatable(0f, 0f)
                        )

                    is VisualEffect.AddPiece ->
                        AnimatedPiece.AppearingPiece(
                            pos = (effect.row to effect.column),
                            owner = effect.owner,
                            type = effect.type,
                            offset = Animatable(0f, 0f)
                        )

                    is VisualEffect.RemovePiece ->
                        AnimatedPiece.DisappearingPiece(
                            pos = (effect.row to effect.column),
                            owner = effect.owner,
                            type = effect.type,
                            offset = Animatable(0f, 0f)
                        )
                }

            }
            // Animate all in parallel
            coroutineScope {
                val animationJobs = mutableListOf<Job>()
                animatedPieces.forEach { piece ->
                    val job = launch {
                        piece.offset.animateTo(1f, animationSpec = tween(Constants.ANIMATION_TIME))
                    }
                    animationJobs.add(job)
                }
                animationJobs.joinAll()
                onAnimationComplete()
            }
        }

        Box(
            modifier = modifier
                .onGloballyPositioned { containerSize = it.size }
        ) {
            // Static board (old board) – but hide pieces that are moving
            val cellHeight = containerSize.height / gameState.board.getHeight()
            val cellWidth = containerSize.width / gameState.board.getWidth()
            val cellHeightDp = with(LocalDensity.current) { cellHeight.toDp() }
            val cellWidthDp = with(LocalDensity.current) { cellWidth.toDp() }

            val hiddenCells = animatedPieces.map { it.pos }.toSet()
            BoardSection(
                gameState = gameState,
                board = gameState.oldBoard,
                hiddenCells = hiddenCells,
                highlightedTriplet = highlightedTriplet,
                isHumanTurn = isHumanTurn,
                submitMove = submitMove,
                moveType = moveType,
                modifier = modifier
            )
            // Overlay for moving pieces
            animatedPieces.forEach { piece ->
                when (piece) {
                    is AnimatedPiece.MovingPiece -> AnimatedMovingPiece(
                        piece = piece,
                        cellWidth = cellWidth,
                        cellHeight = cellHeight,
                        cellWidthDp = cellWidthDp,
                        cellHeightDp = cellHeightDp
                    )

                    is AnimatedPiece.AppearingPiece -> AnimatedAppearingPiece(
                        piece = piece,
                        cellWidth = cellWidth,
                        cellHeight = cellHeight,
                        cellWidthDp = cellWidthDp,
                        cellHeightDp = cellHeightDp
                    )

                    is AnimatedPiece.DisappearingPiece -> AnimatedDisappearingPiece(
                        piece = piece,
                        cellWidth = cellWidth,
                        cellHeight = cellHeight,
                        cellWidthDp = cellWidthDp,
                        cellHeightDp = cellHeightDp
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedMovingPiece(
    piece: AnimatedPiece.MovingPiece,
    cellWidth: Int,
    cellHeight: Int,
    cellWidthDp: Dp,
    cellHeightDp: Dp
) {
    val progress = piece.offset.value
    val startRow = piece.startPos.first
    val startCol = piece.startPos.second
    val endRow = piece.endPos.first
    val endCol = piece.endPos.second

    val startOffsetX = startCol * cellWidth
    val endOffsetX = endCol * cellWidth
    val startOffsetY = startRow * cellHeight
    val endOffsetY = endRow * cellHeight
    val actualX = (startOffsetX + (endOffsetX - startOffsetX) * progress).roundToInt()
    val actualY = (startOffsetY + (endOffsetY - startOffsetY) * progress).roundToInt()

    val painter = painterByCellType(piece.type)
    if (painter != null) {
        Box(
            modifier = Modifier
                .height(cellHeightDp)
                .width(cellWidthDp)
                .offset { IntOffset(x = actualX, y = actualY) }
                .border(4.dp, Color.White)
                .background(Color.Gray)
        ) {
            Image(
                painter = painter,
                contentDescription = "Icon",
                modifier = Modifier.fillMaxSize(1f),
                colorFilter = ColorFilter.tint(colorByCellOwner(piece.owner)),
            )
        }
    }
}

@Composable
fun AnimatedAppearingPiece(
    piece: AnimatedPiece.AppearingPiece,
    cellWidth: Int,
    cellHeight: Int,
    cellWidthDp: Dp,
    cellHeightDp: Dp
) {
    val progress = piece.offset.value
    val row = piece.pos.first
    val column = piece.pos.second

    val offsetX = column * cellWidth
    val offsetY = row * cellHeight
    val alpha = progress
    val scale = progress

    val painter = painterByCellType(piece.type)
    if (painter != null) {
        Box(
            modifier = Modifier
                .height(cellHeightDp)
                .width(cellWidthDp)
                .offset { IntOffset(x = offsetX, y = offsetY) }
                .border(4.dp, Color.White)
                .alpha(alpha)
                .scale(scale)
                .background(Color.Gray)
        ) {
            Image(
                painter = painter,
                contentDescription = "Icon",
                modifier = Modifier.fillMaxSize(1f),
                colorFilter = ColorFilter.tint(colorByCellOwner(piece.owner)),
            )
        }
    }
}

@Composable
fun AnimatedDisappearingPiece(
    piece: AnimatedPiece.DisappearingPiece,
    cellWidth: Int,
    cellHeight: Int,
    cellWidthDp: Dp,
    cellHeightDp: Dp
) {
    val progress = piece.offset.value
    val row = piece.pos.first
    val column = piece.pos.second

    val offsetX = column * cellWidth
    val offsetY = row * cellHeight
    val alpha = 1.0f - progress
    val scale = 1.0f - progress

    val painter = painterByCellType(piece.type)
    if (painter != null) {
        Box(
            modifier = Modifier
                .height(cellHeightDp)
                .width(cellWidthDp)
                .offset { IntOffset(x = offsetX, y = offsetY) }
                .border(4.dp, Color.White)
                .alpha(alpha)
                .scale(scale)
                .background(Color.Gray)
        ) {
            Image(
                painter = painter,
                contentDescription = "Icon",
                modifier = Modifier.fillMaxSize(1f),
                colorFilter = ColorFilter.tint(colorByCellOwner(piece.owner)),
            )
        }
    }
}

//yes, we pass board as additional parameter, although it is in GameState
//thing is, there are two boards here - sometimes we need one, sometimes - another
@Composable
fun BoardSection(
    gameState: GameState,
    board: Board,
    hiddenCells: Set<Pair<Int, Int>>,
    highlightedTriplet: TripletOnBoard?,
    isHumanTurn: Boolean,
    submitMove: (Move) -> Unit,
    moveType: MoveType?,
    modifier: Modifier
) {
    val superHighlightedCells: Set<Pair<Int, Int>> = remember(highlightedTriplet) {
        getHighlights(setOf(highlightedTriplet))
    }
    val highlightedCells: Set<Pair<Int, Int>> = remember(gameState.deletableTriplets) {
        getHighlights(gameState.deletableTriplets)
    }
    Column(
        modifier = modifier
            .fillMaxWidth(1f)
    ) {
        board.cells.forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(1f)
            ) {
                row.forEachIndexed { colIndex, cell ->
                    val painter = if (hiddenCells.contains(rowIndex to colIndex)) {
                        null
                    } else painterByCellType(cell.type)
                    val playerColor = colorByCellOwner(cell.owner)
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
                                enabled = isCellEnabled(
                                    isHumanTurn,
                                    cell,
                                    moveType,
                                    gameState.activePlayerIndex
                                ),
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
private fun painterByCellType(cellType: CellType): Painter? = when (cellType) {
    CellType.CAT -> painterResource(R.drawable.ic_cat)
    CellType.KITTEN -> painterResource(R.drawable.ic_kitten)
    CellType.EMPTY -> null
}

@Composable
private fun colorByCellOwner(cellOwner: Int): Color = when (cellOwner) {
    0 -> Color.Blue
    1 -> Color.Red
    else -> Color.Yellow
}

@Composable
private fun isCellEnabled(
    isHumanTurn: Boolean,
    cell: Cell,
    moveType: MoveType?,
    activePlayerIndex: Int
): Boolean {
    if (!isHumanTurn) {
        return false;
    }
    if (moveType == null) {
        return false
    }
    return when (moveType) {
        MoveType.SET_KITTEN -> cell.type == CellType.EMPTY
        MoveType.SET_CAT -> cell.type == CellType.EMPTY
        MoveType.PROMOTE_KITTEN -> cell.type == CellType.KITTEN && cell.owner == activePlayerIndex
        MoveType.RETURN_CAT -> cell.type == CellType.CAT && cell.owner == activePlayerIndex
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
    setPromoteKittenMove: () -> Unit,
    setReturnCatMove: () -> Unit,
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
                setPromoteKittenMove = setPromoteKittenMove,
                setReturnCatMove = setReturnCatMove,
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
    var selectedTriplet: TripletOnBoard? by remember { mutableStateOf(null) }
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
    setPromoteKittenMove: () -> Unit,
    setReturnCatMove: () -> Unit,
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
            text = "Player #${handIndex + 1}",
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
        Row(
            modifier = Modifier.weight(1f)
        ) {
            Button(
                onClick = setPromoteKittenMove,
                enabled = (isActiveHand && hand.kittenCurrent == 0 && hand.catCurrent == 0 && hand.kittenMax != 0),
                shape = RectangleShape,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "K->C",
                )
            }
            Button(
                onClick = setReturnCatMove,
                enabled = (isActiveHand && hand.kittenCurrent == 0 && hand.catCurrent == 0 && hand.catMax != 0),
                shape = RectangleShape,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "C->0",
                )
            }
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
            style = TextStyle(
                textAlign = TextAlign.Center
            ),
            modifier = Modifier
                .fillMaxWidth(1f)
                .border(1.dp, Color.Gray)
        )
    }
}
