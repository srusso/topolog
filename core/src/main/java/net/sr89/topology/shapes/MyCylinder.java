package net.sr89.topology.shapes;

import com.badlogic.gdx.graphics.g3d.ModelInstance;

/**
 *
 * @param cylinder
 * @param index The index of this cylinder in the helix, starting from 0 at the bottom
 */
public record MyCylinder(ModelInstance cylinder, int index) {
}
