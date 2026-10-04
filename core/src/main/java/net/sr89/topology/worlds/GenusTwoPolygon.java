package net.sr89.topology.worlds;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import net.sr89.topology.HexColors;
import net.sr89.topology.shapes.FoldingMesh;

/**
 * The octagon a₁ b₁ a₁⁻¹ b₁⁻¹ a₂ b₂ a₂⁻¹ b₂⁻¹ with its sides glued, which folds up into the genus 2 surface.
 * <p>
 * Sides of the octagon that are glued together have the same color. The octagon is cut open along the colored sides until
 * it is folded all the way, where the two sides of each color come together. The fundamental group has one generator
 * for each pair of glued sides (all the corners are identified to the same point), and one relation: going around
 * the octagon, [a₁, b₁][a₂, b₂] = 1.
 * <p>
 * Unlike the square of {@link TorusPolygons}, there is no simple formula for how the octagon folds up: the genus 2
 * surface is cut open along four loops and flattened by computer, see {@link GenusTwoOctagon}.
 */
public class GenusTwoPolygon implements World {
    private static final float OCTAGON_RADIUS = 2.4f;
    /** How wide the colored band along the glued sides is. */
    private static final float EDGE_BAND = 0.09f;

    private final FoldingMesh octagon = GenusTwoOctagon.create(OCTAGON_RADIUS, EDGE_BAND, HexColors.greenPastel());

    private float time = 0f;

    public GenusTwoPolygon() {
        reposition(0f);
    }

    @Override
    public String getWorldTitle() {
        return "Octagon -> genus 2 surface";
    }

    @Override
    public void reposition(float deltaTime) {
        time = (time + deltaTime) % FoldTiming.CYCLE;
        octagon.setFold(FoldTiming.amount(time));
    }

    @Override
    public void render(ModelBatch modelBatch, Environment environment) {
        octagon.render(modelBatch, environment);
    }

    @Override
    public void dispose() {
        octagon.dispose();
    }
}
