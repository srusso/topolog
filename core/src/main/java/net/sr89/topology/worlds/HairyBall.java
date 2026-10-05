package net.sr89.topology.worlds;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import net.sr89.topology.HexColors;
import net.sr89.topology.shapes.ParametricSurface;
import net.sr89.topology.shapes.Primitives;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The hairy ball theorem: a continuous field of tangent vectors on the sphere has to be zero somewhere. (You can't comb a
 * hairy ball flat.) More precisely, the indices of the zeros of the field add up to the Euler characteristic of the sphere,
 * which is 2. The index of a zero says how many times the field turns around while going once around the zero.
 * <p>
 * Three fields, one after the other, drawn as arrows (their length is the size of the field), with the points that flow along them:
 * <ol>
 *   <li>The rotation around the vertical axis: two zeros at the poles, each of index +1.</li>
 *   <li>The flow from the north pole to the south pole: a source and a sink, each of index +1.</li>
 *   <li>A field with a single zero, of index +2, where the field turns twice around it (the square of the field
 *       of the Riemann sphere): it has zero in one point only.</li>
 * </ol>
 * The torus is different: it has a field without zeros (its Euler characteristic is 0).
 */
public class HairyBall implements World {
    private static final float RADIUS = 1.8f;
    private static final int ARROWS = 260;
    private static final int PARTICLES = 90;
    private static final float ARROW_ARROW_LENGTH = 0.55f;
    private static final float ARROW_WIDTH = 0.22f;
    private static final float ARROW_MODEL_HEIGHT = 0.85f;
    private static final float SECONDS_PER_FIELD = 12f;
    private static final float PARTICLE_SPEED = 0.8f;

    /** A field of tangent vectors on the unit sphere. */
    private interface Field {
        void at(double x, double y, double z, double[] out);
    }

    private static final String[] TITLES = {
        "Hairy ball theorem: two zeros, of index +1 and +1",
        "Hairy ball theorem: a source and a sink, of index +1 and +1",
        "Hairy ball theorem: a single zero, of index +2"};
    private static final Field[] FIELDS = {HairyBall::rotation, HairyBall::flow, HairyBall::doubleZero};
    /** The zeros of each field, as points of the unit sphere. */
    private static final double[][][] ZEROS = {{{0, 0, 1}, {0, 0, -1}}, {{0, 0, 1}, {0, 0, -1}}, {{0, 0, -1}}};

    private final List<Model> models = new ArrayList<>();
    private final ModelInstance sphere;
    private final ModelInstance[] arrows = new ModelInstance[ARROWS];
    private final double[][] arrowPoints = new double[ARROWS][3];
    private final ModelInstance[] particles = new ModelInstance[PARTICLES];
    private final double[][] particlePoints = new double[PARTICLES][3];
    private final float[] particleAge = new float[PARTICLES];
    private final ModelInstance[] zeros = new ModelInstance[2];
    private final double[] longest = new double[FIELDS.length];
    private final Random random = new Random(7);

    private float time = 0f;
    private int field = -1;
    private final double[] vector = new double[3];
    private final Vector3 tmp = new Vector3(), direction = new Vector3();
    private final Quaternion rotation = new Quaternion();

