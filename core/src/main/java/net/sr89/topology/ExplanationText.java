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
    public static final char BALL = '';

    private static final int BALL_SIZE = 18;
    /** Where the top of the dot is, counting from the top of the line, in pixels of the font. */
    private static final int BALL_TOP = 6;

    private final BitmapFont font;
    private final Texture ballTexture;

    public ExplanationText() {
        this(Gdx.files.internal("bitmapfont/Amble-Regular-26.fnt"));
    }

    /** @param fontFile the font to add the dot to, which must have an empty space in the character {@link #BALL} */
    public ExplanationText(FileHandle fontFile) {
        font = new BitmapFont(fontFile);
        font.getData().markupEnabled = true;

        // The dot is a picture added to the font as one more page, with its own character.
        final int size = BALL_SIZE; // the edge is smoothed by giving the pixels on it a part of the opacity
        final Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                final float distance = (float) Math.hypot(x + 0.5f - size / 2f, y + 0.5f - size / 2f);
                final float alpha = Math.max(0f, Math.min(1f, size / 2f - 1f - distance + 0.5f));
                pixmap.drawPixel(x, y, Color.rgba8888(1f, 1f, 1f, alpha));
            }
        }
        ballTexture = new Texture(pixmap);
        ballTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();

        final TextureRegion region = new TextureRegion(ballTexture);
        font.getRegions().add(region);
        final BitmapFont.Glyph glyph = new BitmapFont.Glyph();
        glyph.id = BALL;
        glyph.page = font.getRegions().size - 1;
        glyph.width = BALL_SIZE;
        glyph.height = BALL_SIZE;
        glyph.xoffset = 2;
        glyph.yoffset = -(BALL_SIZE + BALL_TOP); // in libgdx the Y axis of a glyph's offset goes up
        glyph.xadvance = BALL_SIZE + 5;
        font.getData().setGlyphRegion(glyph, region);
        font.getData().setGlyph(BALL, glyph);
    }

    public BitmapFont getFont() {
        return font;
    }

    /** A dot of the color, in markup, such as {@code ball("YELLOW")} or {@code ball("#A5B68D")}. */
    public static String ball(String color) {
        return "[" + color + "]" + BALL + "[]";
    }

    @Override
    public void dispose() {
        font.dispose();
        ballTexture.dispose();
    }
}
