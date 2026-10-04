package net.sr89.topology.worlds;

import com.badlogic.gdx.math.Interpolation;

/** When a polygon that is folding up into a surface is flat, folding, folded, and unfolding. */
final class FoldTiming {
    // seconds
    private static final float HOLD_FLAT = 3f, FOLDING = 4f, HOLD_FOLDED = 3f;
    /** The length of a whole cycle: stay flat, fold, stay folded, unfold. */
    static final float CYCLE = HOLD_FLAT + FOLDING + HOLD_FOLDED + FOLDING;

    private FoldTiming() {}

    /** How much the polygon is folded, from 0 (flat) to 1 (folded), a number of seconds into the cycle. */
    static float amount(float cycleTime) {
        final float amount;
        if (cycleTime < HOLD_FLAT) {
            amount = 0f;
        } else if (cycleTime < HOLD_FLAT + FOLDING) {
            amount = (cycleTime - HOLD_FLAT) / FOLDING;
        } else if (cycleTime < HOLD_FLAT + FOLDING + HOLD_FOLDED) {
            amount = 1f;
        } else {
            amount = 1f - (cycleTime - HOLD_FLAT - FOLDING - HOLD_FOLDED) / FOLDING;
        }
        return Interpolation.smooth.apply(amount);
    }
}
