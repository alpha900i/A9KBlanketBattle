package com.alpha900i.a9kblanketbattle.domain

import com.alpha900i.a9kblanketbattle.data.Board
import com.alpha900i.a9kblanketbattle.data.CellType
import com.alpha900i.a9kblanketbattle.data.GameState
import com.alpha900i.a9kblanketbattle.data.Hand
import com.alpha900i.a9kblanketbattle.data.TripletOnBoard
import com.alpha900i.a9kblanketbattle.data.VisualEffect
import com.alpha900i.a9kblanketbattle.data.board
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Game.applyMove / Game.applyRemoval, observed through the
 * suspend Game.makeMove entry point.
 *
 * Three invariants under test:
 *
 *   1) turn passing (or not, if triplets are pending)
 *   2) deletableTriplets reset on game over (Board leaves the winning
 *      triplet in MoveResult; Game is what clears it)
 *   3) activePlayerIndex doubles as winnerIndex on game over
 *
 * applyRemoval's contract (inherited from Board.applyRemoval but
 * re-asserted by Game): always advance the player, always winnerIndex = -1,
 * always deletableTriplets = emptySet().
 *
 * Uses a scripted stub player and runBlocking: the stub never delays, and
 * these tests exercise Game + Board together, so no coroutine-test
 * machinery is needed yet (see HumanPlayer tests for that).
 */
class GameTransitionsTest {
    //=========================================================================
    //Test doubles & helpers
    //=========================================================================

    /**
     * Plays a fixed move/triplet whenever asked, without deciding anything
     * itself. Records which entry point fired so dispatch can be verified
     * directly.
     */
    private class ScriptedPlayer(
        override val index: Int,
        private val move: Move? = null,
        private val triplet: TripletOnBoard? = null,
    ) : Player {
        var makeMoveCalls = 0
        var removeTripletCalls = 0

        override suspend fun makeMove(gameState: GameState, applier: (Move) -> Unit) {
            makeMoveCalls++
            val m = move ?: error("ScriptedPlayer[$index] has no move configured")
            applier(m)
        }

        override suspend fun removeTriplet(gameState: GameState, applier: (TripletOnBoard) -> Unit) {
            removeTripletCalls++
            val t = triplet ?: error("ScriptedPlayer[$index] has no triplet configured")
            applier(t)
        }
    }

    private fun gameState(
        initialBoard: Board,
        hands: List<Hand> = listOf(Hand(5, 5, 3, 3), Hand(5, 5, 3, 3)),
        activePlayerIndex: Int = 0,
        gameIsActive: Boolean = true,
        maxPieceAmount: Int = 8,
        deletableTriplets: Set<TripletOnBoard> = emptySet(),
    ): GameState = GameState(
        oldBoard = initialBoard,
        board = initialBoard,
        pendingEffects = emptyList(),
        hands = hands,
        activePlayerIndex = activePlayerIndex,
        gameIsActive = gameIsActive,
        winnerIndex = -1,
        deletableTriplets = deletableTriplets,
        maxPieceAmount = maxPieceAmount,
    )



    //=========================================================================
    //Dispatch: makeMove vs removeTriplet
    //=========================================================================
    @Test
    fun `makeMove dispatches to Player makeMove when no deletable triplets`() {
        runBlocking {
            val state = gameState(board("...", "...", "..."))
            val player0 = ScriptedPlayer(0, move = Move(1, 1, MoveType.SET_KITTEN))
            val player1 = ScriptedPlayer(1) //should not be invoked
            var newState: GameState? = null

            Game().makeMove(listOf(player0, player1), state) { newState = it }

            assertEquals(1, player0.makeMoveCalls, "P0.makeMove should be called once")
            assertEquals(0, player0.removeTripletCalls)
            assertEquals(0, player1.makeMoveCalls, "P1 must not be invoked")
            assertNotNull(newState)
        }
    }

    @Test
    fun `makeMove dispatches to removeTriplet when deletable triplets non-empty`() {
        runBlocking {
            val triplet = TripletOnBoard(0, 0, 0, 1)
            val state = gameState(
                initialBoard = board("ccc", "...", "..."),
                hands = listOf(Hand(5, 5, 0, 3), Hand(5, 5, 3, 3)),
                deletableTriplets = setOf(triplet),
            )
            val player0 = ScriptedPlayer(0, triplet = triplet)
            var newState: GameState? = null

            Game().makeMove(listOf(player0, ScriptedPlayer(1)), state) { newState = it }

            assertEquals(0, player0.makeMoveCalls)
            assertEquals(1, player0.removeTripletCalls)
            assertNotNull(newState)
        }
    }



