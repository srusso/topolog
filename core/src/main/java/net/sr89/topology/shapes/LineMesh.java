package net.sr89.topology.shapes;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;

import java.util.List;

/** Builds a single mesh made of thin, unlit lines. */
public final class LineMesh {
    private LineMesh() {}

    /** @param segments pairs of points: each element is {start, end} */
    public static Model build(List<Vector3[]> segments, Color color) {
        final ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();
        final MeshPartBuilder builder = modelBuilder.part("lines", GL20.GL_LINES,
            Usage.Position | Usage.ColorUnpacked, new Material());
        builder.setColor(color);
        for (Vector3[] segment : segments) {
            builder.line(segment[0], segment[1]);
        }
        return modelBuilder.end();
    }
}
