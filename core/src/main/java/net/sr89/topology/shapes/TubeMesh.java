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
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

/**
 * Builds a single mesh (one vertex buffer, one draw call) shaped like a tube following a curve.
 */
public final class TubeMesh {
    /** A parametric curve. {@code t} is in [0, 1] along the curve; for closed curves t=1 is the same point as t=0. */
    public interface Curve {
        void pointAt(float t, Vector3 out);
    }

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
        final short[][] index = new short[rings][sides];

        final Vector3 center = new Vector3();
        final Vector3 tangent = new Vector3();
        final Vector3 normal = new Vector3();   // first axis of the ring's plane
        final Vector3 binormal = new Vector3(); // second axis of the ring's plane
        final Vector3 offset = new Vector3();
        final Vector3 position = new Vector3();
        final Vector3 ahead = new Vector3();
        final VertexInfo info = new VertexInfo();

        for (int i = 0; i < rings; i++) {
            final float t = (float) i / samples;
            curve.pointAt(t, center);
            tangentAt(curve, t, tangent, ahead, position);

            if (i == 0) {
                initialNormal(tangent, normal);
            } else {
                // Parallel transport: carry the previous normal along, only removing the part that
                // is no longer perpendicular to the new tangent. This keeps the ring from twisting.
                normal.mulAdd(tangent, -normal.dot(tangent)).nor();
            }
            binormal.set(tangent).crs(normal);

            for (int j = 0; j < sides; j++) {
                final float angle = MathUtils.PI2 * j / sides;
                // unit vector pointing from the center line out to the surface; it's also the surface normal
                offset.set(normal).scl(MathUtils.cos(angle)).mulAdd(binormal, MathUtils.sin(angle));
                position.set(center).mulAdd(offset, radius);
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
