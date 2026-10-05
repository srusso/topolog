package net.sr89.topology.worlds;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder.VertexInfo;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import net.sr89.topology.HexColors;
import net.sr89.topology.shapes.ParametricSurface;
import net.sr89.topology.shapes.Primitives;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simplicial homology of the torus, on a triangulation of it: 48 vertices, 144 edges and 96 triangles (48 - 144 + 96 = 0).
 * <p>
 * A 1-chain is a set of edges, a 2-chain a set of triangles (counting modulo 2, to keep it simple: no orientations). The
 * boundary of a 2-chain is the set of edges that belong to an odd number of its triangles. A cycle is a 1-chain without
 * boundary (every vertex has an even number of its edges). The homology H₁ is the cycles, modulo the cycles that are
 * boundaries.
 * <ol>
 *   <li>A disc of triangles grows: its boundary (yellow) is a cycle that is a boundary. It can't be anything else.</li>
 *   <li>A strip of triangles grows between two parallel cycles: its boundary is the two cycles together, so they are
 *       homologous: the same element of H₁.</li>
 *   <li>The cycles a (red) and b (cyan) that go around the torus the long way and the short way are not
 *       boundaries of anything. They give H₁ = ℤ ⊕ ℤ (ℤ/2 ⊕ ℤ/2 with the coefficients used here).</li>
 *   <li>All the triangles together have no boundary, because every edge is in two triangles. This is H₂.</li>
 * </ol>
 */
public class TriangulatedTorusHomology implements World {
    private static final int U = 8, V = 6;
    private static final float MAJOR_RADIUS = 2f, MINOR_RADIUS = 0.8f;
    private static final float TRIANGLE_SHRINK = 0.9f;
    private static final Color PLAIN = HexColors.greenPastel();
    private static final Color CHAIN = new Color(1f, 0.62f, 0.2f, 1f);

    // seconds
    private static final float[] GROW = {7f, 7f, 0f, 8f};
    private static final float HOLD = 2.5f;
    private static final float STATIC_PHASE = 6f;
    private static final String[] TITLES = {
        "Simplicial homology: the boundary of a disc of triangles",
        "Simplicial homology: two cycles that differ by a boundary",
        "Simplicial homology: the cycles a and b are not boundaries",
        "Simplicial homology: all the triangles together have no boundary"};

    private final List<Model> models = new ArrayList<>();
    private final Vector3[] vertices = new Vector3[U * V];
    private final int[][] triangles = new int[2 * U * V][];
    private final int[][] triangleEdges = new int[triangles.length][];
    private final List<int[]> edges = new ArrayList<>();
    private final Map<Long, Integer> edgeIndex = new HashMap<>();
    private final ModelInstance[] base, boundary, cycleA, cycleB;
    private final boolean[] onA, onB;
    private final ModelInstance surface;
    private final Mesh mesh;
    private final float[] meshVertices;
    private final int stride, colorOffset;

    private final int[] discOrder, stripOrder, allOrder;
    private final boolean[] inChain = new boolean[triangles.length];
    private final int[] edgeCount;
    private int phase = -1;
    private int shownSize = -1;
    private float time = 0f;

