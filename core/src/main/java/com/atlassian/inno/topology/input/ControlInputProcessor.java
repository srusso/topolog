package com.atlassian.inno.topology.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;

import java.util.HashMap;
import java.util.Map;

public class ControlInputProcessor implements InputProcessor {

    private record MovementAction(Runnable onKeyDown, Runnable onKeyUp) {

    }

    private final Map<Integer, MovementAction> movementActions;

    public ControlInputProcessor(CameraMovementService cameraMovementService) {
        this.movementActions = initMovementActions(cameraMovementService);
    }

    private Map<Integer, MovementAction> initMovementActions(CameraMovementService cameraMovementService) {
        Map<Integer, MovementAction> actions = new HashMap<>();

        actions.put(Input.Keys.UP, new MovementAction(cameraMovementService::beginMovementUp, cameraMovementService::stopMovementUpDown));
        actions.put(Input.Keys.DOWN, new MovementAction(cameraMovementService::beginMovementDown, cameraMovementService::stopMovementUpDown));
        actions.put(Input.Keys.LEFT, new MovementAction(cameraMovementService::beginMovementLeft, cameraMovementService::stopMovementLeftRight));
        actions.put(Input.Keys.RIGHT, new MovementAction(cameraMovementService::beginMovementRight, cameraMovementService::stopMovementLeftRight));
        actions.put(Input.Keys.W, new MovementAction(cameraMovementService::beginMovementForward, cameraMovementService::stopMovementForwardBackward));
        actions.put(Input.Keys.S, new MovementAction(cameraMovementService::beginMovementBackward, cameraMovementService::stopMovementForwardBackward));
        actions.put(Input.Keys.A, actions.get(Input.Keys.LEFT));
        actions.put(Input.Keys.D, actions.get(Input.Keys.RIGHT));

        return actions;
    }

    @Override
    public boolean keyDown(int i) {
        MovementAction action = movementActions.get(i);
        if (action != null) {
            action.onKeyDown.run();
            return true;
        }
        return false;
    }

    @Override
    public boolean keyUp(int i) {
        if (i == Input.Keys.Q || i == Input.Keys.ESCAPE) {
            Gdx.app.exit();
            return true;
        }

        MovementAction action = movementActions.get(i);
        if (action != null) {
            action.onKeyUp.run();
            return true;
        }

        return false;
    }

    @Override
    public boolean keyTyped(char c) {
        return false;
    }

    @Override
    public boolean touchDown(int i, int i1, int i2, int i3) {
        return false;
    }

    @Override
    public boolean touchUp(int i, int i1, int i2, int i3) {
        return false;
    }

    @Override
    public boolean touchCancelled(int i, int i1, int i2, int i3) {
        return false;
    }

    @Override
    public boolean touchDragged(int i, int i1, int i2) {
        return false;
    }

    @Override
    public boolean mouseMoved(int i, int i1) {
        return false;
    }

    @Override
    public boolean scrolled(float v, float v1) {
        return false;
    }
}