    //=========================================================================
    //Normal move: turn passes, oldBoard updated
    //=========================================================================
    @Test
    fun `normal move passes turn to next player and updates oldBoard`() {
        runBlocking {
            val initialBoard = board("...", "...", "...")
            val state = gameState(initialBoard)
            val player0 = ScriptedPlayer(0, move = Move(1, 1, MoveType.SET_KITTEN))
            var newState: GameState? = null

            Game().makeMove(listOf(player0, ScriptedPlayer(1)), state) { newState = it }
            val result = assertNotNull(newState)

            //Turn passed.
            assertEquals(1, result.activePlayerIndex)
            assertTrue(result.gameIsActive)
            assertEquals(-1, result.winnerIndex)
            assertTrue(result.deletableTriplets.isEmpty())

            //oldBoard now holds the previous board.
            assertEquals(initialBoard, result.oldBoard)

            //New board has the placed piece, owned by P0.
            assertEquals(CellType.KITTEN, result.board.getCellType(1, 1))
            assertEquals(0, result.board.cells[1][1].owner)

            //Hands updated: P0's kittenCurrent -1; P1's unchanged.
            assertEquals(4, result.hands[0].kittenCurrent)
            assertEquals(5, result.hands[1].kittenCurrent)
        }
    }

    @Test
    fun `pendingEffects from Board layer are propagated to GameState`() {
        runBlocking {
            val state = gameState(board("...", "...", "..."))
            val player0 = ScriptedPlayer(0, move = Move(1, 1, MoveType.SET_KITTEN))
            var newState: GameState? = null

            Game().makeMove(listOf(player0, ScriptedPlayer(1)), state) { newState = it }
            val result = assertNotNull(newState)

            assertTrue(result.pendingEffects.isNotEmpty())
            assertTrue(result.pendingEffects.all { it is VisualEffect.AddPiece })
        }
    }



    //=========================================================================
    //2+ triplets: active player is preserved
    //=========================================================================
    @Test
    fun `move creating 2+ triplets keeps active player and populates deletableTriplets`() {
        runBlocking {
            //"+" shape around (2,2): P0 cats at N/S/E/W; P0 makes SET_KITTEN
            //at centre. SET_KITTEN cannot push CATs, so nothing moves; two
            //triplets form. Board.applyMove suppresses auto-removal for 2+.
            val state = gameState(
                initialBoard = board(
                    ".....",
                    "..c..",
                    ".c.c.",
                    "..c..",
                    ".....",
                ),
            )
            val player0 = ScriptedPlayer(0, move = Move(2, 2, MoveType.SET_KITTEN))
            var newState: GameState? = null

            Game().makeMove(listOf(player0, ScriptedPlayer(1)), state) { newState = it }
            val result = assertNotNull(newState)

            //Same active player; game still active; no winner.
            assertEquals(0, result.activePlayerIndex)
            assertTrue(result.gameIsActive)
            assertEquals(-1, result.winnerIndex)

            //Both triplets reported.
            assertEquals(2, result.deletableTriplets.size)
        }
    }



    //=========================================================================
    //Game over: winnerIndex set, deletableTriplets reset
    //=========================================================================
    @Test
    fun `game over sets winner, clears deletableTriplets, and disables gameIsActive`() {
        runBlocking {
            //P0 cats at (0,0),(0,1); SET_CAT at (0,2) completes a cat triplet.
            //Board.applyMove returns gameOver=true AND one deletableTriplet
            //(auto-removal is suppressed when gameOver). Game.applyMove is
            //what drops it from the final state.
            val state = gameState(
                initialBoard = board(
                    "cc..",
                    "....",
                    "....",
                    "....",
                ),
            )
            val player0 = ScriptedPlayer(0, move = Move(0, 2, MoveType.SET_CAT))
            var newState: GameState? = null

            Game().makeMove(listOf(player0, ScriptedPlayer(1)), state) { newState = it }
            val result = assertNotNull(newState)

            assertEquals(0, result.winnerIndex)
            assertFalse(result.gameIsActive)
            assertTrue(
                result.deletableTriplets.isEmpty(),
                "winning triplet must be dropped from state, got ${result.deletableTriplets}",
            )
            //activePlayerIndex doubles as winnerIndex on game over.
            assertEquals(0, result.activePlayerIndex)
        }
    }

