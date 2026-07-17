# Title

Bridge GL11 has no buffer-taking `glGetFloat`: reading `GL_MODELVIEW_MATRIX` / `GL_PROJECTION_MATRIX` throws `NoSuchMethodError` mid-render

# Body

## Summary

`com.genir.renderer.bridge.GL11` implements no `glGetFloat(int, FloatBuffer)` overload. Since
the classloader rewrites `org/lwjgl/opengl/GL11` to the bridge in every jar, a mod that
compiled fine against real LWJGL binds to the bridge at runtime and dies with a
`NoSuchMethodError` the first time it reads a matrix back - from inside its render pass, so it
takes the screen down with it rather than failing at load.

Verified against **v0.7.2** (`fr.jar`, SHA-256
`c7f62dbf7511bad1b12d7eeae620030ad432d9de29ccb2c9a65057b9c5081e09`, 548847 bytes) on Starsector
0.98a-RC8, and re-read on current `master` (last pushed 2026-06-05), where the `glGet*` surface
is unchanged.

## Details

The bridge's entire `glGet*` surface is `glGetInteger(int)`, `glGetInteger(int, IntBuffer)`,
`glGetString(int)`, `glGetFloat(int)`, `glGetError()`, `glGetTexLevelParameteri`, and two
`glGetTexImage` overloads
(`modules/renderer/src/com/genir/renderer/bridge/GL11.java`, the `glGet*` block around
L1523-L1732 on master). `glGetFloat` exists only in its scalar form (L1622), which cannot take
a matrix.

The affected pattern is the standard one for turning a cursor into world coordinates:

```java
FloatBuffer modelview = BufferUtils.createFloatBuffer(16);
GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelview);   // NoSuchMethodError under Fast Rendering
GLU.gluUnProject(mouseX, mouseY, 0f, modelview, projection, viewport, out);
```

Any mod that unprojects the cursor over the campaign map hits this. (`org.lwjgl.util.glu.GLU`
is not on the rewrite list, so `gluUnProject` itself is fine - it is only the matrix read that
fails.)

## Why a pass-through overload would not fix it

Adding `glGetFloat(int, FloatBuffer)` that just delegates to `org.lwjgl.opengl.GL11` would stop
the crash and replace it with something worse.

While `cpuModelView` is set, `TransformManager` deliberately keeps GL's modelview at identity
and multiplies each vertex by the CPU matrix instead
(`TransformManager.setCPUModelView` L22-32 and `getCPUModelView` L47-53;
`VertexInterceptor.glVertex3f`). `shouldDelegate()` (L154-156) means modelview calls are not
forwarded to real GL at all in that mode. So a delegating read hands back identity while the
real transform sits in a Java object, and the caller gets sixteen plausible floats that are
silently wrong - a mod resolves the wrong point on the map with nothing thrown. The current
`NoSuchMethodError` is at least loud.

## Suggested fix

Serve `GL_MODELVIEW_MATRIX` from the tracked state rather than from GL, the same way
`glGetInteger(int, IntBuffer)` already simulates `GL_VIEWPORT` from `attribTracker` (L1569-L1575)
instead of passing it through:

- `GL_MODELVIEW_MATRIX` -> `transformManager.getCPUModelView()` when `cpuModelView` is set,
  otherwise the real GL read.
- Store with `storeTranspose`, not `store`. The bridge's `Matrix4f` fields are row-major
  (`VertexInterceptor.glVertex3f` takes the translation from `m03/m13/m23`), transposed from what
  GL and `gluUnProject` expect - which is the same conversion `setGPUModelView` (L41) already does
  on the way out.
- `GL_PROJECTION_MATRIX` has no CPU shadow, so a plain delegation is correct for it.

Callers would then get the matrix the vertices are actually drawn with, under both renderers.

## Workaround (for anyone who finds this first)

`ContextManager.getThreadContext().transformManager.getCPUModelView()` is public and reachable.
Two caveats: it returns the live mutable matrix, so copy it before holding it; and it returns
identity when the matrix has been pushed to the GPU instead, so identity has to be read as "this
read is not usable" rather than "no transform". Transpose on the way out, as above.