    public TriangulatedTorusHomology() {
        for (int j = 0; j < V; j++) {
            for (int i = 0; i < U; i++) {
                vertices[vertex(i, j)] = torusPoint(i, j, 0f);
            }
        }
        for (int j = 0; j < V; j++) {
            for (int i = 0; i < U; i++) {
                final int a = vertex(i, j), b = vertex(i + 1, j), c = vertex(i + 1, j + 1), d = vertex(i, j + 1);
                final int t = 2 * (j * U + i);
                triangles[t] = new int[] {a, b, c};
                triangles[t + 1] = new int[] {a, c, d};
            }
        }
        for (int t = 0; t < triangles.length; t++) {
            triangleEdges[t] = new int[3];
            for (int k = 0; k < 3; k++) {
                triangleEdges[t][k] = edgeOf(triangles[t][k], triangles[t][(k + 1) % 3]);
            }
        }
        edgeCount = new int[edges.size()];

        // the cycles a (the edges along u, at v = 0) and b (the edges along v, at u = 0)
        onA = new boolean[edges.size()];
        onB = new boolean[edges.size()];
        for (int i = 0; i < U; i++) {
            onA[edgeOf(vertex(i, 0), vertex(i + 1, 0))] = true;
        }
        for (int j = 0; j < V; j++) {
            onB[edgeOf(vertex(0, j), vertex(0, j + 1))] = true;
        }

        // the rods for the edges: thin ones for all, and thick ones for the highlighted ones
        final Model thin = model(Primitives.rod(Color.DARK_GRAY));
        final Model yellow = model(Primitives.rod(Color.YELLOW));
        final Model red = model(Primitives.rod(Color.RED));
        final Model cyan = model(Primitives.rod(Color.CYAN));
        base = new ModelInstance[edges.size()];
        boundary = new ModelInstance[edges.size()];
        cycleA = new ModelInstance[edges.size()];
        cycleB = new ModelInstance[edges.size()];
        for (int e = 0; e < edges.size(); e++) {
            final Vector3 from = lifted(edges.get(e)[0]), to = lifted(edges.get(e)[1]);
            base[e] = new ModelInstance(thin);
            Primitives.place(base[e], from, to, 0.025f);
            boundary[e] = new ModelInstance(yellow);
            Primitives.place(boundary[e], from, to, 0.09f);
            cycleA[e] = new ModelInstance(red);
            Primitives.place(cycleA[e], from, to, 0.09f);
            cycleB[e] = new ModelInstance(cyan);
            Primitives.place(cycleB[e], from, to, 0.09f);
        }

        // the triangles, each with its own vertices, to color them separately, and a bit smaller to see the edges
        final ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();
        final MeshPartBuilder builder = modelBuilder.part("triangles", GL20.GL_TRIANGLES,
            Usage.Position | Usage.Normal | Usage.ColorUnpacked, ParametricSurface.opaqueMaterial(Color.WHITE));
        final VertexInfo info = new VertexInfo();
        for (int[] triangle : triangles) {
            final Vector3 a = vertices[triangle[0]], b = vertices[triangle[1]], c = vertices[triangle[2]];
            final Vector3 center = new Vector3(a).add(b).add(c).scl(1f / 3f);
            final Vector3 normal = new Vector3(b).sub(a).crs(new Vector3(c).sub(a)).nor();
            if (normal.dot(outward(center)) < 0) {
                normal.scl(-1f);
            }
            final short[] index = new short[3];
            for (int k = 0; k < 3; k++) {
                final Vector3 shrunk = new Vector3(center).lerp(vertices[triangle[k]], TRIANGLE_SHRINK).mulAdd(normal, 0.01f);
                info.set(shrunk, normal, PLAIN, null);
                index[k] = builder.vertex(info);
            }
            builder.triangle(index[0], index[1], index[2]);
        }
        final Model surfaceModel = modelBuilder.end();
        models.add(surfaceModel);
        surface = new ModelInstance(surfaceModel);
        mesh = surfaceModel.meshes.first();
        stride = mesh.getVertexSize() / 4;
        colorOffset = mesh.getVertexAttribute(Usage.ColorUnpacked).offset / 4;
        meshVertices = new float[mesh.getNumVertices() * stride];
        mesh.getVertices(meshVertices);

        discOrder = discOrder();
        stripOrder = orderOfRows(3);
        allOrder = orderOfRows(V);
        reposition(0f);
    }

    @Override
    public String getWorldTitle() {
        return TITLES[Math.max(phase, 0)];
    }

    @Override
    public void reposition(float deltaTime) {
        time += deltaTime;
        final float cycle = totalTime();
        float t = time % cycle;
        int current = 0;
        while (t >= phaseTime(current)) {
            t -= phaseTime(current);
            current++;
        }

        final int[] order = current == 0 ? discOrder : current == 1 ? stripOrder : current == 3 ? allOrder : new int[0];
        final float growing = GROW[current];
        final int size = growing > 0 ? Math.round(order.length * Math.min(1f, t / growing)) : 0;
        if (current != phase || size != shownSize) {
            phase = current;
            shownSize = size;
            setChain(Arrays.copyOf(order, size));
        }
    }

    @Override
    public void render(ModelBatch modelBatch, Environment environment) {
        modelBatch.render(surface, environment);
        for (int e = 0; e < edges.size(); e++) {
            modelBatch.render(base[e], environment);
            if (phase == 2) {
                if (onA[e]) {
                    modelBatch.render(cycleA[e], environment);
                }
                if (onB[e]) {
                    modelBatch.render(cycleB[e], environment);
                }
            } else if (edgeCount[e] % 2 == 1) {
                modelBatch.render(boundary[e], environment);
            }
        }
    }

    @Override
    public void dispose() {
        models.forEach(Model::dispose);
    }

    private static float phaseTime(int phase) {
        return phase == 2 ? STATIC_PHASE : GROW[phase] + HOLD;
    }

    private static float totalTime() {
        float total = 0;
        for (int p = 0; p < GROW.length; p++) {
            total += phaseTime(p);
        }
        return total;
    }

