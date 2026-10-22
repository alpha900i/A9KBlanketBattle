package com.alpha900i.a9kblanketbattle.data

import com.alpha900i.a9kblanketbattle.domain.HandChange
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 *   Hand.applyChange(HandChange)   - four independent delta fields
 *   - applyChange: four fields, easy to mix up
 */
class HandApplyChangeTest {

    @Test
    fun `empty change is identity`() {
        val hand = Hand(kittenCurrent = 3, kittenMax = 5, catCurrent = 2, catMax = 4)
        assertEquals(hand, hand.applyChange(HandChange.emptyHand()))
    }

    @Test
    fun `each delta field only modifies its corresponding hand field`() {
        //A base hand with all four fields distinct so a field mix-up
        //produces a visibly wrong result.
        val base = Hand(kittenCurrent = 5, kittenMax = 7, catCurrent = 3, catMax = 4)

        assertEquals(
            Hand(kittenCurrent = 3, kittenMax = 7, catCurrent = 3, catMax = 4),
            base.applyChange(HandChange(deltaCurrentKitten = -2, deltaMaxKitten = 0, deltaCurrentCat = 0, deltaMaxCat = 0)),
            "deltaCurrentKitten",
        )
        assertEquals(
            Hand(kittenCurrent = 5, kittenMax = 11, catCurrent = 3, catMax = 4),
            base.applyChange(HandChange(deltaCurrentKitten = 0, deltaMaxKitten = 4, deltaCurrentCat = 0, deltaMaxCat = 0)),
            "deltaMaxKitten",
        )
        assertEquals(
            Hand(kittenCurrent = 5, kittenMax = 7, catCurrent = 1, catMax = 4),
            base.applyChange(HandChange(deltaCurrentKitten = 0, deltaMaxKitten = 0, deltaCurrentCat = -2, deltaMaxCat = 0)),
            "deltaCurrentCat",
        )
        assertEquals(
            Hand(kittenCurrent = 5, kittenMax = 7, catCurrent = 3, catMax = 6),
            base.applyChange(HandChange(deltaCurrentKitten = 0, deltaMaxKitten = 0, deltaCurrentCat = 0, deltaMaxCat = 2)),
            "deltaMaxCat",
        )
    }

    @Test
    fun `all four deltas apply simultaneously`() {
        val hand = Hand(kittenCurrent = 5, kittenMax = 5, catCurrent = 3, catMax = 3)
        val result = hand.applyChange(
            HandChange(deltaCurrentKitten = -1, deltaMaxKitten = -1, deltaCurrentCat = 2, deltaMaxCat = 1),
        )
        assertEquals(Hand(kittenCurrent = 4, kittenMax = 4, catCurrent = 5, catMax = 4), result)
    }
}

