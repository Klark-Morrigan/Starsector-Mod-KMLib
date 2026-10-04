# Title

Bridge GL11 cannot serve a buffer-taking `glGetFloat`: reading `GL_MODELVIEW_MATRIX` / `GL_PROJECTION_MATRIX` throws `UnsupportedOperationException` mid-render

# Body

## Resolved in v0.9.1rc1

**v0.9.1rc1** (`fr.jar`, SHA-256 `cea6fc460b8d419449ee4ad78d8e2cbd4ea1ca95527b20558f448d8ef630888a`, 584591 bytes) serves `glGetFloat(GL_MODELVIEW_MATRIX, FloatBuffer)` inline on the caller thread. It answers from a new caller-side `MatrixTracker`, which every modelview call updates as it is issued, display lists included, and it writes with `storeTranspose`.

It needs no `cpuMode` branch, so it is simpler than the fix suggested below. The tracker mirrors the same stack the render thread keeps, and that stack is also what GL holds while a program is bound. So the read never stalls and never answers identity in place of a transform.

Every other pname, `GL_PROJECTION_MATRIX` included, throws `UnsupportedOperationException` with the pname in the message. The projection is not needed on the campaign UI, as the last section explains.

The same release renamed `TransformManager` to `MatrixManager`, and `Context.transformManager` to `Context.matrixManager`. That unbinds the workaround below, which a mod no longer needs on v0.9.1rc1.

The rest of this report is as filed against v0.9.0.

## Summary

No bridge class implements `glGetFloat(int, FloatBuffer)`. Since the agent rewrites `org/lwjgl/opengl/GL11` to the bridge in every jar, a mod that compiled fine against real LWJGL binds to the bridge at runtime and dies the first time it reads a matrix back - from inside its render pass, so it takes the screen down with it rather than failing at load.

Verified against **v0.9.0** (`fr.jar`, SHA-256 `7a41e86fbbc6cb97e6bf723d4f310a9c5c7987d9d6305ce225e122206703754e`, 562976 bytes) on Starsector 0.98a-RC8.

v0.8.9 changed how this surfaces, not the gap: `bridge.opengl.GL11` is now the rewrite target, declares LWJGL's whole `glGet*` surface, forwards what `commands.GL11` implements and throws `UnsupportedOperationException` for the rest, and v0.9.0 gave that throw a message naming the method. A call that used to fail to link now links and throws when called. That is a better failure - it cannot be mistaken for a classpath problem - but the matrix still cannot be read.

This was first written against v0.7.2 and re-read on every release since. Five `glGet*` entry points have been added in that span - `glGetTexParameteri` in v0.8.7; `glGetBufferParameteri`, `glGetActiveUniform`, `glGetActiveAttrib` and `glGetProgramBinary` in v0.9.0 - each closing this class of bug for another pname family through `exec.get` or `exec.wait`. v0.9.0rc2 did the better thing for `GL_SCISSOR_BOX`, serving it inline from `attribTracker`, which is exactly what the fix below asks for the modelview. So the gap is a known shape with two known remedies, both already applied in the same jar; the matrix reads are the ones still missing.

Everything around that surface has turned over in the same span - the bridge has moved packages twice, the rewriting moved into an agent, the shadowed game classes became method patches, `Executor` became an interface - and none of it touched which matrix a caller can read back.

## Details

The bridge's implemented `glGet*` surface is `glGetInteger(int)`, `glGetInteger(int, IntBuffer)`, `glGetString(int)`, `glGetFloat(int)`, `glGetError()`, `glGetTexLevelParameteri`, `glGetTexParameteri` and two `glGetTexImage` overloads (`commands/GL11.java`). Everything else on `opengl/GL11` throws, `glGetDouble(int, DoubleBuffer)` included, so the double-precision form is no way round it. `glGetFloat` exists only in its scalar form, which cannot take a matrix.

The affected pattern is the standard one for turning a cursor into world coordinates:

```java
FloatBuffer modelview = BufferUtils.createFloatBuffer(16);
GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelview);   // UnsupportedOperationException under Fast Rendering
GLU.gluUnProject(mouseX, mouseY, 0f, modelview, projection, viewport, out);
```

Any mod that unprojects the cursor over the campaign map hits this. (`org.lwjgl.util.glu.GLU` is not on the rewrite list, so `gluUnProject` itself is fine - it is only the matrix read that fails.)

