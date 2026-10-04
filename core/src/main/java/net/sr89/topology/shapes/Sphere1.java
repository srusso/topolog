package net.sr89.topology.shapes;

import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Matrix4;
import net.sr89.topology.spaces.Sphere;

/**
 * Physical representation of S1 (a circle), or its covering space, drawn as one tube mesh.
 */
public abstract class Sphere1 implements Sphere {
    private final Model model;
    private final ModelInstance instance;
    private float rads = (float) (Math.PI / 3);

    protected Sphere1(Model model) {
        this.model = model;
        this.instance = new ModelInstance(model);
        updateTransform(instance.transform, rads);
    }

    /**
     * Sets how the (static) tube mesh is placed in the world.
     *
     * @param transform The transform to overwrite
     * @param rads The current rotation of this sphere around the Y axis, in turns of the helix
     */
    protected abstract void updateTransform(Matrix4 transform, float rads);

    public void render(ModelBatch modelBatch, Environment environment) {
        modelBatch.render(instance, environment);
    }

    public void reposition(float deltaTime) {
        rads += deltaTime * 0.1f;
        updateTransform(instance.transform, rads);
    }

    public void dispose() {
        model.dispose();
    }
}
