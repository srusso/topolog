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
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import net.sr89.topology.input.CameraMovementService;
import net.sr89.topology.worlds.S1WithCoveringSpace;
import net.sr89.topology.worlds.World;

import static net.sr89.topology.shapes.GridShape.createAxes;

public class LineModelLauncher extends ApplicationAdapter {

    private Stage stage;
    private PerspectiveCamera camera;
    private ModelBatch modelBatch;
    private World s1WithCoveringSpace;
    private Environment environment;
    private Model axesModel;
    private ModelInstance axes;

    private final CameraMovementService cameraMovementService;

    private final Color BACKGROUND_COLOR = HexColors.VERY_DARK_BLUE;

    public LineModelLauncher(CameraMovementService cameraMovementService) {
        this.cameraMovementService = cameraMovementService;
    }

    @Override
    public void create() {
        stage = new Stage(new ScreenViewport());

        int row_height = Gdx.graphics.getWidth() / 12;
        int col_width = Gdx.graphics.getWidth() / 12;
        stage.addActor(getTitleLabel(row_height, col_width));

        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(0f, 7f, 7f);
        camera.lookAt(0, 0, 0);
        camera.near = 1f;
        camera.far = 300f;
        camera.update();

        axesModel = createAxes();
        axes = new ModelInstance(axesModel);

        modelBatch = new ModelBatch();

        s1WithCoveringSpace = new S1WithCoveringSpace();

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(1f, 1f, 1f, -1f, -0.8f, -0.2f));
    }

    private static Label getTitleLabel(int row_height, int col_width) {
        Label title = new Label("S1 with covering space", getTitleStyle());
        title.setSize(col_width, row_height);
        title.setPosition(
            col_width / 2F,
            Gdx.graphics.getHeight() - (row_height)
        );
        title.setAlignment(Align.left);
        return title;
    }

    private static LabelStyle getTitleStyle() {
        LabelStyle labelStyle = new LabelStyle();
        labelStyle.font = new BitmapFont(Gdx.files.internal("bitmapfont/Amble-Regular-26.fnt"));
        labelStyle.fontColor = Color.RED;
        return labelStyle;
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

        s1WithCoveringSpace.reposition(deltaTime);

        modelBatch.begin(camera);
        modelBatch.render(axes, environment);
        s1WithCoveringSpace.render(modelBatch, environment);

        modelBatch.end();

        stage.act();
        stage.draw();
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
        stage.dispose();
        s1WithCoveringSpace.dispose();
    }
}
