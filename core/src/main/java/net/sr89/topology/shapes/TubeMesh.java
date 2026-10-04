package net.sr89.topology.shapes;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder.VertexInfo;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import net.sr89.topology.math.Curve;

/**
 * Builds a single mesh (one vertex buffer, one draw call) shaped like a tube following a curve.
 */
public final class TubeMesh {

    private static final float TANGENT_EPSILON = 1e-3f;

    private TubeMesh() {}

    /**
     * @param curve   the path of the tube's center line, must be defined slightly outside [0, 1]
     *                (used to estimate the tangent); true for any analytic curve
     * @param closed  whether the curve is a loop; closed tubes have no ends and the last ring joins the first.
     *                Open tubes are left uncapped.
     * @param samples number of segments along the curve
     * @param sides   number of vertices around each ring
     */
    public static Model build(Curve curve, boolean closed, int samples, int sides, float radius, Color color) {
        final ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();
        final MeshPartBuilder builder = modelBuilder.part("tube", GL20.GL_TRIANGLES,
            Usage.Position | Usage.Normal, new Material(ColorAttribute.createDiffuse(color)));

        final int rings = closed ? samples : samples + 1;
        final Vector3[] centers = new Vector3[rings];
        final Vector3[] tangents = new Vector3[rings];
        final Vector3[] normals = new Vector3[rings]; // first axis of each ring's plane

        final Vector3 ahead = new Vector3();
        final Vector3 behind = new Vector3();
        for (int i = 0; i < rings; i++) {
            final float t = (float) i / samples;
            centers[i] = new Vector3();
            tangents[i] = new Vector3();
            normals[i] = new Vector3();
            curve.pointAt(t, centers[i]);
            tangentAt(curve, t, tangents[i], ahead, behind);

            if (i == 0) {
                initialNormal(tangents[i], normals[i]);
            } else {
                // Parallel transport: carry the previous normal along, only removing the part that
                // is no longer perpendicular to the new tangent. This keeps the ring from twisting.
                transport(normals[i - 1], tangents[i], normals[i]);
            }
        }
        if (closed) {
            spreadTwist(tangents, normals);
        }

        final short[][] index = new short[rings][sides];
        final Vector3 binormal = new Vector3(); // second axis of the ring's plane
        final Vector3 offset = new Vector3();
        final Vector3 position = new Vector3();
        final VertexInfo info = new VertexInfo();
        for (int i = 0; i < rings; i++) {
            binormal.set(tangents[i]).crs(normals[i]);
            for (int j = 0; j < sides; j++) {
                final double angle = 2 * Math.PI * j / sides;
                // unit vector pointing from the center line out to the surface; it's also the surface normal
                offset.set(normals[i]).scl((float) Math.cos(angle)).mulAdd(binormal, (float) Math.sin(angle));
                position.set(centers[i]).mulAdd(offset, radius);
                info.set(position, offset, null, null);
                index[i][j] = builder.vertex(info);
            }
        }

        final int segments = closed ? rings : rings - 1;
        for (int i = 0; i < segments; i++) {
            final short[] ring = index[i];
            final short[] next = index[(i + 1) % rings];
            for (int j = 0; j < sides; j++) {
                final int nj = (j + 1) % sides;
                // wound so the front face points away from the center line
                builder.triangle(ring[j], ring[nj], next[j]);
                builder.triangle(next[j], ring[nj], next[nj]);
            }
        }
        return modelBuilder.end();
    }

    /** Carries {@code previous} to a plane perpendicular to {@code tangent}, with the smallest possible rotation. */
    private static Vector3 transport(Vector3 previous, Vector3 tangent, Vector3 out) {
        return out.set(previous).mulAdd(tangent, -previous.dot(tangent)).nor();
    }

    /**
     * Carrying a normal all the way around a closed, non-planar curve doesn't necessarily bring it back to where it
     * started, so the last ring would not line up with the first. We measure that angle and undo it gradually
     * over all the rings, so the mismatch is invisible.
     */
    private static void spreadTwist(Vector3[] tangents, Vector3[] normals) {
        final int rings = normals.length;
        final Vector3 first = normals[0];
        final Vector3 arrived = transport(normals[rings - 1], tangents[0], new Vector3());
        final double twist = Math.atan2(tangents[0].dot(new Vector3(first).crs(arrived)), first.dot(arrived));

        final Vector3 sideways = new Vector3();
        for (int i = 1; i < rings; i++) {
            final double angle = -twist * i / rings;
            sideways.set(tangents[i]).crs(normals[i]);
            normals[i].scl((float) Math.cos(angle)).mulAdd(sideways, (float) Math.sin(angle));
        }
    }

    private static void tangentAt(Curve curve, float t, Vector3 out, Vector3 tmpA, Vector3 tmpB) {
        curve.pointAt(t + TANGENT_EPSILON, tmpA);
        curve.pointAt(t - TANGENT_EPSILON, tmpB);
        out.set(tmpA).sub(tmpB).nor();
    }

    /** Any unit vector perpendicular to the tangent. */
    private static void initialNormal(Vector3 tangent, Vector3 out) {
        // cross with the world axis the tangent is least aligned with, to avoid a degenerate (near-zero) result
        final Vector3 axis = Math.abs(tangent.x) <= Math.abs(tangent.y) && Math.abs(tangent.x) <= Math.abs(tangent.z)
            ? Vector3.X : Math.abs(tangent.y) <= Math.abs(tangent.z) ? Vector3.Y : Vector3.Z;
        out.set(tangent).crs(axis).nor();
    }
}
