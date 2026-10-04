package net.sr89.topology.worlds;

import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import net.sr89.topology.shapes.FoldingMesh;

/**
 * A world with a polygon that folds up into a surface, stays folded, unfolds and stays flat, again and again.
 * The polygon is cut open along the sides that are glued, which have the same color in pairs.
 */
abstract class FoldingPolygonWorld implements World {
    private final FoldingMesh polygon;
    private float time = 0f;

    protected FoldingPolygonWorld(FoldingMesh polygon) {
        this.polygon = polygon;
        reposition(0f);
    }

    @Override
    public void reposition(float deltaTime) {
        time = (time + deltaTime) % FoldTiming.CYCLE;
        polygon.setFold(FoldTiming.amount(time));
    }

    @Override
    public void render(ModelBatch modelBatch, Environment environment) {
        polygon.render(modelBatch, environment);
    }

    @Override
    public void dispose() {
        polygon.dispose();
    }
}
