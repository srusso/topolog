package net.sr89.topology;

import com.badlogic.gdx.graphics.Color;

import java.util.HexFormat;

public class HexColors {
    public static final Color PINK_PASTEL = fromHexCode("#E7CCCC");
    public static final Color GREEN_PASTEL = fromHexCode("#A5B68D");

    private static Color fromHexCode(String hexCode) {
        final int red = HexFormat.fromHexDigits(hexCode, 1, 3);
        final int green = HexFormat.fromHexDigits(hexCode, 3, 5);
        final int blue = HexFormat.fromHexDigits(hexCode, 5, 7);

        return new Color(
            normalize(red),
            normalize(green),
            normalize(blue),
            1F);
    }

    private static float normalize(int intCode) {
        return ((float) intCode) / 255.0F;
    }
}
