package net.sr89.topology.worlds;

import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;

public interface World {
    String getWorldTitle();
    void reposition(float deltaTime);
    void render(ModelBatch modelBatch, Environment environment);
    void dispose();
}
