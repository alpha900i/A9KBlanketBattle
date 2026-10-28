package com.alpha900i.a9kblanketbattle.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
import com.alpha900i.a9kblanketbattle.ui.GameScreenActions
import com.alpha900i.a9kblanketbattle.ui.InfoSectionState
import com.alpha900i.a9kblanketbattle.ui.TempMessageType
import com.alpha900i.a9kblanketbattle.util.CustomLog
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun GameScreen(
    gameState: GameState,
    isHumanTurn: Boolean,
    selectedMoveType: MoveType?,
    actions: GameScreenActions,
    infoSectionMessage: InfoSectionState,
    startNewGame: () -> Unit,
    cancelGame: () -> Unit,
    submitMove: (Move) -> Unit,
    submitRemoval: (TripletOnBoard) -> Unit,
    onAnimationComplete: () -> Unit
) {
    var cancelConfirmationNeeded by rememberSaveable { mutableStateOf(false)}
    var gameOverDialogClosed by rememberSaveable(gameState.winnerIndex) { mutableStateOf(false) }

    BackHandler(enabled = true) {
        cancelConfirmationNeeded = gameState.gameInProgress()
        if (!cancelConfirmationNeeded) {
            cancelGame()
        }
    }
    if (cancelConfirmationNeeded) {
        CancelConfirmationAlertDialog(
            confirmAction = {
                cancelConfirmationNeeded = false
                cancelGame()
            },
            cancelAction = {
                cancelConfirmationNeeded = false
            }
        )
    }
    if (gameState.winnerIndex != -1 && !gameOverDialogClosed) {
        GameOverDialog(
            winnerIndex = gameState.winnerIndex,
            confirmAction = {
                gameOverDialogClosed = true
                startNewGame()
            },
            cancelAction = {
                gameOverDialogClosed = true
            }
        )
    }


    LaunchedEffect(Unit) {
        startNewGame()
    }
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
            moveType = selectedMoveType,
            modifier = Modifier.weight(4f)
        )
        if (gameState.deletableTriplets.isEmpty()) {
            HandsSection(
                gameState = gameState,
                isHumanTurn = isHumanTurn,
                setKittenMove = { actions.selectMoveType(MoveType.SET_KITTEN) },
                setCatMove = { actions.selectMoveType(MoveType.SET_CAT) },
                setPromoteKittenMove = { actions.selectMoveType(MoveType.PROMOTE_KITTEN) },
                setReturnCatMove = { actions.selectMoveType(MoveType.RETURN_CAT) },
                setTempMessage = actions::setTempMessage,
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
fun CancelConfirmationAlertDialog(
    confirmAction: () -> Unit,
    cancelAction: () -> Unit
) {
    AlertDialog(
        onDismissRequest = cancelAction,
        title = { Text(stringResource(R.string.confirm_button_title)) },
        text = { Text(stringResource(R.string.game_cancel_confirmation)) },
        confirmButton = {
            Button(onClick = confirmAction) {
                Text(stringResource(R.string.yes_button_title))
            }
        },
        dismissButton = {
            Button(onClick = cancelAction) {
                Text(stringResource(R.string.no_button_title))
            }
        }
    )
}
@Composable
fun GameOverDialog(
    winnerIndex: Int,
    confirmAction: () -> Unit,
    cancelAction: () -> Unit
) {
    Dialog(
        onDismissRequest = cancelAction
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = dimensionResource(R.dimen.dialog_elevation)
        ) {
            Column(
                modifier = Modifier.padding(dimensionResource(R.dimen.dialog_padding))
            ) {
                Text(
                    text = stringResource(R.string.game_over_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier
                        .padding(bottom = dimensionResource(R.dimen.dialog_title_padding))
                        .align(Alignment.CenterHorizontally)
                )
                Image(
                    painter = painterResource(R.drawable.ic_cat),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(colorByCellOwner(winnerIndex)),
                    modifier = Modifier
                        .size(dimensionResource(R.dimen.dialog_image_size))
                        .align(Alignment.CenterHorizontally)
                )
                Text(
                    text = stringResource(R.string.game_over_description, winnerIndex + 1),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier
                    .fillMaxWidth(1f)
                    .height(dimensionResource(R.dimen.dialog_spacing)))
                Row(
                    modifier = Modifier.height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dialog_spacing)),
                ) {
                    Button(
                        onClick = confirmAction,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(1f)
                    ) {
                        Text(stringResource(R.string.new_game_button_title))
                    }
                    Button(
                        onClick = cancelAction,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(1f)
                    ) {
                        Text(stringResource(R.string.stay_on_board_button_title))
                    }
                }
            }
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
    val contentDescription = contentDescriptionByCellType(piece.type)
    if (painter != null) {
        Box(
            modifier = Modifier
                .height(cellHeightDp)
                .width(cellWidthDp)
                .offset { IntOffset(x = actualX, y = actualY) }
                .border(dimensionResource(R.dimen.animated_piece_border_width), Color.White)
                .background(Color.Gray)
        ) {
            Image(
                painter = painter,
                contentDescription = contentDescription,
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
    val contentDescription = contentDescriptionByCellType(piece.type)
    if (painter != null) {
        Box(
            modifier = Modifier
                .height(cellHeightDp)
                .width(cellWidthDp)
                .offset { IntOffset(x = offsetX, y = offsetY) }
                .border(dimensionResource(R.dimen.animated_piece_border_width), Color.White)
                .alpha(alpha)
                .scale(scale)
                .background(Color.Gray)
        ) {
            Image(
                painter = painter,
                contentDescription = contentDescription,
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
    val contentDescription = contentDescriptionByCellType(piece.type)
    if (painter != null) {
        Box(
            modifier = Modifier
                .height(cellHeightDp)
                .width(cellWidthDp)
                .offset { IntOffset(x = offsetX, y = offsetY) }
                .border(dimensionResource(R.dimen.animated_piece_border_width), Color.White)
                .alpha(alpha)
                .scale(scale)
                .background(Color.Gray)
        ) {
            Image(
                painter = painter,
                contentDescription = contentDescription,
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
                    val contentDescription = contentDescriptionByCellType(cell.type)
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
                            .border(dimensionResource(R.dimen.cell_border_width), Color.White)
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
                                contentDescription = contentDescription,
                                contentScale = ContentScale.Inside,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(dimensionResource(R.dimen.piece_padding)),
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
private fun contentDescriptionByCellType(cellType: CellType): String = when (cellType) {
    CellType.CAT -> stringResource(R.string.cell_with_cat_piece_content_description)
    CellType.KITTEN -> stringResource(R.string.cell_with_kitten_piece_content_description)
    CellType.EMPTY -> stringResource(R.string.empty_cell_content_description)
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
        return false
    }
    return when (moveType) {
        null -> false
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
    setTempMessage: (Int, TempMessageType) -> Unit,
    modifier: Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(1f)
    ) {
        gameState.hands.forEachIndexed { index, hand ->
            HandBlock(
                hand = hand,
                moveCount = gameState.moveCount,
                isActiveHand = isHumanTurn && gameState.activePlayerIndex == index,
                handIndex = index,
                setKittenMove = setKittenMove,
                setCatMove = setCatMove,
                setPromoteKittenMove = setPromoteKittenMove,
                setReturnCatMove = setReturnCatMove,
                setTempMessage = setTempMessage,
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
    moveCount: Int,
    isActiveHand: Boolean,
    handIndex: Int,
    setKittenMove: () -> Unit,
    setCatMove: () -> Unit,
    setPromoteKittenMove: () -> Unit,
    setReturnCatMove: () -> Unit,
    setTempMessage: (Int, TempMessageType) -> Unit,
    modifier: Modifier
) {
    CustomLog.d("HandBlock $handIndex $isActiveHand $hand")
    var kittenIsDefault by remember( moveCount,hand) {
        CustomLog.d("kittenIsDefault $handIndex $isActiveHand $hand - ${hand.kittenCurrent > 0}")
        mutableStateOf(hand.kittenCurrent > 0)
    }
    var catIsDefault by remember( moveCount,hand) {
        CustomLog.d("catIsDefault $handIndex $isActiveHand $hand - ${hand.kittenCurrent == 0 && hand.catCurrent > 0}")
        mutableStateOf(hand.kittenCurrent == 0 && hand.catCurrent > 0)
    }
    var kittenActive by remember(moveCount, isActiveHand, kittenIsDefault) { mutableStateOf(kittenIsDefault) }
    var catActive by remember(moveCount, isActiveHand, catIsDefault) { mutableStateOf(catIsDefault) }
    LaunchedEffect(moveCount, kittenIsDefault, catIsDefault, isActiveHand) {
        if (isActiveHand) {
            CustomLog.d("Hand", "Launched effect $kittenIsDefault $catIsDefault")
            if (kittenIsDefault) {
                setKittenMove()
            }
            if (catIsDefault) {
                setCatMove()
            }
        }
    }

    Column(
        modifier = modifier
            .border(dimensionResource(R.dimen.controls_section_border_width), Color.Gray)
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
            val kittenIsEnabled = isActiveHand && hand.kittenCurrent > 0
            val catIsEnabled = isActiveHand && hand.catCurrent > 0
            GeneralButton(
                onClick = {
                    if (isActiveHand) {
                        if (kittenIsEnabled) {
                            kittenActive = true
                            catActive = false
                            setKittenMove()
                        } else {
                            setTempMessage(handIndex, TempMessageType.CANT_SET_KITTEN)
                        }
                    }
                },
                enabled = kittenIsEnabled,
                modifier = Modifier.weight(1f)
            ) {
                PieceBlock(
                    painterByCellType(cellType = CellType.KITTEN),
                    contentDescriptionByCellType(cellType = CellType.KITTEN),
                    hand.kittenCurrent,
                    hand.kittenMax,
                    isActivePiece = kittenActive && isActiveHand,
                    modifier = Modifier.weight(1f)
                )
            }
            GeneralButton(
                onClick = {
                    if (isActiveHand) {
                        if (catIsEnabled) {
                            kittenActive = false
                            catActive = true
                            setCatMove()
                        } else {
                            setTempMessage(handIndex, TempMessageType.CANT_SET_CAT)
                        }
                    }
                },
                enabled = catIsEnabled,
                modifier = Modifier.weight(1f)
            ) {
                PieceBlock(
                    painterByCellType(cellType = CellType.CAT),
                    contentDescriptionByCellType(cellType = CellType.CAT),
                    hand.catCurrent,
                    hand.catMax,
                    isActivePiece = catActive && isActiveHand,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Row(
            modifier = Modifier.weight(1f)
        ) {
            val canPromoteKitten = (isActiveHand && hand.kittenCurrent == 0 && hand.catCurrent == 0 && hand.kittenMax != 0)
            val canReturnCat = (isActiveHand && hand.kittenCurrent == 0 && hand.catCurrent == 0 && hand.catMax != 0)
            GeneralButton(
                onClick = {
                    if (isActiveHand) {
                        if (canPromoteKitten) {
                            setPromoteKittenMove()
                        } else {
                            setTempMessage(handIndex, TempMessageType.CANT_PROMOTE_KITTEN)
                        }
                    }
                },
                enabled = canPromoteKitten,
                modifier = Modifier.weight(1f)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_promote),
                    contentDescription = stringResource(R.string.promote_kitten_button_content_description),
                    colorFilter = ColorFilter.tint(Color.Black),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(1f)
                )
            }
            GeneralButton(
                onClick = {
                    if (isActiveHand) {
                        if (canReturnCat) {
                            setReturnCatMove()
                        } else {
                            setTempMessage(handIndex, TempMessageType.CANT_RETURN_CAT)
                        }
                    }
                },
                enabled = canReturnCat,
                modifier = Modifier.weight(1f)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_return_cat),
                    contentDescription = stringResource(R.string.return_cat_button_content_description),
                    colorFilter = ColorFilter.tint(Color.Black),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(1f)
                )
            }
        }
    }
}

@Composable
fun PieceBlock(
    painter: Painter?,
    contentDescription: String,
    current: Int,
    max: Int,
    isActivePiece: Boolean,
    modifier: Modifier,
) {
    val pieceColor = if (isActivePiece) Color.Blue else Color.White
    Row(
        modifier = modifier
            .fillMaxSize(1f)
            .border(dimensionResource(R.dimen.controls_section_border_width), Color.Gray)
            .background(pieceColor)
    ) {
        if (painter != null) {
            Image(
                painter = painter,
                contentDescription = contentDescription,
                colorFilter = ColorFilter.tint(Color.Black),
                modifier = Modifier
                    .fillMaxHeight(1f)
                    .weight(1f)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxHeight(1f)
                .border(dimensionResource(R.dimen.controls_section_border_width), Color.Gray)
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = "$current/$max",
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 12.sp,
                    maxFontSize = 32.sp,
                    stepSize = 1.sp,
                ),
                style = TextStyle(textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun GeneralButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    val borderColor = if (enabled) Color.Green else Color.Red
    Button(
        onClick = onClick,
        shape = RectangleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White
        ),
        contentPadding = PaddingValues(dimensionResource(R.dimen.no_padding_width)),
        modifier = modifier.border(dimensionResource(R.dimen.button_border_width), borderColor)
    ) {
        content()
    }
}
