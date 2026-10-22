package com.alpha900i.a9kblanketbattle.domain

import com.alpha900i.a9kblanketbattle.data.GameState
import com.alpha900i.a9kblanketbattle.data.TripletOnBoard
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * HumanPlayer's CompletableDeferred lifecycle.
 *
 * HumanPlayer.makeMove / removeTriplet suspend until the matching
 * submitMove / submitRemoval is called from the UI. This file exercises
 * the deferred handshake directly, without going through Game.
 *
 * One test (`double makeMove …`) encodes the fixed semantics for a
 * latent bug in the current finally block — see the comment on that test.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HumanPlayerTest {

    private val state: GameState =
        GameState.startingState(width = 3, height = 3, kittenStart = 5, catStart = 2, gameIsActive = true)



    //=========================================================================
    //submitMove handshake
    //=========================================================================
    @Test
    fun `submitMove before makeMove is a silent no-op`() = runTest {
        val p = HumanPlayer(0)

        //No awaiter yet. Should not throw, should not create a pending
        //deferred that a later makeMove would immediately resolve.
        p.submitMove(Move(0, 0, MoveType.SET_KITTEN))

        var captured: Move? = null
        val job = launch { p.makeMove(state) { captured = it } }
        runCurrent()

        assertNull(captured, "makeMove should still be awaiting - pre-submit must have been dropped")

        p.submitMove(Move(1, 1, MoveType.SET_KITTEN))
        advanceUntilIdle()
        assertEquals(Move(1, 1, MoveType.SET_KITTEN), captured)
        job.join()
    }

    @Test
    fun `submitMove completes a pending makeMove`() = runTest {
        val p = HumanPlayer(0)
        var captured: Move? = null
        val job = launch { p.makeMove(state) { captured = it } }
        runCurrent()
        assertNull(captured, "makeMove must await submitMove")

        p.submitMove(Move(1, 1, MoveType.SET_KITTEN))
        advanceUntilIdle()
        assertEquals(Move(1, 1, MoveType.SET_KITTEN), captured)
        job.join()
    }

    @Test
    fun `submitMove twice - second is a no-op after await already resumed`() = runTest {
        val p = HumanPlayer(0)
        val captured = mutableListOf<Move>()
        val job = launch { p.makeMove(state) { captured.add(it) } }
        runCurrent()

        p.submitMove(Move(1, 1, MoveType.SET_KITTEN))
        advanceUntilIdle()
        //Now deferred is null again (cleared in finally). Second submitMove
        //has no awaiter and does nothing.
        p.submitMove(Move(2, 2, MoveType.SET_CAT))
        advanceUntilIdle()

        assertEquals(listOf(Move(1, 1, MoveType.SET_KITTEN)), captured)
        job.join()
    }



    //=========================================================================
    //submitRemoval handshake
    //=========================================================================
    @Test
    fun `submitRemoval before removeTriplet is a silent no-op`() = runTest {
        val p = HumanPlayer(0)
        p.submitRemoval(TripletOnBoard(0, 0, 0, 1))

        var captured: TripletOnBoard? = null
        val job = launch { p.removeTriplet(state) { captured = it } }
        runCurrent()
        assertNull(captured, "removeTriplet should still be awaiting")

        val triplet = TripletOnBoard(1, 1, 1, 0)
        p.submitRemoval(triplet)
        advanceUntilIdle()
        assertEquals(triplet, captured)
        job.join()
    }

    @Test
    fun `submitRemoval completes a pending removeTriplet`() = runTest {
        val p = HumanPlayer(0)
        val triplet = TripletOnBoard(0, 0, 0, 1)
        var captured: TripletOnBoard? = null
        val job = launch { p.removeTriplet(state) { captured = it } }
        runCurrent()
        assertNull(captured)

        p.submitRemoval(triplet)
        advanceUntilIdle()
        assertEquals(triplet, captured)
        job.join()
    }



    //=========================================================================
    //Deferreds are separate for moves and removals
    //=========================================================================
    @Test
    fun `submitMove does not complete a pending removeTriplet`() = runTest {
        val p = HumanPlayer(0)
        var captured: TripletOnBoard? = null
        val job = launch { p.removeTriplet(state) { captured = it } }
        runCurrent()

        //submitMove should be a no-op here - the two awaiters use separate
        //fields (deferredMove vs deferredRemoval).
        p.submitMove(Move(1, 1, MoveType.SET_KITTEN))
        advanceUntilIdle()
        assertNull(captured, "submitMove must not complete a removeTriplet await")

        //Only submitRemoval should unblock it.
        val triplet = TripletOnBoard(0, 0, 0, 1)
        p.submitRemoval(triplet)
        advanceUntilIdle()
        assertEquals(triplet, captured)
        job.join()
    }



    //=========================================================================
    //Failure paths and reusability
    //=========================================================================
    @Test
    fun `applier exception is rethrown and player is reusable`() = runTest {
        //If the applier throws, the exception must propagate to the caller
        //and the finally block must clear the deferred - so a subsequent
        //makeMove works normally.
        val p = HumanPlayer(0)

        val job1 = launch {
            assertFailsWith<IllegalStateException> {
                p.makeMove(state) { error("applier threw") }
            }
        }
        runCurrent()
        p.submitMove(Move(0, 0, MoveType.SET_KITTEN))
        advanceUntilIdle()
        job1.join()

        //Player still usable.
        var captured: Move? = null
        val job2 = launch { p.makeMove(state) { captured = it } }
        runCurrent()
        p.submitMove(Move(1, 1, MoveType.SET_KITTEN))
        advanceUntilIdle()
        assertEquals(Move(1, 1, MoveType.SET_KITTEN), captured)
        job2.join()
    }



    //=========================================================================
    //Concurrent makeMove (regression for the finally-clobber bug)
    //=========================================================================
    @Test
    fun `double makeMove - first is cancelled, second becomes the live awaiter`() = runTest {
        //REQUIRES the finally-block fix in HumanPlayer.makeMove:
        //   capture the deferred locally and only clear the field if it
        //   still points at that deferred. Without the fix, Call 1's
        //   finally nulls out Call 2's deferred and the final submitMove
        //   becomes a silent no-op - this test then fails on the second
        //   assertion.
        val p = HumanPlayer(0)
        val firstMoveWasCancelled = CompletableDeferred<Boolean>()
        val captured = mutableListOf<Move>()

        val job1 = launch {
            try {
                p.makeMove(state) { captured.add(it) }
            } catch (e: CancellationException) {
                firstMoveWasCancelled.complete(true)
            }
        }
        runCurrent()
        assertTrue(!firstMoveWasCancelled.isCompleted, "call 1 should be awaiting")

        val job2 = launch { p.makeMove(state) { captured.add(it) } }
        runCurrent()

        //Call 2 cancelled Call 1's deferred; Call 1 should be gone.
        assertTrue(firstMoveWasCancelled.isCompleted, "call 1 should have been cancelled")
        assertTrue(captured.isEmpty(), "no move captured yet - call 2 still awaiting")

        //Call 2 should now accept the move.
        p.submitMove(Move(1, 1, MoveType.SET_KITTEN))
        advanceUntilIdle()

        assertEquals(listOf(Move(1, 1, MoveType.SET_KITTEN)), captured)
        job1.join()
        job2.join()
    }
}