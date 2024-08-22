package com.atlassian.inno.topology;

import com.atlassian.inno.topology.input.CameraMovementService;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;

import static com.atlassian.inno.topology.shapes.BasicShapes.createLine;
import static com.atlassian.inno.topology.shapes.GridShape.createAxes;

public class LineModelLauncher extends ApplicationAdapter {

    private PerspectiveCamera camera;
    private ModelBatch modelBatch;
    private Model gridModel;
    private Model lineModel;
    private ModelInstance grid;
    private ModelInstance line;
    private Environment environment;
    private float angle;

    private final CameraMovementService cameraMovementService;

    public LineModelLauncher(CameraMovementService cameraMovementService) {
        this.cameraMovementService = cameraMovementService;
    }

    @Override
    public void create() {
        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(3f, 3f, 3f);
        camera.lookAt(0, 0, 0);
        camera.near = 1f;
        camera.far = 300f;
        camera.update();

        modelBatch = new ModelBatch();

        gridModel = createAxes();
        lineModel = createLine();

        grid = new ModelInstance(gridModel);
        line = new ModelInstance(lineModel);

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(1f, 1f, 1f, -1f, -0.8f, -0.2f));

        angle = 0f;
    }

    @Override
    public void render() {
        // Clear the screen
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        // Rotate the shapes
        angle += Gdx.graphics.getDeltaTime() * 20f;
        grid.transform.setToRotation(Vector3.Y, angle);
        line.transform.setToRotation(Vector3.Y, angle);

        // Render the models
        modelBatch.begin(camera);
        modelBatch.render(grid, environment);
        modelBatch.render(line, environment);
        modelBatch.end();
    }

    @Override
    public void dispose() {
        modelBatch.dispose();
        gridModel.dispose();
        lineModel.dispose();
    }
}
