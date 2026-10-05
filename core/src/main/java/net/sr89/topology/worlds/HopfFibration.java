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
 * The Hopf fibration S³ → S².
 * <p>
 * S³ is the set of points (z₁, z₂) of ℂ² with |z₁|² + |z₂|² = 1, and the Hopf map sends it to the sphere
 * S² by (z₁, z₂) ↦ (2 z₁ z̄₂, |z₁|² − |z₂|²). The points that have the same image are a circle (z₁ e^{is}, z₂ e^{is}): the
 * fibre. So S³ is a union of circles, one for each point of S². We can't draw S³ (it has 4 dimensions), but with the
 * stereographic projection to ℝ³ the fibres are circles, and they have a very nice arrangement:
 * <ul>
 *   <li>The fibres over a circle of latitude of S² make a torus. Different latitudes give nested tori (the Hopf tori).</li>
 *   <li>Any two fibres are linked, exactly once (the two thick circles, red and blue). The linking number of two fibres is
 *       the Hopf invariant of the map, 1, and it is why the Hopf map is not homotopic to a constant map: it generates π₃(S²) = ℤ.</li>
 * </ul>
 * On the right is the sphere S², with one point for each fibre (the same color). The white point moves, and its fibre (the beads) moves with it.
 * The long exact sequence of the fibration gives π₂(S²) ≅ π₁(S¹) = ℤ, and π₃(S²) ≅ π₃(S³) = ℤ.
 */
public class HopfFibration implements World {
    private static final float SCALE = 0.55f;
    private static final double[] LATITUDES = {0.45, 0.9, 1.35, 1.8};
    private static final int FIBRES_PER_LATITUDE = 10;
    private static final int FIBRE_SAMPLES = 160;
    private static final int BEADS = 120;
    private static final Vector3 SPHERE_CENTER = new Vector3(4.6f, 0.3f, 0f);
    private static final float SPHERE_RADIUS = 1f;

    private final List<Model> models = new ArrayList<>();
    private final List<ModelInstance> fibres = new ArrayList<>();
    private final ModelInstance[] beads = new ModelInstance[BEADS];
    private final ModelInstance baseSphere;
    private final ModelInstance movingBase;
    private final List<ModelInstance> baseDots = new ArrayList<>();
    private float time = 0f;
    private final Vector3 tmp = new Vector3();

    public HopfFibration() {
        for (double latitude : LATITUDES) {
            for (int j = 0; j < FIBRES_PER_LATITUDE; j++) {
                final double azimuth = 2 * Math.PI * j / FIBRES_PER_LATITUDE;
                final Color color = Primitives.hue((float) (azimuth / (2 * Math.PI)), 0.8f, (float) (1.0 - 0.12 * latitude));
                final Curve fibre = (t, out) -> fibrePoint(latitude, azimuth, 2 * Math.PI * t, out);
                fibres.add(instance(TubeMesh.build(fibre, true, FIBRE_SAMPLES, 6, 0.04f, color)));

                final ModelInstance dot = new ModelInstance(model(Primitives.ball(0.1f, color)));
                basePoint(latitude, azimuth, tmp);
                dot.transform.setToTranslation(tmp);
                baseDots.add(dot);
            }
        }
        // the circle over the north pole is also a fibre, and two fibres that show the linking
        final Curve core = (t, out) -> fibrePoint(0.0, 0.0, 2 * Math.PI * t, out);
        fibres.add(instance(TubeMesh.build(core, true, FIBRE_SAMPLES, 6, 0.04f, Color.WHITE)));
        final Curve first = (t, out) -> fibrePoint(1.2, 0.0, 2 * Math.PI * t, out);
        final Curve second = (t, out) -> fibrePoint(1.2, Math.PI, 2 * Math.PI * t, out);
        fibres.add(instance(TubeMesh.build(first, true, FIBRE_SAMPLES, 8, 0.08f, Color.RED)));
        fibres.add(instance(TubeMesh.build(second, true, FIBRE_SAMPLES, 8, 0.08f, Color.BLUE)));
        for (double azimuth : new double[] {0.0, Math.PI}) {
            final ModelInstance dot = new ModelInstance(model(Primitives.ball(0.18f, azimuth == 0.0 ? Color.RED : Color.BLUE)));
            basePoint(1.2, azimuth, tmp);
            dot.transform.setToTranslation(tmp);
            baseDots.add(dot);
        }

        baseSphere = instance(ParametricSurface.build((u, v, out) -> out.set(
                (float) (SPHERE_RADIUS * Math.sin(Math.PI * u) * Math.cos(2 * Math.PI * v)),
                (float) (SPHERE_RADIUS * Math.cos(Math.PI * u)),
                (float) (SPHERE_RADIUS * Math.sin(Math.PI * u) * Math.sin(2 * Math.PI * v))).add(SPHERE_CENTER),
            24, 48, true, ParametricSurface.translucentMaterial(HexColors.greenPastel(), 0.3f)));

        final Model bead = model(Primitives.ball(0.09f, Color.WHITE));
        for (int i = 0; i < BEADS; i++) {
            beads[i] = new ModelInstance(bead);
        }
        movingBase = new ModelInstance(model(Primitives.ball(0.2f, Color.WHITE)));
        reposition(0f);
    }

