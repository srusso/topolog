package com.atlassian.inno.topology.shapes;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;

public class BasicShapes {
    public static Model createLine() {
        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();
        MeshPartBuilder builder = modelBuilder.part("lineShape", GL20.GL_LINES,
            VertexAttributes.Usage.Position | VertexAttributes.Usage.ColorUnpacked, new Material());
        builder.setColor(Color.BLUE);
        builder.line(0, 0, 0, 1, 1, 2);
        return modelBuilder.end();
    }

    public static Model createHelix() {
        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();
        MeshPartBuilder builder = modelBuilder.part("circleShape", GL20.GL_LINES,
            VertexAttributes.Usage.Position | VertexAttributes.Usage.ColorUnpacked, new Material());
        builder.setColor(Color.PINK);

        float prevX = helixX(0F);
        float prevY = helixY(0F);
        float prevZ = helixZ(0F);

        for (int i = 1; i <= 400; i++) {
            float s = 0.01F * i;
            float newX = helixX(s);
            float newY = helixY(s);
            float newZ = helixZ(s);
            builder.line(prevX, prevY, prevZ, newX, newY, newZ);
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
