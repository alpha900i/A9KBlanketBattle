package com.alpha900i.a9kblanketbattle.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 *   GameState.gameInProgress()     - active-gate + any-change gate
 *   - gameInProgress: leading `gameIsActive &&` gate is easy to forget
 */
class GameStateGameInProgressTest {
    @Test
    fun `false when gameIsActive is false even with moves done`() {
        // Leading-gate regression: if the `gameIsActive &&` were ever dropped,
        // a finished game with moveCount > 0 would report "in progress."
        val state = GameState.startingState(3, 3, 5, 2, gameIsActive = false)
            .copy(moveCount = 5)
        assertFalse(state.gameInProgress())
    }

    @Test
    fun `false on a fresh active game with no moves`() {
        // The "confirm before leaving" dialog must not fire on a game that
        // has only just started.
        val state = GameState.startingState(3, 3, 5, 2, gameIsActive = true)
        assertEquals(0, state.moveCount)   // sanity
        assertFalse(state.gameInProgress())
    }

    @Test
    fun `true when active and at least one move done`() {
        val state = GameState.startingState(3, 3, 5, 2, gameIsActive = true)
            .copy(moveCount = 1)
        assertTrue(state.gameInProgress())
    }
}