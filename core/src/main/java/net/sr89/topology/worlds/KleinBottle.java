package net.sr89.topology.worlds;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import net.sr89.topology.HexColors;
import net.sr89.topology.math.Curve;
import net.sr89.topology.math.Surface;
import net.sr89.topology.shapes.Flag;
import net.sr89.topology.shapes.ParametricSurface;
import net.sr89.topology.shapes.TubeMesh;

import java.util.ArrayList;
import java.util.List;

/**
 * The Klein bottle, drawn as the usual "figure eight" immersion in ℝ³, which passes through itself (the real
 * Klein bottle doesn't; that needs four dimensions).
 * <p>
 * Its fundamental group is ⟨a, b | a b a⁻¹ b⟩. Take the square with u going around a (red) and v going around b
 * (cyan): the square's edges are glued like a torus, except that when u comes around, v is reversed. This is what the
 * relation says: going around a turns b into b⁻¹, and it's also what makes the surface non-orientable.
 * <p>
 * A flag that moves around a (a ball on the surface and a small ball over it, on one side) shows it: after one lap, it is
 * on the other side of the surface. After two laps, it is back where it started.
 * <p>
 * The curves v = 0 and v = ½ are the same curve of the figure eight, where the surface goes through itself. To go around
 * a without being on that curve (where it would not be clear which sheet the flag is on), a goes from (0, ¼) to (1, ¾) instead
 * of along v = 0; that is the same point of the surface as (0, ¼) once the sides are glued, and it only meets the curve where it
 * goes through v = ½. It is the same loop as v = 0, as far as the fundamental group is concerned.
 */
public class KleinBottle implements World {
    private static final float SCALE = 0.75f;
    private static final float CENTER_RADIUS = 2f;
    private static final int LOOP_SAMPLES = 300;
    private static final float TRACER_SECONDS_PER_LAP = 12f;
    /** The radius of the tubes of the loops. */
    private static final float TUBE_THICKNESS = 0.035f;

    private final List<Model> models = new ArrayList<>();
    private final ModelInstance surface;
    private final ModelInstance loopA;
    private final ModelInstance loopB;
    private final Flag flag = new Flag(Color.YELLOW, Color.WHITE);

    private float laps = 0f; // in [0, 2)
    private final Vector3 position = new Vector3();
    private final Vector3 normal = new Vector3();

    public KleinBottle() {
        final Surface klein = KleinBottle::point;
        surface = instance(ParametricSurface.build(klein, 160, 80, false,
            ParametricSurface.translucentMaterial(HexColors.greenPastel(), 0.4f)));
        // a goes around u; b goes around v, at u = 0
        final Curve a = (t, out) -> point(t, aHeight(t), out);
        final Curve b = (t, out) -> point(0f, t, out);
        loopA = instance(TubeMesh.build(a, true, LOOP_SAMPLES, 8, TUBE_THICKNESS, Color.RED));
        loopB = instance(TubeMesh.build(b, true, LOOP_SAMPLES, 8, TUBE_THICKNESS, Color.CYAN));
        reposition(0f);
    }

    @Override
    public String getWorldTitle() {
        return "Klein bottle: <a, b | a b a^-1 b>";
    }

    @Override
    public void reposition(float deltaTime) {
        laps = (laps + deltaTime / TRACER_SECONDS_PER_LAP) % 2f;
        final float u = laps % 1f;
        point(u, aHeight(u), position);
        ParametricSurface.normalAt(KleinBottle::point, u, aHeight(u), normal);
        // Going around u once brings the normal vector to its opposite (that's non-orientability). To make the flag
        // move continuously, we flip it again for each full lap, so that it's on the other side of the surface
        // in the second lap.
        if (laps >= 1f) {
            normal.scl(-1f);
        }
        flag.place(position, normal);
    }

    @Override
    public void render(ModelBatch modelBatch, Environment environment) {
        modelBatch.render(loopA, environment);
        modelBatch.render(loopB, environment);
        flag.render(modelBatch, environment);
        modelBatch.render(surface, environment);
    }

    @Override
    public void dispose() {
        models.forEach(Model::dispose);
        flag.dispose();
    }

    /** The v coordinate of the loop a when it is at u: it starts at ¼, and is at ¾ when it comes back (the same point). */
    private static float aHeight(float u) {
        return 0.25f + 0.5f * u;
    }

    /** The "figure eight" Klein bottle, with u and v from 0 to 1 (a full turn each). */
    private static void point(float u, float v, Vector3 out) {
        final double bigU = 2 * Math.PI * u, bigV = 2 * Math.PI * v;
        final double radius = CENTER_RADIUS + Math.cos(bigU / 2) * Math.sin(bigV) - Math.sin(bigU / 2) * Math.sin(2 * bigV);
        final double height = Math.sin(bigU / 2) * Math.sin(bigV) + Math.cos(bigU / 2) * Math.sin(2 * bigV);
        // Y is up in the world
        out.set((float) (SCALE * radius * Math.cos(bigU)), (float) (SCALE * height), (float) (SCALE * radius * Math.sin(bigU)));
    }

    private ModelInstance instance(Model model) {
        models.add(model);
        return new ModelInstance(model);
    }
}
