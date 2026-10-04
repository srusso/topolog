package net.sr89.topology.worlds;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.math.Vector3;
import net.sr89.topology.HexColors;
import net.sr89.topology.shapes.FoldingMesh;
import net.sr89.topology.shapes.ParametricSurface;

import java.util.ArrayList;
import java.util.List;

/**
 * The torus as a polygon with its edges glued, shown for two polygons in turn:
 * <ol>
 *   <li>The square with opposite sides glued, a b a⁻¹ b⁻¹.</li>
 *   <li>The hexagon with opposite sides glued, which gives the same surface from a different polygon.</li>
 * </ol>
 * Edges of the polygon that are glued together have the same color. The polygon is cut open along the colored edges until
 * it is folded all the way, where the two edges of each color come together. The fundamental group has one generator for each
 * pair of glued edges (all the vertices are identified to the same point), and one relation: going around the polygon.
 * <p>
 * The octagon that gives the genus 2 surface is in {@link GenusTwoPolygon}.
 */
public class TorusPolygons implements World {
    private static final float TORUS_MAJOR_RADIUS = 1.5f;
    private static final float TORUS_MINOR_RADIUS = 0.55f;
    private static final float SQUARE_SIDE = 3f;
    private static final int SQUARE_CELLS = 40;
    private static final int HEXAGON_CELLS = 20; // lattice steps from the center to a corner
    private static final float HEXAGON_STEP = 0.14f;
    /** How wide the colored band along the glued edges is. */
    private static final float EDGE_BAND = 0.09f;

    private static final float CYCLE = 2 * FoldTiming.CYCLE;

    private static final Color PLAIN = HexColors.greenPastel();

    private final FoldingMesh square;
    private final FoldingMesh hexagon;

    private float time = 0f;
    private String title = "";

    public TorusPolygons() {
        square = createSquare();
        hexagon = createHexagon();
        reposition(0f);
    }

    @Override
    public String getWorldTitle() {
        return title;
    }

    @Override
    public void reposition(float deltaTime) {
        time = (time + deltaTime) % CYCLE;

        if (time < FoldTiming.CYCLE) {
            square.setFold(FoldTiming.amount(time));
            title = "Square, opposite sides glued: a b a^-1 b^-1 -> the torus";
        } else {
            hexagon.setFold(FoldTiming.amount(time - FoldTiming.CYCLE));
            title = "Hexagon, opposite sides glued: also the torus";
        }
    }

    @Override
    public void render(ModelBatch modelBatch, Environment environment) {
        if (time < FoldTiming.CYCLE) {
            square.render(modelBatch, environment);
        } else {
            hexagon.render(modelBatch, environment);
        }
    }

    @Override
    public void dispose() {
        square.dispose();
        hexagon.dispose();
    }

    /**
     * The torus: u goes the long way around (about the Y axis), v goes the short way, both in turns.
     */
    private static void torusPoint(double u, double v, Vector3 out) {
        final double alpha = 2 * Math.PI * u, beta = 2 * Math.PI * v;
        final double ring = TORUS_MAJOR_RADIUS + TORUS_MINOR_RADIUS * Math.cos(beta);
        out.set((float) (ring * Math.cos(alpha)), (float) (TORUS_MINOR_RADIUS * Math.sin(beta)), (float) (ring * Math.sin(alpha)));
    }