    @Override
    public String getWorldTitle() {
        return "Hopf fibration: S^3 is a union of linked circles over S^2";
    }

    @Override
    public void reposition(float deltaTime) {
        time += deltaTime;
        // a point of the sphere that moves around, and its fibre
        final double latitude = 1.0 + 0.6 * Math.sin(0.35 * time);
        final double azimuth = 0.5 * time;
        for (int i = 0; i < BEADS; i++) {
            fibrePoint(latitude, azimuth, 2 * Math.PI * i / BEADS, tmp);
            beads[i].transform.setToTranslation(tmp);
        }
        basePoint(latitude, azimuth, tmp);
        movingBase.transform.setToTranslation(tmp);
    }

    @Override
    public void render(ModelBatch modelBatch, Environment environment) {
        for (ModelInstance fibre : fibres) {
            modelBatch.render(fibre, environment);
        }
        for (ModelInstance bead : beads) {
            modelBatch.render(bead, environment);
        }
        for (ModelInstance dot : baseDots) {
            modelBatch.render(dot, environment);
        }
        modelBatch.render(movingBase, environment);
        modelBatch.render(baseSphere, environment);
    }

    @Override
    public void dispose() {
        models.forEach(Model::dispose);
    }

    /**
     * The point of the fibre over the point of the sphere with the given polar angle and azimuth, drawn in the world: the
     * point of S³ at the parameter s along the circle, after the stereographic projection.
     */
    static void fibrePoint(double polar, double azimuth, double s, Vector3 out) {
        // z1 = cos(polar / 2) e^{i s}, z2 = sin(polar / 2) e^{i (s - azimuth)}: the Hopf map gives the azimuth back
        final double x1 = Math.cos(polar / 2) * Math.cos(s), y1 = Math.cos(polar / 2) * Math.sin(s);
        final double x2 = Math.sin(polar / 2) * Math.cos(s - azimuth), y2 = Math.sin(polar / 2) * Math.sin(s - azimuth);
        final double k = SCALE / (1 - y2);
        out.set((float) (k * x1), (float) (k * x2), (float) (k * y1));
    }

    /** The point of the sphere on the right that stands for the fibre. */
    private static void basePoint(double polar, double azimuth, Vector3 out) {
        out.set((float) (SPHERE_RADIUS * Math.sin(polar) * Math.cos(azimuth)), (float) (SPHERE_RADIUS * Math.cos(polar)),
            (float) (SPHERE_RADIUS * Math.sin(polar) * Math.sin(azimuth))).add(SPHERE_CENTER);
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
