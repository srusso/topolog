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
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import net.sr89.topology.input.CameraMovementService;
import net.sr89.topology.worlds.S1WithCoveringSpace;
import net.sr89.topology.worlds.TorusWithFundamentalGroup;
import net.sr89.topology.worlds.World;

import static net.sr89.topology.shapes.GridShape.createAxes;

public class TopologyApp extends ApplicationAdapter {

    private Stage stage;
    private PerspectiveCamera camera;
    private ModelBatch modelBatch;
    private World s1WithCoveringSpace;
    private World torusWithFundamentalGroup;
    private World currentWorld;
    private Environment environment;
    private BitmapFont titleFont;
    private LabelStyle titleStyle;
    private Model axesModel;
    private ModelInstance axes;

    private static final float MAX_PITCH = 89f;

    private final CameraMovementService cameraMovementService;
    private final Vector3 scratch = new Vector3();

    private final Color BACKGROUND_COLOR = HexColors.VERY_DARK_BLUE;

    public TopologyApp(CameraMovementService cameraMovementService) {
        this.cameraMovementService = cameraMovementService;
    }

    @Override
    public void create() {
        cameraMovementService.captureMouse();

        stage = new Stage(new ScreenViewport());

        titleFont = new BitmapFont(Gdx.files.internal("bitmapfont/Amble-Regular-26.fnt"));
        titleStyle = new LabelStyle();
        titleStyle.font = titleFont;
        titleStyle.fontColor = Color.RED;

        s1WithCoveringSpace = new S1WithCoveringSpace();
        torusWithFundamentalGroup = new TorusWithFundamentalGroup();

        selectWorld(1);

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
        Gdx.gl.glClearColor(BACKGROUND_COLOR.r, BACKGROUND_COLOR.g, BACKGROUND_COLOR.b, 1f);
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

    public void resize(int width, int height) {
        if (width <= 0 || height <= 0 || stage == null) {
            return;
        }
        stage.getViewport().update(width, height, true);
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
        // the title position depends on the window size
        stage.clear();
        stage.addActor(getTitleLabel());
    }

    public void selectWorld(int selection) {
        stage.clear();
        switch (selection) {
            case 1:
                currentWorld = s1WithCoveringSpace;
                break;
            case 2:
                currentWorld = torusWithFundamentalGroup;
                break;
        }
        stage.addActor(getTitleLabel());
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
        s1WithCoveringSpace.dispose();
        torusWithFundamentalGroup.dispose();
    }

    private Label getTitleLabel() {
        final int row_height = Gdx.graphics.getWidth() / 12;
        final int col_width = Gdx.graphics.getWidth() / 12;

        Label title = new Label(currentWorld.getWorldTitle(), titleStyle);
        title.setSize(col_width, row_height);
        title.setPosition(
            col_width / 2F,
            Gdx.graphics.getHeight() - (row_height)
        );
        title.setAlignment(Align.left);
        return title;
    }
}
