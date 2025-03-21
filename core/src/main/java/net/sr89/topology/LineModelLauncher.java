package net.sr89.topology;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.math.Vector3;
import net.sr89.topology.input.CameraMovementService;
import net.sr89.topology.shapes.CylinderHelix;
import net.sr89.topology.shapes.UnitSphere;

import java.util.Arrays;
import java.util.List;

import static net.sr89.topology.shapes.BasicShapes.*;
import static net.sr89.topology.shapes.GridShape.createAxes;

public class LineModelLauncher extends ApplicationAdapter {

    private PerspectiveCamera camera;
    private ModelBatch modelBatch;
    private CylinderHelix cylinderHelix;
    private UnitSphere unitSphere;
    private Environment environment;
    private Model axesModel;
    private Model cylinderModel;
    private ModelInstance axes;

    private final CameraMovementService cameraMovementService;

    private final Color BACKGROUND_COLOR = HexColors.VERY_DARK_BLUE;

    public LineModelLauncher(CameraMovementService cameraMovementService) {
        this.cameraMovementService = cameraMovementService;
    }

    @Override
    public void create() {
        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(0f, 7f, 7f);
        camera.lookAt(0, 0, 0);
        camera.near = 1f;
        camera.far = 300f;
        camera.update();

        cylinderModel = cylinderModel();
        axesModel = createAxes();
        axes = new ModelInstance(axesModel);

        modelBatch = new ModelBatch();

        cylinderHelix = createCylinderHelix(cylinderModel);
        unitSphere = createUnitSphere(cylinderModel);

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(1f, 1f, 1f, -1f, -0.8f, -0.2f));
    }

    @Override
    public void render() {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glClearColor(BACKGROUND_COLOR.r, BACKGROUND_COLOR.g, BACKGROUND_COLOR.b, 1f);

        final float deltaTime = Gdx.graphics.getDeltaTime();
        updateCameraPosition(deltaTime);
        updateCameraRotation(deltaTime);
        cameraMovementService.resetRotations();
        camera.update();

        // Rotate the shapes
        cylinderHelix.reposition(deltaTime);
        unitSphere.reposition(deltaTime);

        // Render the models
        modelBatch.begin(camera);
        modelBatch.render(axes, environment);
        unitSphere.render(modelBatch, environment);
        cylinderHelix.render(modelBatch, environment);

        modelBatch.end();
    }

    private void updateCameraRotation(float deltaTime) {
        Vector3 cameraHorizontalAxis = getCameraHorizontalAxis().rotate(camera.direction, 180);
        Vector3 up = new Vector3(Vector3.Y);
        camera.rotate(cameraHorizontalAxis, cameraMovementService.verticalRotation(deltaTime));
        camera.rotate(up, cameraMovementService.horizontalRotation(deltaTime));
    }

    private void updateCameraPosition(float deltaTime) {
        Vector3 cameraHorizontalAxis = getCameraHorizontalAxis().rotate(camera.direction, 180);
        Vector3 cameraDirection = new Vector3(camera.direction);
        camera.position.add(cameraDirection.scl(cameraMovementService.forwardMovementDelta(deltaTime)));
        camera.position.add(cameraHorizontalAxis.scl(cameraMovementService.leftRightMovementDelta(deltaTime)));
        camera.position.add(new Vector3(Vector3.Y).scl(cameraMovementService.upDownMovementDelta(deltaTime)));
    }

    private Vector3 getCameraHorizontalAxis() {
        return new Vector3(camera.up).crs(camera.direction);
    }

    @Override
    public void dispose() {
        modelBatch.dispose();
        axesModel.dispose();
        cylinderModel.dispose();
    }
}
