# libgdx lessons

## Meshes and models
- Mesh indices are unsigned shorts: at most 65536 vertices per mesh. The genus 2 surface is at about 50k at `1/48` cells, so
  a finer grid overflows (`MarchingTetrahedra` throws a clear error).
- `ModelBatch` only draws on `end()`. Different viewports, scissors or cameras need a `begin`/`end` each (this is why
  the gallery has one per thumbnail, and why an IDE warning about a flush in a loop is a false positive there).
- Transparent surfaces: `BlendingAttribute`, `IntAttribute.createCullFace(GL_NONE)`, and
  `new DepthTestAttribute(GL_LEQUAL, false)` so near triangles don't hide far ones. `ModelBatch` sorts blended renderables
  last, back to front, so opaque things drawn inside show through.
- The default shader lights one side only (`max(0, N.L)`), so non-orientable surfaces (Mobius band, Klein bottle) show a
  hard line where the normals flip. A proper fix needs a custom shader (flip the normal for back faces). Not done.
- `MathUtils.sin/cos` are table lookups (about 2e-4 radian of error). For geometry that is built once, use `Math.sin/cos`.
- The vertex normal of a parametric surface is `du x dv`, which can point inward: `ParametricSurface.build` has a
  `flipNormals` option. At poles (an edge of the parameter square collapsed to a point) the normal is taken from just
  inside the square.
- Vertices that are on a grid point exactly produce several vertices at the same place in marching tetrahedra:
  `MarchingTetrahedra.triangulate` welds them. Zero-length edges make mean value weights blow up.
- `Mesh.setVertices` per frame is fine for a few tens of thousands of vertices (`FoldingMesh`).

## Cameras, viewports and HiDPI
- Use `HdpiUtils.glViewport/glScissor` with logical sizes; on a Retina screen the back buffer is larger than the window.
- The main view is drawn in the part of the window to the right of the gallery: set the viewport, and the camera's
  `viewportWidth/Height` to match, on resize. `stage.getViewport().apply()` before drawing the stage resets the viewport.
- Mouse look: `Gdx.input.getDeltaX/Y` is measured from (0, 0) for the first event (a huge bogus jump), and the cursor
  can be re-based when the window is activated. We capture the cursor only while focused, ignore the first movement
  after getting focus, and ignore any single-frame movement bigger than 150 pixels. Poll keys with `isKeyPressed` each
  frame instead of tracking key down/up events (those get stuck).
- Gradle 8 / libgdx 1.12: `Label` with `setWrap(true)` needs its width from the `Table` cell, or it never wraps.

## Text
- `BitmapFont` from a `.fnt` is a picture made for one size: it is blurry when scaled and on HiDPI screens. `CrispFont`
  draws a system font with Java2D at the real pixel size into a texture and scales it down. Java2D must be headless
  (`java.awt.headless=true`) because the application starts with `-XstartOnFirstThread` on macOS. It falls back to the old font.
- Markup (`[RED]text[]`, `[#A5B68D]`) needs `markupEnabled`; write `[[` for a literal bracket. Titles don't use markup.
- To add a character to a font: add a `Glyph` with `setGlyph`, and in libgdx `yoffset = -(height + distanceFromTopOfLine)`.
  `BitmapFontCache` reads the number of pages when it is created, so add pages before creating labels.

## Animation
- Wrap animated parameters (`turns % 1`) so floats don't lose precision, but only if the motion is periodic in that range.
- Avoid allocating vectors in per-frame code; reuse scratch vectors.
