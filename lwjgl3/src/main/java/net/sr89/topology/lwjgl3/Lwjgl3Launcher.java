package net.sr89.topology.lwjgl3;

import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowAdapter;
import net.sr89.topology.TopologyApp;

/** Launches the desktop (LWJGL3) application. */
public class Lwjgl3Launcher {
    public static void main(String[] args) {
        if (StartupHelper.startNewJvmIfRequired()) return; // This handles macOS support and helps on Windows.
        createApplication();
    }

    private static Lwjgl3Application createApplication() {
        final TopologyApp app = new TopologyApp();
        return new Lwjgl3Application(app, getDefaultConfiguration(app));
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration(TopologyApp app) {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        // Used to capture the mouse only while the window has focus.
        configuration.setWindowListener(new Lwjgl3WindowAdapter() {
            @Override
            public void focusGained() {
                app.onFocusGained();
            }

            @Override
            public void focusLost() {
                app.onFocusLost();
            }
        });
        configuration.setTitle("topology-visual");
        configuration.useVsync(true);
        //// Limits FPS to the refresh rate of the currently active monitor.
        configuration.setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate);
        //// If you remove the above line and set Vsync to false, you can get unlimited FPS, which can be
        //// useful for testing performance, but can also be very stressful to some hardware.
        //// You may also need to configure GPU drivers to fully disable Vsync; this can cause screen tearing.
        configuration.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode());
        // 24-bit depth buffer and 4x MSAA so the thin tubes don't alias and distant geometry doesn't z-fight
        configuration.setBackBufferConfig(8, 8, 8, 8, 24, 0, 4);
        configuration.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png");
        return configuration;
    }
}
