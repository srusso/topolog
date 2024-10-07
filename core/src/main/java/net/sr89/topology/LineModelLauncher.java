package net.sr89.topology;

import net.sr89.topology.input.CameraMovementService;
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

import java.util.Arrays;
import java.util.List;

import static net.sr89.topology.shapes.BasicShapes.createHelix;
import static net.sr89.topology.shapes.BasicShapes.createUnitCircle;
import static net.sr89.topology.shapes.GridShape.createAxes;

public class LineModelLauncher extends ApplicationAdapter {

    private PerspectiveCamera camera;
    private ModelBatch modelBatch;
    private List<Model> models;
    private List<ModelInstance> objectsToRender;
    private Environment environment;
    private float angle;

    private final CameraMovementService cameraMovementService;

    private final Color BACKGROUND_COLOR = HexColors.PINK_PASTEL;

    public LineModelLauncher(CameraMovementService cameraMovementService) {
        this.cameraMovementService = cameraMovementService;
    }

    @Override
    public void create() {
        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(0f, 3f, 3f);
        camera.lookAt(0, 0, 0);
        camera.near = 1f;
        camera.far = 300f;
        camera.update();

        modelBatch = new ModelBatch();

        models = Arrays.asList(
            createAxes(),
            createHelix(),
            createUnitCircle()
        );
        objectsToRender = models.stream().map(ModelInstance::new).toList();

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(1f, 1f, 1f, -1f, -0.8f, -0.2f));

        angle = 0f;
    }

    @Override
    public void render() {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glClearColor(BACKGROUND_COLOR.r, BACKGROUND_COLOR.g, BACKGROUND_COLOR.b, 1f);

        final float deltaTime = Gdx.graphics.getDeltaTime();

        camera.position.add(cameraMovementService.cameraMovement(deltaTime));
        camera.update();

        // Rotate the shapes
        angle += deltaTime * 20f;
        objectsToRender.forEach(o -> o.transform.setToRotation(Vector3.Y, angle));

        // Render the models
        modelBatch.begin(camera);
        objectsToRender.forEach(o -> modelBatch.render(o, environment));
        modelBatch.end();
    }

    @Override
    public void dispose() {
        modelBatch.dispose();
        models.forEach(Model::dispose);
    }
}
