package net.sr89.topology.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import net.sr89.topology.LineModelLauncher;

import java.util.HashMap;
import java.util.Map;

public class ControlInputProcessor implements InputProcessor {

    private record MovementAction(Runnable onKeyDown, Runnable onKeyUp) {

    }

    private final CameraMovementService cameraMovementService;
    private final LineModelLauncher launcher;
    private final Map<Integer, MovementAction> movementActions;

    public ControlInputProcessor(LineModelLauncher launcher, CameraMovementService cameraMovementService) {
        this.launcher = launcher;
        this.movementActions = initMovementActions(cameraMovementService);
        this.cameraMovementService = cameraMovementService;
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

        if (i >= Input.Keys.NUM_1 || i <= Input.Keys.NUM_9) {
            launcher.selectWorld(i - Input.Keys.NUM_0);
        }

        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        cameraMovementService.mouseMoved(screenX, screenY);
        return true;
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
    public boolean scrolled(float v, float v1) {
        return false;
    }
}
