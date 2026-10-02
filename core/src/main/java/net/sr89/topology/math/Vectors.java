package net.sr89.topology.math;

import com.badlogic.gdx.math.Vector3;

public class Vectors {
    /**
     * Adds vector b to vector a, returning a new Vector3 object.
     * NOTE: To eliminate all allocation, this method signature would need to change
     * to accept a mutable 'result' vector as an argument. This implementation
     * preserves the signature while optimizing intermediate object creation.
     */
    public static Vector3 add(Vector3 a, Vector3 b) {
        Vector3 result = new Vector3(a);
        result.add(b);
        return result;
    }

    /**
     * Calculates the direction vector from 'from' to 'to', returning a new Vector3 object.
     * NOTE: To eliminate all allocation, this method signature would need to change
     * to accept a mutable 'result' vector as an argument.
     */
    public static Vector3 direction(Vector3 from, Vector3 to) {
        Vector3 result = new Vector3(to);
        result.sub(from);
        return result;
    }
}