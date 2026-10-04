package net.sr89.topology.worlds;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.DepthTestAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder.VertexInfo;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import net.sr89.topology.HexColors;

import java.util.ArrayList;
import java.util.List;

/**
 * The fundamental group of the (hollow) torus is isomorphic to ℤ×ℤ.
 * <p>
 * The torus is S¹×S¹, so a loop is determined (up to homotopy) by how many times it winds
 * around the "long way" (generator 'a', red) and the "short way" (generator 'b', blue).
 * This world draws a translucent torus, the two generators, and a tracer drawing the loop
 * a<sup>p</sup>b<sup>q</sup> (winding p times longitudinally and q times meridionally).
 */
public class TorusWithFundamentalGroup implements World {
    private static final float MAJOR_RADIUS = 2f;
    private static final float MINOR_RADIUS = 0.7f;
    private static final int SEGMENTS_U = 64;
    private static final int SEGMENTS_V = 32;

    private static final int GENERATOR_MARKERS = 120;
    private static final int PATH_MARKERS = 400;
    private static final int WINDING_P = 2; // times around the long way
    private static final int WINDING_Q = 3; // times around the short way
    private static final float TRACER_SPEED = 0.05f; // full loops per second

    private final Model torusModel;
    private final Model markerModel;
    private final Model tracerModel;
    private final ModelInstance torus;
    private final List<ModelInstance> generatorA = new ArrayList<>();
    private final List<ModelInstance> generatorB = new ArrayList<>();
    private final List<ModelInstance> path = new ArrayList<>();
    private final ModelInstance tracer;

    private float progress = 0f; // in [0, 1)
    private final Vector3 tmp = new Vector3();

    private final List<Model> generatorModels = new ArrayList<>();

    public TorusWithFundamentalGroup() {
        torusModel = createTorusModel();
        markerModel = createSphereModel(0.04f, HexColors.GREEN_PASTEL);
        tracerModel = createSphereModel(0.12f, Color.YELLOW);

        torus = new ModelInstance(torusModel);

        // Slightly outside the surface so the markers aren't hidden inside it.
        final float surfaceRadius = MINOR_RADIUS * 1.03f;
        final Model aModel = createSphereModel(0.05f, Color.RED);
        final Model bModel = createSphereModel(0.05f, Color.CYAN);

        generatorModels.add(aModel);
        generatorModels.add(bModel);

        for (int i = 0; i < GENERATOR_MARKERS; i++) {
            float t = (float) i / GENERATOR_MARKERS;
            generatorA.add(placed(aModel, t, 0f, surfaceRadius));
            generatorB.add(placed(bModel, 0f, t, surfaceRadius));
        }
        for (int i = 0; i < PATH_MARKERS; i++) {
            float t = (float) i / PATH_MARKERS;
            path.add(placed(markerModel, WINDING_P * t, WINDING_Q * t, surfaceRadius));
        }
        tracer = placed(tracerModel, 0f, 0f, surfaceRadius);
    }

    @Override
    public String getWorldTitle() {
        return "Torus with fundamental group";
    }

    @Override
    public void reposition(float deltaTime) {
        progress = (progress + deltaTime * TRACER_SPEED) % 1f;
        setPosition(tracer, WINDING_P * progress, WINDING_Q * progress, MINOR_RADIUS * 1.03f);
    }

    @Override
    public void render(ModelBatch modelBatch, Environment environment) {
        // ModelBatch draws blended renderables (the torus) after opaque ones, back to front.
        generatorA.forEach(m -> modelBatch.render(m, environment));
        generatorB.forEach(m -> modelBatch.render(m, environment));
        path.forEach(m -> modelBatch.render(m, environment));
        modelBatch.render(tracer, environment);
        modelBatch.render(torus, environment);
    }

    @Override
    public void dispose() {
        torusModel.dispose();
        markerModel.dispose();
        tracerModel.dispose();
        generatorModels.forEach(Model::dispose);
    }

