package net.sr89.topology.worlds;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import net.sr89.topology.HexColors;
import net.sr89.topology.math.Curve;
import net.sr89.topology.shapes.ParametricSurface;
import net.sr89.topology.shapes.Primitives;
import net.sr89.topology.shapes.TubeMesh;

import java.util.ArrayList;
import java.util.List;

/**
 * The Brouwer fixed point theorem for the disc: every continuous map from the disc to itself has a fixed point.
 * <p>
 * The proof goes by contradiction. If f had no fixed point, then for each x the points f(x) and x would be different,
 * and the ray that starts at f(x) and goes through x would leave the disc at a point r(x) of the boundary circle. This
 * r would be continuous, and it would leave the points of the circle where they are: a retraction of the disc onto its
 * boundary circle. That can't be, because the fundamental group of the circle is ℤ, and the one of the disc is trivial:
 * a loop that goes around the circle once can't be a retraction's image of a loop in the disc that shrinks to a point.
 * <p>
 * The map f drawn here (a contraction towards a point p) does have a fixed point, p. The construction works
 * everywhere else, and it shows where it breaks: a small circle around p (with colored points) has an image under r that
 * goes around the boundary once, however small the circle is, in the same order of colors. (Here r is the projection from p
 * onto the circle, so the images don't move while the circle shrinks.) At p the circle would
 * shrink to nothing, but its image would still go around once: r can't be continuous there.
 */
public class BrouwerFixedPoint implements World {
    private static final float RADIUS = 2f;
    /** The fixed point of f, in the plane of the disc (x, z). */
    private static final double FIXED_X = 0.5, FIXED_Z = -0.3;
    private static final double CONTRACTION = 0.5;
    private static final int POINTS = 36;
    private static final int COLORS = 12;
    private static final float MIN_RING = 0.1f, MAX_RING = 1.35f;
    private static final float SECONDS_PER_CYCLE = 16f;

    private final List<Model> models = new ArrayList<>();
    private final ModelInstance disc;
    private final ModelInstance boundary;
    private final ModelInstance fixedPoint;
    private final ModelInstance[] ringPoints = new ModelInstance[POINTS];
    private final ModelInstance[] imagePoints = new ModelInstance[POINTS];
    private final ModelInstance[] rays = new ModelInstance[POINTS];

    private float time = 0f;
    private final Vector3 from = new Vector3(), to = new Vector3();
    private final double[] image = new double[2];

    public BrouwerFixedPoint() {
        disc = instance(ParametricSurface.build(
            (u, v, out) -> out.set((float) (RADIUS * u * Math.cos(2 * Math.PI * v)), 0f,
                (float) (RADIUS * u * Math.sin(2 * Math.PI * v))),
            8, 64, true, ParametricSurface.translucentMaterial(HexColors.greenPastel(), 0.35f)));
        final Curve circle = (t, out) -> out.set((float) (RADIUS * Math.cos(2 * Math.PI * t)), 0f,
            (float) (RADIUS * Math.sin(2 * Math.PI * t)));
        boundary = instance(TubeMesh.build(circle, true, 200, 8, 0.03f, Color.LIGHT_GRAY));

        // one color for each group of points, around the color wheel
        final Model[] balls = new Model[COLORS], bigBalls = new Model[COLORS], rods = new Model[COLORS];
        for (int c = 0; c < COLORS; c++) {
            final Color color = Primitives.hue((float) c / COLORS, 0.85f, 1f);
            balls[c] = model(Primitives.ball(0.11f, color));
            bigBalls[c] = model(Primitives.ball(0.15f, color));
            rods[c] = model(Primitives.rod(color));
        }
        for (int i = 0; i < POINTS; i++) {
            final int color = i * COLORS / POINTS;
            ringPoints[i] = new ModelInstance(balls[color]);
            imagePoints[i] = new ModelInstance(bigBalls[color]);
            rays[i] = new ModelInstance(rods[color]);
        }

        fixedPoint = instance(Primitives.ball(0.22f, Color.WHITE));
        fixedPoint.transform.setToTranslation((float) FIXED_X, 0f, (float) FIXED_Z);
        reposition(0f);
    }

    @Override
    public String getWorldTitle() {
        return "Brouwer fixed point theorem";
    }

    @Override
    public void reposition(float deltaTime) {
        time += deltaTime;
        // the circle around the fixed point shrinks and grows again
        final float cycle = (time / SECONDS_PER_CYCLE) % 1f;
        final float size = 0.5f - 0.5f * (float) Math.cos(2 * Math.PI * cycle);
        final double ring = MIN_RING + (MAX_RING - MIN_RING) * size;

        for (int i = 0; i < POINTS; i++) {
            final double angle = 2 * Math.PI * i / POINTS;
            final double x = FIXED_X + ring * Math.cos(angle), z = FIXED_Z + ring * Math.sin(angle);
            ringPoints[i].transform.setToTranslation((float) x, 0f, (float) z);

            // r(x): where the ray from f(x) through x leaves the disc
            retract(x, z, image);
            imagePoints[i].transform.setToTranslation((float) image[0], 0f, (float) image[1]);
            final double[] fx = f(x, z);
            from.set((float) fx[0], 0f, (float) fx[1]);
            to.set((float) image[0], 0f, (float) image[1]);
            Primitives.place(rays[i], from, to, 0.012f);
        }
    }

    @Override
    public void render(ModelBatch modelBatch, Environment environment) {
        modelBatch.render(boundary, environment);
        modelBatch.render(fixedPoint, environment);
        for (int i = 0; i < POINTS; i++) {
            modelBatch.render(rays[i], environment);
            modelBatch.render(ringPoints[i], environment);
            modelBatch.render(imagePoints[i], environment);
        }
        modelBatch.render(disc, environment);
    }

    @Override
    public void dispose() {
        models.forEach(Model::dispose);
    }

    /** The map: a contraction towards the fixed point. It sends the disc into itself. */
    private static double[] f(double x, double z) {
        return new double[] {FIXED_X + CONTRACTION * (x - FIXED_X), FIXED_Z + CONTRACTION * (z - FIXED_Z)};
    }

    /** The point where the ray that starts at f(x) and goes through x leaves the disc (x is not the fixed point). */
    private static void retract(double x, double z, double[] out) {
        final double[] fx = f(x, z);
        double dx = x - fx[0], dz = z - fx[1];
        final double length = Math.hypot(dx, dz);
        dx /= length;
        dz /= length;
        // solve |x + s d| = R for s >= 0
        final double along = x * dx + z * dz;
        final double s = -along + Math.sqrt(along * along - (x * x + z * z) + RADIUS * RADIUS);
        out[0] = x + s * dx;
        out[1] = z + s * dz;
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
