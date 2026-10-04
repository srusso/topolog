# Adding a world

1. Create `worlds/MyWorld.java` implementing `World` (`getWorldTitle`, `reposition`, `render`, `dispose`).
   - Own every `Model` you create and dispose of it once (a `List<Model>` is the usual pattern).
   - A short title. The details go in the explanation.
   - Transparent surfaces are rendered last (the batch sorts them), loops and markers are opaque.
2. Add a text to `WorldExplanations` (colors match the ones in the world; `ExplanationText.ball(color)` is a flat dot for a ball;
   `[[` is a literal bracket).
3. Register it in the list in `TopologyApp.create()`, with a `Framing` (a sphere that contains the whole world; the gallery
   thumbnail camera uses it) and its explanation. The position in the list is its number.
   - Keys 1-9 and 0 select the first ten; any number of worlds can be reached with Tab / Shift+Tab / Page Up / Page Down.
4. Check it headlessly (see build-and-verify.md): it builds, `reposition` runs, the geometry is what you claim.
5. Commit it by itself, with a message that says what the world shows.

Shared pieces: `Tubes` (circle, helix), `TubeMesh`, `ParametricSurface`, `MarchingTetrahedra`, `FoldingMesh`, `Flag`, `SegmentTubes`,
`LineMesh`, `GenusTwoSurface` (loops and surface of genus 2 that other worlds reuse).
