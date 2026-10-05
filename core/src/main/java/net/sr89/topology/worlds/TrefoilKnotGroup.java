package net.sr89.topology.worlds;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import net.sr89.topology.math.Curve;
import net.sr89.topology.shapes.Primitives;
import net.sr89.topology.shapes.TubeMesh;

import java.util.ArrayList;
import java.util.List;

/**
 * The fundamental group of the complement of a knot, for the trefoil, with the Wirtinger presentation.
 * <p>
 * Seen from above, the knot has crossings, where one strand goes over another. The strands are cut where they go under,
 * which divides the knot in arcs: three, for the trefoil (red, cyan and orange). Each arc gives a generator of the
 * fundamental group of the complement: a loop that starts at a base point above the knot (white), goes down to the
 * arc, around it once, and goes back (the lassos). Each crossing gives a relation: the arc that comes out from under
 * is the one that comes in, conjugated by the arc that goes over, because the loop around the under arc can be
 * slid under the over arc, which turns it into a loop around the next arc, with a detour around the over arc.
 * <p>
 * For the trefoil the three relations reduce to ⟨x, y | x y x = y x y⟩, which is also ⟨a, b | a² = b³⟩. This group is not
 * abelian, while the group of an unknotted circle is ℤ: this is how we know that the trefoil is really knotted.
 * <p>
 * The three crossings are marked with yellow balls.
 */
public class TrefoilKnotGroup implements World {
    private static final float SCALE = 0.7f;
    private static final int SAMPLES = 3000;
    private static final float KNOT_RADIUS = 0.12f;
    private static final float RING_RADIUS = 0.5f;
    private static final float BASE_HEIGHT = 3.2f;
    private static final Color[] ARC_COLORS = {Color.RED, Color.CYAN, Color.ORANGE};
    private static final String[] ARC_NAMES = {"red", "cyan", "orange"};

    /** A crossing: the parameter (position along the knot) of the strand that goes over, and of the one that goes under. */
    private record Crossing(double over, double under) {}

    private final List<Model> models = new ArrayList<>();
    private final List<ModelInstance> parts = new ArrayList<>();
    private final List<Crossing> crossings = new ArrayList<>();
    private final double[] arcStarts = new double[3]; // where each arc starts, in increasing order
    private final ModelInstance[] markers;

    public TrefoilKnotGroup() {
        findCrossings();

        // the arcs, each in its own color
        for (int arc = 0; arc < 3; arc++) {
            final double start = arcStarts[arc];
            final double end = arc < 2 ? arcStarts[arc + 1] : arcStarts[0] + 2 * Math.PI;
            final Curve curve = (t, out) -> knotPoint(start + (end - start) * t, out);
            addPart(TubeMesh.build(curve, false, 400, 10, KNOT_RADIUS, ARC_COLORS[arc]));
        }

        // the base point, and a lasso for each arc: a ring around the middle of the arc, and a line to the base point
        final Vector3 base = new Vector3(0f, BASE_HEIGHT, 0f);
        addBall(base, 0.25f, Color.WHITE);
        for (int arc = 0; arc < 3; arc++) {
            final double start = arcStarts[arc];
            final double end = arc < 2 ? arcStarts[arc + 1] : arcStarts[0] + 2 * Math.PI;
            final double middle = (start + end) / 2;
            final Vector3 center = new Vector3(), tangent = new Vector3(), ahead = new Vector3(), behind = new Vector3();
            knotPoint(middle, center);
            knotPoint(middle + 1e-3, ahead);
            knotPoint(middle - 1e-3, behind);
            tangent.set(ahead).sub(behind).nor();
            // two axes perpendicular to the arc
            final Vector3 first = new Vector3(tangent).crs(Vector3.Y);
            if (first.len2() < 1e-6f) {
                first.set(tangent).crs(Vector3.X);
            }
            first.nor();
            final Vector3 second = new Vector3(tangent).crs(first).nor();
            final Color color = ARC_COLORS[arc];
            final Curve ring = (t, out) -> out.set(center)
                .mulAdd(first, RING_RADIUS * (float) Math.cos(2 * Math.PI * t))
                .mulAdd(second, RING_RADIUS * (float) Math.sin(2 * Math.PI * t));
            addPart(TubeMesh.build(ring, true, 48, 6, 0.035f, color));

            // the line goes from the base point to the point of the ring that is the closest to it
            final Vector3 closest = new Vector3(center);
            float best = Float.MAX_VALUE;
            for (int i = 0; i < 48; i++) {
                final Vector3 candidate = new Vector3();
                ring.pointAt(i / 48f, candidate);
                if (candidate.dst2(base) < best) {
                    best = candidate.dst2(base);
                    closest.set(candidate);
                }
            }
            final ModelInstance tail = new ModelInstance(model(Primitives.rod(color)));
            Primitives.place(tail, base, closest, 0.04f);
            addInstance(tail);
        }

        // markers at the crossings, a ball between the two strands
        markers = new ModelInstance[crossings.size()];
        final Model markerModel = model(Primitives.ball(0.3f, Color.YELLOW));
        for (int i = 0; i < markers.length; i++) {
            final Vector3 over = new Vector3(), under = new Vector3();
            knotPoint(crossings.get(i).over(), over);
            knotPoint(crossings.get(i).under(), under);
            markers[i] = new ModelInstance(markerModel);
            markers[i].transform.setToTranslation(over.add(under).scl(0.5f));
        }
    }

