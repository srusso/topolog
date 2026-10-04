package net.sr89.topology.shapes;

import com.badlogic.gdx.math.Vector3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Cuts a closed genus 2 surface (given as a triangle mesh) open along four loops, and flattens the result into a regular
 * octagon.
 * <p>
 * The four loops all go through one point, the base, and leave it in the order that makes the surface fall apart into a
 * single disc, whose boundary goes along each loop twice: that disc is the octagon with its edges to be glued,
 * a₁ b₁ a₁⁻¹ b₁⁻¹ a₂ b₂ a₂⁻¹ b₂⁻¹ (for the loops in the order a₁, b₁, a₂, b₂, up to where it starts and which way around it goes).
 * <p>
 * The steps:
 * <ol>
 *   <li>Give the base enough neighbours for 8 edges to leave it: remove the triangles around it and fill the hole
 *       again, as a fan of triangles around a new vertex.</li>
 *   <li>Find each loop on the mesh edges, as a chain of shortest paths through the points that describe it.</li>
 *   <li>Cut: every vertex on a loop becomes one vertex for each of the pieces that its triangles are split into.</li>
 *   <li>Flatten: put the boundary of the disc on the sides of the octagon, each point at its share of the length of the
 *       side, and spread the other vertices over it, each at the weighted average of its neighbours (Floater's mean value
 *       coordinates, a Tutte embedding). This puts every triangle in the right way around, as the boundary is convex.</li>
 * </ol>
 * Folding the octagon back is then moving each vertex to the place that it came from; the vertices on the two sides of a
 * cut have the same place, so the octagon closes up into the surface.
 */
public final class SurfaceOctagon {
    /**
     * A loop through the base.
     *
     * @param startAngle the direction in which it leaves the base, as an angle in the XY plane, in degrees
     * @param endAngle   the direction from which it comes back to the base
     * @param waypoints  points of the surface through which it goes, in order
     */
    public record Loop(float startAngle, float endAngle, List<Vector3> waypoints) {}

    /**
     * The octagon, and the surface it folds into.
     *
     * @param flat          the position of each vertex in the flat octagon (in the XZ plane, Y = 0)
     * @param folded        the position of each vertex in the surface (equal for vertices on glued edges)
     * @param foldedNormals the normal vectors of the surface at those places
     * @param triangles     the vertices of each triangle
     * @param edgeLoops     the loop (its number in the list given) that each side of the octagon goes along. Side
     *                      i goes from corner i to corner i + 1, and corner 0 is at the angle of 22.5 degrees from the X
     *                      axis towards the Z axis, then the corners go around at 45 degrees each.
     * @param edgeForward   whether each side goes along its loop in the direction in which that loop was described
     */
    public record Result(List<Vector3> flat, List<Vector3> folded, List<Vector3> foldedNormals, List<int[]> triangles,
                         int[] edgeLoops, boolean[] edgeForward) {}

    private static final int SIDES = 8;
    private static final double RELAXATION_FACTOR = 1.5;
    private static final double TOLERANCE = 1e-7;
    private static final int MAX_ITERATIONS = 20000;
    private static final double MIN_LENGTH = 1e-6;
    private static final double MAX_ANGLE = Math.PI - 1e-3;

    private List<Vector3> positions;
    private List<int[]> triangles;
    private int base;
    private int[][] neighbours;
    private boolean[] used;

    private SurfaceOctagon() {}

    /**
     * @param surface   the surface, as a mesh with triangles facing outward
     * @param basePoint a point of the surface, through which all the loops go
     * @param fanRadius the vertices of the mesh closer than this to the base are replaced by a fan around it
     * @param loops     the 4 loops: a₁, b₁, a₂, b₂
     * @param radius    the radius of the octagon
     */
    public static Result build(MarchingTetrahedra.Triangulation surface, Vector3 basePoint, float fanRadius,
                               List<Loop> loops, float radius) {
        return new SurfaceOctagon().run(surface, basePoint, fanRadius, loops, radius);
    }

