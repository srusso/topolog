package net.sr89.topology.math;

import com.badlogic.gdx.math.Vector3;

/**
 * A parametric surface. This is essentially a function [0, 1] × [0, 1] -> ℝ<sup>3</sup>
 */
public interface Surface {
    /**
     * @param u   In range of [0, 1]
     * @param v   In range of [0, 1]
     * @param out The output vector
     */
    void pointAt(float u, float v, Vector3 out);
}
