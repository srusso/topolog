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
 * The Borsuk–Ulam theorem for the sphere: every continuous map g from S² to the plane sends some pair of antipodal points to
 * the same point: g(p) = g(−p). (Think of g as the temperature and the pressure on the Earth: there are two points opposite
 * each other with the same temperature and the same pressure.)
 * <p>
 * Here g = (T, P), with T(p) = z + (something even), and P any function. T(p) = T(−p) exactly where T(p) − T(−p) = 2z is zero, which is the
 * equator (red). On the equator, k(θ) = P(p) − P(−p) is an odd function of the position (k(θ + π) = −k(θ)), so it has to
 * be zero somewhere in half a turn, by the intermediate value theorem. At that pair of points both T and P agree.
 * <p>
 * The graph on the right is k, with the position p on the equator (orange) and the antipodal point (cyan) on it. They
 * turn green when k = 0. The general proof uses the fundamental group: an odd map from S² to the circle can't exist,
 * as it would lift to the double cover of the circle in a way that is impossible.
 */
public class BorsukUlam implements World {
    private static final float RADIUS = 1.5f;
    private static final Vector3 CENTER = new Vector3(-2.4f, 0f, 0f);
    private static final Vector3 PLOT_ORIGIN = new Vector3(0.7f, 0f, 0f);
    private static final float PLOT_WIDTH = 5f;
    private static final int SAMPLES = 400;
    private static final float SECONDS_PER_TURN = 20f;
    private static final float GREEN_DISTANCE = 0.06f;

    private final List<Model> models = new ArrayList<>();
    private final ModelInstance sphere;
    private final ModelInstance equator;
    private final ModelInstance graph;
    private final ModelInstance axis;
    private final ModelInstance halfTurnMark;
    private final ModelInstance[] solutions;
    private final ModelInstance p, antipode, pOnGraph, antipodeOnGraph;
    private final ModelInstance pGreen, antipodeGreen, pOnGraphGreen, antipodeOnGraphGreen;
    private final double scale;

    private float turns = 0f; // of the position p along the equator, in [0, 1)
    private boolean atSolution;
    private final Vector3 tmp = new Vector3();

    public BorsukUlam() {
        sphere = instance(ParametricSurface.build((u, v, out) -> onSphere(Math.PI * u, 2 * Math.PI * v, out),
            32, 64, true, ParametricSurface.translucentMaterial(HexColors.greenPastel(), 0.35f)));

        final Curve equatorCurve = (t, out) -> onSphere(Math.PI / 2, 2 * Math.PI * t, out);
        equator = instance(TubeMesh.build(equatorCurve, true, 200, 8, 0.03f, Color.RED));

        // k is odd, and its largest value sets the height of the graph
        double largest = 0;
        for (int i = 0; i < SAMPLES; i++) {
            largest = Math.max(largest, Math.abs(k(2 * Math.PI * i / SAMPLES)));
        }
        scale = 1.4 / largest;

        final Curve graphCurve = (t, out) -> graphPoint(2 * Math.PI * Math.max(0, Math.min(1, t)), out);
        graph = instance(TubeMesh.build(graphCurve, false, SAMPLES, 8, 0.03f, Color.WHITE));
        axis = new ModelInstance(model(Primitives.rod(Color.GRAY)));
        Primitives.place(axis, new Vector3(PLOT_ORIGIN), new Vector3(PLOT_ORIGIN).add(PLOT_WIDTH, 0f, 0f), 0.02f);
        halfTurnMark = new ModelInstance(model(Primitives.rod(Color.DARK_GRAY)));
        Primitives.place(halfTurnMark, new Vector3(PLOT_ORIGIN).add(PLOT_WIDTH / 2, -1.6f, 0f),
            new Vector3(PLOT_ORIGIN).add(PLOT_WIDTH / 2, 1.6f, 0f), 0.015f);

        // where k = 0: both ends of the pair, on the sphere and on the graph
        final List<Double> zeros = new ArrayList<>();
        for (int i = 0; i < SAMPLES; i++) {
            final double a = 2 * Math.PI * i / SAMPLES, b = 2 * Math.PI * (i + 1) / SAMPLES;
            if (k(a) * k(b) < 0) {
                zeros.add(bisect(a, b));
            }
        }
        final Model greenBall = model(Primitives.ball(0.2f, Color.GREEN));
        solutions = new ModelInstance[2 * zeros.size()];
        for (int i = 0; i < zeros.size(); i++) {
            solutions[2 * i] = new ModelInstance(greenBall);
            equatorPoint(zeros.get(i), tmp);
            solutions[2 * i].transform.setToTranslation(tmp);
            solutions[2 * i + 1] = new ModelInstance(greenBall);
            graphPoint(zeros.get(i), tmp);
            solutions[2 * i + 1].transform.setToTranslation(tmp);
        }

        p = new ModelInstance(model(Primitives.ball(0.2f, Color.ORANGE)));
        pOnGraph = new ModelInstance(model(Primitives.ball(0.16f, Color.ORANGE)));
        antipode = new ModelInstance(model(Primitives.ball(0.2f, Color.CYAN)));
        antipodeOnGraph = new ModelInstance(model(Primitives.ball(0.16f, Color.CYAN)));
        pGreen = new ModelInstance(greenBall);
        antipodeGreen = new ModelInstance(greenBall);
        pOnGraphGreen = new ModelInstance(greenBall);
        antipodeOnGraphGreen = new ModelInstance(greenBall);
        reposition(0f);
    }

