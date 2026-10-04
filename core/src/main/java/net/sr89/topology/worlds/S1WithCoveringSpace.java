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
import net.sr89.topology.shapes.Tube;
import net.sr89.topology.shapes.Tubes;

import java.util.ArrayList;
import java.util.List;

/**
 * The circle S¹ and its universal covering space, the helix (a copy of ℝ wound around it).
 * <p>
 * The covering map drops each point of the helix straight down onto the circle. To show it, a few points of the
 * circle are marked, along with the points of the helix that map to them (their preimages, one for each time the
 * helix goes around). Each of those is right above the point it maps to.
 */
public class S1WithCoveringSpace implements World {
    /** Angles, in full turns around the Y axis, of the marked points of the circle. */
    private static final float[] POINT_ANGLES = {0.08f, 0.38f, 0.66f};
    private static final Color[] POINT_COLORS = {Color.ORANGE, Color.RED, Color.MAGENTA};
    private static final float POINT_DIAMETER = 0.14f;

    private final Tube helix;
    private final Tube unitCircle;

    private final List<Model> models = new ArrayList<>();
    private final List<ModelInstance> circlePoints = new ArrayList<>();
    /** The marked points of the helix, i.e. the preimages of the marked points of the circle. */
    private final List<ModelInstance> lifts = new ArrayList<>();

    public S1WithCoveringSpace() {
        helix = Tubes.helix();
        unitCircle = Tubes.unitCircle();

        for (int i = 0; i < POINT_ANGLES.length; i++) {
            final float x = (float) Math.cos(2 * Math.PI * POINT_ANGLES[i]);
            final float z = (float) Math.sin(2 * Math.PI * POINT_ANGLES[i]);
            final Color color = POINT_COLORS[i];

            final Model sphere = model(new ModelBuilder().createSphere(POINT_DIAMETER, POINT_DIAMETER, POINT_DIAMETER,
                16, 16, new Material(ColorAttribute.createDiffuse(color)), Usage.Position | Usage.Normal));

            final ModelInstance circlePoint = new ModelInstance(sphere);
            circlePoint.transform.setToTranslation(x, 0f, z);
            circlePoints.add(circlePoint);

            for (int sheet = 0; sheet < Tubes.HELIX_SHEETS; sheet++) {
                final ModelInstance lift = new ModelInstance(sphere);
                lift.transform.setToTranslation(x, Tubes.helixHeightOver(POINT_ANGLES[i], sheet), z);
                lifts.add(lift);
            }
        }
    }

    @Override
    public String getWorldTitle() {
        return "S1 with covering space";
    }

    @Override
    public void reposition(float deltaTime) {
        // nothing moves in this world
    }

    @Override
    public void render(ModelBatch modelBatch, Environment environment) {
        unitCircle.render(modelBatch, environment);
        helix.render(modelBatch, environment);
        for (ModelInstance instance : circlePoints) {
            modelBatch.render(instance, environment);
        }
        for (ModelInstance lift : lifts) {
            modelBatch.render(lift, environment);
        }
    }

    @Override
    public void dispose() {
        helix.dispose();
        unitCircle.dispose();
        models.forEach(Model::dispose);
    }

    /** Takes ownership of the model to dispose of it later. */
    private Model model(Model model) {
        models.add(model);
        return model;
    }
}
