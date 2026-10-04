package net.sr89.topology.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.MathUtils;

/**
 * Turns the keyboard and mouse state into camera movement. Everything is polled each frame, so
 * there's no key state to get out of sync (e.g. with two opposite keys held, or a missed key release).
 */
public class CameraMovementService {
    private static final float DEGREES_PER_PIXEL = 0.2f;
    private static final float MOVEMENT_SPEED = 20f;

    /**
     * Mouse movement bigger than this (in pixels) within a single frame is considered a glitch rather than
     * a real movement. The cursor position can be re-based, e.g. at startup or when the window is activated,
     * which shows up as one huge bogus delta.
     */
    private static final int MAX_PLAUSIBLE_MOUSE_DELTA = 150;

    private boolean focused = true;
    private boolean cursorCaptured = false;
    private boolean ignoreNextMouseMove = true; // the first reported delta is measured from (0, 0)
    private int yawPixels = 0;
    private int pitchPixels = 0;

    /** Call when the window gets focus. */
    public void focusGained() {
        focused = true;
        // the cursor may be recentered as the window gets activated, which can look like a big movement
        ignoreNextMouseMove = true;
    }

    /** Call when the window loses focus. */
    public void focusLost() {
        focused = false;
    }

    public float forwardMovementDelta(float deltaTime) {
        return movementDelta(axis(Keys.W, Keys.S), deltaTime);
    }

    public float leftRightMovementDelta(float deltaTime) {
        return movementDelta(axis(Keys.D, Keys.A) + axis(Keys.RIGHT, Keys.LEFT), deltaTime);
    }

    public float upDownMovementDelta(float deltaTime) {
        return movementDelta(axis(Keys.UP, Keys.DOWN), deltaTime);
    }

    /** Reads the mouse movement for this frame. Call once per frame, before reading the rotations. */
    public void update() {
        // The cursor is captured while we have focus, so the mouse can turn the camera indefinitely
        // without hitting the screen edge, and released otherwise.
        if (focused != cursorCaptured) {
            Gdx.input.setCursorCatched(focused);
            cursorCaptured = focused;
        }

        final int dx = Gdx.input.getDeltaX();
        final int dy = Gdx.input.getDeltaY();
        yawPixels = 0;
        pitchPixels = 0;
        if (!focused || (dx == 0 && dy == 0)) {
            return;
        }
        if (ignoreNextMouseMove) {
            ignoreNextMouseMove = false;
            return;
        }
        if (Math.abs(dx) > MAX_PLAUSIBLE_MOUSE_DELTA || Math.abs(dy) > MAX_PLAUSIBLE_MOUSE_DELTA) {
            return;
        }
        yawPixels = dx;
        pitchPixels = dy;
    }

    /** Degrees to turn left/right. Mouse deltas are already per-frame distances, so no deltaTime scaling. */
    public float horizontalRotation() {
        return -yawPixels * DEGREES_PER_PIXEL;
    }

    /** Degrees to turn up/down. */
    public float verticalRotation() {
        return -pitchPixels * DEGREES_PER_PIXEL;
    }

    /** 1 if only the positive key is held, -1 if only the negative key is held, otherwise 0. */
    private static float axis(int positiveKey, int negativeKey) {
        return (Gdx.input.isKeyPressed(positiveKey) ? 1 : 0) - (Gdx.input.isKeyPressed(negativeKey) ? 1 : 0);
    }

    private static float movementDelta(float direction, float deltaTime) {
        return MathUtils.clamp(direction, -1f, 1f) * MOVEMENT_SPEED * deltaTime;
    }
}
