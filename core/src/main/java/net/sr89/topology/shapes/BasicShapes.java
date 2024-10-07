package net.sr89.topology.shapes;

import net.sr89.topology.HexColors;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;

public class BasicShapes {
    private final static Color helixColor = HexColors.GREEN_PASTEL;

    public static Model createUnitCircle() {
        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();
        MeshPartBuilder builder = modelBuilder.part("circleShape", GL20.GL_LINES,
            VertexAttributes.Usage.Position | VertexAttributes.Usage.ColorUnpacked, new Material());
        builder.setColor(Color.WHITE);

        float prevX = helixX(0F);
        float prevY = helixY(0F);

        for (int i = 1; i <= 100; i++) {
            float s = 0.01F * i;
            float newX = helixX(s);
            float newY = helixY(s);

            if (i % 100 >= 20 && i % 100 <= 35) {
                builder.setColor(Color.RED);
            } else {
                builder.setColor(helixColor);
            }

            // note: the Y and Z axes are inverted compared to the notation in Hatcher
            builder.line(prevX, 0, prevY, newX, 0, newY);

            prevX = newX;
            prevY = newY;
        }

        return modelBuilder.end();
    }

    public static Model createHelix() {
        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();
        MeshPartBuilder builder = modelBuilder.part("circleShape", GL20.GL_LINES,
            VertexAttributes.Usage.Position | VertexAttributes.Usage.ColorUnpacked, new Material());
        builder.setColor(helixColor);

        float prevX = helixX(0F);
        float prevY = helixY(0F);
        float prevZ = helixZ(0F);
        final float helixShift = 0.7f; // how far up we are moving the covering space above the circle

        for (int i = 1; i <= 300; i++) {
            float s = 0.01F * i;
            float newX = helixX(s);
            float newY = helixY(s);
            float newZ = helixZ(s);

            if (i % 100 >= 20 && i % 100 <= 35) {
                builder.setColor(Color.RED);
            } else {
                builder.setColor(helixColor);
            }

            // note: the Y and Z axes are inverted compared to the notation in Hatcher
            builder.line(prevX, prevZ + helixShift, prevY, newX, newZ + helixShift, newY);

            prevX = newX;
            prevY = newY;
            prevZ = newZ;
        }

        return modelBuilder.end();
    }

    private static float helixZ(float s) {
        return s;
    }

    private static float helixY(float s) {
        return (float) Math.sin(2 * Math.PI * s);
    }

    private static float helixX(float s) {
        return (float) Math.cos(2 * Math.PI * s);
    }
}