    public HairyBall() {
        sphere = instance(ParametricSurface.build(
            (u, v, out) -> out.set((float) (RADIUS * Math.sin(Math.PI * u) * Math.cos(2 * Math.PI * v)),
                (float) (RADIUS * Math.cos(Math.PI * u)), (float) (RADIUS * Math.sin(Math.PI * u) * Math.sin(2 * Math.PI * v))),
            32, 64, true, ParametricSurface.translucentMaterial(HexColors.greenPastel(), 0.3f)));

        // points spread evenly on the sphere (a spiral with the golden angle)
        final double golden = Math.PI * (3 - Math.sqrt(5));
        for (int i = 0; i < ARROWS; i++) {
            final double z = 1 - 2 * (i + 0.5) / ARROWS, radius = Math.sqrt(1 - z * z);
            arrowPoints[i] = new double[] {radius * Math.cos(golden * i), radius * Math.sin(golden * i), z};
        }
        for (int f = 0; f < FIELDS.length; f++) {
            for (double[] point : arrowPoints) {
                FIELDS[f].at(point[0], point[1], point[2], vector);
                longest[f] = Math.max(longest[f], Math.sqrt(vector[0] * vector[0] + vector[1] * vector[1] + vector[2] * vector[2]));
            }
        }

        final Model arrow = model(new ModelBuilder().createArrow(0, 0, 0, 0, 1, 0, 0.3f, 0.12f, 8, GL20.GL_TRIANGLES,
            new Material(ColorAttribute.createDiffuse(Color.WHITE)), Usage.Position | Usage.Normal));
        for (int i = 0; i < ARROWS; i++) {
            arrows[i] = new ModelInstance(arrow);
        }
        final Model particle = model(Primitives.ball(0.09f, Color.CYAN));
        for (int i = 0; i < PARTICLES; i++) {
            particles[i] = new ModelInstance(particle);
            respawn(i);
            particleAge[i] = random.nextFloat() * 6f;
        }
        final Model zero = model(Primitives.ball(0.26f, Color.YELLOW));
        zeros[0] = new ModelInstance(zero);
        zeros[1] = new ModelInstance(zero);
        reposition(0f);
    }

    @Override
    public String getWorldTitle() {
        return TITLES[Math.max(field, 0)];
    }

    @Override
    public void reposition(float deltaTime) {
        time += deltaTime;
        final int current = (int) (time / SECONDS_PER_FIELD) % FIELDS.length;
        if (current != field) {
            field = current;
            for (int i = 0; i < ARROWS; i++) {
                placeArrow(i);
            }
        }

        // the points follow the field (with a step in the middle, to be more exact)
        final double[] k1 = new double[3], k2 = new double[3];
        final double step = deltaTime * PARTICLE_SPEED / longest[field] * 1.0;
        for (int i = 0; i < PARTICLES; i++) {
            final double[] p = particlePoints[i];
            FIELDS[field].at(p[0], p[1], p[2], k1);
            final double[] middle = normalized(p[0] + 0.5 * step * k1[0], p[1] + 0.5 * step * k1[1], p[2] + 0.5 * step * k1[2]);
            FIELDS[field].at(middle[0], middle[1], middle[2], k2);
            final double[] next = normalized(p[0] + step * k2[0], p[1] + step * k2[1], p[2] + step * k2[2]);
            System.arraycopy(next, 0, p, 0, 3);
            particleAge[i] += deltaTime;
            if (particleAge[i] > 8f) {
                respawn(i);
            }
            particles[i].transform.setToTranslation((float) (1.02 * RADIUS * p[0]), (float) (1.02 * RADIUS * p[2]),
                (float) (1.02 * RADIUS * p[1]));
        }

        final double[][] zeroPoints = ZEROS[field];
        for (int i = 0; i < zeros.length; i++) {
            if (i < zeroPoints.length) {
                zeros[i].transform.setToTranslation((float) (1.02 * RADIUS * zeroPoints[i][0]),
                    (float) (1.02 * RADIUS * zeroPoints[i][2]), (float) (1.02 * RADIUS * zeroPoints[i][1]));
            } else {
                zeros[i].transform.setToScaling(0f, 0f, 0f);
            }
        }
    }

    @Override
    public void render(ModelBatch modelBatch, Environment environment) {
        for (ModelInstance arrow : arrows) {
            modelBatch.render(arrow, environment);
        }
        for (ModelInstance particle : particles) {
            modelBatch.render(particle, environment);
        }
        modelBatch.render(zeros[0], environment);
        modelBatch.render(zeros[1], environment);
        modelBatch.render(sphere, environment);
    }

    @Override
    public void dispose() {
        models.forEach(Model::dispose);
    }

