# Title

Bridge GL11 cannot serve a buffer-taking `glGetFloat`: reading `GL_MODELVIEW_MATRIX` / `GL_PROJECTION_MATRIX` throws `UnsupportedOperationException` mid-render

# Body

## Summary

No bridge class implements `glGetFloat(int, FloatBuffer)`. Since the agent rewrites `org/lwjgl/opengl/GL11` to the bridge in every jar, a mod that compiled fine against real LWJGL binds to the bridge at runtime and dies the first time it reads a matrix back - from inside its render pass, so it takes the screen down with it rather than failing at load.

Verified against **v0.8.9** (`fr.jar`, SHA-256 `e669b6dd6b9c1fc4e34b44b40e9e03c19374d8dc46e7820b9e3a4813cbc3208f`, 716583 bytes) on Starsector 0.98a-RC8.

v0.8.9 changed how this surfaces, without changing the gap. `com.genir.renderer.bridge.opengl.GL11` is the new rewrite target and declares LWJGL's whole `glGet*` surface, `glGetFloat(int, FloatBuffer)` included, forwarding the implemented entry points to `com.genir.renderer.bridge.commands.GL11` and throwing `UnsupportedOperationException` for the rest. So the call that used to fail to link now links and throws when called. That is a better failure - it names the method rather than the descriptor, and it cannot be mistaken for a classpath problem - but the matrix still cannot be read, and the pattern below still has no supported form.

This was first written against v0.7.2 and re-read on every release since. One `glGet*` entry point has been added in that whole span - `glGetTexParameteri`, in v0.8.7 - and it is telling: it closed exactly this class of bug for a different pname family, by delegating through `exec.get`. So the gap reported here is a known shape with a known remedy; the matrix reads are simply the ones still missing.

Everything around that surface has meanwhile turned over repeatedly - v0.7.4 moved the bridge from `com.genir.renderer.bridge` to `com.genir.renderer.bridge.commands` and the command interfaces to `com.genir.renderer.bridge.interfaces`; v0.8.0 replaced the system classloader with a Java agent, moving the rewriting itself into a second jar (`fr.agent.jar`, `com.genir.renderer.agent`); v0.8.3 added a compressed-texture path inside `glGetTexImage`; v0.8.4 repacked `VertexInterceptor`'s vertex arrays and moved program tracking from `AttribTracker` to a `ShaderTracker`; v0.8.5rc1 moved that tracking back to `AttribTracker`, reworked context creation, and moved texture loading off the startup path; v0.8.6 rewrote `TextureTracker` to record each texture's bound target; v0.8.7rc1 re-laid-out the frame's packed command arguments; and v0.8.9 added the facade package, merged the agent's transformation tables into one `Transformations` class, moved the compressed-texture read into a `TextureReadManager`, and reworked `Context` and `Executor`. None of that touches which matrix a caller can read back.

## Details

The bridge's entire implemented `glGet*` surface is `glGetInteger(int)`, `glGetInteger(int, IntBuffer)`, `glGetString(int)`, `glGetFloat(int)`, `glGetError()`, `glGetTexLevelParameteri`, `glGetTexParameteri`, and two `glGetTexImage` overloads (`commands/GL11.java`, the `glGet*` block; in the shipped v0.8.9 jar it decompiles to L1228-L1480). Everything else on `opengl/GL11` throws. `glGetFloat` is implemented only in its scalar form, which cannot take a matrix - and it answers `GL_LINE_WIDTH` inline, so the shape for serving a value from tracked state without a stall is already there, and it keeps being reached for. `glIsTexture` took it in v0.8.4 and `glGetTexLevelParameteri`'s three size pnames in v0.8.5rc1: both now answer from a caller-side `TextureTracker` and demote the real GL call to a deferred assertion, turning reads that used to stall into ones that cannot. v0.8.9 applied the same reasoning again in `stall/BufferManager`, serving a mapped buffer range from a CPU-side scratch buffer rather than a synchronous readback. The modelview is the same shape of problem with no such treatment.

The affected pattern is the standard one for turning a cursor into world coordinates:

```java
FloatBuffer modelview = BufferUtils.createFloatBuffer(16);
GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelview);   // UnsupportedOperationException under Fast Rendering
GLU.gluUnProject(mouseX, mouseY, 0f, modelview, projection, viewport, out);
```

Any mod that unprojects the cursor over the campaign map hits this. (`org.lwjgl.util.glu.GLU` is not on the rewrite list, so `gluUnProject` itself is fine - it is only the matrix read that fails.)

## Why a pass-through overload would not fix it

Making the declared `glGetFloat(int, FloatBuffer)` delegate to `org.lwjgl.opengl.GL11` instead of throwing would stop the crash and replace it with something worse.

While `cpuMode` is set, `TransformManager` deliberately keeps GL's modelview at identity and multiplies each vertex by the CPU matrix instead (`TransformManager.setCPUMode` and `getCPUModelView`; `VertexInterceptor.glVertex3f`). `shouldDelegate()` means modelview calls are not forwarded to real GL at all in that mode. So a delegating read hands back identity while the real transform sits in a Java object, and the caller gets sixteen plausible floats that are silently wrong - a mod resolves the wrong point on the map with nothing thrown. The current `UnsupportedOperationException` is at least loud, and the facade's throw-by-default is the right posture for exactly this reason: a declared-but-unserved read should refuse rather than guess.

