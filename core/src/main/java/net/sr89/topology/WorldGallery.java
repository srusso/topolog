package net.sr89.topology;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import net.sr89.topology.worlds.World;

import java.util.ArrayList;
import java.util.List;

/**
 * A column of miniature, live renderings of the worlds, stacked vertically on the left side of the window.
 * <p>
 * Each thumbnail is drawn directly into its own part of the window (using a GL viewport and scissor), with its
 * own camera that frames the whole world. The one that is currently shown in the main view is highlighted.
 * Coordinates are in logical pixels (as given by {@link com.badlogic.gdx.Graphics#getWidth()}), with the origin at the
 * bottom left, which is also what the scene2d stage uses.
 */
public class WorldGallery {
    /** A sphere containing the whole world, used to point a camera at it. */
    public record Framing(Vector3 center, float radius) {}

    /** A world to show in the gallery, and where to point the thumbnail's camera to see all of it. */
    public record Entry(World world, Framing framing) {}

    private static final float PADDING = 16f;
    private static final float MAX_THUMBNAIL_SIZE = 240f;
    /** Below this size, the thumbnails stop shrinking and the gallery scrolls instead. */
    private static final float MIN_THUMBNAIL_SIZE = 130f;
    /** Space for the "N more" indications above and below, when scrolling. */
    private static final float INDICATOR_HEIGHT = 30f;
    private static final float MAX_WIDTH_FRACTION = 0.2f;
    private static final float BORDER = 3f;
    private static final float FIELD_OF_VIEW = 45f;
    /** How much further than the exact fit the camera stays, so thick lines and the like are not cut at the edge. */
    private static final float FRAMING_MARGIN = 1.1f;
    /** From which direction the thumbnails look at the worlds, pointing from the world towards the camera. */
    private static final Vector3 VIEW_DIRECTION = new Vector3(0f, 0.7f, 0.7f).nor();

    private final List<Entry> entries;
    private final Environment environment;
    private final ModelBatch modelBatch;
    private final PerspectiveCamera camera = new PerspectiveCamera(FIELD_OF_VIEW, 1f, 1f);
    private final Group numbers = new Group();
    private final List<Label> numberLabels = new ArrayList<>();
    private final Label moreAbove;
    private final Label moreBelow;

    private final Color background = HexColors.veryDarkBlue().mul(0.75f);
    private final Color selectedBackground = HexColors.veryDarkBlue().lerp(Color.WHITE, 0.1f);
    private final Color border = HexColors.greenPastel();

    private float thumbnailSize;
    private float width;
    private float windowHeight;
    /** How many thumbnails fit in the window; if there are more worlds than that, the gallery scrolls. */
    private int visibleCount;
    /** The index of the first world shown. */
    private int firstVisible;

    public WorldGallery(List<Entry> entries, Environment environment, ModelBatch modelBatch, LabelStyle labelStyle) {
        this.entries = entries;
        this.environment = environment;
        this.modelBatch = modelBatch;
        camera.near = 0.1f;
        camera.far = 100f;
        for (int i = 0; i < entries.size(); i++) {
            // The keys 1 to 9 select the first nine worlds, 0 the tenth. The others can only be reached by stepping.
            final Label number = new Label(i == 9 ? "0" : String.valueOf(i + 1), labelStyle);
            numberLabels.add(number);
            numbers.addActor(number);
        }
        moreAbove = new Label("", labelStyle);
        moreBelow = new Label("", labelStyle);
        numbers.addActor(moreAbove);
        numbers.addActor(moreBelow);
        visibleCount = entries.size();
    }

    /** Labels with the number key of each world. Add them to the stage. */
    public Group getNumbers() {
        return numbers;
    }

    /** How much space the gallery takes on the left side of the window, including its padding. */
    public float getWidth() {
        return width;
    }

