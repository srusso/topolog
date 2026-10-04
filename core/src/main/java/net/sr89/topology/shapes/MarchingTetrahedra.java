package net.sr89.topology.shapes;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder.VertexInfo;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns an implicit surface {@code f(x, y, z) = 0} into a triangle mesh.
 * <p>
 * The space is cut into a grid of cubes, each cube into 6 tetrahedra, and every tetrahedron the surface passes
 * through contributes one or two triangles (marching tetrahedra). This is the simpler cousin of marching cubes: there
 * is no big lookup table, and the result is always a closed, consistent mesh. Vertices shared between neighbouring
 * tetrahedra are welded, and normals come from the gradient of the function, so shading is smooth.
 * <p>
 * The function must be negative inside and positive outside. The surface must stay inside the bounds.
 */
public final class MarchingTetrahedra {
    @FunctionalInterface
    public interface Field {
        float value(float x, float y, float z);
    }

    /** Corner n of a cube sits at offset (n & 1, (n >> 1) & 1, (n >> 2) & 1). All tetrahedra share the diagonal 0-7. */
    private static final int[][] TETRAHEDRA = {
        {0, 1, 3, 7}, {0, 3, 2, 7}, {0, 2, 6, 7}, {0, 6, 4, 7}, {0, 4, 5, 7}, {0, 5, 1, 7}
    };

    private static final float GRADIENT_STEP = 1e-3f;
    private static final int MAX_VERTICES = 1 << 16; // libgdx indices are unsigned shorts

    private final Field field;
    private final Vector3 min;
    private final float cell;
    private final int pointsX, pointsY, pointsZ;
    private final float[] values;

    private final Map<Long, Integer> edgeVertices = new HashMap<>();
    private final List<Vector3> positions = new ArrayList<>();
    private final List<Vector3> normals = new ArrayList<>();
    private final List<int[]> triangles = new ArrayList<>();

    private MarchingTetrahedra(Field field, Vector3 min, Vector3 max, float cell) {
        this.field = field;
        this.min = new Vector3(min);
        this.cell = cell;
        pointsX = (int) Math.ceil((max.x - min.x) / cell) + 1;
        pointsY = (int) Math.ceil((max.y - min.y) / cell) + 1;
        pointsZ = (int) Math.ceil((max.z - min.z) / cell) + 1;
        values = new float[pointsX * pointsY * pointsZ];
    }

    /**
     * @param min  lower corner of the region to sample
     * @param max  upper corner of the region to sample
     * @param cell size of the grid cells. Smaller cells give a finer mesh (and quadratically more triangles).
     */
    public static Model build(Field field, Vector3 min, Vector3 max, float cell, Material material) {
        return new MarchingTetrahedra(field, min, max, cell).build(material);
    }

    private Model build(Material material) {
        final Vector3 p = new Vector3();
        for (int id = 0; id < values.length; id++) {
            gridPoint(id, p);
            values[id] = field.value(p.x, p.y, p.z);
        }

        final int[] corners = new int[8];
        for (int k = 0; k < pointsZ - 1; k++) {
            for (int j = 0; j < pointsY - 1; j++) {
                for (int i = 0; i < pointsX - 1; i++) {
                    for (int n = 0; n < 8; n++) {
                        corners[n] = gridId(i + (n & 1), j + ((n >> 1) & 1), k + ((n >> 2) & 1));
                    }
                    for (int[] tetrahedron : TETRAHEDRA) {
                        polygonize(corners[tetrahedron[0]], corners[tetrahedron[1]],
                            corners[tetrahedron[2]], corners[tetrahedron[3]]);
                    }
                }
            }
        }

        if (positions.size() > MAX_VERTICES) {
            throw new IllegalStateException(
                "Mesh has " + positions.size() + " vertices, the maximum is " + MAX_VERTICES + ". Use bigger cells.");
        }

        final ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();
        final MeshPartBuilder builder = modelBuilder.part("surface", GL20.GL_TRIANGLES,
            Usage.Position | Usage.Normal, material);
        final short[] index = new short[positions.size()];
        final VertexInfo info = new VertexInfo();
        for (int v = 0; v < index.length; v++) {
            info.set(positions.get(v), normals.get(v), null, null);
            index[v] = builder.vertex(info);
        }
        for (int[] t : triangles) {
            builder.triangle(index[t[0]], index[t[1]], index[t[2]]);
        }
        return modelBuilder.end();
    }

