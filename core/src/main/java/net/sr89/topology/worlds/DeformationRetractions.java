package net.sr89.topology.worlds;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.math.Vector3;
import net.sr89.topology.shapes.FoldingMesh;
import net.sr89.topology.shapes.ParametricSurface;

import java.util.ArrayList;
import java.util.List;

/**
 * Deformation retractions: a space X is deformed onto a subspace A, by a homotopy that starts as the identity of X, ends as a
 * map into A, and never moves the points of A. Then X and A are homotopy equivalent, and have the same fundamental group,
 * the same homology...
 * <p>
 * Three of them, one after the other. Each point moves in a straight line to its place in A; the points of A are red:
 * <ol>
 *   <li>The disc onto its center: the disc is contractible.</li>
 *   <li>The annulus onto its core circle: the annulus has the homotopy type of the circle, so its fundamental group is ℤ.</li>
 *   <li>The Möbius band onto its core circle: so the Möbius band also has the fundamental group ℤ, even though it is not an annulus.</li>
 * </ol>
 */
public class DeformationRetractions implements World {
    private static final float RADIUS = 2.2f;
    private static final float CORE_RADIUS = 1.6f;
    private static final float INNER_RADIUS = 1.0f;
    private static final int SECTORS = 64;
    private static final float MOBIUS_RADIUS = 1.8f, MOBIUS_HALF_WIDTH = 0.7f;
    private static final int MOBIUS_STEPS = 128, MOBIUS_STRIPES = 10;

    private static final Color LIGHT = new Color(0.65f, 0.71f, 0.55f, 1f);
    private static final Color DARK = new Color(0.42f, 0.50f, 0.36f, 1f);
    private static final Color FIXED = Color.RED;
    private static final String[] TITLES = {
        "Deformation retraction: the disc onto a point",
        "Deformation retraction: the annulus onto its core circle",
        "Deformation retraction: the Mobius band onto its core circle"};

    private final FoldingMesh[] shapes;
    private float time = 0f;
    private int current = 0;

    public DeformationRetractions() {
        shapes = new FoldingMesh[] {createDisc(), createAnnulus(), createMobius()};
        reposition(0f);
    }

    @Override
    public String getWorldTitle() {
        return TITLES[current];
    }

    @Override
    public void reposition(float deltaTime) {
        time = (time + deltaTime) % (shapes.length * FoldTiming.CYCLE);
        current = (int) (time / FoldTiming.CYCLE);
        shapes[current].setFold(FoldTiming.amount(time - current * FoldTiming.CYCLE));
    }

    @Override
    public void render(ModelBatch modelBatch, Environment environment) {
        shapes[current].render(modelBatch, environment);
    }

    @Override
    public void dispose() {
        for (FoldingMesh shape : shapes) {
            shape.dispose();
        }
    }

    /** The disc, as rings of vertices. It is deformed onto the point at its center. */
    private static FoldingMesh createDisc() {
        final int rings = 14;
        return polarMesh(0f, RADIUS, rings, (flat, out) -> out.set(0f, 0f, 0f), ring -> ring == 0 ? FIXED : bandColor(ring));
    }

    /** The annulus, deformed onto the circle halfway between its two sides. */
    private static FoldingMesh createAnnulus() {
        final int rings = 12; // from the radius 1.0 to 2.2, in steps of 0.1: the circle of radius 1.6 is a ring
        return polarMesh(INNER_RADIUS, RADIUS, rings,
            (flat, out) -> out.set(flat).nor().scl(CORE_RADIUS),
            ring -> Math.abs(INNER_RADIUS + (RADIUS - INNER_RADIUS) * ring / rings - CORE_RADIUS) < 1e-4 ? FIXED : bandColor(ring));
    }

    private interface RingColor {
        Color of(int ring);
    }

    private static Color bandColor(int ring) {
        return ring % 2 == 0 ? LIGHT : DARK;
    }

    private static FoldingMesh polarMesh(float inner, float outer, int rings, java.util.function.BiConsumer<Vector3, Vector3> retraction,
                                         RingColor ringColor) {
        final List<Vector3> positions = new ArrayList<>();
        final List<Color> colors = new ArrayList<>();
        for (int ring = 0; ring <= rings; ring++) {
            final float radius = inner + (outer - inner) * ring / rings;
            for (int i = 0; i < SECTORS; i++) {
                final double angle = 2 * Math.PI * i / SECTORS;
                positions.add(new Vector3((float) (radius * Math.cos(angle)), 0f, (float) (radius * Math.sin(angle))));
                colors.add(ringColor.of(ring));
            }
        }
        final List<int[]> triangles = new ArrayList<>();
        for (int ring = 0; ring < rings; ring++) {
            for (int i = 0; i < SECTORS; i++) {
                final int next = (i + 1) % SECTORS;
                final int a = ring * SECTORS + i, b = ring * SECTORS + next;
                final int c = (ring + 1) * SECTORS + next, d = (ring + 1) * SECTORS + i;
                triangles.add(new int[] {a, d, c});
                triangles.add(new int[] {a, c, b});
            }
        }
        return new FoldingMesh(positions, triangles, colors, retraction, ParametricSurface.opaqueMaterial(Color.WHITE));
    }

    /** The Möbius band, deformed onto its core circle: every point goes to the core, across the width. */
    private static FoldingMesh createMobius() {
        final List<Vector3> positions = new ArrayList<>();
        final List<Color> colors = new ArrayList<>();
        for (int i = 0; i <= MOBIUS_STEPS; i++) {
            final double angle = 2 * Math.PI * i / MOBIUS_STEPS;
            for (int j = 0; j <= MOBIUS_STRIPES; j++) {
                final double offset = MOBIUS_HALF_WIDTH * (2.0 * j / MOBIUS_STRIPES - 1);
                final double ring = MOBIUS_RADIUS + offset * Math.cos(angle / 2);
                positions.add(new Vector3((float) (ring * Math.cos(angle)), (float) (offset * Math.sin(angle / 2)),
                    (float) (ring * Math.sin(angle))));
                colors.add(j == MOBIUS_STRIPES / 2 ? FIXED : bandColor(j));
            }
        }
        final List<int[]> triangles = new ArrayList<>();
        for (int i = 0; i < MOBIUS_STEPS; i++) {
            for (int j = 0; j < MOBIUS_STRIPES; j++) {
                final int a = i * (MOBIUS_STRIPES + 1) + j, b = (i + 1) * (MOBIUS_STRIPES + 1) + j;
                final int c = b + 1, d = a + 1;
                triangles.add(new int[] {a, b, c});
                triangles.add(new int[] {a, c, d});
            }
        }
        // the core circle below each point: the same angle around the axis
        return new FoldingMesh(positions, triangles, colors, (flat, out) -> {
            final double angle = Math.atan2(flat.z, flat.x);
            out.set((float) (MOBIUS_RADIUS * Math.cos(angle)), 0f, (float) (MOBIUS_RADIUS * Math.sin(angle)));
        }, ParametricSurface.opaqueMaterial(Color.WHITE));
    }
}
