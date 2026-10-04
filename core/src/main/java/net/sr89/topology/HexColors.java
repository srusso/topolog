package net.sr89.topology;

import com.badlogic.gdx.graphics.Color;

/** The colors used by the application. Each call returns a new {@link Color}, so callers are free to mutate it. */
public final class HexColors {
    private static final String VERY_DARK_BLUE = "213339";
    private static final String GREEN_PASTEL = "A5B68D";
    private static final String BLUE_PASTEL = "7FA3D6";

    private HexColors() {}

    public static Color veryDarkBlue() {
        return Color.valueOf(VERY_DARK_BLUE);
    }

    public static Color greenPastel() {
        return Color.valueOf(GREEN_PASTEL);
    }

    public static Color bluePastel() {
        return Color.valueOf(BLUE_PASTEL);
    }
}
