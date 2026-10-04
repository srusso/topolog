package net.sr89.topology.worlds;

/**
 * The torus as a square with opposite sides glued, a b a⁻¹ b⁻¹. The sides of the square that are glued together have the
 * same color: red (a) and cyan (b). The fundamental group has the two generators, and the relation of going around the square.
 */
public class SquareTorus extends FoldingPolygonWorld {
    public SquareTorus() {
        super(TorusPolygonMeshes.createSquare());
    }

    @Override
    public String getWorldTitle() {
        return "Square -> torus";
    }
}
