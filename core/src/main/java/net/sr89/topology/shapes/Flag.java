package net.sr89.topology.shapes;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;

/**
 * A marker that sits on a surface and also tells which side of the surface it's on: a big ball on the surface,
 * and a small ball floating over it, in the direction of the surface's normal.
 * Moving it around a loop that can't be oriented brings it back to the other side.
 */
public class Flag {
    private static final float BODY_DIAMETER = 0.2f;
    private static final float MARKER_DIAMETER = 0.11f;
    private static final float MARKER_DISTANCE = 0.24f;

    private final Model bodyModel;
    private final Model markerModel;
    private final ModelInstance body;
    private final ModelInstance marker;
    private final Vector3 tmp = new Vector3();

    public Flag(Color bodyColor, Color markerColor) {
        bodyModel = sphere(BODY_DIAMETER, bodyColor);
        markerModel = sphere(MARKER_DIAMETER, markerColor);
        body = new ModelInstance(bodyModel);
        marker = new ModelInstance(markerModel);
    }

    /** @param normal a unit vector, pointing away from the surface on the side where the flag is */
    public void place(Vector3 position, Vector3 normal) {
        body.transform.setToTranslation(position);
        tmp.set(normal).scl(MARKER_DISTANCE).add(position);
        marker.transform.setToTranslation(tmp);
    }

    public void render(ModelBatch modelBatch, Environment environment) {
        modelBatch.render(body, environment);
        modelBatch.render(marker, environment);
    }

    public void dispose() {
        bodyModel.dispose();
        markerModel.dispose();
    }

    private static Model sphere(float diameter, Color color) {
        return new ModelBuilder().createSphere(diameter, diameter, diameter, 12, 12,
            new Material(ColorAttribute.createDiffuse(color)), Usage.Position | Usage.Normal);
    }
}
