package net.sr89.topology.shapes;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder.VertexInfo;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * A flat shape, made of triangles, that can be folded up into a surface in ℝ³.
 * <p>
 * Every vertex of the flat shape has a place on the folded surface (given by a function), and folding by an amount
 * t from 0 to 1 moves each vertex a fraction t of the way from its flat position to its folded position. Vertices that
 * are glued together in the surface (such as the two sides of the square that become the same circle of the
 * torus) end up in the same place at t = 1, but are separate vertices: the shape is cut open along those edges until
 * then.
 * <p>
 * The vertices can also have a color each, which is used to show the edges that are glued together.
 */
public class FoldingMesh {
    private final Model model;
    private final ModelInstance instance;
    private final Mesh mesh;
    private final int[][] triangles;
    private final Vector3[] flat;
    private final Vector3[] folded;
    private final Vector3[] flatNormals;
    private final Vector3[] foldedNormals;

    private final float[] vertices;
    private final int stride, positionOffset, normalOffset;

    /**
     * @param flatPositions where each vertex is when the shape is flat
     * @param triangles     the vertices of each triangle, as indices into flatPositions
     * @param colors        the color of each vertex
     * @param fold          computes the position of a vertex in the folded surface, given its flat position
     */
    public FoldingMesh(List<Vector3> flatPositions, List<int[]> triangles, List<Color> colors,
                       BiConsumer<Vector3, Vector3> fold, Material material) {
        final int count = flatPositions.size();
        this.triangles = triangles.toArray(new int[0][]);
        flat = flatPositions.stream().map(Vector3::new).toArray(Vector3[]::new);
        folded = new Vector3[count];
        for (int i = 0; i < count; i++) {
            folded[i] = new Vector3();
            fold.accept(flat[i], folded[i]);
        }
        flatNormals = vertexNormals(flat);
        foldedNormals = vertexNormals(folded);
        orientFlatNormalsUp();
        orientFoldedNormalsOutward();

        final ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();
        final MeshPartBuilder builder = modelBuilder.part("folding", GL20.GL_TRIANGLES,
            Usage.Position | Usage.Normal | Usage.ColorUnpacked, material);
        final short[] index = new short[count];
        final VertexInfo info = new VertexInfo();
        for (int i = 0; i < count; i++) {
            info.set(flat[i], flatNormals[i], colors.get(i), null);
            index[i] = builder.vertex(info);
        }
        for (int[] t : this.triangles) {
            builder.triangle(index[t[0]], index[t[1]], index[t[2]]);
        }
        model = modelBuilder.end();
        instance = new ModelInstance(model);
        mesh = model.meshes.first();
        vertices = new float[mesh.getNumVertices() * mesh.getVertexSize() / 4];
        stride = mesh.getVertexSize() / 4;
        positionOffset = mesh.getVertexAttribute(Usage.Position).offset / 4;
        normalOffset = mesh.getVertexAttribute(Usage.Normal).offset / 4;
        mesh.getVertices(vertices);
    }

    /** @param amount 0 for the flat shape, 1 for the surface */
    public void setFold(float amount) {
        final Vector3 position = new Vector3(), normal = new Vector3();
        for (int i = 0; i < flat.length; i++) {
            position.set(flat[i]).lerp(folded[i], amount);
            normal.set(flatNormals[i]).lerp(foldedNormals[i], amount).nor();
            // vertices were added in the same order as the positions
            vertices[i * stride + positionOffset] = position.x;
            vertices[i * stride + positionOffset + 1] = position.y;
            vertices[i * stride + positionOffset + 2] = position.z;
            vertices[i * stride + normalOffset] = normal.x;
            vertices[i * stride + normalOffset + 1] = normal.y;
            vertices[i * stride + normalOffset + 2] = normal.z;
        }
        mesh.setVertices(vertices);
    }

    public void render(ModelBatch modelBatch, Environment environment) {
        modelBatch.render(instance, environment);
    }

    public void dispose() {
        model.dispose();
    }

    /** Smooth normals: the sum of the normals of the triangles around each vertex. */
    private Vector3[] vertexNormals(Vector3[] positions) {
        final Vector3[] normals = new Vector3[positions.length];
        for (int i = 0; i < normals.length; i++) {
            normals[i] = new Vector3();
        }
        final Vector3 faceNormal = new Vector3(), edge = new Vector3();
        for (int[] t : triangles) {
            faceNormal.set(positions[t[1]]).sub(positions[t[0]]);
            edge.set(positions[t[2]]).sub(positions[t[0]]);
            faceNormal.crs(edge); // its length is twice the area of the triangle, which weights the sum
            for (int corner : t) {
                normals[corner].add(faceNormal);
            }
        }
        for (Vector3 normal : normals) {
            if (normal.len2() < 1e-12f) {
                normal.set(Vector3.Y);
            } else {
                normal.nor();
            }
        }
        return normals;
    }

    private void orientFlatNormalsUp() {
        if (flatNormals[0].y < 0) {
            for (Vector3 normal : flatNormals) {
                normal.scl(-1f);
            }
        }
    }

    /** The folded surface is closed, so the normal at its point farthest from the origin must point away from it. */
    private void orientFoldedNormalsOutward() {
        int farthest = 0;
        for (int i = 1; i < folded.length; i++) {
            if (folded[i].len2() > folded[farthest].len2()) {
                farthest = i;
            }
        }
        if (foldedNormals[farthest].dot(folded[farthest]) < 0) {
            for (Vector3 normal : foldedNormals) {
                normal.scl(-1f);
            }
        }
    }
}
