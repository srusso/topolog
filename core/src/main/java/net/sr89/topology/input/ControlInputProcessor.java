package net.sr89.topology.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;

import java.util.function.IntConsumer;

/** Handles discrete key presses (quit, select world). Continuous camera movement is polled by {@link CameraMovementService}. */
public class ControlInputProcessor extends InputAdapter {
    private final IntConsumer worldSelector;

    /** @param worldSelector called with 1-9 when the matching number key is released */
    public ControlInputProcessor(IntConsumer worldSelector) {
        this.worldSelector = worldSelector;
    }

    @Override
    public boolean keyUp(int keycode) {
        if (keycode == Input.Keys.Q || keycode == Input.Keys.ESCAPE) {
            Gdx.app.exit();
            return true;
        }

        if (keycode >= Input.Keys.NUM_1 && keycode <= Input.Keys.NUM_9) {
            worldSelector.accept(keycode - Input.Keys.NUM_0);
            return true;
        }

        return false;
    }
}