    private static FoldingMesh createSquare() {
        final List<Vector3> positions = new ArrayList<>();
        final List<Color> colors = new ArrayList<>();
        final int points = SQUARE_CELLS + 1;
        for (int i = 0; i < points; i++) {
            for (int j = 0; j < points; j++) {
                final float x = ((float) i / SQUARE_CELLS - 0.5f) * SQUARE_SIDE;
                final float z = ((float) j / SQUARE_CELLS - 0.5f) * SQUARE_SIDE;
                positions.add(new Vector3(x, 0f, z));
                // the sides at left and right are glued to each other (b), and so are those at the top and the bottom (a)
                final float half = SQUARE_SIDE / 2;
                final float toSidesOfB = half - Math.abs(x), toSidesOfA = half - Math.abs(z);
                if (Math.min(toSidesOfA, toSidesOfB) > EDGE_BAND) {
                    colors.add(PLAIN);
                } else {
                    colors.add(toSidesOfA < toSidesOfB ? Color.RED : Color.CYAN);
                }
            }
        }
        final List<int[]> triangles = new ArrayList<>();
        for (int i = 0; i < SQUARE_CELLS; i++) {
            for (int j = 0; j < SQUARE_CELLS; j++) {
                final int a = i * points + j, b = (i + 1) * points + j, c = (i + 1) * points + j + 1, d = i * points + j + 1;
                triangles.add(new int[] {a, b, c});
                triangles.add(new int[] {a, c, d});
            }
        }
        return new FoldingMesh(positions, triangles, colors,
            (flat, out) -> torusPoint(flat.x / SQUARE_SIDE + 0.5, flat.z / SQUARE_SIDE + 0.5, out),
            ParametricSurface.opaqueMaterial(Color.WHITE));
    }

    private static FoldingMesh createHexagon() {
        // The points of a hexagon-shaped patch of the triangular lattice: i * (1, 0) + j * (1/2, √3/2), scaled
        final int k = HEXAGON_CELLS;
        final int[][] indexOf = new int[2 * k + 1][2 * k + 1];
        final List<Vector3> positions = new ArrayList<>();
        final List<Color> colors = new ArrayList<>();
        final double apothem = k * HEXAGON_STEP * Math.sqrt(3) / 2;
        for (int i = -k; i <= k; i++) {
            for (int j = -k; j <= k; j++) {
                if (Math.abs(i + j) > k) {
                    indexOf[i + k][j + k] = -1;
                    continue;
                }
                indexOf[i + k][j + k] = positions.size();
                final float x = HEXAGON_STEP * (i + j / 2f);
                final float z = HEXAGON_STEP * (float) (j * Math.sqrt(3) / 2);
                positions.add(new Vector3(x, 0f, z));
                colors.add(hexagonEdgeColor(x, z, apothem));
            }
        }
        final List<int[]> triangles = new ArrayList<>();
        for (int i = -k; i < k; i++) {
            for (int j = -k; j < k; j++) {
                final int a = indexOf[i + k][j + k], b = indexOf[i + 1 + k][j + k];
                final int c = indexOf[i + 1 + k][j + 1 + k], d = indexOf[i + k][j + 1 + k];
                // The two triangles of the rhombus (cut along its short diagonal), if their corners are inside the hexagon.
                if (a >= 0 && b >= 0 && d >= 0) {
                    triangles.add(new int[] {a, b, d});
                }
                if (b >= 0 && c >= 0 && d >= 0) {
                    triangles.add(new int[] {b, c, d});
                }
            }
        }

        // Moving by 2 apothems towards the middle of an edge gets to the same place on the opposite edge. Writing a point
        // as a combination of two of those moves (and where the torus is concerned only the fractional part counts)
        // gives the point of the torus.
        final double move = 2 * apothem;
        return new FoldingMesh(positions, triangles, colors, (flat, out) -> {
            final double alpha = flat.x / (move * Math.cos(Math.PI / 6));
            final double beta = (flat.z - alpha * move * Math.sin(Math.PI / 6)) / move;
            torusPoint(alpha, beta, out);
        }, ParametricSurface.opaqueMaterial(Color.WHITE));
    }

    /** The edges of the hexagon are in pairs of opposite edges, with the edge of the first pair facing 30°. */
    private static Color hexagonEdgeColor(float x, float z, double apothem) {
        final Color[] pairColors = {Color.RED, Color.CYAN, Color.ORANGE};
        double nearest = Double.MAX_VALUE;
        int nearestEdge = 0;
        for (int edge = 0; edge < 6; edge++) {
            final double angle = Math.toRadians(30 + 60 * edge);
            final double distance = apothem - (x * Math.cos(angle) + z * Math.sin(angle));
            if (distance < nearest) {
                nearest = distance;
                nearestEdge = edge;
            }
        }
        return nearest > EDGE_BAND ? PLAIN : pairColors[nearestEdge % 3];
    }
}
