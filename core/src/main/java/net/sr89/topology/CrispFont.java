package net.sr89.topology;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.awt.Rectangle;

/**
 * Makes a font whose letters are drawn at the size of the pixels of the screen, so that text is sharp. A font that
 * is a picture made for one size looks blurry or blocky when it is drawn at another, and so does text on a
 * screen with a high pixel density (such as a Retina screen) where one unit of the interface is several pixels.
 * <p>
 * The letters are drawn with Java2D, from a font of the system, into a texture; the interface measures in units
 * of its own, so the font is scaled down to be of the size asked for in them. Java2D is used without a window.
 */
final class CrispFont {
    private static final int FIRST_CHARACTER = 32, LAST_CHARACTER = 126;
    private static final int ATLAS_SIZE = 1024;
    private static final int PADDING = 2;

    private final Texture texture;
    private final BitmapFont font;

    /**
     * @param family      the name of a font, such as {@link Font#SANS_SERIF}
     * @param size        the height of the letters, in units of the interface
     * @param density     how many pixels there are in a unit of the interface (2 on a Retina screen)
     * @param extra       a character to add to the font, drawn as a round dot as big as a capital letter
     * @param markup      whether the text can have colors, with the markup of libgdx
     */
    static CrispFont create(String family, float size, float density, char extra, boolean markup) {
        // use Java2D without opening a window (which would also not work with how the application starts on macOS)
        System.setProperty("java.awt.headless", "true");
        return new CrispFont(family, size, density, extra, markup);
    }

    /** How many pixels there are in a unit of the interface (2 on a Retina screen). */
    static float density() {
        final int width = Gdx.graphics.getWidth();
        return width > 0 ? Math.max(1f, (float) Gdx.graphics.getBackBufferWidth() / width) : 1f;
    }

    private CrispFont(String family, float size, float density, char extra, boolean markup) {
        final int pixelSize = Math.max(8, Math.round(size * density));
        final Font awtFont = new Font(family, Font.PLAIN, pixelSize);

        final BufferedImage atlas = new BufferedImage(ATLAS_SIZE, ATLAS_SIZE, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D graphics = atlas.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setFont(awtFont);
        graphics.setColor(java.awt.Color.WHITE);
        final FontRenderContext context = graphics.getFontRenderContext();
        final FontMetrics metrics = graphics.getFontMetrics();
        final int ascent = metrics.getAscent();
        final int lineHeight = metrics.getAscent() + metrics.getDescent() + metrics.getLeading();

        final BitmapFont.BitmapFontData data = new BitmapFont.BitmapFontData();
        int x = PADDING, y = PADDING, rowHeight = 0;
        float capHeight = 0, xHeight = 0, deepest = 0, spaceAdvance = pixelSize / 3f;
        for (int c = FIRST_CHARACTER; c <= LAST_CHARACTER; c++) {
            final GlyphVector vector = awtFont.createGlyphVector(context, String.valueOf((char) c));
            final float advance = vector.getGlyphMetrics(0).getAdvanceX();
            final Rectangle bounds = vector.getPixelBounds(context, 0, 0);

            final BitmapFont.Glyph glyph = new BitmapFont.Glyph();
            glyph.id = c;
            glyph.xadvance = Math.round(advance);
            if (bounds.width > 0 && bounds.height > 0) {
                if (x + bounds.width + PADDING > ATLAS_SIZE) {
                    x = PADDING;
                    y += rowHeight + PADDING;
                    rowHeight = 0;
                }
                // the origin of the letter, such that its box is at (x, y)
                graphics.drawGlyphVector(vector, x - bounds.x, y - bounds.y);
                glyph.srcX = x;
                glyph.srcY = y;
                glyph.width = bounds.width;
                glyph.height = bounds.height;
                glyph.xoffset = bounds.x;
                // in libgdx, the offset goes up: from the bottom of the letter to the baseline, below the top of the line
                glyph.yoffset = -(bounds.height + ascent + bounds.y);
                x += bounds.width + PADDING;
                rowHeight = Math.max(rowHeight, bounds.height);
                deepest = Math.max(deepest, bounds.y + bounds.height);
                if (c == 'H') {
                    capHeight = bounds.height;
                } else if (c == 'x') {
                    xHeight = bounds.height;
                }
            } else if (c == ' ') {
                spaceAdvance = glyph.xadvance;
            }
            data.setGlyph(c, glyph);
        }

        // the extra character: a dot, as big as a capital letter, sitting on the line
        final int diameter = Math.max(4, Math.round(capHeight));
        if (x + diameter + PADDING > ATLAS_SIZE) {
            x = PADDING;
            y += rowHeight + PADDING;
        }
        graphics.fill(new Ellipse2D.Float(x, y, diameter, diameter));
        final BitmapFont.Glyph dot = new BitmapFont.Glyph();
        dot.id = extra;
        dot.srcX = x;
        dot.srcY = y;
        dot.width = diameter;
        dot.height = diameter;
        dot.xoffset = Math.round(diameter * 0.1f);
        dot.yoffset = -(diameter + ascent - diameter);
        dot.xadvance = Math.round(diameter * 1.25f);
        data.setGlyph(extra, dot);
        graphics.dispose();

        final Pixmap pixmap = new Pixmap(ATLAS_SIZE, ATLAS_SIZE, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        for (int px = 0; px < ATLAS_SIZE; px++) {
            for (int py = 0; py < ATLAS_SIZE; py++) {
                final int alpha = atlas.getRGB(px, py) >>> 24;
                if (alpha != 0) {
                    pixmap.drawPixel(px, py, Color.rgba8888(1f, 1f, 1f, alpha / 255f));
                }
            }
        }
        texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();

        data.lineHeight = lineHeight;
        data.capHeight = capHeight;
        data.xHeight = xHeight;
        data.ascent = ascent - capHeight; // from the top of the line to the top of the capital letters
        data.descent = -deepest;
        data.down = -lineHeight;
        data.spaceXadvance = spaceAdvance;
        data.markupEnabled = markup;
        data.setScale(1f / density);

        final Array<TextureRegion> regions = Array.with(new TextureRegion(texture));
        font = new BitmapFont(data, regions, true);
    }

    BitmapFont getFont() {
        return font;
    }

    void dispose() {
        font.dispose();
        texture.dispose();
    }
}
