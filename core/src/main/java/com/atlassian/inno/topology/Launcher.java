package com.atlassian.inno.topology;

import com.badlogic.gdx.ApplicationAdapter;

/**
 * {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms.
 */
public class Launcher extends ApplicationAdapter {

    ApplicationAdapter adapter = new LineModelLauncher();

    @Override
    public void create() {
        adapter.create();
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
