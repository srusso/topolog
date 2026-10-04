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
import net.sr89.topology.HexColors;
import net.sr89.topology.math.Curve;
import net.sr89.topology.shapes.LineMesh;
import net.sr89.topology.shapes.ParametricSurface;
import net.sr89.topology.shapes.TubeMesh;

import java.util.ArrayList;
import java.util.List;

/**
 * The plane ℝ² is the universal covering space of the torus T² = ℝ² / ℤ².
 * <p>
 * The covering map wraps the plane around the torus: the point (u, v) of the plane goes to the point of the torus
 * that is u turns around the long way, and v turns around the short way. Points that differ by whole numbers in
 * both coordinates land on the same point of the torus, so every point of the torus has a lattice of preimages, one
 * in each unit square of the plane. The group of those translations, ℤ×ℤ, is the fundamental group of the torus.
 * <p>
 * This world draws the plane above the torus, with the preimages of one point of the torus, and a straight path
 * in the plane from (0, 0) to (p, q). That path is a lift of the loop a<sup>p</sup>b<sup>q</sup> on the torus: it
 * ends at a different preimage of the starting point, and the number of the preimage it ends at tells which element of
 * the fundamental group the loop is. A tracer moves along both at once.
 */
public class UniversalCoverOfTorus implements World {
    private static final float MAJOR_RADIUS = 1.4f;
    private static final float MINOR_RADIUS = 0.5f;

    /** Which part of the plane is drawn: u from MIN_U to MAX_U, and v from MIN_V to MAX_V. */
    private static final int MIN_U = -1, MAX_U = 3, MIN_V = -1, MAX_V = 4;
    private static final float TILE_SIZE = 0.8f;
    private static final float PLANE_HEIGHT = 3.2f;

    private static final int WINDING_P = 2; // times around the long way
    private static final int WINDING_Q = 3; // times around the short way
    private static final float TRACER_SECONDS = 20f;
    private static final int PATH_SAMPLES = 400;
    /** The path is drawn slightly outside the surface so it isn't hidden inside it. */
    private static final float PATH_RADIUS = MINOR_RADIUS * 1.04f;

    private final List<Model> models = new ArrayList<>();
    private final ModelInstance torus;
    private final ModelInstance grid;
    private final ModelInstance fundamentalSquare;
    private final List<ModelInstance> preimages = new ArrayList<>();
    private final ModelInstance basePoint;
    private final ModelInstance planePath;
    private final ModelInstance torusPath;
    private final ModelInstance planeTracer;
    private final ModelInstance torusTracer;

    private float progress = 0f; // in [0, 1)
    private final Vector3 tmp = new Vector3();

    public UniversalCoverOfTorus() {
        torus = instance(ParametricSurface.build(
            (u, v, out) -> torusPoint(u, v, MINOR_RADIUS, out), 96, 48, true,
            ParametricSurface.translucentMaterial(HexColors.greenPastel(), 0.4f)));

        grid = instance(createGrid());
        fundamentalSquare = instance(ParametricSurface.build(
            (a, b, out) -> planePoint(a, b, out), 1, 1, false,
            ParametricSurface.translucentMaterial(Color.ORANGE, 0.35f)));

        final Model preimage = model(sphere(0.12f, Color.ORANGE));
        for (int m = MIN_U; m <= MAX_U; m++) {
            for (int n = MIN_V; n <= MAX_V; n++) {
                final ModelInstance instance = new ModelInstance(preimage);
                planePoint(m, n, tmp);
                instance.transform.setToTranslation(tmp);
                preimages.add(instance);
            }
        }
        basePoint = new ModelInstance(preimage);
        torusPoint(0f, 0f, PATH_RADIUS, tmp);
        basePoint.transform.setToTranslation(tmp);

        final Curve planeLine = (t, out) -> planePoint(WINDING_P * t, WINDING_Q * t, out);
        final Curve torusLoop = (t, out) -> torusPoint(WINDING_P * t, WINDING_Q * t, PATH_RADIUS, out);
        planePath = instance(TubeMesh.build(planeLine, false, PATH_SAMPLES, 8, 0.03f, Color.CYAN));
        torusPath = instance(TubeMesh.build(torusLoop, true, PATH_SAMPLES, 8, 0.03f, Color.CYAN));

        final Model tracer = model(sphere(0.2f, Color.YELLOW));
        planeTracer = new ModelInstance(tracer);
        torusTracer = new ModelInstance(tracer);
        reposition(0f);
    }

    @Override
    public String getWorldTitle() {
        return "Universal cover of the torus: R^2 -> T^2";
    }

    @Override
    public void reposition(float deltaTime) {
        progress = (progress + deltaTime / TRACER_SECONDS) % 1f;
        planePoint(WINDING_P * progress, WINDING_Q * progress, tmp);
        planeTracer.transform.setToTranslation(tmp);
        torusPoint(WINDING_P * progress, WINDING_Q * progress, PATH_RADIUS, tmp);
        torusTracer.transform.setToTranslation(tmp);
    }

    @Override
    public void render(ModelBatch modelBatch, Environment environment) {
        // ModelBatch draws blended renderables (the torus, the highlighted square) after opaque ones, back to front.
        modelBatch.render(grid, environment);
        for (ModelInstance preimage : preimages) {
            modelBatch.render(preimage, environment);
        }
        modelBatch.render(basePoint, environment);
        modelBatch.render(planePath, environment);
        modelBatch.render(torusPath, environment);
        modelBatch.render(planeTracer, environment);
        modelBatch.render(torusTracer, environment);
        modelBatch.render(fundamentalSquare, environment);
        modelBatch.render(torus, environment);
    }

    @Override
    public void dispose() {
        models.forEach(Model::dispose);
    }

    /** Where the point (u, v) of the plane is drawn: the drawn part of the plane is centered above the torus. */
    private static void planePoint(float u, float v, Vector3 out) {
        out.set((u - (MIN_U + MAX_U) / 2f) * TILE_SIZE, PLANE_HEIGHT, (v - (MIN_V + MAX_V) / 2f) * TILE_SIZE);
    }

    /**
     * The covering map. u goes the long way around the torus (about the Y axis), v goes the short way (around the
     * tube); both in turns, so 1 = one full revolution.
     */
    private static void torusPoint(float u, float v, float tubeRadius, Vector3 out) {
        final double alpha = 2 * Math.PI * u, beta = 2 * Math.PI * v;
        final double ring = MAJOR_RADIUS + tubeRadius * Math.cos(beta);
        out.set((float) (ring * Math.cos(alpha)), (float) (tubeRadius * Math.sin(beta)), (float) (ring * Math.sin(alpha)));
    }

    private Model createGrid() {
        final List<Vector3[]> lines = new ArrayList<>();
        for (int m = MIN_U; m <= MAX_U; m++) {
            lines.add(new Vector3[] {planePoint(m, MIN_V), planePoint(m, MAX_V)});
        }
        for (int n = MIN_V; n <= MAX_V; n++) {
            lines.add(new Vector3[] {planePoint(MIN_U, n), planePoint(MAX_U, n)});
        }
        return LineMesh.build(lines, Color.LIGHT_GRAY);
    }

    private static Vector3 planePoint(float u, float v) {
        final Vector3 out = new Vector3();
        planePoint(u, v, out);
        return out;
    }

    private static Model sphere(float diameter, Color color) {
        return new ModelBuilder().createSphere(diameter, diameter, diameter, 12, 12,
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