## A pass-through overload would return identity

Making the declared `glGetFloat(int, FloatBuffer)` delegate to `org.lwjgl.opengl.GL11` instead of throwing would stop the crash and replace it with something worse.

While `cpuMode` is set, `TransformManager` deliberately keeps GL's modelview at identity and multiplies each vertex by the CPU matrix instead (`TransformManager.setCPUMode` and `getCPUModelView`; `VertexInterceptor.glVertex3f`). `shouldDelegate()` means modelview calls are not forwarded to real GL at all in that mode. So a delegating read hands back identity while the real transform sits in a Java object, and the caller gets sixteen plausible floats that are silently wrong - a mod resolves the wrong point on the map with nothing thrown. The current `UnsupportedOperationException` is at least loud, and the facade's throw-by-default is the right posture for exactly this reason: a declared-but-unserved read should refuse rather than guess.

## Reading `getCPUModelView()` on the caller thread races the render thread

The obvious next thought - have the bridge read `getCPUModelView()` directly - is wrong too, for an independent reason: the read has to happen on the render thread.

The bridge is deferred and double-buffered. A `glTranslatef` on the caller thread only records a command; the command that actually mutates `TransformManager` runs later, when `AsyncExecutor.executeCommands` replays the frame on `FR-Render`. So `TransformManager.modelView` is render-thread state, mutated a frame behind the caller. A caller-thread read of `getCPUModelView()` samples whatever unrelated transform the render thread is replaying at that instant, torn field-by-field as that thread writes it - and, being a real non-identity matrix, it slips past any identity guard. The observed symptom is a cursor unproject that resolves a different, scattered map cell almost every frame.

This is the same asymmetry that makes the existing `GL_VIEWPORT` simulation safe: viewport is read from `attribTracker` on the caller thread (caller-side state), whereas `TransformManager` is executor-side state and cannot be.

## A synchronous render-thread read trips the stall detector

The correct-looking answer - run the read on the render thread and block for it through `Executor.get`/`wait` - reads the right matrix but is closed to mods, because a per-frame stall is fatal. `wait` calls `StallDetector.detectStall`, which throws `RuntimeException("Asynchronous pipeline stall")` once a caller stalls on 30 of any 60 frames. A cursor unproject on the open map runs every frame, so a synchronous read there stalls 60 of 60 and brings the game down within about a second.

Since v0.8.7 detection is armed in `initEpilogue`, after every mod's `onApplicationLoad` and before the main menu is drawn, so a per-frame stalling read is fatal the first time the campaign map is opened. Through v0.8.7rc1 it was armed on the first combat frame, which let the same code survive a menu-and-map session and die on the first battle. The earlier arming point is the better one - it removed the intermittency that made this hard to diagnose - and it also means a mod that worked around the missing read with a synchronous `Executor.get` fails at once rather than eventually, which is part of why an inline answer is worth having.

This is a constraint on mods, not on the bridge itself: the bridge owns the detector, so an in-bridge implementation is free of it.

## Suggested fix

Serve `GL_MODELVIEW_MATRIX` from the tracked state rather than from GL, and - like the existing `GL_VIEWPORT` simulation, and the `GL_SCISSOR_BOX` one v0.9.0rc2 put beside it - answer it inline on the caller thread with no stall. That needs the current CPU modelview mirrored in caller-side state (an `AttribTracker`-style shadow updated as `glTranslatef`/`glLoadMatrix`/`glPushMatrix`/`glPopMatrix` records commands), so the getter can return it without a render-thread round trip:

- `GL_MODELVIEW_MATRIX` -> the caller-side modelview shadow when `cpuMode` is set, otherwise the real GL read. Serving it from `transformManager` on the caller thread instead reintroduces the render-thread race above; serving it through `Executor.get`/`wait` reintroduces the stall.
- Decide "when `cpuMode` is set" on the caller side too. `cpuMode` is render-thread state: it flips as the frame replays, in the `glUseProgram` command and around each `VertexInterceptor` draw, so reading it from the caller thread races just as reading the matrix does. The caller side already tracks the equivalent - `attribTracker.getCurrentProgram()` and the matrix mode - so the shadow can follow `shouldDelegate()`'s rule without touching render-thread state.
- In the other branch, "the real GL read" is the `exec.get` round trip, so it stalls. That is acceptable there, since a bound program is the case where the matrix really does live in GL, but it leaves a per-frame reader that draws under a shader outside the inline path.
- Store with `storeTranspose`, not `store`. The bridge's `Matrix4f` fields are row-major (`VertexInterceptor.glVertex3f` takes the translation from `m03/m13/m23`), transposed from what GL and `gluUnProject` expect - which is the same conversion `setGPUMode` already does on the way out.
- `GL_PROJECTION_MATRIX` has no CPU shadow, so delegating it gives the right answer. Delegation here means `exec.get`, though: the path `glGetFloat(int)` already takes for every pname except `GL_LINE_WIDTH`. So it stalls, and serves a one-off read but not a per-frame one. A per-frame projection read would need a caller-side shadow of its own, fed by the same calls recorded while the tracked matrix mode is `GL_PROJECTION`.

