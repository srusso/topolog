package net.sr89.topology.shapes;

import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Matrix4;

/**
 * A tube following a curve, drawn as a single mesh. The mesh itself is static; animation is done
 * by changing the transform of the instance.
 */
public class Tube {
    /** Decides where the (static) tube mesh is placed in the world at a given moment. */
    @FunctionalInterface
    public interface Placement {
        /**
         * @param transform The transform to overwrite
         * @param turns How far the animation has progressed, in full turns. Placements must look the same
         *              for turns and turns + 1, since the value wraps around.
         */
        void place(Matrix4 transform, float turns);
    }

    private static final float INITIAL_TURNS = (float) (Math.PI / 3);
    private static final float TURNS_PER_SECOND = 0.1f;

    private final Model model;
    private final ModelInstance instance;
    private final Placement placement;
    private float turns = INITIAL_TURNS;

    /** @param placement how to place the mesh over time, or null if the tube never moves */
    public Tube(Model model, Placement placement) {
        this.model = model;
        this.instance = new ModelInstance(model);
        this.placement = placement;
        if (placement != null) {
            placement.place(instance.transform, turns);
        }
    }

    public void render(ModelBatch modelBatch, Environment environment) {
        modelBatch.render(instance, environment);
    }

    public void reposition(float deltaTime) {
        if (placement != null) {
            turns = (turns + deltaTime * TURNS_PER_SECOND) % 1f;
            placement.place(instance.transform, turns);
        }
    }

    public void dispose() {
        model.dispose();
    }
}
