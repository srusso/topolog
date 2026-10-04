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
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector3;
import net.sr89.topology.HexColors;
import net.sr89.topology.math.Curve;
import net.sr89.topology.shapes.ParametricSurface;
import net.sr89.topology.shapes.TubeMesh;

import java.util.ArrayList;
import java.util.List;

/**
 * The real projective plane ℝP², drawn as Boy's surface: the nicest way to fit it in ℝ³ without cutting it open
 * (it still has to pass through itself, along three curves that meet at one point, the triple point).
 * <p>
 * Its fundamental group is ⟨a | a²⟩ = ℤ/2: going around the loop a (red) twice is the same as not moving at all.
 * <p>
 * The surface is the image of a rectangle (u across, v from the bottom pole to the top pole). The bottom and top sides are
 * squeezed to a point, the same one, which is the lowest point of the surface (green): it is where all the loops start and
 * end. The left and right sides are glued with a flip. The left side, from pole to pole, is the loop a (red). Each vertical
 * line of the rectangle is a loop like that, and all of them are equivalent to a.
 * <p>
 * Following a up the left side and then down the right side gives a loop that goes around a twice, and that bounds the whole
 * rectangle. So it can be shrunk to nothing by moving the right side towards the left side, which sweeps over the
 * rectangle in between: that moving side is drawn as yellow beads, going back and forth between the left side (where the
 * loop goes up a and straight down it again, which is trivial) and the right side (where it is a², going twice around).
 * A tracer goes around the loop all the time.
 * <p>
 * Things to look for: the lowest point where every loop starts (green), the triple point on the vertical axis above it
 * (white), where the red loop and two other sheets of the surface cross, and the three petals of double points
 * around it, 120° apart.
 */
public class ProjectivePlane implements World {
    private static final float SCALE = 1f;
    /**
     * Moves the surface so that its vertical axis of symmetry is the Y axis, and it is centered vertically.
     * (The surface is 3 high, and its lowest point is at height 0.)
     */
    private static final Vector3 CENTER_OFFSET = new Vector3(0f, -1.5f, 0f);
    private static final int LOOP_SAMPLES = 200;
    private static final int BEADS = 48;
    private static final float TRACER_SECONDS_PER_LAP = 6f;
    private static final float SECONDS_PER_SWEEP = 14f;

    private final List<Model> models = new ArrayList<>();
    private final ModelInstance surface;
    private final ModelInstance loopA;
    private final ModelInstance lowestPoint;
    private final ModelInstance triplePoint;
    private final ModelInstance[] beads = new ModelInstance[BEADS + 1];
    private final ModelInstance tracer;

    private float time = 0f;
    private float laps = 0f; // in [0, 1)
    private final Vector3 tmp = new Vector3();

    public ProjectivePlane() {
        surface = instance(ParametricSurface.build(ProjectivePlane::point, 160, 80, false,
            ParametricSurface.translucentMaterial(HexColors.greenPastel(), 0.4f)));

        // the loop a: the left side of the rectangle
        final Curve a = (t, out) -> point(0f, t, out);
        loopA = instance(TubeMesh.build(a, false, LOOP_SAMPLES, 8, 0.04f, Color.RED));

        lowestPoint = instance(sphere(0.18f, Color.GREEN));
        point(0f, 0f, tmp);
        lowestPoint.transform.setToTranslation(tmp);
        triplePoint = instance(sphere(0.14f, Color.WHITE));
        // the triple point is at (0, 0, 1) in the coordinates of the parametrization
        toWorld(0f, 0f, 1f, tmp);
        triplePoint.transform.setToTranslation(tmp);

        // beads, big enough to be seen on top of the red loop, along the side that sweeps over the rectangle
        final Model bead = model(sphere(0.1f, Color.YELLOW));
        for (int i = 0; i <= BEADS; i++) {
            beads[i] = new ModelInstance(bead);
        }
        tracer = instance(sphere(0.2f, Color.ORANGE));
        reposition(0f);
    }

    @Override
    public String getWorldTitle() {
        return "Projective plane (Boy's surface): <a | a^2>";
    }

    @Override
    public void reposition(float deltaTime) {
        time += deltaTime;
        laps = (laps + deltaTime / TRACER_SECONDS_PER_LAP) % 1f;

        // The side that moves, from the left side (0) to the right one (1) and back, smoothly.
        final float cycle = (time / SECONDS_PER_SWEEP) % 1f;
        final float u = Interpolation.smooth.apply(cycle < 0.5f ? 2 * cycle : 2 - 2 * cycle);
        for (int i = 0; i <= BEADS; i++) {
            point(u, (float) i / BEADS, tmp);
            beads[i].transform.setToTranslation(tmp);
        }

        // Up the left side, then down the moving side. They both start and end at the lowest point, so the loop is
        // closed whatever u is, even though u is changing.
        if (laps < 0.5f) {
            point(0f, 2 * laps, tmp);
        } else {
            point(u, 2 * (1 - laps), tmp);
        }
        tracer.transform.setToTranslation(tmp);
    }

    @Override
    public void render(ModelBatch modelBatch, Environment environment) {
        modelBatch.render(loopA, environment);
        modelBatch.render(lowestPoint, environment);
        modelBatch.render(triplePoint, environment);
        for (ModelInstance bead : beads) {
            modelBatch.render(bead, environment);
        }
        modelBatch.render(tracer, environment);
        modelBatch.render(surface, environment);
    }

    @Override
    public void dispose() {
        models.forEach(Model::dispose);
    }

    /**
     * Boy's surface, with Apéry's parametrization (see MathWorld, "Boy Surface"). u is the position across the rectangle
     * (from 0 to 1), v goes from one pole to the other (from 0 to 1).
     */
    private static void point(float u, float v, Vector3 out) {
        final double bigU = Math.PI * u, bigV = Math.PI * (v - 0.5);
        final double cosV = Math.cos(bigV);
        final double denominator = 2 - Math.sqrt(2) * Math.sin(3 * bigU) * Math.sin(2 * bigV);
        final double x = (Math.sqrt(2) * cosV * cosV * Math.cos(2 * bigU) + Math.cos(bigU) * Math.sin(2 * bigV)) / denominator;
        final double y = (Math.sqrt(2) * cosV * cosV * Math.sin(2 * bigU) - Math.sin(bigU) * Math.sin(2 * bigV)) / denominator;
        final double z = 3 * cosV * cosV / denominator;
        toWorld((float) x, (float) y, (float) z, out);
    }

    /** From the coordinates of the parametrization (Z up) to the world (Y up). */
    private static void toWorld(float x, float y, float z, Vector3 out) {
        out.set(SCALE * x, SCALE * z, SCALE * y).add(CENTER_OFFSET);
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