    private Result run(MarchingTetrahedra.Triangulation surface, Vector3 basePoint, float fanRadius,
                       List<Loop> loops, float radius) {
        positions = new ArrayList<>();
        for (Vector3 position : surface.positions()) {
            positions.add(new Vector3(position));
        }
        triangles = new ArrayList<>(surface.triangles());
        final List<Vector3> normals = new ArrayList<>(surface.normals());

        insertFan(basePoint, fanRadius);
        normals.add(new Vector3(Vector3.Z)); // the base is at the top of the surface, where it is flat
        buildNeighbours();

        final List<List<Integer>> paths = findPaths(loops);
        final Cut cut = cut(paths);
        return flatten(cut, paths, normals, radius);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // 1. The fan around the base

    private void insertFan(Vector3 basePoint, float fanRadius) {
        final boolean[] removed = new boolean[positions.size()];
        for (int v = 0; v < removed.length; v++) {
            removed[v] = positions.get(v).dst(basePoint) < fanRadius;
        }
        final List<int[]> kept = new ArrayList<>();
        final Set<Long> directed = new HashSet<>();
        for (int[] t : triangles) {
            if (!(removed[t[0]] || removed[t[1]] || removed[t[2]])) {
                kept.add(t);
                for (int e = 0; e < 3; e++) {
                    directed.add(key(t[e], t[(e + 1) % 3]));
                }
            }
        }
        // The edges around the hole are the ones that have no triangle on the other side.
        final Map<Integer, Integer> next = new HashMap<>();
        for (int[] t : kept) {
            for (int e = 0; e < 3; e++) {
                final int from = t[e], to = t[(e + 1) % 3];
                if (!directed.contains(key(to, from)) && next.put(from, to) != null) {
                    throw new IllegalStateException("The hole around the base touches itself");
                }
            }
        }
        final List<Integer> ring = new ArrayList<>();
        final int first = next.keySet().iterator().next();
        int current = first;
        do {
            ring.add(current);
            current = next.get(current);
        } while (current != first);
        if (ring.size() != next.size()) {
            throw new IllegalStateException("The hole around the base is not a single loop");
        }

        base = positions.size();
        positions.add(new Vector3(basePoint));
        // The hole is on the right of each of its edges (u -> v), so the triangle that fills it goes from v to u.
        for (int i = 0; i < ring.size(); i++) {
            kept.add(new int[] {base, ring.get((i + 1) % ring.size()), ring.get(i)});
        }
        triangles = kept;
    }

    private void buildNeighbours() {
        final List<Set<Integer>> sets = new ArrayList<>();
        for (int v = 0; v < positions.size(); v++) {
            sets.add(new HashSet<>());
        }
        for (int[] t : triangles) {
            for (int e = 0; e < 3; e++) {
                sets.get(t[e]).add(t[(e + 1) % 3]);
                sets.get((t[(e + 1) % 3])).add(t[e]);
            }
        }
        neighbours = new int[positions.size()][];
        for (int v = 0; v < neighbours.length; v++) {
            neighbours[v] = sets.get(v).stream().mapToInt(Integer::intValue).toArray();
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // 2. The loops

    private List<List<Integer>> findPaths(List<Loop> loops) {
        used = new boolean[positions.size()];
        final List<List<Integer>> paths = new ArrayList<>();
        final List<Float> spokeAngles = new ArrayList<>();
        final List<Integer> spokeNumbers = new ArrayList<>();
        for (int number = 0; number < loops.size(); number++) {
            final Loop loop = loops.get(number);
            final List<Integer> path = new ArrayList<>();
            path.add(base);

            final int start = spoke(loop.startAngle());
            used[start] = true;
            path.add(start);
            int current = start;
            for (Vector3 waypoint : loop.waypoints()) {
                final int target = nearest(waypoint);
                current = extend(path, current, target);
            }
            final int end = spoke(loop.endAngle());
            used[end] = true;
            extend(path, current, end);
            shortcut(path);
            path.add(base);
            paths.add(path);

            spokeAngles.add(loop.startAngle());
            spokeNumbers.add(2 * number);
            spokeAngles.add(loop.endAngle());
            spokeNumbers.add(2 * number + 1);
        }
        checkOrderAroundBase(paths, spokeAngles, spokeNumbers);
        return paths;
    }

    /**
     * Skips the parts of a path that make a detour: if a vertex is next to a later one of the path, they are joined
     * directly. Without this, an edge of the mesh could join two vertices on the same side of the octagon, and
     * everything between this edge and the side would be squeezed into the side.
     */
    private void shortcut(List<Integer> path) {
        // path.get(0) is the base, which stays
        for (int i = 1; i < path.size() - 1; i++) {
            int farthest = i;
            for (int neighbour : neighbours[path.get(i)]) {
                final int index = path.lastIndexOf(neighbour);
                if (index > farthest) {
                    farthest = index;
                }
            }
            for (int removed = i + 1; removed < farthest; removed++) {
                used[path.get(removed)] = false;
            }
            if (farthest > i + 1) {
                path.subList(i + 1, farthest).clear();
            }
        }
    }

    /** The edges leave the base in the order that was asked for, which is what makes the cut a disc. */
    private void checkOrderAroundBase(List<List<Integer>> paths, List<Float> angles, List<Integer> numbers) {
        final List<Integer> byWanted = new ArrayList<>();
        final List<Integer> byActual = new ArrayList<>();
        for (int i = 0; i < numbers.size(); i++) {
            byWanted.add(i);
            byActual.add(i);
        }
        byWanted.sort(Comparator.comparingDouble(i -> ((angles.get(i) % 360) + 360) % 360));
        byActual.sort(Comparator.comparingDouble(i -> actualAngle(paths, numbers.get(i))));
        // the order is a cycle: it is the same if it starts from a different place
        final int offset = byActual.indexOf(byWanted.get(0));
        for (int i = 0; i < byWanted.size(); i++) {
            if (!byWanted.get(i).equals(byActual.get((offset + i) % byActual.size()))) {
                final StringBuilder message = new StringBuilder("The loops do not leave the base in the order that was asked for:");
                for (int k = 0; k < numbers.size(); k++) {
                    message.append(String.format(" [%d: wanted %.0f, got %.0f]", k, angles.get(k), actualAngle(paths, numbers.get(k))));
                }
                throw new IllegalStateException(message.toString());
            }
        }
    }

    private double actualAngle(List<List<Integer>> paths, int spokeNumber) {
        final List<Integer> path = paths.get(spokeNumber / 2);
        final int vertex = spokeNumber % 2 == 0 ? path.get(1) : path.get(path.size() - 2);
        return angleAroundBase(vertex);
    }

    private double angleAroundBase(int vertex) {
        final Vector3 base = positions.get(this.base), p = positions.get(vertex);
        final double degrees = Math.toDegrees(Math.atan2(p.y - base.y, p.x - base.x));
        return (degrees + 360) % 360;
    }

    /** The neighbour of the base, not already used, that is in the direction of the angle. */
    private int spoke(float angle) {
        int best = -1;
        double bestDifference = Double.MAX_VALUE;
        for (int neighbour : neighbours[base]) {
            if (used[neighbour]) {
                continue;
            }
            double difference = Math.abs(angleAroundBase(neighbour) - ((angle % 360) + 360) % 360);
            difference = Math.min(difference, 360 - difference);
            if (difference < bestDifference) {
                bestDifference = difference;
                best = neighbour;
            }
        }
        if (best < 0) {
            throw new IllegalStateException("The base has no free neighbour left");
        }
        return best;
    }

    /** The vertex closest to the point, that is not already used, and is not the base or in the fan. */
    private int nearest(Vector3 point) {
        int best = -1;
        float bestDistance = Float.MAX_VALUE;
        for (int v = 0; v < positions.size(); v++) {
            if (used[v] || v == base || neighbours[v].length == 0 || isNextToBase(v)) {
                continue;
            }
            final float distance = positions.get(v).dst2(point);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = v;
            }
        }
        return best;
    }

    private boolean isNextToBase(int vertex) {
        for (int neighbour : neighbours[base]) {
            if (neighbour == vertex) {
                return true;
            }
        }
        return false;
    }

    /** Adds the shortest path from the current end of a path to a target, through vertices that are free. */
    private int extend(List<Integer> path, int from, int to) {
        final boolean[] blocked = new boolean[positions.size()];
        for (int v = 0; v < blocked.length; v++) {
            blocked[v] = used[v];
        }
        blocked[base] = true;
        blocked[from] = false;
        blocked[to] = false;

        final double[] distance = new double[positions.size()];
        final int[] previous = new int[positions.size()];
        Arrays.fill(distance, Double.MAX_VALUE);
        Arrays.fill(previous, -1);
        final PriorityQueue<double[]> queue = new PriorityQueue<>(Comparator.comparingDouble(entry -> entry[0]));
        distance[from] = 0;
        queue.add(new double[] {0, from});
        while (!queue.isEmpty()) {
            final double[] entry = queue.poll();
            final int vertex = (int) entry[1];
            if (entry[0] > distance[vertex]) {
                continue;
            }
            if (vertex == to) {
                break;
            }
            for (int neighbour : neighbours[vertex]) {
                if (blocked[neighbour]) {
                    continue;
                }
                final double candidate = distance[vertex] + positions.get(vertex).dst(positions.get(neighbour));
                if (candidate < distance[neighbour]) {
                    distance[neighbour] = candidate;
                    previous[neighbour] = vertex;
                    queue.add(new double[] {candidate, neighbour});
                }
            }
        }
        if (previous[to] < 0 && from != to) {
            throw new IllegalStateException("No path between two points of a loop");
        }
        final List<Integer> hop = new ArrayList<>();
        for (int v = to; v != from; v = previous[v]) {
            hop.add(v);
        }
        for (int i = hop.size() - 1; i >= 0; i--) {
            path.add(hop.get(i));
            used[hop.get(i)] = true;
        }
        return to;
    }

    // ---------------------------------------------------------------------------------------------------------------
    // 3. The cut

    /** The disc that results from the cut: its vertices, where each one came from, and its triangles. */
    private record Cut(List<Integer> source, List<int[]> triangles) {}

    private Cut cut(List<List<Integer>> paths) {
        final Set<Long> cutEdges = new HashSet<>();
        for (List<Integer> path : paths) {
            for (int i = 0; i + 1 < path.size(); i++) {
                cutEdges.add(undirectedKey(path.get(i), path.get(i + 1)));
            }
        }

        // The corners of the triangles are joined if they share a vertex and an edge that isn't cut: each group of
        // corners that gets joined is one of the pieces around a vertex, that become one vertex.
        final int[] parent = new int[3 * triangles.size()];
        for (int i = 0; i < parent.length; i++) {
            parent[i] = i;
        }
        final Map<Long, Integer> firstTriangleOfEdge = new HashMap<>();
        for (int t = 0; t < triangles.size(); t++) {
            final int[] triangle = triangles.get(t);
            for (int e = 0; e < 3; e++) {
                final long edge = undirectedKey(triangle[e], triangle[(e + 1) % 3]);
                final Integer other = firstTriangleOfEdge.putIfAbsent(edge, t);
                if (other != null && !cutEdges.contains(edge)) {
                    for (int vertex : new int[] {triangle[e], triangle[(e + 1) % 3]}) {
                        union(parent, 3 * t + cornerOf(triangle, vertex), 3 * other + cornerOf(triangles.get(other), vertex));
                    }
                }
            }
        }

        final List<Integer> source = new ArrayList<>();
        final Map<Integer, Integer> newVertex = new HashMap<>();
        final Map<Integer, Integer> idOfVertex = new HashMap<>();
        final List<int[]> result = new ArrayList<>();
        for (int t = 0; t < triangles.size(); t++) {
            final int[] triangle = triangles.get(t);
            final int[] mapped = new int[3];
            for (int corner = 0; corner < 3; corner++) {
                final int root = find(parent, 3 * t + corner);
                Integer id = newVertex.get(root);
                if (id == null) {
                    id = source.size();
                    source.add(triangle[corner]);
                    newVertex.put(root, id);
                }
                mapped[corner] = id;
            }
            result.add(mapped);
        }
        return new Cut(source, result);
    }

    private static int cornerOf(int[] triangle, int vertex) {
        return triangle[0] == vertex ? 0 : triangle[1] == vertex ? 1 : 2;
    }

    private static int find(int[] parent, int x) {
        while (parent[x] != x) {
            parent[x] = parent[parent[x]];
            x = parent[x];
        }
        return x;
    }

    private static void union(int[] parent, int a, int b) {
        parent[find(parent, a)] = find(parent, b);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // 4. The flat octagon

    private Result flatten(Cut cut, List<List<Integer>> paths, List<Vector3> normals, float radius) {
        final int count = cut.source().size();
        final List<int[]> discTriangles = cut.triangles();

        // the boundary of the disc: the edges that have no triangle on the other side, going with the disc on the left
        final Set<Long> directed = new HashSet<>();
        for (int[] t : discTriangles) {
            for (int e = 0; e < 3; e++) {
                directed.add(key(t[e], t[(e + 1) % 3]));
            }
        }
        final Map<Integer, Integer> next = new HashMap<>();
        for (int[] t : discTriangles) {
            for (int e = 0; e < 3; e++) {
                final int from = t[e], to = t[(e + 1) % 3];
                if (!directed.contains(key(to, from)) && next.put(from, to) != null) {
                    throw new IllegalStateException("The cut surface touches itself along its boundary");
                }
            }
        }
        final List<Integer> boundary = new ArrayList<>();
        final int first = next.keySet().iterator().next();
        int current = first;
        do {
            boundary.add(current);
            current = next.get(current);
        } while (current != first);
        if (boundary.size() != next.size()) {
            throw new IllegalStateException("The cut surface has more than one boundary");
        }

        // the corners of the octagon are the places where the boundary goes through the base
        final List<Integer> corners = new ArrayList<>();
        for (int i = 0; i < boundary.size(); i++) {
            if (cut.source().get(boundary.get(i)) == base) {
                corners.add(i);
            }
        }
        if (corners.size() != SIDES) {
            throw new IllegalStateException("The boundary of the cut surface has " + corners.size() + " corners, not 8");
        }

        // which loop each side goes along, and in which direction
        final Map<Long, int[]> loopOfEdge = new HashMap<>();
        for (int number = 0; number < paths.size(); number++) {
            final List<Integer> path = paths.get(number);
            for (int i = 0; i + 1 < path.size(); i++) {
                loopOfEdge.put(undirectedKey(path.get(i), path.get(i + 1)), new int[] {number, path.get(i)});
            }
        }

        final double[] x = new double[count], z = new double[count];
        final boolean[] fixed = new boolean[count];
        final int[] edgeLoops = new int[SIDES];
        final boolean[] edgeForward = new boolean[SIDES];
        final int[] timesUsed = new int[paths.size()];
        for (int side = 0; side < SIDES; side++) {
            final int from = corners.get(side), to = corners.get((side + 1) % SIDES);
            final List<Integer> segment = new ArrayList<>();
            for (int i = from; ; i = (i + 1) % boundary.size()) {
                segment.add(boundary.get(i));
                if (i == to && segment.size() > 1) {
                    break;
                }
            }

            final int first1 = cut.source().get(segment.get(0)), second = cut.source().get(segment.get(1));
            final int[] loopAndStart = loopOfEdge.get(undirectedKey(first1, second));
            edgeLoops[side] = loopAndStart[0];
            edgeForward[side] = loopAndStart[1] == first1;
            timesUsed[loopAndStart[0]]++;

            final double[] length = new double[segment.size()];
            for (int i = 1; i < segment.size(); i++) {
                length[i] = length[i - 1] + positions.get(cut.source().get(segment.get(i)))
                    .dst(positions.get(cut.source().get(segment.get(i - 1))));
            }
            final double a1 = 2 * Math.PI * side / SIDES + Math.PI / SIDES;
            final double a2 = 2 * Math.PI * (side + 1) / SIDES + Math.PI / SIDES;
            for (int i = 0; i < segment.size(); i++) {
                final double fraction = length[i] / length[length.length - 1];
                final int vertex = segment.get(i);
                x[vertex] = radius * ((1 - fraction) * Math.cos(a1) + fraction * Math.cos(a2));
                z[vertex] = radius * ((1 - fraction) * Math.sin(a1) + fraction * Math.sin(a2));
                fixed[vertex] = true;
            }
        }
        for (int number = 0; number < timesUsed.length; number++) {
            if (timesUsed[number] != 2) {
                throw new IllegalStateException("A loop is on " + timesUsed[number] + " sides of the octagon, not 2");
            }
        }

        spreadInside(cut, discTriangles, fixed, x, z);

        final List<Vector3> flat = new ArrayList<>(), folded = new ArrayList<>(), foldedNormals = new ArrayList<>();
        for (int v = 0; v < count; v++) {
            flat.add(new Vector3((float) x[v], 0f, (float) z[v]));
            folded.add(new Vector3(positions.get(cut.source().get(v))));
            foldedNormals.add(new Vector3(normals.get(cut.source().get(v))));
        }
        return new Result(flat, folded, foldedNormals, discTriangles, edgeLoops, edgeForward);
    }

    /**
     * Puts every vertex that isn't on the boundary at the weighted average of its neighbours, using the mean value
     * weights of the 3D surface. Solved by successive over-relaxation.
     */
    private void spreadInside(Cut cut, List<int[]> discTriangles, boolean[] fixed, double[] x, double[] z) {
        final int count = cut.source().size();
        final List<Map<Integer, Double>> weights = new ArrayList<>();
        for (int v = 0; v < count; v++) {
            weights.add(new HashMap<>());
        }
        for (int[] t : discTriangles) {
            for (int corner = 0; corner < 3; corner++) {
                final int i = t[corner], a = t[(corner + 1) % 3], b = t[(corner + 2) % 3];
                final Vector3 pi = positions.get(cut.source().get(i));
                final Vector3 pa = positions.get(cut.source().get(a)), pb = positions.get(cut.source().get(b));
                final Vector3 da = new Vector3(pa).sub(pi), db = new Vector3(pb).sub(pi);
                final double lengthA = Math.max(da.len(), MIN_LENGTH), lengthB = Math.max(db.len(), MIN_LENGTH);
                final double angle = Math.acos(Math.max(-1, Math.min(1, da.dot(db) / (lengthA * lengthB))));
                final double tangent = Math.tan(Math.min(angle, MAX_ANGLE) / 2);
                weights.get(i).merge(a, tangent / lengthA, Double::sum);
                weights.get(i).merge(b, tangent / lengthB, Double::sum);
            }
        }
        final int[][] around = new int[count][];
        final double[][] weight = new double[count][];
        for (int v = 0; v < count; v++) {
            around[v] = weights.get(v).keySet().stream().mapToInt(Integer::intValue).toArray();
            weight[v] = new double[around[v].length];
            for (int k = 0; k < around[v].length; k++) {
                weight[v][k] = weights.get(v).get(around[v][k]);
            }
        }

        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            double largest = 0;
            for (int v = 0; v < count; v++) {
                if (fixed[v]) {
                    continue;
                }
                double sum = 0, sumX = 0, sumZ = 0;
                for (int k = 0; k < around[v].length; k++) {
                    sum += weight[v][k];
                    sumX += weight[v][k] * x[around[v][k]];
                    sumZ += weight[v][k] * z[around[v][k]];
                }
                final double changeX = RELAXATION_FACTOR * (sumX / sum - x[v]);
                final double changeZ = RELAXATION_FACTOR * (sumZ / sum - z[v]);
                x[v] += changeX;
                z[v] += changeZ;
                largest = Math.max(largest, Math.max(Math.abs(changeX), Math.abs(changeZ)));
            }
            if (largest < TOLERANCE) {
                return;
            }
        }
    }

    // ---------------------------------------------------------------------------------------------------------------

    private static long key(int from, int to) {
        return ((long) from << 32) | (to & 0xffffffffL);
    }

    private static long undirectedKey(int a, int b) {
        return key(Math.min(a, b), Math.max(a, b));
    }
}