    /** Chooses the triangles of the chain, colors them, and counts how many triangles each edge is in. */
    private void setChain(int[] chain) {
        Arrays.fill(inChain, false);
        Arrays.fill(edgeCount, 0);
        for (int t : chain) {
            inChain[t] = true;
            for (int e : triangleEdges[t]) {
                edgeCount[e]++;
            }
        }
        for (int t = 0; t < triangles.length; t++) {
            final Color color = inChain[t] ? CHAIN : PLAIN;
            for (int k = 0; k < 3; k++) {
                final int at = (3 * t + k) * stride + colorOffset;
                meshVertices[at] = color.r;
                meshVertices[at + 1] = color.g;
                meshVertices[at + 2] = color.b;
                meshVertices[at + 3] = 1f;
            }
        }
        mesh.setVertices(meshVertices);
    }

    /** The triangles by their distance from the first one; only the prefixes that are discs are used. */
    private int[] discOrder() {
        final Vector3[] centers = new Vector3[triangles.length];
        for (int t = 0; t < triangles.length; t++) {
            centers[t] = new Vector3(vertices[triangles[t][0]]).add(vertices[triangles[t][1]]).add(vertices[triangles[t][2]]).scl(1f / 3f);
        }
        final int seed = 2 * (2 * U + 1);
        final List<Integer> sorted = new ArrayList<>();
        for (int t = 0; t < triangles.length; t++) {
            sorted.add(t);
        }
        sorted.sort((a, b) -> Float.compare(centers[a].dst2(centers[seed]), centers[b].dst2(centers[seed])));

        // keep adding triangles, but only keep the ones that make the chain a disc (and stop before it wraps around)
        final List<Integer> chain = new ArrayList<>();
        for (int t : sorted) {
            if (chain.size() >= 36) {
                break;
            }
            chain.add(t);
            if (!isDisc(chain)) {
                chain.remove(chain.size() - 1);
            }
        }
        return chain.stream().mapToInt(Integer::intValue).toArray();
    }

    /** The triangles of the first rows of cells, one cell after the other. */
    private int[] orderOfRows(int rows) {
        final int[] order = new int[2 * U * rows];
        for (int k = 0; k < order.length; k++) {
            order[k] = k; // the triangles are numbered row by row
        }
        return order;
    }

    /** Whether the triangles are a disc: connected, with one boundary, so with Euler characteristic 1. */
    private boolean isDisc(List<Integer> chain) {
        final Map<Integer, Integer> counts = new HashMap<>();
        final boolean[] vertexUsed = new boolean[vertices.length];
        for (int t : chain) {
            for (int e : triangleEdges[t]) {
                counts.merge(e, 1, Integer::sum);
            }
            for (int v : triangles[t]) {
                vertexUsed[v] = true;
            }
        }
        int vertexCount = 0;
        for (boolean used : vertexUsed) {
            vertexCount += used ? 1 : 0;
        }
        if (vertexCount - counts.size() + chain.size() != 1) {
            return false;
        }
        // a single boundary: the boundary edges, joined at the vertices, make one cycle
        final Map<Integer, Integer> boundaryAt = new HashMap<>();
        int boundaryEdges = 0;
        for (Map.Entry<Integer, Integer> entry : counts.entrySet()) {
            if (entry.getValue() == 1) {
                boundaryEdges++;
                for (int v : edges.get(entry.getKey())) {
                    boundaryAt.merge(v, 1, Integer::sum);
                }
            }
        }
        return boundaryEdges > 0 && boundaryAt.values().stream().allMatch(count -> count == 2);
    }

    private static int vertex(int i, int j) {
        return Math.floorMod(j, V) * U + Math.floorMod(i, U);
    }

    private int edgeOf(int a, int b) {
        final long key = ((long) Math.min(a, b) << 32) | Math.max(a, b);
        return edgeIndex.computeIfAbsent(key, k -> {
            edges.add(new int[] {Math.min(a, b), Math.max(a, b)});
            return edges.size() - 1;
        });
    }

    private static Vector3 torusPoint(int i, int j, float lift) {
        final double alpha = 2 * Math.PI * i / U, beta = 2 * Math.PI * j / V;
        final double minor = MINOR_RADIUS + lift;
        final double ring = MAJOR_RADIUS + minor * Math.cos(beta);
        return new Vector3((float) (ring * Math.cos(alpha)), (float) (minor * Math.sin(beta)), (float) (ring * Math.sin(alpha)));
    }

    /** The direction pointing out of the torus, at a point. */
    private static Vector3 outward(Vector3 point) {
        final Vector3 ringCenter = new Vector3(point.x, 0f, point.z).nor().scl(MAJOR_RADIUS);
        return new Vector3(point).sub(ringCenter).nor();
    }

    /** The place of a vertex, a bit above the surface, for the rods. */
    private Vector3 lifted(int vertex) {
        return new Vector3(vertices[vertex]).mulAdd(outward(vertices[vertex]), 0.03f);
    }

    /** Takes ownership of the model to dispose of it later. */
    private Model model(Model model) {
        models.add(model);
        return model;
    }
}
