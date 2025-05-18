package net.sr89.topology.worlds;

import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import net.sr89.topology.shapes.CylinderHelix;
import net.sr89.topology.shapes.UnitSphere;

import static net.sr89.topology.shapes.BasicShapes.*;

public class S1WithCoveringSpace implements World {
    private final Model cylinderModel;
    private final CylinderHelix cylinderHelix;
    private final UnitSphere unitSphere;

    public S1WithCoveringSpace() {
        cylinderModel = cylinderModel();
        cylinderHelix = createCylinderHelix(cylinderModel);
        unitSphere = createUnitSphere(cylinderModel);
    }

    @Override
    public String getWorldTitle() {
        return "S1 with covering space";
    }

    @Override
    public void reposition(float deltaTime) {
        cylinderHelix.reposition(deltaTime);
        unitSphere.reposition(deltaTime);
    }

    @Override
    public void render(ModelBatch modelBatch, Environment environment) {
        unitSphere.render(modelBatch, environment);
        cylinderHelix.render(modelBatch, environment);
    }

    @Override
    public void dispose() {
        cylinderModel.dispose();
    }
}
