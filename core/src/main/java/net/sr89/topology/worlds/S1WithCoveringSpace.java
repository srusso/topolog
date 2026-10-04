package net.sr89.topology.worlds;

import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import net.sr89.topology.shapes.CylinderHelix;
import net.sr89.topology.shapes.UnitCircle;

import static net.sr89.topology.shapes.BasicShapes.*;

public class S1WithCoveringSpace implements World {
    private final CylinderHelix cylinderHelix;
    private final UnitCircle unitCircle;

    public S1WithCoveringSpace() {
        cylinderHelix = createCylinderHelix();
        unitCircle = createUnitSphere();
    }

    @Override
    public String getWorldTitle() {
        return "S1 with covering space";
    }

    @Override
    public void reposition(float deltaTime) {
        cylinderHelix.reposition(deltaTime);
        unitCircle.reposition(deltaTime);
    }

    @Override
    public void render(ModelBatch modelBatch, Environment environment) {
        unitCircle.render(modelBatch, environment);
        cylinderHelix.render(modelBatch, environment);
    }

    @Override
    public void dispose() {
        cylinderHelix.dispose();
        unitCircle.dispose();
    }
}
