package net.sr89.topology.worlds;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import net.sr89.topology.math.Curve;
import net.sr89.topology.shapes.SegmentTubes;
import net.sr89.topology.shapes.TubeMesh;

import java.util.ArrayList;
import java.util.List;

/**
 * The wedge of two circles S¹ ∨ S¹ (a figure eight), and its universal covering space.
 * <p>
 * The fundamental group of the figure eight is the free group on two generators a and b (the two loops): a word in
 * a, b and their inverses is trivial only when it reduces to nothing, so, unlike on the torus, a b a⁻¹ b⁻¹ is not the
 * same as the constant loop. The universal cover is a tree in which every vertex has four edges (one for each of a,
 * a⁻¹, b, b⁻¹): its vertices are the elements of the group, and moving along the edge of a letter is multiplying by it.
 * It is drawn above, to depth {@value #TREE_DEPTH}, with each edge in the color of the loop that it covers.
 * <p>
 * A tracer moves around the loop a b a⁻¹ b⁻¹ on the figure eight, and along its lift in the tree, starting from the
 * root (green). The loop closes up, but the lift ends somewhere else (red): the commutator is not the identity.
 */
public class WedgeOfCircles implements World {
    private static final int TREE_DEPTH = 4;
    private static final float TREE_HEIGHT = 2.6f;
    private static final float ROOT_EDGE_LENGTH = 1.4f;
    private static final float EDGE_SHRINKING = 0.5f;
    private static final float EDGE_RADIUS = 0.018f;
    private static final float PATH_RADIUS = 0.04f;
    private static final int LOOP_SAMPLES = 200;
    private static final float SECONDS_PER_LETTER = 5f;

    /** The letters, in the order they are around a vertex: a, b, a⁻¹, b⁻¹. Letter i + 2 is the inverse of letter i. */
    private static final int A = 0, B = 1, A_INVERSE = 2, B_INVERSE = 3;
    /** The word we trace: a b a⁻¹ b⁻¹ */
    private static final int[] WORD = {A, B, A_INVERSE, B_INVERSE};

    private final List<Model> models = new ArrayList<>();
    private final ModelInstance loopA;
    private final ModelInstance loopB;
    private final ModelInstance treeEdgesA;
    private final ModelInstance treeEdgesB;
    private final ModelInstance lift;
    private final ModelInstance root;
    private final ModelInstance liftEnd;
    private final ModelInstance tracerOnTree;
    private final ModelInstance tracerOnLoops;

    /** The vertices of the lift: the root, then the vertex after each letter of the word. */
    private final Vector3[] liftVertices = new Vector3[WORD.length + 1];

    private float progress = 0f; // in [0, WORD.length)
    private final Vector3 tmp = new Vector3();

    public WedgeOfCircles() {
        // The two circles, tangent at the origin: a on the left, b on the right.
        final Curve a = (t, out) -> loopPoint(A, t, out);
        final Curve b = (t, out) -> loopPoint(B, t, out);
        loopA = instance(TubeMesh.build(a, true, LOOP_SAMPLES, 8, 0.035f, Color.RED));
        loopB = instance(TubeMesh.build(b, true, LOOP_SAMPLES, 8, 0.035f, Color.CYAN));

        final List<Vector3[]> edgesOfA = new ArrayList<>();
        final List<Vector3[]> edgesOfB = new ArrayList<>();
        final Vector3 rootPosition = new Vector3(0f, TREE_HEIGHT, 0f);
        for (int letter = 0; letter < 4; letter++) {
            grow(rootPosition, letter, 1, edgesOfA, edgesOfB);
        }
        treeEdgesA = instance(SegmentTubes.build(edgesOfA, EDGE_RADIUS, Color.RED));
        treeEdgesB = instance(SegmentTubes.build(edgesOfB, EDGE_RADIUS, Color.CYAN));

        // the lift of the word, starting at the root
        liftVertices[0] = new Vector3(rootPosition);
        final List<Vector3[]> liftEdges = new ArrayList<>();
        Vector3 position = new Vector3(rootPosition);
        for (int i = 0; i < WORD.length; i++) {
            final Vector3 next = new Vector3(position).add(step(WORD[i], i + 1));
            liftVertices[i + 1] = next;
            liftEdges.add(new Vector3[] {position, next});
            position = next;
        }
        lift = instance(SegmentTubes.build(liftEdges, PATH_RADIUS, Color.YELLOW));

        root = new ModelInstance(model(sphere(0.2f, Color.GREEN)));
        root.transform.setToTranslation(liftVertices[0]);
        liftEnd = new ModelInstance(model(sphere(0.2f, Color.RED)));
        liftEnd.transform.setToTranslation(liftVertices[WORD.length]);

        final Model tracer = model(sphere(0.24f, Color.YELLOW));
        tracerOnTree = new ModelInstance(tracer);
        tracerOnLoops = new ModelInstance(tracer);
        reposition(0f);
    }

