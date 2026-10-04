# Topology and meshes: how things are built, and what was checked

Shapes (`shapes/`), their maths (`math/`), and the worlds (`worlds/`).

- **Tubes** (`TubeMesh`): one mesh per curve, rings along a `Curve`, with parallel transport so rings don't twist. For closed
  curves that aren't planar the carried frame doesn't come back to where it started, so the twist is spread over all the rings.
  The tangent is a central difference, so curves must be defined a little outside [0, 1] (wrap periodic ones).
  `SegmentTubes` makes many straight tubes in one mesh; `LineMesh` makes 1 pixel lines (almost invisible on HiDPI).
- **Parametric surfaces** (`ParametricSurface`): a grid of quads; seams are not welded, so normals can differ across them.
- **Implicit surfaces** (`MarchingTetrahedra`): marching tetrahedra on a grid (6 tetrahedra per cube around the diagonal 0-7), no lookup
  tables; always closed and consistent. Genus 2 is `(x^2(1-x^2) - y^2)^2 + z^2 = r^2`; the neck is the plane x = 0.
- **Folding** (`FoldingMesh`): flat triangles with a folded position per vertex, linearly interpolated. The square and hexagon
  have formulas (torus coordinates; for the hexagon, the coordinates in the lattice of translations that glue opposite sides).
- **Octagon -> genus 2** (`SurfaceOctagon`, `GenusTwoOctagon`): no simple formula exists. We cut the genus 2 mesh open along four
  loops through one point (the top of the crossing) and flatten the disc onto a regular octagon with mean value
  coordinates (a Tutte embedding). What mattered:
  - The base needs at least 8 edges: remove the triangles around it and re-fan from a new vertex.
  - The loops must leave the base in the order b2, a2, b1, a1, b1, a1, b2, a2 (by angle) for the result to be a disc.
  - Loops are chains of shortest paths through waypoints; shortcut detours (otherwise whole regions get squeezed onto the boundary).
  - Successive over-relaxation: factor 1.5 converges in about 4000 sweeps; 1.8 diverges (the weights are not symmetric).
  - The flattened triangles come out with a few hundred of zero area (a vertex whose neighbours are all on one side). That is harmless.
  - A first attempt, an analytic map from two pentagons to punctured tori, was topologically right but had many flipped
    and intersecting triangles that did not go away with resolution.
- **Boy's surface** (`ProjectivePlane`): Apery's parametrization (MathWorld); its domain here is `u in [0, pi], v in [-pi/2, pi/2]`,
  with `(0, v) ~ (pi, -v)` and both pole rows collapsed to one point. Verified: immersion off the poles, one triple point
  with 3-fold symmetry, quotient Euler characteristic 1. The poles are not on the self-intersection set.
- **Non-orientable flags** (`Flag`): a ball on the surface plus a small one over it along the normal. Moving it around an
  orientation-reversing loop, the normal has flipped by the end; to animate over two laps without jumps, flip the normal
  on the second lap.
- In the Klein bottle (figure eight) the curves v = 0 and v = 1/2 are the same curve (the self-intersection): don't put loops or
  markers on it.
- Surfaces in the figure-eight genus 2 equation are in the XY plane (Z thin) and are laid flat for the world (`toWorld`).
  Which side faces the default camera (it looks from +z) matters: loops can end up on the back.
