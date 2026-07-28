# Title

Bridge GL11 has no buffer-taking `glGetFloat`: reading `GL_MODELVIEW_MATRIX` / `GL_PROJECTION_MATRIX` throws `NoSuchMethodError` mid-render

# Body

## Summary

`com.genir.renderer.bridge.commands.GL11` implements no `glGetFloat(int, FloatBuffer)` overload.
Since the classloader rewrites `org/lwjgl/opengl/GL11` to the bridge in every jar, a mod that
compiled fine against real LWJGL binds to the bridge at runtime and dies with a
`NoSuchMethodError` the first time it reads a matrix back - from inside its render pass, so it
takes the screen down with it rather than failing at load.

Verified against **v0.7.6** (`fr.jar`, SHA-256
`f8b00d3bef7d5ad0cf59e74c23d2d045c4a5e4c8c2b62f3ed11e41597f194cae`, 632557 bytes) on Starsector
0.98a-RC8. It was first written against v0.7.2 and re-read on every release since: the `glGet*`
surface has not changed across them, though the bridge package has (v0.7.4 moved it from
`com.genir.renderer.bridge` to `com.genir.renderer.bridge.commands`, and the command interfaces
to `com.genir.renderer.bridge.interfaces`).

## Details

The bridge's entire `glGet*` surface is `glGetInteger(int)`, `glGetInteger(int, IntBuffer)`,
`glGetString(int)`, `glGetFloat(int)`, `glGetError()`, `glGetTexLevelParameteri`, and two
`glGetTexImage` overloads (`GL11.java`, the `glGet*` block; in the shipped v0.7.6 jar it
decompiles to L1264-L1462). `glGetFloat` exists only in its scalar form, which cannot take a
matrix - and it answers `GL_LINE_WIDTH` inline, so the shape for serving a value from tracked
state without a stall is already there.

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

While `cpuMode` is set, `TransformManager` deliberately keeps GL's modelview at identity
and multiplies each vertex by the CPU matrix instead
(`TransformManager.setCPUMode` and `getCPUModelView`; `VertexInterceptor.glVertex3f`).
`shouldDelegate()` means modelview calls are not forwarded to real GL at all in that mode.
So a delegating read hands back identity while the
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

## Why a synchronous render-thread read is not open to mods either

The correct-looking answer - run the read on the render thread and block for it, via
`Executor.get`/`wait` - reads the right matrix but is closed to mods, because a per-frame stall is
fatal. `Executor.wait` calls `StallDetector.detectStall`
(`.../bridge/context/Executor.java`, the `wait` method), which counts stalled frames and throws
`RuntimeException("Asynchronous pipeline stall")` once a caller stalls on 30 of any 60 frames
(`.../bridge/context/stall/StallDetector.java`, `update`/`detectStall`). A cursor unproject on the
open map runs every frame, so a synchronous read there stalls 60 of 60 and brings the game down
within about a second. A mod cannot read the modelview synchronously every frame at all.

Detection is armed on the first `CombatEngine` construction (`overrides/CombatEngine.java`), so a
stalling read is silently tolerated until then. That makes the failure look intermittent to mod
authors: the same code can survive a menu-and-map session and die on the first battle.

This is a constraint on mods, not on the bridge itself: the bridge owns the detector, so an
in-bridge implementation is free of it.

## Suggested fix

Serve `GL_MODELVIEW_MATRIX` from the tracked state rather than from GL, and - like the existing
`GL_VIEWPORT` simulation - answer it inline on the caller thread with no stall. That needs the
current CPU modelview mirrored in caller-side state (an `AttribTracker`-style shadow updated as
`glTranslatef`/`glLoadMatrix`/`glPushMatrix`/`glPopMatrix` records commands), so the getter can
return it without a render-thread round trip:

- `GL_MODELVIEW_MATRIX` -> the caller-side modelview shadow when `cpuMode` is set, otherwise
  the real GL read. Serving it from `transformManager` on the caller thread instead reintroduces
  the render-thread race above; serving it through `Executor.get`/`wait` reintroduces the stall.
- Store with `storeTranspose`, not `store`. The bridge's `Matrix4f` fields are row-major
  (`VertexInterceptor.glVertex3f` takes the translation from `m03/m13/m23`), transposed from what
  GL and `gluUnProject` expect - which is the same conversion `setGPUMode` already does on the
  way out.
- `GL_PROJECTION_MATRIX` has no CPU shadow, so a plain delegation is correct for it.

Callers would then get the matrix the vertices are actually drawn with, under both renderers, with
no stall and no threading hazard.

## Workaround (for anyone who finds this first)

Read the CPU matrix one frame late, without stalling. `Context.exec.execute(GLCommand)` enqueues a
command and returns immediately - no `wait`, no stall - and the command runs on the `FR-Render`
thread at your pass's position in the stream, where the matrix is your caller's transform. Have it
copy the matrix into a holder you own, and read the *previous* frame's copy.

`GLCommand` is `com.genir.renderer.bridge.interfaces.GLCommand` from v0.7.4 and
`com.genir.renderer.bridge.context.commands.GLCommand` before it; its method is
`run(Context, float[], int)`, and a command that takes no packed arguments ignores the last two:

```java
// held on your reader, published across the render/game thread boundary
private final AtomicReference<float[]> latest = new AtomicReference<>();

float[] readModelview() {
    Context ctx = ContextManager.getThreadContext();          // null on an unregistered thread
    if (ctx == null) {
        return null;
    }
    ctx.exec.execute((c, args, off) -> {                      // runs on FR-Render, no stall
        Matrix4f m = c.transformManager.getCPUModelView();
        FloatBuffer buf = BufferUtils.createFloatBuffer(16);
        m.storeTranspose(buf);                                // fields are row-major; see above
        buf.flip();
        float[] out = new float[16];
        buf.get(out);
        latest.set(out);                                      // copy MUST finish inside the command
    });
    return latest.get();                                      // previous frame's copy; null at first
}
```

Four caveats: reading `getCPUModelView()` inline on the caller thread instead races the render
thread and resolves a wrong point every frame, and a synchronous `Executor.get`/`wait` read stalls
the pipeline and gets the game killed by the stall detector - the deferred `execute` hop avoids
both and is the point of the workaround; the copy has to complete inside the command, since the
matrix it reads is live and mutated again once the command returns; the value is a frame or two
stale (the render thread runs a frame behind, and a frame's copy is only guaranteed complete a
frame later), invisible for a still map and trailing by a frame or two of pan velocity while
panning; and it returns identity when the matrix has been pushed to the GPU instead, so identity
has to be read as "this read is not usable" rather than "no transform".
