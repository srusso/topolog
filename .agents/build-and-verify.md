# Building, and checking things without a screen

## Building
- `./gradlew compileJava -q --offline` compiles. `./gradlew :lwjgl3:jar` also checks the desktop module.
- **Gradle 8.8 cannot run on Java 25.** Run it with an older JDK, for instance
  `JAVA_HOME=<a JDK 22 or older> ./gradlew ...`. It fails with "Unsupported class file major version 69" when a build
  script has to be recompiled (so it can look fine until you change a `build.gradle`).
- The project targets Java 17: don't use newer library methods (`Math.clamp` is Java 21; use `MathUtils.clamp`).
- When you run your own test programs, compile and run them with the same JDK. A class compiled by a newer
  JDK than the one running it fails with `UnsupportedClassVersionError`. (If `JAVA_HOME` is set to an old JDK for Gradle,
  run your own `java` without it, e.g. `env -u JAVA_HOME java ...`, or use the same one for both.)
- You can't open the application window from an agent session. Do not claim that something looks right.

## Checking geometry headlessly
We check meshes and animations with small throwaway programs (put them in the scratchpad, not in the repo):
- Compile against `core/build/classes/java/main` and the libgdx jars in `~/.gradle/caches`
  (`gdx`, `gdx-platform` natives, `gdx-jnigen-loader`, and `gdx-backend-lwjgl3` if you need `Lwjgl3Files`).
- Call `GdxNativesLoader.load()`, and stub the GL: `Gdx.gl`, `Gdx.gl20` as a `java.lang.reflect.Proxy` of `GL20` and
  `GL30` returning 1 for ints, `Gdx.app` (with working `hashCode`/`equals`, since meshes are kept in a hash map), and
  `Gdx.graphics` for fonts. Then `Model`s and `Mesh`es can be built and read back (`mesh.getVertices`, `getIndices`).
  Mind that vertex layout order is Position, ColorUnpacked, Normal (use `getVertexAttribute(...).offset`).
- Things worth asserting:
  - Euler characteristic of the mesh, with vertices identified the way the topology says (not by position: a
    triple point or a degenerate grid point merges vertices that should stay apart). A torus is 0, genus 2 is -2,
    the projective plane is 1, the Klein bottle 0.
  - Every edge has exactly two triangles (closed surface), triangles face the same way as their normals.
  - Curves close (gap about 1e-6), lie on the surface (evaluate the equation), and don't jump between frames.
  - Glued vertices coincide when a polygon is folded; flat triangles have positive area, and their areas add up
    to the area of the polygon.
  - Bounding boxes against the `Framing` sphere used for the gallery thumbnails.
- For text, lay it out with `GlyphLayout` and check for stray markup characters; you can also composite glyphs
  into a PNG and look at it (the image viewer tool can open PNGs).
- Time the startup cost of a world: several worlds build meshes of 50 thousand vertices.
