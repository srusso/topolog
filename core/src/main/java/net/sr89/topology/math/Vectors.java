package net.sr89.topology.math;

import com.badlogic.gdx.math.Vector3;

public class Vectors {
    public static Vector3 add(Vector3 a, Vector3 b) {
        Vector3 one = new Vector3(a);
        Vector3 two = new Vector3(b);
        return one.add(two);
    }

    public static Vector3 direction(Vector3 from, Vector3 to) {
        Vector3 one = new Vector3(from);
        Vector3 two = new Vector3(to);
        return two.sub(one);
    }
}