    /** Recomputes the layout for a window of the given size. */
    public void layout(int windowWidth, int windowHeight) {
        this.windowHeight = windowHeight;
        final int count = entries.size();
        final int fitting = (int) ((windowHeight - PADDING) / (MIN_THUMBNAIL_SIZE + PADDING));
        // if they don't all fit, the space needed for the "N more" indications leaves room for fewer
        final int fittingWhenScrolling = (int) ((windowHeight - PADDING - 2 * INDICATOR_HEIGHT) / (MIN_THUMBNAIL_SIZE + PADDING));
        visibleCount = count <= fitting ? count : Math.max(1, fittingWhenScrolling);
        final float indicators = scrolls() ? 2 * INDICATOR_HEIGHT : 0f;

        final float fitHeight = (windowHeight - indicators - PADDING * (visibleCount + 1)) / visibleCount;
        final float fitWidth = windowWidth * MAX_WIDTH_FRACTION - 2 * PADDING;
        thumbnailSize = Math.max(1f, Math.min(MAX_THUMBNAIL_SIZE, Math.min(fitHeight, fitWidth)));
        width = thumbnailSize + 2 * PADDING;
        updateLabels();
    }

    /** Draws the thumbnails. Leaves the GL viewport and scissor test as they were found (full window, no scissor). */
    public void render(int selectedIndex) {
        scrollTo(selectedIndex);
        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        for (int i = firstVisible; i < firstVisible + visibleCount; i++) {
            final float bottom = thumbnailBottom(i - firstVisible);
            if (i == selectedIndex) {
                clear(PADDING - BORDER, bottom - BORDER, thumbnailSize + 2 * BORDER, border);
            }
            final Entry entry = entries.get(i);
            clear(PADDING, bottom, thumbnailSize, i == selectedIndex ? selectedBackground : background);
            HdpiUtils.glViewport((int) PADDING, (int) bottom, (int) thumbnailSize, (int) thumbnailSize);

            frame(entry.framing());
            // One begin/end per thumbnail is intentional: ModelBatch only draws on end(), and the viewport, scissor
            // and camera are different for each thumbnail, so what's queued must be drawn before they change.
            modelBatch.begin(camera);
            entry.world().render(modelBatch, environment);
            modelBatch.end();
        }
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
        HdpiUtils.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    private boolean scrolls() {
        return entries.size() > visibleCount;
    }

    /** Scrolls the least that's needed for the selected world to be shown. */
    private void scrollTo(int selectedIndex) {
        final int before = firstVisible;
        if (selectedIndex < firstVisible) {
            firstVisible = selectedIndex;
        } else if (selectedIndex >= firstVisible + visibleCount) {
            firstVisible = selectedIndex - visibleCount + 1;
        }
        firstVisible = Math.max(0, Math.min(firstVisible, entries.size() - visibleCount));
        if (firstVisible != before) {
            updateLabels();
        }
    }

    /** Puts the number of each shown world on its thumbnail, hides the others, and shows how many are out of view. */
    private void updateLabels() {
        for (int i = 0; i < entries.size(); i++) {
            final Label number = numberLabels.get(i);
            final boolean shown = i >= firstVisible && i < firstVisible + visibleCount;
            number.setVisible(shown);
            if (shown) {
                number.setPosition(PADDING + 6f, thumbnailBottom(i - firstVisible) + thumbnailSize - number.getHeight() - 4f);
            }
        }
        final int above = firstVisible, below = entries.size() - firstVisible - visibleCount;
        moreAbove.setText(above > 0 ? "^ " + above + " more" : "");
        moreBelow.setText(below > 0 ? "v " + below + " more" : "");
        moreAbove.setPosition(PADDING, windowHeight - INDICATOR_HEIGHT + 4f);
        moreBelow.setPosition(PADDING, 4f);
    }

    /** The y coordinate of the bottom of a thumbnail, given its place from the top among the ones shown. */
    private float thumbnailBottom(int slot) {
        final float top = windowHeight - PADDING - (scrolls() ? INDICATOR_HEIGHT : 0f);
        return top - (slot + 1) * thumbnailSize - slot * PADDING;
    }

    /** Fills a square of the window with a color, and clears its depth buffer, then restricts drawing to it. */
    private void clear(float x, float y, float size, Color color) {
        HdpiUtils.glScissor((int) x, (int) y, (int) size, (int) size);
        Gdx.gl.glClearColor(color.r, color.g, color.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
    }

    /** Points the camera at the world, at the distance where its whole bounding sphere fits, with a bit of margin. */
    private void frame(Framing framing) {
        final float distance = FRAMING_MARGIN * framing.radius() / MathUtils.sinDeg(FIELD_OF_VIEW / 2);
        camera.position.set(VIEW_DIRECTION).scl(distance).add(framing.center());
        camera.up.set(Vector3.Y);
        camera.lookAt(framing.center());
        camera.update();
    }
}
