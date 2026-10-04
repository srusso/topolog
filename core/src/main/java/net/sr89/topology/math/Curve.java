package net.sr89.topology.math;

import com.badlogic.gdx.math.Vector3;

/**
 * A parametric curve. This is essentially a function [0, 1] -> ℝ<sup>3</sup>
 */
public interface Curve {
    /**
     * @param t   In range of [0, 1] along the curve. For closed curves t=1 is the same point as t=0.
     * @param out The output vector
     */
    void pointAt(float t, Vector3 out);
}
