package net.sr89.topology.shapes;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;

import java.util.List;

/** Builds a single mesh made of many straight tubes (thin cylinders), each between two points. */
public final class SegmentTubes {
    private static final int SIDES = 8;

    private SegmentTubes() {}

    /** @param segments pairs of points: each element is {start, end} */
    public static Model build(List<Vector3[]> segments, float radius, Color color) {
        final ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();
        final MeshPartBuilder builder = modelBuilder.part("tubes", GL20.GL_TRIANGLES,
            Usage.Position | Usage.Normal, new Material(ColorAttribute.createDiffuse(color)));

        final Vector3 direction = new Vector3(), middle = new Vector3();
        final Quaternion rotation = new Quaternion();
        final Matrix4 transform = new Matrix4();
        for (Vector3[] segment : segments) {
            direction.set(segment[1]).sub(segment[0]);
            final float length = direction.len();
            middle.set(segment[0]).add(segment[1]).scl(0.5f);
            // a cylinder of height 1 along the Y axis, scaled to the right length, turned to the right direction
            rotation.setFromCross(Vector3.Y, direction.nor());
            transform.set(middle, rotation, new Vector3(1f, length, 1f));
            builder.setVertexTransform(transform);
            builder.cylinder(2 * radius, 1f, 2 * radius, SIDES);
        }
        return modelBuilder.end();
    }
}
