package net.sr89.topology;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import net.sr89.topology.input.CameraMovementService;
import net.sr89.topology.input.ControlInputProcessor;
import net.sr89.topology.worlds.S1WithCoveringSpace;
import net.sr89.topology.worlds.TorusWithFundamentalGroup;
import net.sr89.topology.worlds.World;

import java.util.List;

import static net.sr89.topology.shapes.GridShape.createAxes;

public class TopologyApp extends ApplicationAdapter {

    private Stage stage;
    private PerspectiveCamera camera;
    private ModelBatch modelBatch;
    private List<World> worlds;
    private World currentWorld;
    private Environment environment;
    private BitmapFont titleFont;
    private Label titleLabel;
    private Model axesModel;
    private ModelInstance axes;

    private static final float MAX_PITCH = 89f;

    private static final float TITLE_PADDING = 24f;

    private final CameraMovementService cameraMovementService = new CameraMovementService();
    private final Vector3 scratch = new Vector3();

    private final Color backgroundColor = HexColors.veryDarkBlue();

    @Override
    public void create() {
        stage = new Stage(new ScreenViewport());

        titleFont = new BitmapFont(Gdx.files.internal("bitmapfont/Amble-Regular-26.fnt"));
        titleLabel = new Label("", new LabelStyle(titleFont, Color.RED));
        final Table root = new Table();
        root.setFillParent(true);
        root.top().left().pad(TITLE_PADDING);
        root.add(titleLabel);
        stage.addActor(root);

        worlds = List.of(new S1WithCoveringSpace(), new TorusWithFundamentalGroup());
        selectWorld(1);

        Gdx.input.setInputProcessor(new ControlInputProcessor(this::selectWorld));

        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(0f, 7f, 7f);
        camera.lookAt(0, 0, 0);
        camera.near = 0.1f;
        camera.far = 300f;
        camera.update();

        axesModel = createAxes();
        axes = new ModelInstance(axesModel);

        modelBatch = new ModelBatch();

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(1f, 1f, 1f, -1f, -0.8f, -0.2f));
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(backgroundColor.r, backgroundColor.g, backgroundColor.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        final float deltaTime = Gdx.graphics.getDeltaTime();
        cameraMovementService.update();
        updateCameraPosition(deltaTime);
        updateCameraRotation();
        camera.update();

        currentWorld.reposition(deltaTime);

        modelBatch.begin(camera);
        modelBatch.render(axes, environment);
        currentWorld.render(modelBatch, environment);

        modelBatch.end();

        stage.act();
        stage.draw();
    }

    /** Call when the window gets focus. */
    public void onFocusGained() {
        cameraMovementService.focusGained();
    }

    /** Call when the window loses focus. */
    public void onFocusLost() {
        cameraMovementService.focusLost();
    }

    @Override
    public void resize(int width, int height) {
        if (width <= 0 || height <= 0 || stage == null) {
            return;
        }
        stage.getViewport().update(width, height, true);
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
    }

    /** @param selection The 1-based number of the world to show. Numbers without a world are ignored. */
    public void selectWorld(int selection) {
        if (selection < 1 || selection > worlds.size()) {
            return;
        }
        currentWorld = worlds.get(selection - 1);
        titleLabel.setText(currentWorld.getWorldTitle());
    }

    private void updateCameraRotation() {
        final float yaw = cameraMovementService.horizontalRotation();
        float pitch = cameraMovementService.verticalRotation();

        // don't let the camera flip over the poles
        final float currentPitch = MathUtils.asin(camera.direction.y) * MathUtils.radiansToDegrees;
        pitch = MathUtils.clamp(currentPitch + pitch, -MAX_PITCH, MAX_PITCH) - currentPitch;

        camera.rotate(cameraRight(), pitch);
        camera.rotate(Vector3.Y, yaw);
    }

    private void updateCameraPosition(float deltaTime) {
        camera.position.mulAdd(camera.direction, cameraMovementService.forwardMovementDelta(deltaTime));
        camera.position.mulAdd(cameraRight(), cameraMovementService.leftRightMovementDelta(deltaTime));
        camera.position.mulAdd(Vector3.Y, cameraMovementService.upDownMovementDelta(deltaTime));
    }

    /** Unit vector pointing to the camera's right. Reuses a scratch vector, so don't hold on to it. */
    private Vector3 cameraRight() {
        return scratch.set(camera.direction).crs(camera.up).nor();
    }

    @Override
    public void dispose() {
        modelBatch.dispose();
        axesModel.dispose();
        stage.dispose();
        titleFont.dispose();
        worlds.forEach(World::dispose);
    }
}
