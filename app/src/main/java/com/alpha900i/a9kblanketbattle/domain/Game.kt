package com.alpha900i.a9kblanketbattle.domain

import com.alpha900i.a9kblanketbattle.data.CellType
import com.alpha900i.a9kblanketbattle.data.GameState
import com.alpha900i.a9kblanketbattle.data.TripletOnBoard

class Game {
    suspend fun makeMove(
        players: List<Player>,
        gameState: GameState,
        stateUpdater: (GameState) -> Unit
    ) {
        if (gameState.deletableTriplets.isEmpty()) {                                //normal move
            players[gameState.activePlayerIndex].makeMove(gameState) { move ->
                applyMove(gameState, move, stateUpdater)
            }
        } else if (gameState.deletableTriplets.size == 1) {
            applyRemoval(gameState, gameState.deletableTriplets.first(), stateUpdater)
        } else {
            players[gameState.activePlayerIndex].removeTriplet(gameState) { tripletToDelete ->
                applyRemoval(gameState, tripletToDelete, stateUpdater)
            }
        }
    }

    private fun applyMove(
        gameState: GameState,
        move: Move,
        stateUpdater: (GameState) -> Unit
    ) {
        val (newBoard, handChanges, gameOver, winnerIndex, deletableTriplets) = gameState.board.applyMove(move, gameState.activePlayerIndex)
        //modify hands
        val newHands = gameState.hands.mapIndexed { index, hand ->
            hand.applyChange(handChanges[index])
        }

        if (gameOver || deletableTriplets.isEmpty()) {                              // if game is over or there is nothing to delete - passing turn
            val emptyCount = gameState.board.cells.sumOf { row -> row.count { it.type == CellType.EMPTY } }

            //update state
            val newPlayerIndex = (gameState.activePlayerIndex + 1) % PLAYER_COUNT
            val moveIsExpected = emptyCount > 0 && !gameOver
            val newState = GameState(
                board = newBoard,
                hands = newHands,
                activePlayerIndex = if (!gameOver) newPlayerIndex else winnerIndex,
                gameIsActive = moveIsExpected,
                winnerIndex = winnerIndex,
                deletableTriplets = if (!gameOver) deletableTriplets else setOf()
            )
            stateUpdater(newState)
        } else {   // if game is not over, but there is something to delete - we do not change player
            val newState = GameState(
                board = newBoard,
                hands = newHands,
                activePlayerIndex = gameState.activePlayerIndex,
                gameIsActive = gameState.gameIsActive,
                winnerIndex = -1,
                deletableTriplets = deletableTriplets
            )
            stateUpdater(newState)
        }
    }

    private fun applyRemoval(
        gameState: GameState,
        tripletToRemove: TripletOnBoard,
        stateUpdater: (GameState) -> Unit
    ) {
        val (newBoard, handChanges, gameOver, winnerIndex, deletableTriplets) = gameState.board.applyRemoval(tripletToRemove)
        val newHands = gameState.hands.mapIndexed { index, hand ->
            hand.applyChange(handChanges[index])
        }

        val emptyCount = gameState.board.cells.sumOf { row -> row.count { it.type == CellType.EMPTY } }

        //update state
        val newPlayerIndex = (gameState.activePlayerIndex + 1) % PLAYER_COUNT
        val moveIsExpected = emptyCount > 0 && !gameOver
        val newState = GameState(
            board = newBoard,
            hands = newHands,
            activePlayerIndex = newPlayerIndex,
            gameIsActive = moveIsExpected,
            winnerIndex = winnerIndex,
            deletableTriplets = deletableTriplets
        )
        stateUpdater(newState)
    }



    companion object {
        const val PLAYER_COUNT: Int = 2
    }
}