    @Override
    public String getWorldTitle() {
        return "Borsuk-Ulam theorem";
    }

    @Override
    public void reposition(float deltaTime) {
        turns = (turns + deltaTime / SECONDS_PER_TURN) % 1f;
        final double angle = 2 * Math.PI * turns;
        atSolution = Math.abs(k(angle) * scale) < GREEN_DISTANCE;

        equatorPoint(angle, tmp);
        p.transform.setToTranslation(tmp);
        pGreen.transform.setToTranslation(tmp);
        equatorPoint(angle + Math.PI, tmp);
        antipode.transform.setToTranslation(tmp);
        antipodeGreen.transform.setToTranslation(tmp);
        graphPoint(angle, tmp);
        pOnGraph.transform.setToTranslation(tmp);
        pOnGraphGreen.transform.setToTranslation(tmp);
        graphPoint(angle + Math.PI, tmp);
        antipodeOnGraph.transform.setToTranslation(tmp);
        antipodeOnGraphGreen.transform.setToTranslation(tmp);
    }

    @Override
    public void render(ModelBatch modelBatch, Environment environment) {
        modelBatch.render(equator, environment);
        modelBatch.render(graph, environment);
        modelBatch.render(axis, environment);
        modelBatch.render(halfTurnMark, environment);
        for (ModelInstance solution : solutions) {
            modelBatch.render(solution, environment);
        }
        if (atSolution) {
            modelBatch.render(pGreen, environment);
            modelBatch.render(antipodeGreen, environment);
            modelBatch.render(pOnGraphGreen, environment);
            modelBatch.render(antipodeOnGraphGreen, environment);
        } else {
            modelBatch.render(p, environment);
            modelBatch.render(antipode, environment);
            modelBatch.render(pOnGraph, environment);
            modelBatch.render(antipodeOnGraph, environment);
        }
        modelBatch.render(sphere, environment);
    }

    @Override
    public void dispose() {
        models.forEach(Model::dispose);
    }

    /** The point of the sphere with the given polar angle (from the top) and azimuth, in the world: the sphere's Z axis is up. */
    private static void onSphere(double polar, double azimuth, Vector3 out) {
        final double x = Math.sin(polar) * Math.cos(azimuth), y = Math.sin(polar) * Math.sin(azimuth), z = Math.cos(polar);
        out.set((float) (RADIUS * x), (float) (RADIUS * z), (float) (RADIUS * y)).add(CENTER);
    }

    private static void equatorPoint(double angle, Vector3 out) {
        onSphere(Math.PI / 2, angle, out);
    }

    /** The second function: any continuous function would do. */
    private static double pressure(double x, double y, double z) {
        return 0.8 * x + 0.6 * y * y + 0.5 * z + 0.4 * x * y + 0.3 * Math.sin(2 * y + x);
    }

    /** P(p) - P(-p), for the point of the equator at the angle. It is odd: k(angle + π) = -k(angle). */
    private static double k(double angle) {
        final double x = Math.cos(angle), y = Math.sin(angle);
        return pressure(x, y, 0) - pressure(-x, -y, 0);
    }

    private void graphPoint(double angle, Vector3 out) {
        out.set(PLOT_ORIGIN).add((float) (PLOT_WIDTH * angle / (2 * Math.PI)), (float) (scale * k(angle)), 0f);
    }

    private static double bisect(double a, double b) {
        for (int i = 0; i < 50; i++) {
            final double middle = (a + b) / 2;
            if (k(a) * k(middle) <= 0) {
                b = middle;
            } else {
                a = middle;
            }
        }
        return (a + b) / 2;
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
