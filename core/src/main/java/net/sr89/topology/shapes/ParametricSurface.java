package net.sr89.topology.shapes;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.DepthTestAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder.VertexInfo;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import net.sr89.topology.math.Surface;

/**
 * Builds a single mesh for a parametric surface, by sampling it on a grid of (u, v) values.
 * <p>
 * Vertices are not shared along the edges of the grid, even if the surface closes up there, so the mesh is simply a
 * grid of quads. Normals come from the surface itself (the cross product of its partial derivatives), so a surface
 * that closes up smoothly is shaded smoothly across the seam. Surfaces that can't be oriented (such as the Möbius
 * band) have normals that flip across the seam.
 */
public final class ParametricSurface {
    private static final float DERIVATIVE_STEP = 1e-3f;
    private static final float DEGENERATE_OFFSET = 1e-2f;
    private static final Vector3 SCRATCH_A = new Vector3(), SCRATCH_B = new Vector3(), SCRATCH_C = new Vector3(), SCRATCH_D = new Vector3();

    private ParametricSurface() {}

    /**
     * @param uSegments number of quads in the u direction
     * @param vSegments number of quads in the v direction
     * @param flipNormals the normals are the cross product of the u and v derivatives; set this if that points
     *                    inward (or to the "wrong" side) for the way the surface is parametrized
     */
    public static Model build(Surface surface, int uSegments, int vSegments, boolean flipNormals, Material material) {
        return build(surface, uSegments, vSegments, flipNormals, material, null);
    }

    /** The color of each point of a surface. */
    @FunctionalInterface
    public interface SurfaceColor {
        Color colorAt(float u, float v);
    }

    /**
     * Like {@link #build(Surface, int, int, boolean, Material)}, but every vertex has its own color (that multiplies
     * the color of the material, so use a white one). Vertex number {@code i * (vSegments + 1) + j} is the one at
     * {@code (i / uSegments, j / vSegments)}, so the colors can be changed later.
     *
     * @param colors the color of each point, or null for no colors
     */
    public static Model build(Surface surface, int uSegments, int vSegments, boolean flipNormals, Material material,
                              SurfaceColor colors) {
        final ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();
        final MeshPartBuilder builder = modelBuilder.part("surface", GL20.GL_TRIANGLES,
            colors == null ? Usage.Position | Usage.Normal : Usage.Position | Usage.Normal | Usage.ColorUnpacked, material);

        final short[][] index = new short[uSegments + 1][vSegments + 1];
        final Vector3 position = new Vector3();
        final Vector3 normal = new Vector3();
        final VertexInfo info = new VertexInfo();
        for (int i = 0; i <= uSegments; i++) {
            for (int j = 0; j <= vSegments; j++) {
                final float u = (float) i / uSegments, v = (float) j / vSegments;
                surface.pointAt(u, v, position);
                normalAt(surface, u, v, normal);
                if (flipNormals) {
                    normal.scl(-1f);
                }
                info.set(position, normal, colors == null ? null : colors.colorAt(u, v), null);
                index[i][j] = builder.vertex(info);
            }
        }
        for (int i = 0; i < uSegments; i++) {
            for (int j = 0; j < vSegments; j++) {
                builder.triangle(index[i][j], index[i + 1][j], index[i + 1][j + 1]);
                builder.triangle(index[i][j], index[i + 1][j + 1], index[i][j + 1]);
            }
        }
        return modelBuilder.end();
    }

    /** A translucent material that is visible from both sides, for surfaces that should be seen through. */
    public static Material translucentMaterial(Color color, float opacity) {
        return new Material(
            ColorAttribute.createDiffuse(color),
            new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, opacity),
            IntAttribute.createCullFace(GL20.GL_NONE),
            // Test against the depth buffer but don't write to it, so we can see through to the far side.
            new DepthTestAttribute(GL20.GL_LEQUAL, false));
    }

    /** An opaque material that is visible from both sides. */
    public static Material opaqueMaterial(Color color) {
        return new Material(ColorAttribute.createDiffuse(color), IntAttribute.createCullFace(GL20.GL_NONE));
    }

    /**
     * The unit normal of the surface at (u, v): the cross product of the u and v derivatives, normalized.
     * <p>
     * Where the parametrization is degenerate (for instance at a pole, where a whole edge of the square is squeezed into
     * a single point), the normal is taken a little way inside the square, which is its limit from there.
     * Uses scratch vectors shared by all calls, so it must not be called from several threads.
     */
    public static void normalAt(Surface surface, float u, float v, Vector3 out) {
        if (tryNormalAt(surface, u, v, out)) {
            return;
        }
        final float towardsMiddleU = u < 0.5f ? DEGENERATE_OFFSET : -DEGENERATE_OFFSET;
        final float towardsMiddleV = v < 0.5f ? DEGENERATE_OFFSET : -DEGENERATE_OFFSET;
        if (!tryNormalAt(surface, u, v + towardsMiddleV, out) && !tryNormalAt(surface, u + towardsMiddleU, v, out)) {
            out.set(Vector3.Y);
        }
    }

    private static boolean tryNormalAt(Surface surface, float u, float v, Vector3 out) {
        // Stay inside [0, 1] at the edges, by using a one-sided difference there
        final float u0 = Math.max(u - DERIVATIVE_STEP, 0f), u1 = Math.min(u + DERIVATIVE_STEP, 1f);
        final float v0 = Math.max(v - DERIVATIVE_STEP, 0f), v1 = Math.min(v + DERIVATIVE_STEP, 1f);
        surface.pointAt(u0, v, SCRATCH_A);
        surface.pointAt(u1, v, SCRATCH_B);
        surface.pointAt(u, v0, SCRATCH_C);
        surface.pointAt(u, v1, SCRATCH_D);
        final Vector3 alongU = SCRATCH_B.sub(SCRATCH_A), alongV = SCRATCH_D.sub(SCRATCH_C);
        final float lengths = alongU.len() * alongV.len();
        out.set(alongU).crs(alongV);
        // the derivatives are (nearly) parallel, or one of them is (nearly) zero
        if (lengths < 1e-12f || out.len() < 1e-4f * lengths) {
            return false;
        }
        out.nor();
        return true;
    }
}
