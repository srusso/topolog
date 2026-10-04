package net.sr89.topology.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;

import java.util.function.IntConsumer;

/**
 * Handles discrete key presses: quit, and choosing the world.
 * <p>
 * The number keys 1 to 9 choose the first nine worlds, and 0 the tenth. Tab, or Page Down, goes to the next world, and
 * Shift+Tab, or Page Up, to the previous one, so there is no limit to how many worlds there can be. E shows or hides the
 * explanation of the world.
 * Continuous camera movement is polled by {@link CameraMovementService}.
 */
public class ControlInputProcessor extends InputAdapter {
    private final IntConsumer worldSelector;
    private final IntConsumer worldStepper;
    private final Runnable explanationToggler;

    /**
     * @param worldSelector called with the number of a world (starting from 1) when the key for it is released
     * @param worldStepper  called with 1 to go to the next world, and with -1 to go to the previous one
     * @param explanationToggler called when the key for showing or hiding the explanation of the world is released
     */
    public ControlInputProcessor(IntConsumer worldSelector, IntConsumer worldStepper, Runnable explanationToggler) {
        this.worldSelector = worldSelector;
        this.worldStepper = worldStepper;
        this.explanationToggler = explanationToggler;
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
        if (keycode == Input.Keys.NUM_0) {
            worldSelector.accept(10);
            return true;
        }

        if (keycode == Input.Keys.E) {
            explanationToggler.run();
            return true;
        }

        if (keycode == Input.Keys.PAGE_DOWN) {
            worldStepper.accept(1);
            return true;
        }
        if (keycode == Input.Keys.PAGE_UP) {
            worldStepper.accept(-1);
            return true;
        }
        if (keycode == Input.Keys.TAB) {
            final boolean backwards = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
            worldStepper.accept(backwards ? -1 : 1);
            return true;
        }

        return false;
    }
}
