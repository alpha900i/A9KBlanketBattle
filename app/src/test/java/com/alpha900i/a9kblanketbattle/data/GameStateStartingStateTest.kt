package com.alpha900i.a9kblanketbattle.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
/**
 *   GameState.startingState(...)   - factory invariants
 *   - startingState: max = start must hold on both hands
 */
class GameStateStartingStateTest {

    @Test
    fun `both hands start with current equal to max`() {
        val state = GameState.startingState(
            width = 3, height = 4,
            kittenStart = 5, catStart = 2,
            gameIsActive = true,
        )

        state.hands.forEachIndexed { index, hand ->
            assertEquals(5, hand.kittenCurrent, "hand[$index].kittenCurrent")
            assertEquals(5, hand.kittenMax, "hand[$index].kittenMax")
            assertEquals(2, hand.catCurrent, "hand[$index].catCurrent")
            assertEquals(2, hand.catMax, "hand[$index].catMax")
        }
    }

    @Test
    fun `activePlayerIndex is 0, winnerIndex is -1, no pending effects or triplets`() {
        val state = GameState.startingState(
            width = 3, height = 4,
            kittenStart = 5, catStart = 2,
            gameIsActive = true,
        )

        assertEquals(0, state.activePlayerIndex)
        assertEquals(-1, state.winnerIndex)
        assertTrue(state.pendingEffects.isEmpty())
        assertTrue(state.deletableTriplets.isEmpty())
    }

    @Test
    fun `gameIsActive is taken from the factory argument`() {
        val active = GameState.startingState(3, 4, 5, 2, gameIsActive = true)
        val inactive = GameState.startingState(3, 4, 5, 2, gameIsActive = false)
        assertTrue(active.gameIsActive)
        assertFalse(inactive.gameIsActive)
    }

    @Test
    fun `both boards are empty with the requested dimensions`() {
        val state = GameState.startingState(
            width = 3, height = 4,
            kittenStart = 5, catStart = 2,
            gameIsActive = true,
        )

        assertEquals(3, state.board.getWidth(), "board width")
        assertEquals(4, state.board.getHeight(), "board height")
        assertEquals(3, state.oldBoard.getWidth(), "oldBoard width")
        assertEquals(4, state.oldBoard.getHeight(), "oldBoard height")
        assertFalse(state.board.hasPieces(), "board must be empty")
        assertFalse(state.oldBoard.hasPieces(), "oldBoard must be empty")
    }

    @Test
    fun `maxPieceAmount equals kittenStart plus catStart`() {
        assertEquals(
            7,
            GameState.startingState(
                3,
                3,
                kittenStart = 5,
                catStart = 2,
                gameIsActive = true
            ).maxPieceAmount
        )
        assertEquals(
            2,
            GameState.startingState(
                3,
                3,
                kittenStart = 2,
                catStart = 0,
                gameIsActive = true
            ).maxPieceAmount
        )
        assertEquals(
            4,
            GameState.startingState(
                3,
                3,
                kittenStart = 0,
                catStart = 4,
                gameIsActive = true
            ).maxPieceAmount
        )
    }
}