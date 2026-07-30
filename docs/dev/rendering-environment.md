# Rendering Environment

Facts about the GL substrate KM mods draw through: how the game sets up its
matrices, and how the Fast Rendering mod changes what GL calls mean. None of it
is recoverable from KM source. It was read out of decompiled game and mod jars,
and it is the context behind why KMLib's map and UI code is shaped the way it
is.

Consumer repositories (KMU, KMO, ...) should link here rather than restate any
of it.

## Index

- [Versions this was verified against](#versions-this-was-verified-against)
- [How to re-verify](#how-to-re-verify)
- [Fast Rendering](#fast-rendering)
  - [It is an install patch, not a mod](#it-is-an-install-patch-not-a-mod)
  - [It rewrites GL class references in every jar](#it-rewrites-gl-class-references-in-every-jar)
  - [It tracks the modelview on the CPU](#it-tracks-the-modelview-on-the-cpu)
  - [It defers every GL call to a render thread](#it-defers-every-gl-call-to-a-render-thread)
  - [What the GL11 bridge can and cannot read back](#what-the-gl11-bridge-can-and-cannot-read-back)
  - [Its reach goes past the GL bridge](#its-reach-goes-past-the-gl-bridge)
- [The campaign UI's GL setup](#the-campaign-uis-gl-setup)
  - [Projection, viewport, and two coordinate spaces](#projection-viewport-and-two-coordinate-spaces)
  - [The modelview around a map render](#the-modelview-around-a-map-render)
  - [Map pan and zoom state](#map-pan-and-zoom-state)
- [Traps](#traps)

## Versions this was verified against

Everything here was read out of these exact artifacts. Treat the page as unproven
against anything else.

| What | Version | Identity |
| --- | --- | --- |
| Starsector | `0.98a-RC8` | - |
| Fast Rendering | `v0.7.7` | `fr.jar` SHA-256 `3162638435a2538e3b3a8457a010c1abac370f1477a256fbdc4beeffdf71f021`, 632640 bytes |

Fast Rendering names itself only coarsely. `fr.jar` carries no manifest,
`com.genir` holds no version constant, and the `fr.readme.txt` beside it names no
release. What it does carry is a display string in its shadowed copy of the
game's own version class - `"Starsector 0.98a-RC8 FR7.7"`
(`starsector-core/fr/com/fs/starfarer/Version.java:27`, `:42`), which is what
puts `FR7.7` on the launcher and main menu. That is a two-component release
number baked into a string literal, so it identifies a minor line and not a
build: it cannot separate `v0.7.1` from `v0.7.1b`, and it changes only when
genir bumps the literal. So the version above was established by hashing
`fr.jar` against the release assets, and the SHA-256 is what actually identifies
the bytes these citations were read from.

Releases are published at
[Halke1986/starsector-render](https://github.com/Halke1986/starsector-render/releases),
which ships `fast-rendering-<version>.zip` with `fr.jar` inside. To identify an
arbitrary install, hash its jar and compare:

```
sha256sum "<starsector>/starsector-core/fr.jar"
```

Sizes still separate neighbouring releases (`v0.7.4` is 555210 bytes, `v0.7.5` is
617776, `v0.7.6` is 632557, `v0.7.7` is 632640), so a size mismatch is a fast
first check before hashing. Treat only the mismatch as informative: `v0.7.6` and
`v0.7.7` are 83 bytes apart, close enough that a size *match* is weak evidence
and hashing is what settles it.

Names below are release-specific, and a rename is not announced. `v0.7.4` moved
the whole GL bridge from `com.genir.renderer.bridge` to
`com.genir.renderer.bridge.commands`, and the command interfaces to
`com.genir.renderer.bridge.interfaces`, with nothing in that release's notes to
say so. Every KM fact that names a bridge type - the detection string, the
compile-only stubs - is therefore a fact about one range of releases, and the
hash is what says which.

## How to re-verify

Every claim below carries a `file:line` into the decompiled sources cache at
`<starsector>\.sources-cache\`, whose layout mirrors the relative JAR path under
the game install. Regenerate it with the `/jar-search` command.

Re-verify rather than trust this page. Line numbers drift on any re-decompile
even when nothing changed, so the surrounding symbol names are the durable part
of a citation and the line number is only a hint.

The two version dependencies rot differently, and it is worth knowing which is
which before relying on a fact:

- **Starsector facts** (the campaign UI's GL setup) are stable across patch
  releases but sit behind obfuscated names that are renamed every build. Expect
  the symbols to move, not the behaviour.
- **Fast Rendering facts** are the volatile half. They describe mod internals
  that are not published API, carry no compatibility promise, and can change in
  any release. A hash mismatch means every Fast Rendering claim below is
  unverified until re-read.

## Fast Rendering

Fast Rendering (`com.genir.renderer`, by genir) replaces the game's GL calls with
a batching, deferred renderer. It is widely installed, so KM code that touches GL
has to work under it and under stock LWJGL both.

### It is an install patch, not a mod

It lives at `starsector-core\fr.jar` beside its own launcher, `fr.bat`, and its
own JVM argument file, `fr.vmparams`. There is no folder under `mods\`, so
searching `mods\` for it finds nothing and its presence is not declared in any
`mod_info.json`.

Nothing on disk is rewritten. `fr.vmparams` sets
`-Djava.system.class.loader=com.genir.renderer.loaders.AppClassLoader`, so the
patch is the system classloader, applied to bytes on their way into the JVM; the
game's own jars are read unmodified. Two consequences worth holding onto: the
install is only patched when the player launches `fr.bat` rather than
`starsector.exe`, so the same install runs stock or patched depending on which
one was double-clicked, and a `starfarer.api.vanilla.jar` sitting in a core
directory is somebody else's doing, not Fast Rendering's.

`fr.bat` also passes `-javaagent:PatchLibAgent.jar` when that file is present, and
`AppClassLoader` routes an agent's classes back through its own transformer
(`.../loaders/AppClassLoader.java:112-124`, `:126-148`), which is how a
PatchLib-based mod and Fast Rendering coexist.

### It rewrites GL class references in every jar

Its classloader rewrites constant-pool class entries, so a reference compiled
against LWJGL resolves to the bridge at runtime. `org/lwjgl/opengl/GL11` becomes
`com/genir/renderer/bridge/commands/GL11`, and the same holds for `GL13`, `GL14`,
`GL15`, `GL20`, `GL30`-`GL33`, `GL40`-`GL44`, `Display`, `GLContext`, `GLSync`,
`SharedDrawable`, and `java/net/URLClassLoader`
(`ScriptTransformations.transformations`,
`starsector-core/fr/com/genir/renderer/loaders/ScriptTransformations.java:9`).

That list is the one applied to mod jars. The game's own classes get a narrower
but deeper rewrite, selected by package prefix - `com.fs.`, `sound.`,
`zzz.com.fs.` take the game transformer, `org.lwjgl.util.glu.` and
`com.thoughtworks.xstream.` take their own
(`.../loaders/AppClassLoader.java:95-110`) - which redirects `GL11`, `GL14`,
`Display`, `GLContext`, and `org/lwjgl/util/Display`, swaps janino's
`JavaSourceClassLoader` for a plain `ClassLoader`, and renames the display-list
calls `glGenLists` / `glNewList` / `glEndList` / `glCallList` to `*_restricted`
variants (`.../loaders/AppClassLoader.java:23`).

This applies to KM jars. It is blunt and total: there is no opt-out, no
annotation, and no per-mod exclusion. A KM jar compiles against real LWJGL and
links fine, then binds to the bridge at runtime, so a bridge gap surfaces as a
`NoSuchMethodError` from inside a render pass rather than as a build failure.

Two useful consequences:

- `org.lwjgl.util.glu.GLU` is **not** redirected: a mod's call to `gluUnProject`
  still lands on the real GLU class, which is plain matrix arithmetic and touches
  no GL, so it behaves identically under both renderers. (GLU's own body is
  transformed as it loads, but only to point its internal GL calls at the bridge.)
- The rewrite is the cheapest detection there is. Because a KM jar's own class
  constants are rewritten too, `GL11.class.getName()` reports a
  `com.genir.renderer.` name under Fast Rendering. That needs no reflection and no
  `Class.forName`, and it tests the condition that actually matters: that this
  jar's GL references were redirected. Match the **package prefix**, not the whole
  name - the exact name was `com.genir.renderer.bridge.GL11` through `v0.7.3` and
  is `com.genir.renderer.bridge.commands.GL11` from `v0.7.4`, and a full-name
  comparison silently reports "stock" on the releases it does not know, which is
  the worst of the three answers because it routes callers into GL reads the
  bridge cannot serve.

### It tracks the modelview on the CPU

This is the fact that breaks naive GL code, and it is a design choice, not a bug.

`VertexInterceptor.glVertex3f` multiplies each vertex by
`TransformManager.getCPUModelView()` before submitting it
(`.../bridge/context/VertexInterceptor.java:108-112`), and in that mode
`TransformManager.setCPUMode()` loads **identity** into GL
(`.../bridge/context/TransformManager.java:25-32`).

So while Fast Rendering is in CPU mode, GL's modelview does not describe what is
being drawn. Reading `GL_MODELVIEW_MATRIX` back would return identity while the
real transform sits in a Java object. A crash on the read is the polite failure;
a pass-through implementation returning identity would be the impolite one,
because it yields silently wrong coordinates instead.

The matrix is reachable, publicly: `ContextManager.getThreadContext()` is public
static (`.../bridge/context/ContextManager.java:16`), `Context.transformManager`
is a public final field (`.../bridge/context/Context.java:32`), and
`getCPUModelView()` is public (`.../bridge/context/TransformManager.java:44`).

Four cautions on using it. The first is where, not what, and it dwarfs the rest:
the matrix cannot be read correctly from the calling thread at all, because the
bridge mutates it on a separate render thread a step behind. That is its own
section below ([It defers every GL call to a render
thread](#it-defers-every-gl-call-to-a-render-thread)); the remaining three assume
the read already runs there.

It returns the live mutable matrix, not a copy, so callers must copy before
holding it. And it returns identity when Fast Rendering has instead pushed the
matrix to the GPU (`.../bridge/context/TransformManager.java:44-49`), so identity
means "this read is not usable", not "no transform". That is safe to lean on
because a real campaign-UI pass is never identity (see
[The modelview around a map render](#the-modelview-around-a-map-render)).

The third is the quiet one: **its `Matrix4f` fields are row-major**, transposed
from the convention LWJGL's own `Matrix4f` uses. `VertexInterceptor.glVertex3f`
takes the translation from `m03/m13/m23`
(`.../bridge/context/VertexInterceptor.java:110-112`), and `MatrixStack` writes it
there too (`.../bridge/context/MatrixStack.java:52-55`), so the first index is the
row. LWJGL's own methods read the same fields as `m<col><row>` and put a
translation in `m30/m31/m32`.

So `getCPUModelView().store(buffer)` yields the matrix **transposed** relative to
what GL and `gluUnProject` expect; `storeTranspose` is what gives the column-major
layout. That is not a correction but the same conversion Fast Rendering itself
does when it hands the matrix to GL
(`TransformManager.setGPUMode`, `.../bridge/context/TransformManager.java:39-40`),
and it reads back with `loadTranspose` on the way in
(`MatrixStack.glLoadMatrix`, `.../bridge/context/MatrixStack.java:150`).

This is worth care because it fails silently: a transposed modelview is still
sixteen plausible floats, so nothing throws and a map overlay just resolves the
wrong point. For a translate-only map pass the pan lands in slots 3/7 instead of
12/13.

None of this is published API. It is mod internals, and a genir refactor can
break any of it, so code reading it should fail safe rather than assume.

### It defers every GL call to a render thread

The reason a naive `getCPUModelView()` read is not just transposed but flatly
wrong: it reads the matrix from the wrong thread, at the wrong time.

The bridge is a deferred, double-buffered renderer. A `GL11.glTranslatef` on the
game thread does not touch the modelview - it appends a command to a frame buffer
(`.../bridge/commands/GL11.java:405-407`), and `Executor.execute` only records it
(`.../bridge/context/Executor.java:30-34`). The command that actually mutates
`TransformManager` runs later, when the frame is replayed on a dedicated
single-thread executor named `FR-Render`
(`.../bridge/context/Executor.java:24`, `:149-159`). The same holds for
`glPushMatrix`, `glPopMatrix`, `glLoadIdentity`, `glScalef` and the rest: all
enqueue, none mutate inline.

So `TransformManager` is render-thread state, mutated roughly a frame behind the
game thread that enqueues the calls. A KM overlay's `renderOnMap` runs on the
game thread; reading `getCPUModelView()` directly from there samples whatever
unrelated transform the render thread happens to be replaying at that instant,
and reads it field-by-field while that thread writes it - a torn read of a matrix
that was never the map's. The identity guard cannot catch it, because the sample
is a real non-identity transform belonging to some other draw. The failure is a
confident wrong point every frame, not an absent one.

This is the asymmetry that makes the viewport safe but the modelview not. The
viewport read (`glGetInteger(int, IntBuffer)`) is answered synchronously from
`context.attribTracker` on the calling thread
(`.../bridge/commands/GL11.java:1311-1318`), so it is caller-side state and reads
true from anywhere. `TransformManager` is executor-side state, so it does not.

A synchronous readback reads the right matrix but is a trap of its own. The
executor can run a read in-band and return it - `Executor.get(GLGetter)` submits a
callback and blocks for the result (`.../bridge/context/Executor.java:70-76`),
which runs it on the render thread at the caller's own stream position, exactly
where the modelview is the map's. But `get` routes through `Executor.wait`, which
swaps frames and blocks until the queue drains - a **pipeline stall** - and the
bridge actively punishes stalling. `Executor.wait` calls `StallDetector.detectStall`
(`.../bridge/context/Executor.java:83`), which counts stalled frames and **throws
`RuntimeException("Asynchronous pipeline stall")` once a caller stalls on 30 of any
60 frames** (`.../bridge/context/stall/StallDetector.java:19-42`). A per-frame
synchronous read on the open map hits 60 of 60 and takes the game down within about
a second. So a synchronous read is not an option for anything drawn every frame.

Counting only starts once the game is up: the detector is armed on the first
`CombatEngine` construction, which is also what Fast Rendering treats as "game
initialised" (`.../overrides/CombatEngine.java:37-39`). Stalls during loading and
in the launcher are therefore free, and a stalling read can survive a session that
never reaches combat. That is a reason to distrust a clean test, not a licence.

The viewport read escapes this only because it never stalls: the bridge answers
`GL_VIEWPORT` inline from `attribTracker` and returns before reaching `exec.wait`
(`.../bridge/commands/GL11.java:1311-1318`). The modelview has no such inline path.

The fix is to read **one frame late, without stalling**. `Executor.execute(GLCommand)`
enqueues a command and returns immediately - no `wait`, no stall
(`.../bridge/context/Executor.java:30-34`). So each frame the reader enqueues a
fire-and-forget command that, when the render thread replays it at the caller's
stream position, copies `getCPUModelView()` (transpose included) into a holder the
reader owns; and it returns the copy a *prior* frame's command left there. The
holder is published across the two threads through an `AtomicReference`, which also
makes the cross-thread read tear-free. The result is a frame or two stale - the
render thread runs a frame behind and a frame's copy is only guaranteed complete a
frame later, so a read sees the frame-before-last's value or last's. That is
invisible for a still map, since the cursor moves but the transform does not, and
trails by a frame or two of pan velocity while panning - and costs zero stalls.

This costs KMLib three mirrored members in its compile-only bridge stubs
(`Context.exec`, `Executor.execute`, `GLCommand`) beyond the modelview read itself -
see `build.gradle` and `src/bridgestubs/java`. `GLCommand` is the one that moved:
it lives in `com.genir.renderer.bridge.interfaces` from `v0.7.4`, having been
`com.genir.renderer.bridge.context.commands` before, and its method is
`run(Context, float[], int)` - the float array carries the packed arguments of the
`execute` overloads that take them (`.../bridge/context/Executor.java:36-68`), and
a command that reads nothing from it ignores both parameters.

### What the GL11 bridge can and cannot read back

Drawing is broadly covered. Reading state back is not: the bridge's entire
`glGet*` surface is `glGetInteger(int)` and `glGetInteger(int, IntBuffer)`,
`glGetString`, `glGetFloat(int)`, `glGetError`, `glGetTexLevelParameteri`, and
two `glGetTexImage` overloads (`.../bridge/commands/GL11.java:1264-1462`).

There is **no buffer-taking `glGetFloat`** at all, so `GL_MODELVIEW_MATRIX` and
`GL_PROJECTION_MATRIX` cannot be read through the bridge in any form.
`glGetInteger(int, IntBuffer)` is bridged and delegates to real GL, so
`GL_VIEWPORT` reads work unchanged under both renderers
(`.../bridge/commands/GL11.java:1307`).

A second axis matters as much as which reads exist: whether a read is answered
inline or by stalling the pipeline. Only the pnames the bridge shadows in
caller-side state are inline, and everything else routes through `exec.get` /
`exec.wait`, which is the stall the detector counts. Inline today:

- `glGetInteger(int)` for the texture binding, matrix mode, active texture, array
  buffer binding, **current program**, framebuffer binding, and vertex array
  binding, plus four cached limits
  (`.../bridge/commands/GL11.java:1264-1296`).
- `glGetInteger(int, IntBuffer)` for `GL_VIEWPORT` only.
- `glIsEnabled` for stencil test, alpha test, texture 2D, blend, lighting, and
  **scissor test** (`.../bridge/commands/GL11.java:1466-1485`).
- `glGetFloat(int)` for `GL_LINE_WIDTH` (`.../bridge/commands/GL11.java:1357-1361`).

The set grows release by release, and each addition is somebody's crash being
fixed: `GL_CURRENT_PROGRAM` went inline in `v0.7.4` and `GL_SCISSOR_TEST` in
`v0.7.5`, both to stop a per-frame reader from tripping the stall detector. Two
consequences for KM code. Whether a given read is fatal depends on the Fast
Rendering version, so "it worked on my install" proves less than it looks; and the
scissor **box** is still not shadowed - only the enable flag is - so the current
clip rectangle remains unreadable without a stall, which is why clip composition
is the caller's job (`kmlib.starsector.ui.render.gl.UiScissor`).

Matrix reads are best avoided outright rather than worked around: the campaign
UI's projection is derivable arithmetically (below), and the modelview is
available from `TransformManager` - though only through the render thread, never
by reading it back through GL (see [It defers every GL call to a render
thread](#it-defers-every-gl-call-to-a-render-thread)).

### Its reach goes past the GL bridge

`fr.jar` also ships patched copies of core game classes under `com.fs.*` (and
`sound.*`), not just the GL bridge, and they shadow the game's own copies in
`starfarer_obf.jar` / `fs.common_obf.jar`. There are 20 on `v0.7.7`, reaching well
past rendering: `Version`, `BaseGameState`, `combat/CombatEngine`,
`combat/CombatState`, `combat/entities/Ship`, `combat/ai/admiral/G`,
`graphics/TextureLoader`, `graphics/LayeredRenderer`, `loading/SpecStore`,
`loading/ScriptStore`, `loading/ResourceLoaderState`, `campaign/save/B`,
`campaign/rules/oOOO`, and `api/impl/combat/threat/RoilingSwarmEffect` among them.
The set grows: `combat/entities/Ship` and the `combat/E/o0OO` bounds class arrived
in `v0.7.5`, `sound/C` in `v0.7.6`.

These are compiled against readable aliases - `proxy.com.fs.graphics.Sprite`,
`proxy.com.fs.starfarer.combat.collision.Bounds` and the like - which
`ObfTransformations` maps onto the game's obfuscated names as each class loads
(`.../loaders/ObfTransformations.java:9`). So a name that looks like a public game
type in a decompile of `fr.jar` may be an alias for an obfuscated one, and that
table is where to resolve it.

So when behaviour differs under Fast Rendering, the bridge is not the only place
to look, and a decompile of the game's own jar is not necessarily what is
executing. List the current set with:

```
Glob "**/com/fs/**/*.java" in <starsector>/.sources-cache/starsector-core/fr
```

## The campaign UI's GL setup

### Projection, viewport, and two coordinate spaces

`CampaignState.render` enters 2D mode through `com.fs.graphics.util.B`
immediately before rendering the screen panel that holds the whole UI tree,
including the map
(`starsector-core/starfarer_obf/com/fs/starfarer/campaign/CampaignState.java:1582-1587`).
That helper does `glLoadIdentity()` then
`glOrtho(0, width, 0, height, -6000, 6000)`, and sets the viewport to the full
window scaled by `Display.getPixelScaleFactor()`
(`starsector-core/fs.common_obf/com/fs/graphics/util/B.java:130-145`).

The width and height it passes come from two obfuscated `StarfarerSettings`
statics (`.../campaign/CampaignState.java:1551-1552`), which are exactly what the
public `SettingsAPI.getScreenWidth()` and `getScreenHeight()` return
(`.../settings/StarfarerSettings.java:1905-1911`). Read that pair of files
together to re-confirm it; the obfuscated names themselves are not reproduced
here, since they are non-ASCII and change every game build.

So the projection is a known, axis-aligned ortho in UI virtual units and never
needs to be read back from GL: `Global.getSettings().getScreenWidth()` and
`getScreenHeight()` reconstruct it.

The depth range is `-6000..6000`. The caller passes `1000`, but the helper
overwrites that argument (`.../util/B.java:131`). It makes no difference to
unprojected x/y, since an axis-aligned ortho has no shear.

Two coordinate spaces follow, and mixing them is the failure mode:

- **UI virtual units** span `0..getScreenWidth()`. This is what the ortho and all
  UI widget positions are in.
- **Physical pixels** span `0..getScreenWidth() * getPixelScaleFactor()`. This is
  what `GL_VIEWPORT` reports and what `org.lwjgl.input.Mouse.getX/getY` return
  (bottom-left origin).

`gluUnProject` reconciles them when handed the real viewport, which is why the
viewport is worth reading from GL rather than deriving. `getScreenWidthPixels()`
and `getScreenHeightPixels()` are the API-side equivalents
(`.../settings/StarfarerSettings.java:1913-1919`).

### The modelview around a map render

The campaign UI's base modelview is `identity + translate(0.01, 0.01)`
(`.../util/B.java:142-143`).

The sector map widget adds exactly one modelview change around the terrain
render: `glTranslatef((int)centerX, (int)centerY, 0)`, where the centre is the
widget's own screen position
(`starsector-core/starfarer_obf/com/fs/starfarer/coreui/A/H.java:432-433`). Pan
is not a matrix translation of its own: the map panel sits in a scroller, so
scrolling moves the widget's position and the translate follows.

So the whole world-to-screen transform for a map overlay is that translation plus
the per-vertex `factor` the game hands to `renderOnMap`, and no part of it is a
rotation or a shear. A real map pass therefore never has an identity modelview,
which is what makes identity usable as an error signal.

### Map pan and zoom state

`com.fs.starfarer.campaign.CampaignUIPersistentData` implements `DoNotObfuscate`,
so its name is stable across game builds, and it carries the sector map's live
pan and zoom: `hyperMapCoordinates`, `hyperMapZoom`, and the in-system
equivalents (`.../campaign/CampaignUIPersistentData.java:29-35`). They are
written during the map's own pass
(`.../coreui/A/G.java:334-336`), not only on close, so they are live rather than
last-session values.

Reconstructing a screen transform from them is a trap, though, and was rejected
for KMLib's map transform: inverting them needs the map panel's position and
size, which live on `coreui.A.G` and `coreui.A.H`. Those names are obfuscated and
not stable across game builds, unlike `CampaignUIPersistentData` itself.

## Traps

- **`com.fs.starfarer.prototype` is dead demo code.** It contains its own GL setup
  with a hardcoded `glOrtho(0, 1024, 0, 768, ...)`
  (`.../prototype/super/A.java:36-39`, `.../prototype/super/Object.java:36`). It
  looks like the game's real projection and is not; it is a standalone LWJGL demo
  with its own `main`. The real path is `CampaignState.render` (above).
- **A grep for `glOrtho` finds the wrong answer first.** The hits in
  `com.fs.graphics` are the live ones; check the caller before believing any of
  them.
- **A GL call that compiles is not a GL call that resolves.** Under Fast
  Rendering the binding changes after compile, so the bridge's surface, not
  LWJGL's, is what a KM jar actually gets.
