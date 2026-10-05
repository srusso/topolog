package net.sr89.topology.worlds;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import net.sr89.topology.HexColors;
import net.sr89.topology.math.Curve;
import net.sr89.topology.shapes.ParametricSurface;
import net.sr89.topology.shapes.Primitives;
import net.sr89.topology.shapes.TubeMesh;

import java.util.ArrayList;
import java.util.List;

/**
 * Cup products in cohomology, on the torus and on the genus 2 surface, seen through Poincaré duality.
 * <p>
 * On a closed surface, every class of H¹ is dual to a class of H₁: a curve. The cup product of two classes of H¹ is then the
 * intersection of their curves, counted with signs: ⟨α ∪ β, [M]⟩ = the number of points where the two curves cross. So it
 * can be seen: two curves that cross once give 1, and two curves that don't meet give 0.
 * <p>
 * Pairs of curves, one after the other:
 * <ul>
 *   <li>On the torus: a and b, which cross once (a ∪ b = 1); and a with a copy of itself pushed off to the side, which doesn't
 *       meet it (a ∪ a = 0). The cup product is antisymmetric: b ∪ a = −(a ∪ b).</li>
 *   <li>On the genus 2 surface: a₁ and b₁ cross once (1); a₁ and a₂ are on different handles (0); a₂ and b₂ cross once
 *       (1); b₁ and b₂ (0); and a₁ with a pushed-off copy of itself (0).</li>
 * </ul>
 * So the cup product pairs a₁* with b₁* and a₂* with b₂*, and gives 0 on any other pair of the basis: the ring structure of H*(M).
 */
public class CupProducts implements World {
    private static final float SECONDS_PER_PAIR = 4.5f;
    private static final float TUBE = 0.045f;
    private static final float TORUS_MAJOR = 1.4f, TORUS_MINOR = 0.55f;
    private static final Vector3 TORUS_CENTER = new Vector3(-3.7f, 0f, 0f);
    private static final Vector3 GENUS_CENTER = new Vector3(2.2f, 0f, 0f);
    private static final float GENUS_SCALE = 0.85f;
    private static final int SAMPLES = 400;

    /** Two curves, where they meet (null if they don't), and the text that describes them. */
    private record Pair(int first, int second, Vector3 meeting, String title) {}

    private final List<Model> models = new ArrayList<>();
    private final List<ModelInstance> curves = new ArrayList<>();
    private final List<Pair> pairs = new ArrayList<>();
    private final ModelInstance torus, genus;
    private final ModelInstance meetingBall;

    private float time = 0f;
    private int shown = 0;

