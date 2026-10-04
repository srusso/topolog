package net.sr89.topology.shapes;

import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;

/** A tube following a curve, drawn as a single mesh. */
public class Tube {
    private final Model model;
    private final ModelInstance instance;

    public Tube(Model model) {
        this.model = model;
        this.instance = new ModelInstance(model);
    }

    public void render(ModelBatch modelBatch, Environment environment) {
        modelBatch.render(instance, environment);
    }

    public void dispose() {
        model.dispose();
    }
}
