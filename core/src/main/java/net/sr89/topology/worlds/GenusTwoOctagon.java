package net.sr89.topology.worlds;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import net.sr89.topology.shapes.FoldingMesh;
import net.sr89.topology.shapes.MarchingTetrahedra;
import net.sr89.topology.shapes.ParametricSurface;
import net.sr89.topology.shapes.SurfaceOctagon;

import java.util.ArrayList;
import java.util.List;

/**
 * The octagon a₁ b₁ a₁⁻¹ b₁⁻¹ a₂ b₂ a₂⁻¹ b₂⁻¹ that folds up into the genus 2 surface of {@link GenusTwoSurface}.
 * <p>
 * The octagon is what remains of the surface when it is cut open along four loops through one point, the top of the
 * crossing of the figure eight (the base). The loops are chosen as follows, for the surface in the unit coordinates of its equation
 * (where the left lobe has x &lt; 0), and the other lobe has the same by turning around the Z axis half a turn:
 * <ul>
 *   <li>a₁ goes around the hole of the left lobe, along the crest of the surface (where g(x, y) = 0, at the top).</li>
 *   <li>b₁ goes around the tube of the left lobe, once. It leaves the base towards the hole, over the top, down the wall
 *       of the hole and under the surface, up the outer wall on the side of y &gt; 0, and comes back to the base from y &gt; 0.</li>
 * </ul>
 * The four of them leave the base in this order, which is what makes the rest of the surface a single disc:
 * b₂ (0°), a₂ (45°), b₁ (90°), a₁ (135°), b₁ (180°), a₁ (225°), b₂ (270°), a₂ (315°).
 */
final class GenusTwoOctagon {
    private static final float CELL = 1f / 32f;
    private static final float FAN_RADIUS = 0.075f;
    /** Which side of the octagon each loop is, as a color; the sides that are glued together have the same color. */
    static final Color[] LOOP_COLORS = {Color.RED, Color.CYAN, Color.ORANGE, Color.MAGENTA};

    private GenusTwoOctagon() {}

    /** The octagon, and the surface that it folds into, in the unit coordinates of the surface's equation. */
    static SurfaceOctagon.Result compute(float octagonRadius) {
        final float r = GenusTwoSurface.TUBE_RADIUS;
        final Vector3 base = new Vector3(0f, 0f, r);

        // a₁: along the crest, from the base around the left lobe and back
        final List<Vector3> crest = new ArrayList<>();
        for (int k = 1; k <= 9; k++) {
            final double psi = Math.PI * k / 10;
            crest.add(new Vector3((float) -Math.sin(psi), (float) (Math.sin(psi) * Math.cos(psi)), r));
        }

        // b₁: around the tube of the left lobe
        final float innerRim = -(float) Math.sqrt((1 - Math.sqrt(1 - 4 * r)) / 2); // on the wall of the hole, at y = 0
        final float outerRimX = -0.1f;
        final float outerRimY = (float) Math.sqrt(outerRimX * outerRimX * (1 - outerRimX * outerRimX) + r);
        final List<Vector3> meridian = List.of(
            new Vector3(innerRim, 0f, 0f),
            onSurface(-0.25f, 0f, false),
            onSurface(-0.12f, 0.2f, false),
            new Vector3(outerRimX, outerRimY, 0f),
            onSurface(-0.08f, 0.2f, true));

        final List<SurfaceOctagon.Loop> loops = List.of(
            new SurfaceOctagon.Loop(135, 225, crest),
            new SurfaceOctagon.Loop(180, 90, meridian),
            new SurfaceOctagon.Loop(315, 45, turnAroundZ(crest)),
            new SurfaceOctagon.Loop(0, 270, turnAroundZ(meridian)));

        final MarchingTetrahedra.Triangulation surface = GenusTwoSurface.createTriangulation(CELL);
        return SurfaceOctagon.build(surface, base, FAN_RADIUS, loops, octagonRadius);
    }

    /**
     * The octagon as a mesh that folds up into the surface, in the coordinates of the world. The sides of the octagon
     * that are glued together are colored the same.
     */
    static FoldingMesh create(float octagonRadius, float edgeBand, Color plain) {
        final SurfaceOctagon.Result result = compute(octagonRadius);

        final List<Vector3> folded = new ArrayList<>(), normals = new ArrayList<>();
        for (int i = 0; i < result.folded().size(); i++) {
            final Vector3 p = result.folded().get(i), n = result.foldedNormals().get(i);
            final Vector3 world = new Vector3();
            GenusTwoSurface.toWorld(p.x, p.y, p.z, world);
            folded.add(world);
            // the same turn of the axes, without the scaling
            normals.add(new Vector3(n.x, n.z, -n.y));
        }

        final float apothem = octagonRadius * (float) Math.cos(Math.PI / 8);
        final List<Color> colors = new ArrayList<>();
        for (Vector3 flat : result.flat()) {
            colors.add(edgeColor(flat, result.edgeLoops(), apothem, edgeBand, plain));
        }
        return new FoldingMesh(result.flat(), result.triangles(), colors, folded, normals,
            ParametricSurface.opaqueMaterial(Color.WHITE));
    }

    private static Color edgeColor(Vector3 flat, int[] edgeLoops, float apothem, float edgeBand, Color plain) {
        double nearest = Double.MAX_VALUE;
        int nearestSide = 0;
        for (int side = 0; side < edgeLoops.length; side++) {
            // the sides face the angles 45°, 90°, ... (the corners are at 22.5° and every 45° after that)
            final double angle = Math.PI / 4 * (side + 1);
            final double distance = apothem - (flat.x * Math.cos(angle) + flat.z * Math.sin(angle));
            if (distance < nearest) {
                nearest = distance;
                nearestSide = side;
            }
        }
        return nearest > edgeBand ? plain : LOOP_COLORS[edgeLoops[nearestSide]];
    }

    /** A point of the surface above or below the given place, on the top or the bottom of it. */
    private static Vector3 onSurface(float x, float y, boolean top) {
        final float r = GenusTwoSurface.TUBE_RADIUS;
        final float g = GenusTwoSurface.g(x, y);
        final float z = (float) Math.sqrt(r * r - g * g);
        return new Vector3(x, y, top ? z : -z);
    }

    /** The same points turned around the Z axis by half a turn, which is a symmetry of the surface. */
    private static List<Vector3> turnAroundZ(List<Vector3> points) {
        final List<Vector3> result = new ArrayList<>();
        for (Vector3 point : points) {
            result.add(new Vector3(-point.x, -point.y, point.z));
        }
        return result;
    }
}
