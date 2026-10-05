package net.sr89.topology;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import net.sr89.topology.input.CameraMovementService;
import net.sr89.topology.input.ControlInputProcessor;
import net.sr89.topology.WorldGallery.Framing;
import net.sr89.topology.worlds.GenusTwoPolygon;
import net.sr89.topology.worlds.HexagonTorus;
import net.sr89.topology.worlds.GenusTwoSurface;
import net.sr89.topology.worlds.KleinBottle;
import net.sr89.topology.worlds.MobiusBand;
import net.sr89.topology.worlds.PathLiftingOnCircle;
import net.sr89.topology.worlds.ProjectivePlane;
import net.sr89.topology.worlds.S1WithCoveringSpace;
import net.sr89.topology.worlds.SquareTorus;
import net.sr89.topology.worlds.TorusWithFundamentalGroup;
import net.sr89.topology.worlds.UniversalCoverOfTorus;
import net.sr89.topology.worlds.VanKampenGenusTwo;
import net.sr89.topology.worlds.WedgeOfCircles;
import net.sr89.topology.worlds.BrouwerFixedPoint;
import net.sr89.topology.worlds.BorsukUlam;
import net.sr89.topology.worlds.TrefoilKnotGroup;
import net.sr89.topology.worlds.TriangulatedTorusHomology;
import net.sr89.topology.worlds.HairyBall;
import net.sr89.topology.worlds.CupProducts;
import net.sr89.topology.worlds.HopfFibration;
import net.sr89.topology.worlds.DegreeOfSphereMaps;
import net.sr89.topology.worlds.DeformationRetractions;
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
    private CrispFont sharpTitleFont;
    private Label titleLabel;
    private Table titleRoot;
    private Cell<Label> titleCell;
    private ExplanationText explanationText;
    private Label explanationLabel;
    private Table explanationPanel;
    private Cell<Table> explanationPanelCell;
    private Cell<Label> explanationLabelCell;
    private Texture panelTexture;
    private Drawable panelBackground;
    private boolean explanationShown = true;
    private WorldGallery gallery;
    private List<WorldGallery.Entry> galleryEntries;
    private Model axesModel;
    private ModelInstance axes;

    private static final float MAX_PITCH = 89f;

    private static final float TITLE_PADDING = 24f;
    /** The height of the letters of the titles, in units of the interface. */
    private static final float TITLE_SIZE = 24f;
    private static final float EXPLANATION_PADDING = 12f;
    private static final float MAX_EXPLANATION_WIDTH = 620f;
    private static final String EXPLANATION_HINT = "[LIGHT_GRAY]Press [WHITE]E[LIGHT_GRAY] to explain this world[]";

    private final CameraMovementService cameraMovementService = new CameraMovementService();
    private final Vector3 scratch = new Vector3();

    private final Color backgroundColor = HexColors.veryDarkBlue();

    @Override
    public void create() {
        stage = new Stage(new ScreenViewport());

        titleFont = createTitleFont();
        titleLabel = new Label("", new LabelStyle(titleFont, Color.RED));
        titleRoot = new Table();
        titleRoot.setFillParent(true);
        titleRoot.top().left().pad(TITLE_PADDING);
        titleLabel.setWrap(true); // a long title goes on a second line instead of being cut off
        titleCell = titleRoot.add(titleLabel).left();

        // the explanation of the world goes under the title, on a dark panel to be easy to read
        explanationText = new ExplanationText();
        final Pixmap white = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        white.setColor(Color.WHITE);
        white.fill();
        panelTexture = new Texture(white);
        white.dispose();
        panelBackground = new TextureRegionDrawable(new TextureRegion(panelTexture)).tint(new Color(0f, 0f, 0f, 0.6f));
        explanationLabel = new Label("", new LabelStyle(explanationText.getFont(), Color.WHITE));
        explanationLabel.setWrap(true);
        explanationLabel.setFontScale(explanationText.getScale());
        explanationPanel = new Table();
        explanationPanel.top().left().pad(EXPLANATION_PADDING);
        explanationLabelCell = explanationPanel.add(explanationLabel).left().top();
        titleRoot.row();
        explanationPanelCell = titleRoot.add(explanationPanel).left().top().padTop(10f);
        stage.addActor(titleRoot);

        // each world, with where to point a thumbnail camera to see all of it
        galleryEntries = List.of(
            new WorldGallery.Entry(new S1WithCoveringSpace(), new Framing(new Vector3(0f, 1.85f, 0f), 2.1f), WorldExplanations.S1_WITH_COVERING_SPACE),
            new WorldGallery.Entry(new TorusWithFundamentalGroup(), new Framing(new Vector3(), 2.8f), WorldExplanations.TORUS),
            new WorldGallery.Entry(new GenusTwoSurface(), new Framing(new Vector3(), 2.4f), WorldExplanations.GENUS_TWO),
            new WorldGallery.Entry(new UniversalCoverOfTorus(), new Framing(new Vector3(0f, 1.5f, 0f), 3.2f), WorldExplanations.UNIVERSAL_COVER_OF_TORUS),
            new WorldGallery.Entry(new PathLiftingOnCircle(), new Framing(new Vector3(0f, 1.85f, 0f), 2.1f), WorldExplanations.PATH_LIFTING),
            new WorldGallery.Entry(new KleinBottle(), new Framing(new Vector3(), 3.0f), WorldExplanations.KLEIN_BOTTLE),
            new WorldGallery.Entry(new ProjectivePlane(), new Framing(new Vector3(), 2.75f), WorldExplanations.PROJECTIVE_PLANE),
            new WorldGallery.Entry(new MobiusBand(), new Framing(new Vector3(), 2.4f), WorldExplanations.MOBIUS_BAND),
            new WorldGallery.Entry(new SquareTorus(), new Framing(new Vector3(), 2.4f), WorldExplanations.SQUARE_TORUS),
            new WorldGallery.Entry(new HexagonTorus(), new Framing(new Vector3(), 3.0f), WorldExplanations.HEXAGON_TORUS),
            new WorldGallery.Entry(new GenusTwoPolygon(), new Framing(new Vector3(), 3.0f), WorldExplanations.GENUS_TWO_POLYGON),
            new WorldGallery.Entry(new WedgeOfCircles(), new Framing(new Vector3(0f, 1.3f, 0f), 3.3f), WorldExplanations.WEDGE_OF_CIRCLES),
            new WorldGallery.Entry(new VanKampenGenusTwo(), new Framing(new Vector3(), 4.2f), WorldExplanations.VAN_KAMPEN),
            new WorldGallery.Entry(new BrouwerFixedPoint(), new Framing(new Vector3(), 2.6f), WorldExplanations.BROUWER_FIXED_POINT),
            new WorldGallery.Entry(new BorsukUlam(), new Framing(new Vector3(0.7f, 0f, 0f), 4.8f), WorldExplanations.BORSUK_ULAM),
            new WorldGallery.Entry(new TrefoilKnotGroup(), new Framing(new Vector3(0f, 1.4f, 0f), 3.8f), WorldExplanations.TREFOIL_KNOT_GROUP),
            new WorldGallery.Entry(new TriangulatedTorusHomology(), new Framing(new Vector3(), 3.1f), WorldExplanations.TRIANGULATED_TORUS_HOMOLOGY),
            new WorldGallery.Entry(new HairyBall(), new Framing(new Vector3(), 2.3f), WorldExplanations.HAIRY_BALL),
            new WorldGallery.Entry(new CupProducts(), new Framing(new Vector3(-0.8f, 0f, 0f), 4.9f), WorldExplanations.CUP_PRODUCTS),
            new WorldGallery.Entry(new HopfFibration(), new Framing(new Vector3(1.4f, 0.3f, 0f), 4.1f), WorldExplanations.HOPF_FIBRATION),
            new WorldGallery.Entry(new DegreeOfSphereMaps(), new Framing(new Vector3(), 4.4f), WorldExplanations.DEGREE_OF_SPHERE_MAPS),
            new WorldGallery.Entry(new DeformationRetractions(), new Framing(new Vector3(), 2.7f), WorldExplanations.DEFORMATION_RETRACTIONS));
        worlds = galleryEntries.stream().map(WorldGallery.Entry::world).toList();
        selectWorld(1);

        Gdx.input.setInputProcessor(new ControlInputProcessor(this::selectWorld, this::stepWorld, this::toggleExplanation));

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

        gallery = new WorldGallery(galleryEntries, environment, modelBatch, titleLabel.getStyle());
        stage.addActor(gallery.getNumbers());
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    /** A font drawn at the size of the pixels of the screen, so the text is sharp. If that doesn't work, a font that is a picture. */
    private BitmapFont createTitleFont() {
        try {
            sharpTitleFont = CrispFont.create(java.awt.Font.SANS_SERIF, TITLE_SIZE, CrispFont.density(), '\uE000', false);
            return sharpTitleFont.getFont();
        } catch (Throwable problem) {
            Gdx.app.error("TopologyApp", "Can't make a sharp font, using a font that is a picture", problem);
            final BitmapFont font = new BitmapFont(Gdx.files.internal("bitmapfont/Amble-Regular-26.fnt"));
            font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            return font;
        }
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

        // all worlds keep moving, so the thumbnails are alive
        worlds.forEach(world -> world.reposition(deltaTime));

        gallery.render(worlds.indexOf(currentWorld));

        // the main view takes the space to the right of the gallery
        HdpiUtils.glViewport((int) gallery.getWidth(), 0, Gdx.graphics.getWidth() - (int) gallery.getWidth(), Gdx.graphics.getHeight());
        modelBatch.begin(camera);
        modelBatch.render(axes, environment);
        currentWorld.render(modelBatch, environment);

        modelBatch.end();

        titleLabel.setText(currentWorld.getWorldTitle()); // some worlds change their title as they go
        stage.getViewport().apply();
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
        gallery.layout(width, height);
        titleRoot.padLeft(gallery.getWidth() + TITLE_PADDING);
        // the title has the space that the gallery doesn't take
        final float available = Math.max(100f, width - gallery.getWidth() - 2 * TITLE_PADDING);
        titleCell.width(available);
        final float panelWidth = Math.min(MAX_EXPLANATION_WIDTH, available);
        explanationPanelCell.width(panelWidth);
        explanationLabelCell.width(panelWidth - 2 * EXPLANATION_PADDING);
        camera.viewportWidth = width - gallery.getWidth();
        camera.viewportHeight = height;
        camera.update();
    }

    /** @param delta 1 to show the next world, -1 to show the previous one; going past the end wraps around */
    public void stepWorld(int delta) {
        final int index = Math.floorMod(worlds.indexOf(currentWorld) + delta, worlds.size());
        selectWorld(index + 1);
    }

    /** @param selection The 1-based number of the world to show. Numbers without a world are ignored. */
    public void selectWorld(int selection) {
        if (selection < 1 || selection > worlds.size()) {
            return;
        }
        currentWorld = worlds.get(selection - 1);
        titleLabel.setText(currentWorld.getWorldTitle());
        updateExplanation();
    }

    /** Shows or hides the explanation of the world. */
    public void toggleExplanation() {
        explanationShown = !explanationShown;
        updateExplanation();
    }

    private void updateExplanation() {
        if (explanationShown) {
            explanationLabel.setText(galleryEntries.get(worlds.indexOf(currentWorld)).explanation());
            explanationPanel.setBackground(panelBackground);
        } else {
            explanationLabel.setText(EXPLANATION_HINT);
            explanationPanel.setBackground((Drawable) null);
        }
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
        if (sharpTitleFont != null) {
            sharpTitleFont.dispose();
        } else {
            titleFont.dispose();
        }
        explanationText.dispose();
        panelTexture.dispose();
        worlds.forEach(World::dispose);
    }
}
