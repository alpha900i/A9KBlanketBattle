package com.alpha900i.a9kblanketbattle.data

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
/**
 *   Hand.changed()
 *   - "is any piece not in hand?"
 *   - changed(): "changed" means "not all in hand", not "was modified"
 */
class HandChangedTest {
    @Test
    fun `changed is false when every piece is in hand`() {
        //current == max on both fields is the "all in hand" invariant.
        assertFalse(Hand(kittenCurrent = 5, kittenMax = 5, catCurrent = 3, catMax = 3).changed())
        //Zero-everything is also "all in hand" in the degenerate sense.
        assertFalse(Hand(kittenCurrent = 0, kittenMax = 0, catCurrent = 0, catMax = 0).changed())
    }

    @Test
    fun `changed is true when any piece is not in hand`() {
        assertTrue(
            Hand(kittenCurrent = 3, kittenMax = 5, catCurrent = 3, catMax = 3).changed(),
            "kitten on board"
        )
        assertTrue(
            Hand(kittenCurrent = 5, kittenMax = 5, catCurrent = 1, catMax = 3).changed(),
            "cat on board"
        )
        assertTrue(
            Hand(kittenCurrent = 4, kittenMax = 5, catCurrent = 2, catMax = 3).changed(),
            "both on board"
        )
    }
}