    @Test
    fun `single auto-removed triplet still passes turn and applies removal effects`() {
        runBlocking {
            //P0 cats at (0,0),(0,1); SET_KITTEN at (0,2) forms a single
            //mixed triplet that Board auto-removes. Game.applyMove sees
            //deletableTriplets empty, so it passes turn. But the effects
            //and hand changes must still reflect both place AND remove.
            val state = gameState(
                initialBoard = board(
                    "cc..",
                    "....",
                    "....",
                    "....",
                ),
            )
            val player0 = ScriptedPlayer(0, move = Move(0, 2, MoveType.SET_KITTEN))
            var newState: GameState? = null

            Game().makeMove(listOf(player0, ScriptedPlayer(1)), state) { newState = it }
            val result = assertNotNull(newState)

            //Turn passed; no triplets pending.
            assertEquals(1, result.activePlayerIndex)
            assertTrue(result.deletableTriplets.isEmpty())

            //Board cells cleared.
            assertEquals(CellType.EMPTY, result.board.getCellType(0, 0))
            assertEquals(CellType.EMPTY, result.board.getCellType(0, 1))
            assertEquals(CellType.EMPTY, result.board.getCellType(0, 2))

            //Effects: one AddPiece + three RemovePieces.
            val adds = result.pendingEffects.filterIsInstance<VisualEffect.AddPiece>()
            val removes = result.pendingEffects.filterIsInstance<VisualEffect.RemovePiece>()
            assertEquals(1, adds.size)
            assertEquals(3, removes.size)

            //Hand deltas combine placement and removal:
            //   -1 kitten current (place), -1 kitten max (1 kitten converted),
            //   +3 cat current (2 cats + 1 kitten), +1 cat max
            assertEquals(4, result.hands[0].kittenCurrent)
            assertEquals(4, result.hands[0].kittenMax)
            assertEquals(6, result.hands[0].catCurrent)
            assertEquals(4, result.hands[0].catMax)
        }
    }

    //=========================================================================
    //Removal transitions
    //=========================================================================
    @Test
    fun `removal passes turn, keeps winnerIndex -1, resets deletableTriplets, updates oldBoard`() {
        runBlocking {
            val triplet = TripletOnBoard(0, 0, 0, 1)
            val initialBoard = board("ccc", "...", "...")
            val state = gameState(
                initialBoard = initialBoard,
                hands = listOf(Hand(5, 5, 0, 3), Hand(5, 5, 3, 3)),
                deletableTriplets = setOf(triplet),
            )
            var newState: GameState? = null

            Game().makeMove(
                listOf(ScriptedPlayer(0, triplet = triplet), ScriptedPlayer(1)),
                state
            ) {
                newState = it
            }
            val result = assertNotNull(newState)

            //Turn advanced.
            assertEquals(1, result.activePlayerIndex)
            assertTrue(result.gameIsActive)

            //Removal can never end the game.
            assertEquals(-1, result.winnerIndex)

            //No triplets pending after removal.
            assertTrue(result.deletableTriplets.isEmpty())

            //oldBoard preserved.
            assertEquals(initialBoard, result.oldBoard)

            //Board cells cleared.
            assertEquals(CellType.EMPTY, result.board.getCellType(0, 0))
            assertEquals(CellType.EMPTY, result.board.getCellType(0, 1))
            assertEquals(CellType.EMPTY, result.board.getCellType(0, 2))

            //Three RemovePiece effects, one per cleared cell.
            val removes = result.pendingEffects.filterIsInstance<VisualEffect.RemovePiece>()
            assertEquals(3, removes.size)
            assertTrue(removes.all { it.type == CellType.CAT && it.owner == 0 })

            //Hand: three cats returned; max unchanged.
            assertEquals(3, result.hands[0].catCurrent)
            assertEquals(3, result.hands[0].catMax)
            //P1 unaffected.
            assertEquals(3, result.hands[1].catCurrent)
        }
    }



    //=========================================================================
    //Hand isolation
    //=========================================================================
    @Test
    fun `passing turn only modifies the acting player's hand`() {
        runBlocking {
            val state = gameState(board("...", "...", "..."))
            val player0 = ScriptedPlayer(0, move = Move(1, 1, MoveType.SET_KITTEN))
            var newState: GameState? = null

            Game().makeMove(listOf(player0, ScriptedPlayer(1)), state) { newState = it }
            val result = assertNotNull(newState)

            assertEquals(4, result.hands[0].kittenCurrent)
            assertEquals(5, result.hands[0].kittenMax)
            assertEquals(5, result.hands[1].kittenCurrent)
            assertEquals(5, result.hands[1].kittenMax)
        }
    }
}