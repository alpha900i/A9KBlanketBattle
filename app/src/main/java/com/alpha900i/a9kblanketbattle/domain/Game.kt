package com.alpha900i.a9kblanketbattle.domain

import com.alpha900i.a9kblanketbattle.data.GamePhase
import com.alpha900i.a9kblanketbattle.data.GameState
import com.alpha900i.a9kblanketbattle.data.TripletOnBoard

/**
 * Collection of game logic methods
 * Yes, currently those methods are all effectively static and class has no state. This, though
 * isn't final decision and state in some form can later arrive in Game - and not something that
 * is specific to GameState, but something different. Maybe, say, difficulty level.
 * I can make it, i.e., into companion object or into object, or just into a bunch of methods. I just
 * feel better about current way for now.
 */
class Game {
    /**
     * Given current [gameState] and list of 2 [players], make single move and process its results with next method
     * Move can be either "actual move" or "removal of removable triplet"
     */
    suspend fun makeMove(
        players: List<Player>,
        gameState: GameState,
        stateUpdater: (GameState) -> Unit
    ) {
        if (gameState.gamePhase is GamePhase.AwaitingTripletSelection) {
            //triplet needs to be removed
            players[gameState.activePlayerIndex].removeTriplet(gameState.gamePhase.triplets) { tripletToDelete ->
                applyRemoval(gameState, tripletToDelete, stateUpdater)
            }
        } else {
            //nothing to remove, time to make normal turn
            players[gameState.activePlayerIndex].makeMove(gameState) { move ->
                applyMove(gameState, move, stateUpdater)
            }
        }
    }


    /**
     * Applies [move] to current [gameState], forming new state and updating it through lambda in viewModel
     * - apply move on board
     * - apply changes on hand
     * - check, if game is over. If so, new state would know this and cause game over
     * - check, whether some triplet deletion is required. If so, new state would have them and cause "triplet removal" flow
     * - if neither of previous two is true, just update game state and pass turn to other player
     */
    private fun applyMove(
        gameState: GameState,
        move: Move,
        stateUpdater: (GameState) -> Unit
    ) {
        val (
            newBoard,
            pendingEffects,
            handChanges,
            gameOver,
            winnerIndex,
            deletableTriplets
        ) = gameState.board.applyMove(move, gameState.activePlayerIndex, gameState.maxPieceAmount)
        //modify hands
        val newHands = gameState.hands.mapIndexed { index, hand ->
            hand.applyChange(handChanges[index])
        }
        val newPhase: GamePhase = when {
            gameOver -> GamePhase.GameOver(winnerIndex)
            deletableTriplets.isNotEmpty() -> GamePhase.AwaitingTripletSelection(deletableTriplets)
            else -> GamePhase.InProgress
        }
        val newPlayerIndex = when (newPhase) {
            is GamePhase.GameOver -> winnerIndex
            is GamePhase.AwaitingTripletSelection -> gameState.activePlayerIndex
            else -> (gameState.activePlayerIndex + 1) % PLAYER_COUNT
        }
        val newMoveCount = when (newPhase) {
            is GamePhase.GameOver -> gameState.moveCount
            is GamePhase.AwaitingTripletSelection -> gameState.moveCount
            else -> gameState.moveCount + 1
        }

        val newState = GameState(
            board = newBoard,
            oldBoard = gameState.board,
            pendingEffects = pendingEffects,
            hands = newHands,
            activePlayerIndex = newPlayerIndex,
            maxPieceAmount = gameState.maxPieceAmount,
            moveCount = newMoveCount,
            gamePhase = newPhase
        )
        stateUpdater(newState)
    }

    /**
     * Removes [tripletToRemove] from current [gameState], forming new state and updating it through lambda in viewModel
     * - removes triplet from board
     * - apply changes on hand
     * - update game state and pass turn to other player
     * Game over or new removal is impossible here
     */
    private fun applyRemoval(
        gameState: GameState,
        tripletToRemove: TripletOnBoard,
        stateUpdater: (GameState) -> Unit
    ) {
        val (newBoard, pendingEffects, handChanges, gameOver, winnerIndex, deletableTriplets) = gameState.board.applyRemoval(tripletToRemove)
        val newHands = gameState.hands.mapIndexed { index, hand ->
            hand.applyChange(handChanges[index])
        }

        //update state
        val newPhase: GamePhase = when {
            gameOver -> GamePhase.GameOver(winnerIndex)
            deletableTriplets.isNotEmpty() -> GamePhase.AwaitingTripletSelection(deletableTriplets)
            else -> GamePhase.InProgress
        }
        val newMoveCount = gameState.moveCount + 1
        val newPlayerIndex = (gameState.activePlayerIndex + 1) % PLAYER_COUNT
        val newState = GameState(
            board = newBoard,
            oldBoard = gameState.board,
            pendingEffects = pendingEffects,
            hands = newHands,
            activePlayerIndex = newPlayerIndex,
            maxPieceAmount = gameState.maxPieceAmount,
            moveCount = newMoveCount,
            gamePhase = newPhase
        )
        stateUpdater(newState)
    }



    companion object {
        const val PLAYER_COUNT: Int = 2
    }
}