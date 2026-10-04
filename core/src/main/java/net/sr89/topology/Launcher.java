package net.sr89.topology;

import net.sr89.topology.input.CameraMovementService;
import net.sr89.topology.input.ControlInputProcessor;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;

/**
 * {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms.
 */
public class Launcher extends ApplicationAdapter {

    private final TopologyApp adapter = new TopologyApp(new CameraMovementService());

    @Override
    public void create() {
        adapter.create();
        Gdx.input.setInputProcessor(new ControlInputProcessor(adapter::selectWorld));
    }

    @Override
    public void resize(int width, int height) {
        adapter.resize(width, height);
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
