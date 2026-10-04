package net.sr89.topology.worlds;

/**
 * The torus as a hexagon with opposite sides glued: a different polygon that gives the same surface. The sides that are
 * glued together have the same color, in three pairs: red, cyan and orange.
 */
public class HexagonTorus extends FoldingPolygonWorld {
    public HexagonTorus() {
        super(TorusPolygonMeshes.createHexagon());
    }

    @Override
    public String getWorldTitle() {
        return "Hexagon -> torus";
    }
}
