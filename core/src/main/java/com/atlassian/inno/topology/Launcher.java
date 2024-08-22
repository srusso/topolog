package com.atlassian.inno.topology;

import com.atlassian.inno.topology.input.ControlInputProcessor;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;

/**
 * {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms.
 */
public class Launcher extends ApplicationAdapter {

    ApplicationAdapter adapter = new LineModelLauncher();

    @Override
    public void create() {
        adapter.create();
        Gdx.input.setInputProcessor(new ControlInputProcessor());
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
