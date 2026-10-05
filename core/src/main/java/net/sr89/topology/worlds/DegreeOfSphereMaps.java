package net.sr89.topology.worlds;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import net.sr89.topology.shapes.ParametricSurface;
import net.sr89.topology.shapes.Primitives;

import java.util.ArrayList;
import java.util.List;

/**
 * π₂(S²) = ℤ: the maps from the sphere to the sphere, up to homotopy, are classified by their degree, an integer that counts
 * how many times the map covers the target (with a sign if it turns the orientation around).
 * <p>
 * The map of degree d used here keeps the latitude and multiplies the longitude by d: f(θ, φ) = (θ, d φ). The target sphere (right) has
 * the colors of a color wheel for the longitude, brighter near the north pole. The domain sphere (left) has the color of the image of
 * each of its points: so for d = 2 the whole pattern appears twice, for d = −1 once, but mirrored, and for d = 0 only a
 * line of colors is left. The white ball moves on the target; the yellow balls are its preimages: there are |d| of them
 * (counted with signs, their number is d for any point, which is why the degree doesn't depend on the point).
 * <p>
 * Two maps of the same degree can be deformed into each other, and the ones of different degrees can't: that is π₂(S²) = ℤ,
 * from the Hurewicz theorem (the degree is the isomorphism with H₂(S²) = ℤ), or by lifting to the universal cover.
 */
public class DegreeOfSphereMaps implements World {
    private static final float RADIUS = 1.5f;
    private static final Vector3 DOMAIN_CENTER = new Vector3(-2.5f, 0f, 0f);
    private static final Vector3 TARGET_CENTER = new Vector3(2.5f, 0f, 0f);
    private static final int[] DEGREES = {1, 2, 3, -1, -2, 0};
    private static final float SECONDS_PER_DEGREE = 9f;
    private static final int U_SEGMENTS = 48, V_SEGMENTS = 96;
    private static final int MAX_PREIMAGES = 3;

    private final List<Model> models = new ArrayList<>();
    private final ModelInstance domain, target;
    private final Mesh domainMesh;
    private final float[] domainVertices;
    private final int stride, colorOffset;
    private final ModelInstance targetPoint;
    private final ModelInstance[] preimages = new ModelInstance[MAX_PREIMAGES];

    private float time = 0f;
    private int shownDegree = Integer.MIN_VALUE;
    private final Vector3 tmp = new Vector3();

    public DegreeOfSphereMaps() {
        target = instance(ParametricSurface.build((u, v, out) -> onSphere(u, v, TARGET_CENTER, out), U_SEGMENTS, V_SEGMENTS,
            true, ParametricSurface.opaqueMaterial(Color.WHITE), (u, v) -> colorOf(Math.PI * u, 2 * Math.PI * v)));
        final Model domainModel = model(ParametricSurface.build((u, v, out) -> onSphere(u, v, DOMAIN_CENTER, out),
            U_SEGMENTS, V_SEGMENTS, true, ParametricSurface.opaqueMaterial(Color.WHITE),
            (u, v) -> colorOf(Math.PI * u, 2 * Math.PI * v)));
        domain = new ModelInstance(domainModel);
        domainMesh = domainModel.meshes.first();
        stride = domainMesh.getVertexSize() / 4;
        colorOffset = domainMesh.getVertexAttribute(Usage.ColorUnpacked).offset / 4;
        domainVertices = new float[domainMesh.getNumVertices() * stride];
        domainMesh.getVertices(domainVertices);

        targetPoint = instance(Primitives.ball(0.26f, Color.WHITE));
        final Model preimage = model(Primitives.ball(0.26f, Color.YELLOW));
        for (int i = 0; i < MAX_PREIMAGES; i++) {
            preimages[i] = new ModelInstance(preimage);
        }
        reposition(0f);
    }

    @Override
    public String getWorldTitle() {
        final int degree = DEGREES[currentIndex()];
        return "pi2(S^2) = Z: a map of degree " + degree
            + (degree == 0 ? " (it misses most of the target)" : degree < 0 ? " (it turns the orientation around)" : "");
    }

    @Override
    public void reposition(float deltaTime) {
        time += deltaTime;
        final int degree = DEGREES[currentIndex()];
        if (degree != shownDegree) {
            shownDegree = degree;
            recolorDomain(degree);
        }

        // a point of the target moves around, and the points of the domain that go to it
        final double polar = Math.PI / 2 + 0.9 * Math.sin(0.4 * time), azimuth = 0.6 * time;
        onSphere(polar, azimuth, TARGET_CENTER, tmp, 1.03);
        targetPoint.transform.setToTranslation(tmp);
        for (int k = 0; k < MAX_PREIMAGES; k++) {
            if (degree != 0 && k < Math.abs(degree)) {
                onSphere(polar, (azimuth + 2 * Math.PI * k) / degree, DOMAIN_CENTER, tmp, 1.03);
                preimages[k].transform.setToTranslation(tmp);
            } else {
                preimages[k].transform.setToScaling(0f, 0f, 0f);
            }
        }
    }

    @Override
    public void render(ModelBatch modelBatch, Environment environment) {
        modelBatch.render(domain, environment);
        modelBatch.render(target, environment);
        modelBatch.render(targetPoint, environment);
        for (ModelInstance preimage : preimages) {
            modelBatch.render(preimage, environment);
        }
    }

    @Override
    public void dispose() {
        models.forEach(Model::dispose);
    }

    private int currentIndex() {
        return (int) (time / SECONDS_PER_DEGREE) % DEGREES.length;
    }

    /** The domain gets, at each point, the color of the image of that point: the longitude multiplied by the degree. */
    private void recolorDomain(int degree) {
        for (int i = 0; i <= U_SEGMENTS; i++) {
            for (int j = 0; j <= V_SEGMENTS; j++) {
                final Color color = colorOf(Math.PI * i / U_SEGMENTS, degree * 2 * Math.PI * j / V_SEGMENTS);
                final int at = (i * (V_SEGMENTS + 1) + j) * stride + colorOffset;
                domainVertices[at] = color.r;
                domainVertices[at + 1] = color.g;
                domainVertices[at + 2] = color.b;
                domainVertices[at + 3] = 1f;
            }
        }
        domainMesh.setVertices(domainVertices);
    }

    /** The color wheel for the longitude, brighter near the north pole. */
    private static Color colorOf(double polar, double azimuth) {
        return Primitives.hue((float) (azimuth / (2 * Math.PI)), 0.9f, (float) (0.45 + 0.55 * (1 - polar / Math.PI)));
    }

    private static void onSphere(float u, float v, Vector3 center, Vector3 out) {
        onSphere(Math.PI * u, 2 * Math.PI * v, center, out, 1.0);
    }

    private static void onSphere(double polar, double azimuth, Vector3 center, Vector3 out) {
        onSphere(polar, azimuth, center, out, 1.0);
    }

    private static void onSphere(double polar, double azimuth, Vector3 center, Vector3 out, double scale) {
        out.set((float) (scale * RADIUS * Math.sin(polar) * Math.cos(azimuth)), (float) (scale * RADIUS * Math.cos(polar)),
            (float) (scale * RADIUS * Math.sin(polar) * Math.sin(azimuth))).add(center);
    }

    /** Takes ownership of the model to dispose of it later. */
    private Model model(Model model) {
        models.add(model);
        return model;
    }

    private ModelInstance instance(Model model) {
        return new ModelInstance(model(model));
    }
}
