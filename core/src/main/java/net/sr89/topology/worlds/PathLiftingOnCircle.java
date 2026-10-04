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
import net.sr89.topology.shapes.Tube;
import net.sr89.topology.shapes.TubeMesh;
import net.sr89.topology.shapes.Tubes;

import java.util.ArrayList;
import java.util.List;

/**
 * Path lifting for the covering map from the helix to the circle.
 * <p>
 * A loop γ on the circle can be lifted to the helix: there is exactly one path γ̃ on the helix that starts at a
 * chosen point above γ's start, and drops straight down onto γ at every moment. Even though γ is a loop, its lift
 * usually isn't: it ends on a different sheet of the helix, above the same point of the circle. How many sheets up
 * it ended is the winding number of γ, which is the element of the fundamental group ℤ of the circle that γ is.
 * <p>
 * The loop here goes around the circle twice, but it wiggles: it turns back for a while, and goes forward again.
 * The lift follows it up and down the helix, and still ends exactly 2 sheets above where it started.
 * Two tracers move along γ and its lift at the same time.
 */
public class PathLiftingOnCircle implements World {
    /** How many times the loop goes around the circle. */
    private static final int WINDING_NUMBER = 2;
    /** Where on the circle the loop starts, in turns. Chosen so that the lift stays within the helix. */
    private static final float START_ANGLE = 0.4f;
    /** How much the loop turns back and forth, and how many times, so it isn't a plain walk around the circle. */
    private static final float WIGGLE = 0.22f;
    private static final int WIGGLES = 3;
    private static final float SECONDS_PER_LOOP = 24f;
    private static final int LIFT_SAMPLES = 600;

    private final Tube circle = Tubes.unitCircle();
    private final Tube helix = Tubes.helix();
    private final List<Model> models = new ArrayList<>();
    private final ModelInstance lift;
    private final ModelInstance start;
    private final ModelInstance end;
    private final ModelInstance startOnCircle;
    private final ModelInstance circleTracer;
    private final ModelInstance helixTracer;

    private float progress = 0f; // in [0, 1)
    private final Vector3 tmp = new Vector3();

    public PathLiftingOnCircle() {
        // the lift, drawn over the helix, a bit thicker so that it stands out
        final Curve liftCurve = (t, out) -> helixPoint(angle(t), out);
        lift = instance(TubeMesh.build(liftCurve, false, LIFT_SAMPLES, 8, 0.04f, Color.ORANGE));

        final Model startModel = model(sphere(0.16f, Color.GREEN));
        final Model endModel = model(sphere(0.16f, Color.RED));
        start = new ModelInstance(startModel);
        helixPoint(angle(0f), tmp);
        start.transform.setToTranslation(tmp);
        end = new ModelInstance(endModel);
        helixPoint(angle(1f), tmp);
        end.transform.setToTranslation(tmp);
        // the start and the end of the loop are the same point of the circle
        startOnCircle = new ModelInstance(startModel);
        circlePoint(angle(0f), tmp);
        startOnCircle.transform.setToTranslation(tmp);

        final Model tracer = model(sphere(0.2f, Color.YELLOW));
        circleTracer = new ModelInstance(tracer);
        helixTracer = new ModelInstance(tracer);
        reposition(0f);
    }

    @Override
    public String getWorldTitle() {
        return "Path lifting on the circle";
    }

    @Override
    public void reposition(float deltaTime) {
        progress = (progress + deltaTime / SECONDS_PER_LOOP) % 1f;
        circlePoint(angle(progress), tmp);
        circleTracer.transform.setToTranslation(tmp);
        helixPoint(angle(progress), tmp);
        helixTracer.transform.setToTranslation(tmp);
    }

    @Override
    public void render(ModelBatch modelBatch, Environment environment) {
        circle.render(modelBatch, environment);
        helix.render(modelBatch, environment);
        modelBatch.render(lift, environment);
        modelBatch.render(start, environment);
        modelBatch.render(end, environment);
        modelBatch.render(startOnCircle, environment);
        modelBatch.render(circleTracer, environment);
        modelBatch.render(helixTracer, environment);
    }

    @Override
    public void dispose() {
        circle.dispose();
        helix.dispose();
        models.forEach(Model::dispose);
    }

    /**
     * The loop γ, and its lift, at time t from 0 to 1. This is the angle of the loop around the circle, counted in
     * turns without wrapping around: the loop is the point of the circle at this angle (mod 1), and the lift is the point
     * of the helix at this height (see {@link Tubes#helixHeightOver}).
     */
    private static float angle(float t) {
        return START_ANGLE + WINDING_NUMBER * t + WIGGLE * (float) Math.sin(2 * Math.PI * WIGGLES * t);
    }

    private static void circlePoint(float angle, Vector3 out) {
        out.set((float) Math.cos(2 * Math.PI * angle), 0f, (float) Math.sin(2 * Math.PI * angle));
    }

    /** The point of the helix above the circle point at the given angle, on the sheet given by the whole part. */
    private static void helixPoint(float angle, Vector3 out) {
        circlePoint(angle, out);
        out.y = Tubes.helixHeightOver(0f, 0) + angle;
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