    public CupProducts() {
        torus = instance(ParametricSurface.build((u, v, out) -> torusPoint(u, v, 0f, out), 64, 32, true,
            ParametricSurface.translucentMaterial(HexColors.greenPastel(), 0.35f)));
        torus.transform.setToTranslation(TORUS_CENTER);

        genus = instance(GenusTwoSurface.createSurfaceModel(
            ParametricSurface.translucentMaterial(HexColors.greenPastel(), 0.3f), null));
        genus.transform.setToTranslation(GENUS_CENTER).rotate(Vector3.X, -90f)
            .scale(GenusTwoSurface.SCALE * GENUS_SCALE, GenusTwoSurface.SCALE * GENUS_SCALE, GenusTwoSurface.SCALE * GENUS_SCALE);

        final float level = 0.5f * GenusTwoSurface.TUBE_RADIUS;
        final Curve[] curveFunctions = {
            // 0, 1, 2: on the torus, a, b, and a pushed-off a
            (t, out) -> torusPoint(t, 0.25f, 0.04f, out),
            (t, out) -> torusPoint(0f, t, 0.04f, out),
            (t, out) -> torusPoint(t, 0.4f, 0.04f, out),
            // 3 to 7: on the genus 2 surface: a1, b1, a2, b2, and a pushed-off a1
            GenusTwoSurface.aroundHole(-1),
            GenusTwoSurface.throughHole(-1),
            GenusTwoSurface.aroundHole(+1),
            GenusTwoSurface.throughHole(+1),
            GenusTwoSurface.aroundHole(-1, 1.5f * level)};
        final Color[] colors = {Color.RED, Color.CYAN, Color.PINK, Color.RED, Color.CYAN, Color.ORANGE, Color.MAGENTA, Color.PINK};
        for (int i = 0; i < curveFunctions.length; i++) {
            final ModelInstance curve = instance(TubeMesh.build(curveFunctions[i], true, 200, 8, TUBE, colors[i]));
            if (i < 3) {
                curve.transform.setToTranslation(TORUS_CENTER);
            } else {
                curve.transform.setToTranslation(GENUS_CENTER).scale(GENUS_SCALE, GENUS_SCALE, GENUS_SCALE);
            }
            curves.add(curve);
        }

        pairs.add(pair(curveFunctions, 0, 1, TORUS_CENTER, 1f, "Cup product on the torus: a with b crosses once, so it is 1"));
        pairs.add(pair(curveFunctions, 0, 2, TORUS_CENTER, 1f, "Cup product on the torus: a with a pushed-off copy of itself never meets, so a cup a = 0"));
        pairs.add(pair(curveFunctions, 3, 4, GENUS_CENTER, GENUS_SCALE, "Genus 2 surface: a1 and b1 cross once, so a1 cup b1 = 1"));
        pairs.add(pair(curveFunctions, 3, 5, GENUS_CENTER, GENUS_SCALE, "Genus 2 surface: a1 and a2 are on different handles, so a1 cup a2 = 0"));
        pairs.add(pair(curveFunctions, 5, 6, GENUS_CENTER, GENUS_SCALE, "Genus 2 surface: a2 and b2 cross once, so a2 cup b2 = 1"));
        pairs.add(pair(curveFunctions, 4, 6, GENUS_CENTER, GENUS_SCALE, "Genus 2 surface: b1 and b2 are on different handles, so b1 cup b2 = 0"));
        pairs.add(pair(curveFunctions, 3, 7, GENUS_CENTER, GENUS_SCALE, "Genus 2 surface: a1 with a pushed-off copy of itself never meets, so a1 cup a1 = 0"));

        meetingBall = instance(Primitives.ball(0.22f, Color.GREEN));
        reposition(0f);
    }

    @Override
    public String getWorldTitle() {
        return pairs.get(shown).title();
    }

    @Override
    public void reposition(float deltaTime) {
        time += deltaTime;
        shown = (int) (time / SECONDS_PER_PAIR) % pairs.size();
        final Vector3 meeting = pairs.get(shown).meeting();
        if (meeting != null) {
            meetingBall.transform.setToTranslation(meeting);
        } else {
            meetingBall.transform.setToScaling(0f, 0f, 0f);
        }
    }

    @Override
    public void render(ModelBatch modelBatch, Environment environment) {
        final Pair pair = pairs.get(shown);
        modelBatch.render(curves.get(pair.first()), environment);
        modelBatch.render(curves.get(pair.second()), environment);
        modelBatch.render(meetingBall, environment);
        modelBatch.render(torus, environment);
        modelBatch.render(genus, environment);
    }

    @Override
    public void dispose() {
        models.forEach(Model::dispose);
    }

    /** The two curves, and the point where they meet if they do, in the world (with the place and the size of their surface). */
    private static Pair pair(Curve[] curves, int first, int second, Vector3 center, float scale, String title) {
        final Vector3 a = new Vector3(), b = new Vector3();
        float best = Float.MAX_VALUE;
        final Vector3 meeting = new Vector3();
        for (int i = 0; i < SAMPLES; i++) {
            curves[first].pointAt((float) i / SAMPLES, a);
            for (int j = 0; j < SAMPLES; j++) {
                curves[second].pointAt((float) j / SAMPLES, b);
                if (a.dst2(b) < best) {
                    best = a.dst2(b);
                    meeting.set(a).add(b).scl(0.5f);
                }
            }
        }
        // two curves that go through the same point are closer than the sampling
        final boolean cross = Math.sqrt(best) < 0.03f;
        return new Pair(first, second, cross ? meeting.scl(scale).add(center) : null, title);
    }

    /** The torus, with the long way around as u and the short way as v, in turns; the Y axis is the axis of the torus. */
    private static void torusPoint(float u, float v, float lift, Vector3 out) {
        final double alpha = 2 * Math.PI * u, beta = 2 * Math.PI * v;
        final double minor = TORUS_MINOR + lift;
        final double ring = TORUS_MAJOR + minor * Math.cos(beta);
        out.set((float) (ring * Math.cos(alpha)), (float) (minor * Math.sin(beta)), (float) (ring * Math.sin(alpha)));
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