    /** Emits the triangles where the surface crosses the tetrahedron with the given corners. */
    private void polygonize(int a, int b, int c, int d) {
        final int[] tetrahedron = {a, b, c, d};
        final int[] inside = new int[4];
        final int[] outside = new int[4];
        int insideCount = 0, outsideCount = 0;
        for (int corner : tetrahedron) {
            if (values[corner] < 0) {
                inside[insideCount++] = corner;
            } else {
                outside[outsideCount++] = corner;
            }
        }

        switch (insideCount) {
            case 1 -> addTriangle(
                vertex(inside[0], outside[0]), vertex(inside[0], outside[1]), vertex(inside[0], outside[2]));
            case 3 -> addTriangle(
                vertex(outside[0], inside[0]), vertex(outside[0], inside[1]), vertex(outside[0], inside[2]));
            case 2 -> {
                // The surface cuts through four edges, forming a quad.
                final int e1 = vertex(inside[0], outside[0]);
                final int e2 = vertex(inside[0], outside[1]);
                final int e3 = vertex(inside[1], outside[1]);
                final int e4 = vertex(inside[1], outside[0]);
                addTriangle(e1, e2, e3);
                addTriangle(e1, e3, e4);
            }
            default -> { } // entirely inside or outside: the surface doesn't pass through here
        }
    }

    /** Adds a triangle, flipping it if needed so that it faces the direction in which the function increases. */
    private void addTriangle(int a, int b, int c) {
        final Vector3 pa = positions.get(a);
        final Vector3 faceNormal = new Vector3(positions.get(b)).sub(pa).crs(new Vector3(positions.get(c)).sub(pa));
        final Vector3 outward = new Vector3(normals.get(a)).add(normals.get(b)).add(normals.get(c));
        triangles.add(faceNormal.dot(outward) >= 0 ? new int[] {a, b, c} : new int[] {a, c, b});
    }

    /** The vertex where the surface crosses the edge between two grid points. Created once per edge. */
    private int vertex(int idA, int idB) {
        final int lower = Math.min(idA, idB), upper = Math.max(idA, idB);
        final long key = (long) lower * values.length + upper;
        final Integer existing = edgeVertices.get(key);
        if (existing != null) {
            return existing;
        }

        final Vector3 a = gridPoint(lower, new Vector3());
        final Vector3 b = gridPoint(upper, new Vector3());
        final float valueA = values[lower], valueB = values[upper];
        final Vector3 position = a.lerp(b, valueA / (valueA - valueB));

        final Vector3 normal = new Vector3(
            field.value(position.x + GRADIENT_STEP, position.y, position.z)
                - field.value(position.x - GRADIENT_STEP, position.y, position.z),
            field.value(position.x, position.y + GRADIENT_STEP, position.z)
                - field.value(position.x, position.y - GRADIENT_STEP, position.z),
            field.value(position.x, position.y, position.z + GRADIENT_STEP)
                - field.value(position.x, position.y, position.z - GRADIENT_STEP)).nor();

        positions.add(position);
        normals.add(normal);
        final int index = positions.size() - 1;
        edgeVertices.put(key, index);
        return index;
    }

    private int gridId(int i, int j, int k) {
        return (k * pointsY + j) * pointsX + i;
    }

    private Vector3 gridPoint(int id, Vector3 out) {
        final int i = id % pointsX;
        final int j = (id / pointsX) % pointsY;
        final int k = id / (pointsX * pointsY);
        return out.set(min.x + i * cell, min.y + j * cell, min.z + k * cell);
    }
}
