package net.sr89.topology.worlds;

import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;

/**
 * <a href="https://en.wikipedia.org/wiki/Homotopy_groups_of_spheres#">Wikipedia</a>π1(S2)_=_0
 */
public class S2FundamentalGroup implements World {
    @Override
    public String getWorldTitle() {
        return "S2 with fundamental group";
    }

    @Override
    public void reposition(float deltaTime) {

    }

    @Override
    public void render(ModelBatch modelBatch, Environment environment) {

    }

    @Override
    public void dispose() {

    }
}