    private ModelInstance placed(Model model, float uTurns, float vTurns, float tubeRadius) {
        ModelInstance instance = new ModelInstance(model);
        setPosition(instance, uTurns, vTurns, tubeRadius);
        return instance;
    }

    private void setPosition(ModelInstance instance, float uTurns, float vTurns, float tubeRadius) {
        torusPoint(uTurns, vTurns, tubeRadius, tmp);
        instance.transform.setToTranslation(tmp);
    }

    private static Model createTorusModel() {
        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();
        Material material = new Material(
            ColorAttribute.createDiffuse(HexColors.GREEN_PASTEL),
            new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, 0.35f),
            IntAttribute.createCullFace(GL20.GL_NONE), // hollow: visible from inside too
            // Test against the depth buffer but don't write to it, otherwise a near triangle of this
            // translucent surface would hide the far ones that happen to be drawn after it.
            new DepthTestAttribute(GL20.GL_LEQUAL, false)
        );
        MeshPartBuilder builder = modelBuilder.part("torus", GL20.GL_TRIANGLES,
            Usage.Position | Usage.Normal, material);

        // Vertices are generated on a (SEGMENTS_U) x (SEGMENTS_V) grid that wraps around in both directions.
        final Vector3 center = new Vector3();
        final VertexInfo info = new VertexInfo();
        final short[] indices = new short[SEGMENTS_U * SEGMENTS_V];
        final Vector3 position = new Vector3();
        final Vector3 normal = new Vector3();
        for (int i = 0; i < SEGMENTS_U; i++) {
            for (int j = 0; j < SEGMENTS_V; j++) {
                final float u = (float) i / SEGMENTS_U, v = (float) j / SEGMENTS_V;
                torusPoint(u, v, MINOR_RADIUS, position);
                // The normal points from the tube's center circle to the surface point.
                torusPoint(u, 0f, 0f, center);
                normal.set(position).sub(center).nor();
                info.set(position, normal, null, null);
                indices[i * SEGMENTS_V + j] = builder.vertex(info);
            }
        }
        for (int i = 0; i < SEGMENTS_U; i++) {
            final int nextI = (i + 1) % SEGMENTS_U;
            for (int j = 0; j < SEGMENTS_V; j++) {
                final int nextJ = (j + 1) % SEGMENTS_V;
                final short a = indices[i * SEGMENTS_V + j];
                final short b = indices[nextI * SEGMENTS_V + j];
                final short c = indices[nextI * SEGMENTS_V + nextJ];
                final short d = indices[i * SEGMENTS_V + nextJ];
                builder.triangle(a, b, c);
                builder.triangle(a, c, d);
            }
        }
        return modelBuilder.end();
    }

    private static Model createSphereModel(float radius, Color color) {
        return new ModelBuilder().createSphere(radius * 2, radius * 2, radius * 2, 12, 12,
            new Material(ColorAttribute.createDiffuse(color)), Usage.Position | Usage.Normal);
    }

    /**
     * Most important method in this class. Everything else depends on it: the torus vertices (polygons) themselves, and all the generator and marker spheres.
     * <p>
     * Point on a torus. u goes the long way around (about the Y axis), v goes the short way
     * (around the tube); both in turns, so 1 = one full revolution.
     *
     * @param uTurns How far around the long way (around the Y axis, the big ring). 1.0 is one full revolution.
     * @param vTurns How far around the tube (the short way). 1.0 is one full revolution.
     * @param tubeRadius Distance from the tube's centre circle. Passing MINOR_RADIUS gives a point on the surface. Passing 0 gives a point on the centre circle.
     * @param out Output vector
     */
    private static void torusPoint(float uTurns, float vTurns, float tubeRadius, Vector3 out) {
        final float u = MathUtils.PI2 * uTurns;
        final float v = MathUtils.PI2 * vTurns;
        final float ring = MAJOR_RADIUS + tubeRadius * MathUtils.cos(v);
        out.set(ring * MathUtils.cos(u), tubeRadius * MathUtils.sin(v), ring * MathUtils.sin(u));
    }
}