## Why reading `getCPUModelView()` on the caller thread also does not fix it

The obvious next thought - have the bridge read `getCPUModelView()` directly - is wrong too, for a second, independent reason: the read has to happen on the render thread.

The bridge is deferred and double-buffered. A `glTranslatef` on the caller thread only records a command (`Executor.execute`); the command that actually mutates `TransformManager` runs later, when the frame is replayed on the single-thread `FR-Render` executor (`Executor`, thread named in the `ExecutorFactory.newSingleThreadExecutor` call; commands run in `Executor.executeCommands`). So `TransformManager.modelView` is render-thread state, mutated a frame behind the caller. A caller-thread read of `getCPUModelView()` samples whatever unrelated transform the render thread is replaying at that instant, torn field-by-field as that thread writes it - and, being a real non-identity matrix, it slips past any identity guard. The observed symptom is a cursor unproject that resolves a different, scattered map cell almost every frame.

This is the same asymmetry that makes the existing `GL_VIEWPORT` simulation safe: viewport is read from `attribTracker` on the caller thread (caller-side state), whereas `TransformManager` is executor-side state and cannot be.

## Why a synchronous render-thread read is not open to mods either

The correct-looking answer - run the read on the render thread and block for it, via `Executor.get`/`wait` - reads the right matrix but is closed to mods, because a per-frame stall is fatal. `Executor.wait` calls `StallDetector.detectStall` (`.../bridge/context/Executor.java`, the `wait` method), which counts stalled frames and throws `RuntimeException("Asynchronous pipeline stall")` once a caller stalls on 30 of any 60 frames (`.../bridge/context/stall/StallDetector.java`, `update`/`detectStall`). A cursor unproject on the open map runs every frame, so a synchronous read there stalls 60 of 60 and brings the game down within about a second. A mod cannot read the modelview synchronously every frame at all.

Since v0.8.7 detection is armed at the end of game initialisation, in `ResourceLoader.initEpilogue` - after every mod's `onApplicationLoad`, before the main menu is drawn - so a per-frame stalling read is fatal the first time the campaign map is opened. Through v0.8.7rc1 it was armed on the first combat frame instead (`overrides/CombatEngine.render`), which made the same code survive a menu-and-map session and die on the first battle.

Worth noting in both directions. The v0.8.7 arming point is the better one, and it removes the intermittency that made this hard for mod authors to diagnose. It also means a mod that worked around the missing matrix read with a synchronous `Executor.get` now fails immediately rather than eventually, so the pressure to get the read right has moved earlier - which is part of why an inline answer is worth having.

This is a constraint on mods, not on the bridge itself: the bridge owns the detector, so an in-bridge implementation is free of it.

## Suggested fix

Serve `GL_MODELVIEW_MATRIX` from the tracked state rather than from GL, and - like the existing `GL_VIEWPORT` simulation - answer it inline on the caller thread with no stall. That needs the current CPU modelview mirrored in caller-side state (an `AttribTracker`-style shadow updated as `glTranslatef`/`glLoadMatrix`/`glPushMatrix`/`glPopMatrix` records commands), so the getter can return it without a render-thread round trip:

- `GL_MODELVIEW_MATRIX` -> the caller-side modelview shadow when `cpuMode` is set, otherwise the real GL read. Serving it from `transformManager` on the caller thread instead reintroduces the render-thread race above; serving it through `Executor.get`/`wait` reintroduces the stall.
- Store with `storeTranspose`, not `store`. The bridge's `Matrix4f` fields are row-major (`VertexInterceptor.glVertex3f` takes the translation from `m03/m13/m23`), transposed from what GL and `gluUnProject` expect - which is the same conversion `setGPUMode` already does on the way out.
- `GL_PROJECTION_MATRIX` has no CPU shadow, so a plain delegation is correct for it.

Callers would then get the matrix the vertices are actually drawn with, under both renderers, with no stall and no threading hazard.

## Workaround (for anyone who finds this first)

Read the CPU matrix one frame late, without stalling. `Context.exec.execute(GLCommand)` enqueues a command and returns immediately - no `wait`, no stall - and the command runs on the `FR-Render` thread at your pass's position in the stream, where the matrix is your caller's transform. Have it copy the matrix into a holder you own, and read the *previous* frame's copy.

`GLCommand` is `com.genir.renderer.bridge.interfaces.GLCommand` from v0.7.4 and `com.genir.renderer.bridge.context.commands.GLCommand` before it; its method is `run(Context, float[], int)`, and a command that takes no packed arguments ignores the last two:

```java
// held on your reader, published across the render/game thread boundary
private final AtomicReference<float[]> latest = new AtomicReference<>();

float[] readModelview() {
    Context ctx = ContextManager.getThreadContext();          // null off-thread, or before setup
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

Four caveats: reading `getCPUModelView()` inline on the caller thread instead races the render thread and resolves a wrong point every frame, and a synchronous `Executor.get`/`wait` read stalls the pipeline and gets the game killed by the stall detector - the deferred `execute` hop avoids both and is the point of the workaround; the copy has to complete inside the command, since the matrix it reads is live and mutated again once the command returns; the value is a frame or two stale (the render thread runs a frame behind, and a frame's copy is only guaranteed complete a frame later), invisible for a still map and trailing by a frame or two of pan velocity while panning; and it returns identity when the matrix has been pushed to the GPU instead, so identity has to be read as "this read is not usable" rather than "no transform".
