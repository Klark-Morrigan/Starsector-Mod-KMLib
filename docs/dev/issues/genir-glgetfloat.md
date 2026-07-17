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

## Why reading `getCPUModelView()` on the caller thread also does not fix it

The obvious next thought - have the bridge read `getCPUModelView()` directly - is wrong too, for
a second, independent reason: the read has to happen on the render thread.

The bridge is deferred and double-buffered. A `glTranslatef` on the caller thread only records a
command (`Executor.execute`); the command that actually mutates `TransformManager` runs later,
when the frame is replayed on the single-thread `FR-Render` executor (`Executor`, thread named in
the `ExecutorFactory.newSingleThreadExecutor` call; commands run in `Executor.executeCommands`).
So `TransformManager.modelView` is render-thread state, mutated a frame behind the caller. A
caller-thread read of `getCPUModelView()` samples whatever unrelated transform the render thread
is replaying at that instant, torn field-by-field as that thread writes it - and, being a real
non-identity matrix, it slips past any identity guard. The observed symptom is a cursor unproject
that resolves a different, scattered map cell almost every frame.

This is the same asymmetry that makes the existing `GL_VIEWPORT` simulation safe: viewport is read
from `attribTracker` on the caller thread (caller-side state), whereas `TransformManager` is
executor-side state and cannot be.

## Suggested fix

Serve `GL_MODELVIEW_MATRIX` from the tracked state rather than from GL, but read it on the render
thread so the value is this caller's transform and not a torn mid-flight one - the read has to be
run as a command through the executor (`Executor.get`/`wait`), the way the bridge's own
render-thread readbacks already work, not answered inline like the `GL_VIEWPORT` simulation:

- `GL_MODELVIEW_MATRIX` -> run a getter on the executor that returns
  `transformManager.getCPUModelView()` when `cpuModelView` is set, otherwise the real GL read.
  Answering it inline on the caller thread reintroduces the race above.
- Copy (and transpose) inside that getter, before it returns - once control leaves the render
  thread the matrix is mutated again.
- Store with `storeTranspose`, not `store`. The bridge's `Matrix4f` fields are row-major
  (`VertexInterceptor.glVertex3f` takes the translation from `m03/m13/m23`), transposed from what
  GL and `gluUnProject` expect - which is the same conversion `setGPUModelView` (L41) already does
  on the way out.
- `GL_PROJECTION_MATRIX` has no CPU shadow, so a plain delegation is correct for it.

Callers would then get the matrix the vertices are actually drawn with, under both renderers.

## Workaround (for anyone who finds this first)

Read the CPU matrix, but do it on the render thread - not inline. `Context.exec.get(GLGetter)` is
public and runs the getter in-band on the `FR-Render` thread at your pass's position in the command
stream, so the matrix is your caller's transform and stable while you copy it:

```java
Context ctx = ContextManager.getThreadContext();          // null on an unregistered thread
float[] modelview = ctx.exec.get(c -> {
    Matrix4f m = c.transformManager.getCPUModelView();
    FloatBuffer buf = BufferUtils.createFloatBuffer(16);
    m.storeTranspose(buf);                                 // fields are row-major; see above
    buf.flip();
    float[] out = new float[16];
    buf.get(out);
    return out;                                            // copy MUST finish inside the getter
});
```

Three caveats: reading `getCPUModelView()` inline on the caller thread instead races the render
thread and resolves a wrong point every frame (see above), so the `exec.get` hop is the point of
the workaround, not an optimisation; the matrix it hands out is live and mutable, so the copy has
to complete inside the getter; and it returns identity when the matrix has been pushed to the GPU
instead, so identity has to be read as "this read is not usable" rather than "no transform".
`get` blocks until the frame drains (one pipeline stall), which is why this belongs on a map pass,
not a hot loop.
