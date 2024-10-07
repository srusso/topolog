package net.sr89.topology;

import net.sr89.topology.input.CameraMovementService;
import net.sr89.topology.input.ControlInputProcessor;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;

/**
 * {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms.
 */
public class Launcher extends ApplicationAdapter {

    private static final CameraMovementService CAMERA_MOVEMENT_SERVICE = new CameraMovementService();
    ApplicationAdapter adapter = new LineModelLauncher(CAMERA_MOVEMENT_SERVICE);

    @Override
    public void create() {
        adapter.create();
        Gdx.input.setInputProcessor(new ControlInputProcessor(CAMERA_MOVEMENT_SERVICE));
    }

    @Override
    public void render() {
        adapter.render();
    }

    @Override
    public void dispose() {
        adapter.dispose();
    }
}