Callers would then get the matrix the vertices are actually drawn with, under both renderers, with no stall and no threading hazard.

## Implemented workaround

This is what my mods ship today, so you can see which internals they lean on until the read is served.

It reads the CPU matrix a frame late, without stalling. Each frame it enqueues a command through `Context.exec.execute(GLCommand)`, which returns immediately. The command runs on `FR-Render` at the pass's own position in the stream, where the matrix is the caller's transform, and copies it into a holder the mod owns. The read returns the copy a *previous* frame's command left there. Condensed into one class (the shipped code splits it so a stock install never loads a bridge type):

```java
// Reached only when GL11.class.getName().startsWith("com.genir.renderer.").
private final AtomicReference<float[]> latestCopy = new AtomicReference<>();
private volatile boolean isBridgeUnavailable;

float[] readModelview() {
    if (isBridgeUnavailable) {
        return null;
    }
    try {
        Context ctx = ContextManager.getThreadContext();      // null before setup or after teardown
        if (ctx == null) {
            return null;
        }
        ctx.exec.execute((c, args, off) -> {                  // runs on FR-Render, no stall
            if (isBridgeUnavailable) {
                return;
            }
            try {
                FloatBuffer buf = BufferUtils.createFloatBuffer(16);
                c.transformManager.getCPUModelView().storeTranspose(buf);  // fields are row-major
                buf.flip();
                float[] out = new float[16];
                buf.get(out);
                latestCopy.set(out);                          // the copy finishes inside the command
            } catch (LinkageError | RuntimeException e) {     // escaping would abandon the frame
                degrade(e);
            }
        });
    } catch (LinkageError | RuntimeException e) {             // the bridge failing on this thread
        degrade(e);
        return null;
    }
    return latestCopy.get();                                  // a prior frame's copy; null at first
}

private void degrade(Throwable cause) {
    isBridgeUnavailable = true;                               // off the bridge for the session
    latestCopy.set(null);
    // logged and reported to the player once
}
```

The caller treats identity as no reading, since a campaign-UI pass is never identity: its base modelview is identity plus `glTranslatef(0.01, 0.01, 0)`. Identity therefore means the matrix was on the GPU at that point in the stream, that is, a program was bound.

What this costs, and why each part is there:

- **A frame or two of lag.** The render thread runs a frame behind, and a frame's copy is only guaranteed complete a frame later. That is invisible on a still map and trails by a frame or two of pan velocity while panning.
- **A guard inside the command.** A command that throws on `FR-Render` abandons the rest of its frame and is re-thrown, wrapped, on the game thread at the next `swapFrames` (`AsyncExecutor.swapFrames`, `rethrowAndClearException`), where no mod frame can catch it. A bug in a hover read would otherwise cost a frame and then the game.
- **A binding to six members that are not API.** `ContextManager.getThreadContext`, `Context.exec`, `Context.transformManager`, `Executor.execute(GLCommand)`, `GLCommand.run` and `TransformManager.getCPUModelView`, plus the `com.genir.renderer.` prefix on the rewritten `GL11` name for detection. One has already moved: `Context.exec` became the `executor.Executor` interface in v0.9.0, and a field links by its type, so the jar had to be rebound. Any further move turns the read off for the session, which is why the guard and the latch are there. Served inline, the read would need none of them.

The projection is not read at all. On the campaign UI it is `glOrtho(0, w, 0, h, -6000, 6000)` over the screen size from `SettingsAPI`, so mods rebuild it arithmetically and pair it with `GL_VIEWPORT`, which the bridge already answers inline. That is why the modelview is the read this report is about.
