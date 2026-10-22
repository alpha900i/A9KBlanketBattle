package com.alpha900i.a9kblanketbattle.data

import com.alpha900i.a9kblanketbattle.domain.HandChange
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
/**
 *   GameState.gameInProgress()     - active-gate + any-change gate
 *   - gameInProgress: leading `gameIsActive &&` gate is easy to forget
 */
class GameStateGameInProgressTest {

    @Test
    fun `false when gameIsActive is false even with pieces on board`() {
        //To catch an accidental removal of the leading `gameIsActive &&`
        //gate, the state must be one where the *rest* of the condition
        //would return true. Pieces on the board satisfies that.
        val base = GameState.startingState(3, 3, 5, 2, gameIsActive = false)
        val state = base.copy(board = board("k..", "...", "..."))
        assertFalse(state.gameInProgress())
    }

    @Test
    fun `false on a fresh active game with empty board and unchanged hands`() {
        val state = GameState.startingState(3, 3, 5, 2, gameIsActive = true)
        assertFalse(state.gameInProgress())
    }

    @Test
    fun `true when active and board has pieces`() {
        val base = GameState.startingState(3, 3, 5, 2, gameIsActive = true)
        val state = base.copy(board = board("k..", "...", "..."))
        assertTrue(state.gameInProgress())
    }

    @Test
    fun `true when active and P0 hand is changed`() {
        val base = GameState.startingState(3, 3, 5, 2, gameIsActive = true)
        val state = base.copy(
            hands = listOf(
                base.hands[0].applyChange(HandChange(-1, 0, 0, 0)),
                base.hands[1],
            ),
        )
        assertTrue(state.gameInProgress())
    }

    @Test
    fun `true when active and P1 hand is changed`() {
        //Separate from the P0 case on purpose: a `hands[0]`-twice bug would
        //pass the P0 test and fail this one.
        val base = GameState.startingState(3, 3, 5, 2, gameIsActive = true)
        val state = base.copy(
            hands = listOf(
                base.hands[0],
                base.hands[1].applyChange(HandChange(-1, 0, 0, 0)),
            ),
        )
        assertTrue(state.gameInProgress())
    }
}