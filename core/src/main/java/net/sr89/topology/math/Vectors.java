package net.sr89.topology.math;

import com.badlogic.gdx.math.Vector3;

public class Vectors {
    /**
     * Adds vector b to vector a, storing the result in the provided mutable 'result' vector.
     * This method avoids creating new Vector3 objects, minimizing garbage collection overhead.
     *
     * @param a The first vector.
     * @param b The second vector.
     * @param result The mutable vector to store the sum in.
     * @return The 'result' vector after the operation.
     */
    public static Vector3 add(Vector3 a, Vector3 b, Vector3 result) {
        result.set(a).add(b);
        return result;
    }

    /**
     * Calculates the direction vector from 'from' to 'to', storing the result in the provided mutable 'result' vector.
     * This method avoids creating new Vector3 objects, minimizing garbage collection overhead.
     *
     * @param from The starting vector.
     * @param to The ending vector.
     * @param result The mutable vector to store the difference (to - from) in.
     * @return The 'result' vector after the operation.
     */
    public static Vector3 direction(Vector3 from, Vector3 to, Vector3 result) {
        // Set result to 'to', then subtract 'from' from it.
        result.set(to).sub(from);
        return result;
    }
}