    @Override
    public String getWorldTitle() {
        return "Wedge of two circles: free group on a, b";
    }

    @Override
    public void reposition(float deltaTime) {
        progress = (progress + deltaTime / SECONDS_PER_LETTER) % WORD.length;
        final int index = (int) progress;
        final float t = progress - index;
        final int letter = WORD[index];

        // on the tree: from the vertex before the letter to the vertex after it
        tmp.set(liftVertices[index]).lerp(liftVertices[index + 1], t);
        tracerOnTree.transform.setToTranslation(tmp);

        // on the figure eight: once around the loop of the letter, backwards for the inverse letters
        final boolean inverse = letter >= 2;
        loopPoint(letter % 2, inverse ? 1 - t : t, tmp);
        tracerOnLoops.transform.setToTranslation(tmp);
    }

    @Override
    public void render(ModelBatch modelBatch, Environment environment) {
        modelBatch.render(loopA, environment);
        modelBatch.render(loopB, environment);
        modelBatch.render(treeEdgesA, environment);
        modelBatch.render(treeEdgesB, environment);
        modelBatch.render(lift, environment);
        modelBatch.render(root, environment);
        modelBatch.render(liftEnd, environment);
        modelBatch.render(tracerOnTree, environment);
        modelBatch.render(tracerOnLoops, environment);
    }

    @Override
    public void dispose() {
        models.forEach(Model::dispose);
    }

    /** The loop a (letter 0, on the left) or b (letter 1, on the right), once around, starting from the origin. */
    private static void loopPoint(int loop, float t, Vector3 out) {
        final double angle = 2 * Math.PI * t;
        final float side = loop == A ? -1f : 1f;
        out.set((float) (side * (1 - Math.cos(angle))), 0f, (float) Math.sin(angle));
    }

    /**
     * The edge of a letter, as a vector. In every vertex of the tree the four letters point in the same four directions,
     * in the order a, b, a⁻¹, b⁻¹, and edges are shorter the farther they are from the root, so that the tree never overlaps itself.
     */
    private static Vector3 step(int letter, int depth) {
        final double length = ROOT_EDGE_LENGTH * Math.pow(EDGE_SHRINKING, depth - 1);
        final double angle = Math.PI / 2 * letter;
        return new Vector3((float) (length * Math.cos(angle)), 0f, (float) (length * Math.sin(angle)));
    }

    /** Adds the edge for a letter from a vertex at the given position, and all the edges beyond it. */
    private void grow(Vector3 from, int letter, int depth, List<Vector3[]> edgesOfA, List<Vector3[]> edgesOfB) {
        final Vector3 to = new Vector3(from).add(step(letter, depth));
        (letter % 2 == 0 ? edgesOfA : edgesOfB).add(new Vector3[] {from, to});
        if (depth == TREE_DEPTH) {
            return;
        }
        for (int next = 0; next < 4; next++) {
            if (next != (letter + 2) % 4) { // can't go back along the same edge
                grow(to, next, depth + 1, edgesOfA, edgesOfB);
            }
        }
    }

    private static Model sphere(float diameter, Color color) {
        return new ModelBuilder().createSphere(diameter, diameter, diameter, 16, 16,
            new Material(ColorAttribute.createDiffuse(color)), Usage.Position | Usage.Normal);
    }

    /** Takes ownership of the model to dispose of it later. */
    private Model model(Model model) {
        models.add(model);
        return model;
    }

    private ModelInstance instance(Model model) {
        return new ModelInstance(model(model));
    }
}