    /** Puts the arrow of a point of the sphere, along the field there, as long as the field is big. */
    private void placeArrow(int i) {
        final double[] p = arrowPoints[i];
        FIELDS[field].at(p[0], p[1], p[2], vector);
        // the sphere's Z axis is up in the world, so its Y axis is the world's Z
        direction.set((float) vector[0], (float) vector[2], (float) vector[1]);
        final float size = direction.len() / (float) longest[field];
        if (size < 0.03f) {
            arrows[i].transform.setToScaling(0f, 0f, 0f);
            return;
        }
        tmp.set((float) (RADIUS * p[0]), (float) (RADIUS * p[2]), (float) (RADIUS * p[1])).scl(1.01f);
        rotation.setFromCross(Vector3.Y, direction.nor());
        arrows[i].transform.set(tmp, rotation,
            new Vector3(ARROW_WIDTH, ARROW_ARROW_LENGTH * size / ARROW_MODEL_HEIGHT, ARROW_WIDTH));
    }

    private void respawn(int i) {
        final double z = 2 * random.nextDouble() - 1, angle = 2 * Math.PI * random.nextDouble(), radius = Math.sqrt(1 - z * z);
        particlePoints[i] = new double[] {radius * Math.cos(angle), radius * Math.sin(angle), z};
        particleAge[i] = 0f;
    }

    private static double[] normalized(double x, double y, double z) {
        final double length = Math.sqrt(x * x + y * y + z * z);
        return new double[] {x / length, y / length, z / length};
    }

    /** The rotation around the vertical axis. */
    private static void rotation(double x, double y, double z, double[] out) {
        out[0] = -y;
        out[1] = x;
        out[2] = 0;
    }

    /** The vertical direction, without the part that is not tangent to the sphere: a flow from the top to the bottom, going down. */
    private static void flow(double x, double y, double z, double[] out) {
        out[0] = -z * x;
        out[1] = -z * y;
        out[2] = 1 - z * z;
        // the field is up here: reverse it to flow from the north pole (a source) to the south pole (a sink)
        out[0] = -out[0];
        out[1] = -out[1];
        out[2] = -out[2];
    }

    /**
     * A field with a single zero of index 2. In the coordinate w = (x + iy) / (1 − z) of the sphere (the plane, with the
     * sphere's north pole at infinity), it is w' = w², which is zero only at w = 0: the south pole. Near the north pole
     * the other coordinate is used, u = 1/w, where the field is u' = −1: no zero.
     */
    private static void doubleZero(double x, double y, double z, double[] out) {
        final double h = 1e-6;
        if (z <= 0) {
            final double a = x / (1 - z), b = y / (1 - z);
            // w^2
            final double da = a * a - b * b, db = 2 * a * b;
            final double[] p = fromW(a, b), q = fromW(a + h * da, b + h * db);
            out[0] = (q[0] - p[0]) / h;
            out[1] = (q[1] - p[1]) / h;
            out[2] = (q[2] - p[2]) / h;
        } else {
            final double a = x / (1 + z), b = -y / (1 + z); // u = (x - iy) / (1 + z)
            final double[] p = fromU(a, b), q = fromU(a - h, b);
            out[0] = (q[0] - p[0]) / h;
            out[1] = (q[1] - p[1]) / h;
            out[2] = (q[2] - p[2]) / h;
        }
    }

    /** The point of the sphere that has the coordinate w = a + ib. */
    private static double[] fromW(double a, double b) {
        final double n = a * a + b * b;
        return new double[] {2 * a / (1 + n), 2 * b / (1 + n), (n - 1) / (1 + n)};
    }

    /** The point of the sphere that has the coordinate u = a + ib in the chart of the north pole. */
    private static double[] fromU(double a, double b) {
        final double n = a * a + b * b;
        return new double[] {2 * a / (1 + n), -2 * b / (1 + n), (1 - n) / (1 + n)};
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
