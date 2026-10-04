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
import net.sr89.topology.shapes.ParametricSurface;
import net.sr89.topology.shapes.TubeMesh;

import java.util.ArrayList;
import java.util.List;

/**
 * The real projective plane ℝP², drawn as Boy's surface: the nicest way to fit it in ℝ³ without cutting it open
 * (it still has to pass through itself, along three curves that meet at a point).
 * <p>
 * Its fundamental group is ⟨a | a²⟩ = ℤ/2: going around the loop a (red) twice is the same as not moving at all.
 * <p>
 * The surface is a rectangle whose top and bottom sides are squeezed to a point, and whose left and right sides are
 * glued with a flip. The left side, from pole to pole, is the loop a. Following a up the left side and then down
 * the right side gives a loop that goes twice around a, and it bounds the whole rectangle. So it can be shrunk to
 * nothing by moving the right side towards the left one, which sweeps over the rectangle in between. Those
 * intermediate sides are drawn, and a tracer goes around the loop for each of them in turn, from a lap
 * that is trivial (the right side on top of the left side: going up and down the same path) to a lap that is a².
 */
public class ProjectivePlane implements World {
    private static final float SCALE = 1f;
    /** Moves the surface so that its bounding box is centered at the origin (measured on the mesh). */
    private static final Vector3 CENTER_OFFSET = new Vector3(-0.377f, -1.498f, 0.146f);
    private static final int STEPS = 5;
    private static final int LOOP_SAMPLES = 200;
    private static final float TRACER_SECONDS_PER_LAP = 6f;

    private final List<Model> models = new ArrayList<>();
    private final ModelInstance surface;
    private final List<ModelInstance> sides = new ArrayList<>();
    private final ModelInstance tracer;

    private float laps = 0f; // in [0, STEPS + 1)
    private int shownLap = -1;
    private final Vector3 tmp = new Vector3();

    public ProjectivePlane() {
        surface = instance(ParametricSurface.build(ProjectivePlane::point, 160, 80, false,
            ParametricSurface.translucentMaterial(HexColors.greenPastel(), 0.4f)));

        // the vertical sides of the rectangle, at u = k / STEPS. The first one (k = 0) is the loop a.
        for (int k = 0; k <= STEPS; k++) {
            final float u = (float) k / STEPS;
            final Curve side = (t, out) -> point(u, t, out);
            // Boy's surface passes through itself, so the pole at the end of a side is a single point of the surface
            sides.add(instance(TubeMesh.build(side, false, LOOP_SAMPLES, 8, k == 0 ? 0.045f : 0.03f, Color.GRAY)));
        }

        tracer = instance(sphere(0.2f, Color.YELLOW));
        reposition(0f);
    }

    @Override
    public String getWorldTitle() {
        return "Projective plane (Boy's surface): <a | a^2>";
    }

    @Override
    public void reposition(float deltaTime) {
        laps = (laps + deltaTime / TRACER_SECONDS_PER_LAP) % (STEPS + 1);
        final int lap = (int) laps;
        final float t = laps - lap;

        if (lap != shownLap) {
            shownLap = lap;
            for (int k = 0; k <= STEPS; k++) {
                final Color color = k == 0 ? Color.RED : k == lap ? Color.YELLOW : Color.GRAY;
                sides.get(k).materials.first().set(ColorAttribute.createDiffuse(color));
            }
        }

        // Up the left side (u = 0), then down the side at u = lap / STEPS.
        final float u = (float) lap / STEPS;
        if (t < 0.5f) {
            point(0f, 2 * t, tmp);
        } else {
            point(u, 2 * (1 - t), tmp);
        }
        tracer.transform.setToTranslation(tmp);
    }

    @Override
    public void render(ModelBatch modelBatch, Environment environment) {
        for (ModelInstance side : sides) {
            modelBatch.render(side, environment);
        }
        modelBatch.render(tracer, environment);
        modelBatch.render(surface, environment);
    }

    @Override
    public void dispose() {
        models.forEach(Model::dispose);
    }

    /**
     * Boy's surface, with Apéry's parametrization. u is the position across the rectangle (from 0 to 1), v goes from
     * one pole to the other (from 0 to 1).
     */
    private static void point(float u, float v, Vector3 out) {
        final double bigU = Math.PI * u, bigV = Math.PI * (v - 0.5);
        final double cosV = Math.cos(bigV);
        final double denominator = 2 - Math.sqrt(2) * Math.sin(3 * bigU) * Math.sin(2 * bigV);
        final double x = (Math.sqrt(2) * cosV * cosV * Math.cos(2 * bigU) + Math.cos(bigU) * Math.sin(2 * bigV)) / denominator;
        final double y = (Math.sqrt(2) * cosV * cosV * Math.sin(2 * bigU) - Math.sin(bigU) * Math.sin(2 * bigV)) / denominator;
        final double z = 3 * cosV * cosV / denominator;
        // Y is up in the world
        out.set((float) (SCALE * x), (float) (SCALE * z), (float) (SCALE * y)).add(CENTER_OFFSET);
    }

    private static Model sphere(float diameter, Color color) {
        return new ModelBuilder().createSphere(diameter, diameter, diameter, 16, 16,
            new Material(ColorAttribute.createDiffuse(color)), Usage.Position | Usage.Normal);
    }

    private ModelInstance instance(Model model) {
        models.add(model);
        return new ModelInstance(model);
    }
}
