package net.sr89.topology.shapes;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.CylinderShapeBuilder;
import net.sr89.topology.HexColors;

import java.util.ArrayList;
import java.util.List;

public class BasicShapes {
    private final static Color helixColor = HexColors.GREEN_PASTEL;

    private static final int cylinderCount = 400;

    // TODO make this a "fat" circle using cylinders
    public static Model createUnitCircle() {
        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();
        MeshPartBuilder builder = modelBuilder.part("circleShape", GL20.GL_LINES,
            VertexAttributes.Usage.Position | VertexAttributes.Usage.ColorUnpacked, new Material());
        builder.setColor(Color.WHITE);

        float prevX = helixX(0F);
        float prevZ = helixZ(0F);

        for (int i = 1; i <= cylinderCount; i++) {
            float s = 0.01F * i;
            float newX = helixX(s);
            float newZ = helixZ(s);

            // TODO after turning these into cylinders, put the same exact color logic in the helix
            if (i  >= 80 && i <= 140) {
                builder.setColor(Color.RED);
            } else {
                builder.setColor(helixColor);
            }

            builder.line(prevX, 0, prevZ, newX, 0, newZ);

            prevX = newX;
            prevZ = newZ;
        }

        return modelBuilder.end();
    }

    public static CylinderHelix createCylinderHelix() {
        Model cylinderModel = cylinderModel();

        List<MyCylinder> instances = new ArrayList<>();

        for (int i = 1; i <= cylinderCount; i++) {
            ModelInstance cylinder = new ModelInstance(cylinderModel);

            instances.add(new MyCylinder(cylinder, i));
        }

        return new CylinderHelix(instances);
    }

    private static Model cylinderModel() {
        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();
        Material material = new Material();
        material.set();
        MeshPartBuilder builder = modelBuilder.part("cylinder", GL20.GL_TRIANGLES,
            VertexAttributes.Usage.Position | VertexAttributes.Usage.ColorUnpacked, material);
        builder.setColor(helixColor);

        CylinderShapeBuilder.build(builder, 1, 1, 1, 100);

        return modelBuilder.end();
    }

    // note: the Y and Z axes are inverted compared to the notation in Hatcher

    private static float helixZ(float s) {
        return (float) Math.sin(2 * Math.PI * s);
    }

    private static float helixX(float s) {
        return (float) Math.cos(2 * Math.PI * s);
    }
}
