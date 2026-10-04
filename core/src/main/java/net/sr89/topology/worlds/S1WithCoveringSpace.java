package net.sr89.topology.worlds;

import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import net.sr89.topology.shapes.Tube;
import net.sr89.topology.shapes.Tubes;

public class S1WithCoveringSpace implements World {
    private final Tube helix;
    private final Tube unitCircle;

    public S1WithCoveringSpace() {
        helix = Tubes.helix();
        unitCircle = Tubes.unitCircle();
    }

    @Override
    public String getWorldTitle() {
        return "S1 with covering space";
    }

    @Override
    public void reposition(float deltaTime) {
        helix.reposition(deltaTime);
        unitCircle.reposition(deltaTime);
    }

    @Override
    public void render(ModelBatch modelBatch, Environment environment) {
        unitCircle.render(modelBatch, environment);
        helix.render(modelBatch, environment);
    }

    @Override
    public void dispose() {
        helix.dispose();
        unitCircle.dispose();
    }
}