    @Override
    public String getWorldTitle() {
        return "Trefoil knot group";
    }

    @Override
    public void reposition(float deltaTime) {
        // nothing moves in this world
    }

    @Override
    public void render(ModelBatch modelBatch, Environment environment) {
        for (ModelInstance part : parts) {
            modelBatch.render(part, environment);
        }
        for (ModelInstance marker : markers) {
            modelBatch.render(marker, environment);
        }
    }

    @Override
    public void dispose() {
        models.forEach(Model::dispose);
    }

    /** The trefoil, with t from 0 to 2π. The Z coordinate of the usual formulas is the height. */
    private static void knotPoint(double t, Vector3 out) {
        final double x = Math.sin(t) + 2 * Math.sin(2 * t);
        final double y = Math.cos(t) - 2 * Math.cos(2 * t);
        final double z = -Math.sin(3 * t);
        out.set((float) (SCALE * x), (float) (SCALE * z), (float) (SCALE * y));
    }

    /** Finds the crossings of the knot seen from above, and where each arc starts (just after going under a strand). */
    private void findCrossings() {
        final double[][] xz = new double[SAMPLES][2];
        final Vector3 point = new Vector3();
        for (int i = 0; i < SAMPLES; i++) {
            knotPoint(2 * Math.PI * i / SAMPLES, point);
            xz[i][0] = point.x;
            xz[i][1] = point.z;
        }
        final List<double[]> found = new ArrayList<>();
        for (int i = 0; i < SAMPLES; i++) {
            for (int j = i + SAMPLES / 20; j < SAMPLES - SAMPLES / 20 + i && j < SAMPLES; j++) {
                if (Math.hypot(xz[i][0] - xz[j][0], xz[i][1] - xz[j][1]) < 0.04) {
                    final double[] refined = refine(2 * Math.PI * i / SAMPLES, 2 * Math.PI * j / SAMPLES);
                    if (refined != null && found.stream().noneMatch(f -> Math.abs(f[0] - refined[0]) < 1e-2 && Math.abs(f[1] - refined[1]) < 1e-2)) {
                        found.add(refined);
                    }
                }
            }
        }
        final Vector3 a = new Vector3(), b = new Vector3();
        final List<Double> unders = new ArrayList<>();
        for (double[] pair : found) {
            knotPoint(pair[0], a);
            knotPoint(pair[1], b);
            final boolean firstIsOver = a.y > b.y;
            final double over = firstIsOver ? pair[0] : pair[1], under = firstIsOver ? pair[1] : pair[0];
            crossings.add(new Crossing(over, under));
            unders.add(under);
        }
        if (crossings.size() != 3) {
            throw new IllegalStateException("The trefoil should have 3 crossings, found " + crossings.size());
        }
        unders.sort(Double::compare);
        for (int i = 0; i < 3; i++) {
            arcStarts[i] = unders.get(i);
        }
    }

    /** Newton's method for two parameters s, t where the knot, seen from above, has the same place. */
    private static double[] refine(double s, double t) {
        final Vector3 ps = new Vector3(), pt = new Vector3(), ds = new Vector3(), dt = new Vector3(), tmp = new Vector3();
        for (int iteration = 0; iteration < 30; iteration++) {
            knotPoint(s, ps);
            knotPoint(t, pt);
            final double fx = ps.x - pt.x, fz = ps.z - pt.z;
            if (Math.hypot(fx, fz) < 1e-9) {
                return new double[] {s % (2 * Math.PI), t % (2 * Math.PI)};
            }
            knotPoint(s + 1e-6, tmp);
            ds.set(tmp).sub(ps).scl(1e6f);
            knotPoint(t + 1e-6, tmp);
            dt.set(tmp).sub(pt).scl(1e6f);
            // solve [ds, -dt] [ds_, dt_] = -f
            final double a = ds.x, b = -dt.x, c = ds.z, d = -dt.z;
            final double determinant = a * d - b * c;
            if (Math.abs(determinant) < 1e-9) {
                return null;
            }
            s += (-fx * d + fz * b) / determinant;
            t += (-a * fz + c * fx) / determinant;
        }
        return null;
    }

    /** The arc that contains the parameter. */
    private int arcOf(double parameter) {
        final double t = ((parameter % (2 * Math.PI)) + 2 * Math.PI) % (2 * Math.PI);
        int arc = 2; // before the first start, we are on the arc that started at the last one
        for (int i = 0; i < 3; i++) {
            if (t >= arcStarts[i]) {
                arc = i;
            }
        }
        return arc;
    }

    private void addPart(Model model) {
        addInstance(new ModelInstance(model(model)));
    }

    private void addInstance(ModelInstance instance) {
        parts.add(instance);
    }

    private void addBall(Vector3 position, float diameter, Color color) {
        final ModelInstance ball = new ModelInstance(model(Primitives.ball(diameter, color)));
        ball.transform.setToTranslation(position);
        addInstance(ball);
    }

    /** Takes ownership of the model to dispose of it later. */
    private Model model(Model model) {
        models.add(model);
        return model;
    }
}
