package net.sr89.topology;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

/**
 * The font of the explanations of the worlds. Its text can have colors, with the markup of libgdx ({@code [RED]red[]},
 * {@code [#A5B68D]green[]}), and it has one more character, {@link #BALL}: a flat, round dot, in the color of the text around it,
 * to refer to the balls of the worlds. Write {@code [[} for a {@code [].
 */
public class ExplanationText implements Disposable {
    /** A flat round dot, as big as a letter. Put it in a color, like {@code [YELLOW]} + BALL + {@code []}. */
    public static final char BALL = '\uE000';

    /** The height of the letters, in units of the interface. */
    private static final float SIZE = 17f;
    /** The scale at which the font that is a picture (the fallback) is about as big. */
    private static final float FALLBACK_SCALE = 0.7f;
    private static final int FALLBACK_BALL_SIZE = 18;
    /** Where the top of the dot is, counting from the top of the line, in pixels of the font. */
    private static final int FALLBACK_BALL_TOP = 6;

    private final BitmapFont font;
    private final float scale;
    private final CrispFont crispFont;
    private final Texture ballTexture;

    /**
     * A font drawn at the size of the pixels of the screen, so it is sharp. If that doesn't work (it needs a font from the
     * system), the font of the titles is used instead, which is a picture and is not as sharp.
     */
    public ExplanationText() {
        CrispFont crisp = null;
        try {
            crisp = CrispFont.create(java.awt.Font.SANS_SERIF, SIZE, CrispFont.density(), BALL, true);
        } catch (Throwable problem) {
            Gdx.app.error("ExplanationText", "Can't make a sharp font, using the font of the titles", problem);
        }
        if (crisp != null) {
            crispFont = crisp;
            font = crisp.getFont();
            scale = 1f;
            ballTexture = null;
        } else {
            crispFont = null;
            font = new BitmapFont(Gdx.files.internal("bitmapfont/Amble-Regular-26.fnt"));
            scale = FALLBACK_SCALE;
            ballTexture = addBall(font);
        }
        font.getData().markupEnabled = true;
    }

    /** @param fontFile the font of the titles, to use instead of a sharp one. */
    public ExplanationText(FileHandle fontFile) {
        crispFont = null;
        font = new BitmapFont(fontFile);
        scale = FALLBACK_SCALE;
        ballTexture = addBall(font);
        font.getData().markupEnabled = true;
    }

    /** The dot is a picture added to the font as one more page, with its own character. */
    private static Texture addBall(BitmapFont font) {
        final int size = FALLBACK_BALL_SIZE; // the edge is smoothed by giving the pixels on it a part of the opacity
        final Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                final float distance = (float) Math.hypot(x + 0.5f - size / 2f, y + 0.5f - size / 2f);
                final float alpha = Math.max(0f, Math.min(1f, size / 2f - 1f - distance + 0.5f));
                pixmap.drawPixel(x, y, Color.rgba8888(1f, 1f, 1f, alpha));
            }
        }
        final Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();

        final TextureRegion region = new TextureRegion(texture);
        font.getRegions().add(region);
        final BitmapFont.Glyph glyph = new BitmapFont.Glyph();
        glyph.id = BALL;
        glyph.page = font.getRegions().size - 1;
        glyph.width = FALLBACK_BALL_SIZE;
        glyph.height = FALLBACK_BALL_SIZE;
        glyph.xoffset = 2;
        glyph.yoffset = -(FALLBACK_BALL_SIZE + FALLBACK_BALL_TOP); // in libgdx the Y axis of a glyph's offset goes up
        glyph.xadvance = FALLBACK_BALL_SIZE + 5;
        font.getData().setGlyphRegion(glyph, region);
        font.getData().setGlyph(BALL, glyph);
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return texture;
    }

    public BitmapFont getFont() {
        return font;
    }

    /** The scale to draw the font at to get letters of the size meant for the explanations. */
    public float getScale() {
        return scale;
    }

    /** A dot of the color, in markup, such as {@code ball("YELLOW")} or {@code ball("#A5B68D")}. */
    public static String ball(String color) {
        return "[" + color + "]" + BALL + "[]";
    }

    @Override
    public void dispose() {
        if (crispFont != null) {
            crispFont.dispose();
        } else {
            font.dispose();
            ballTexture.dispose();
        }
    }
}
