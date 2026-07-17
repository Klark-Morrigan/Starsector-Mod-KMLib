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
| Fast Rendering | `v0.7.2` | `fr.jar` SHA-256 `c7f62dbf7511bad1b12d7eeae620030ad432d9de29ccb2c9a65057b9c5081e09`, 548847 bytes |

Fast Rendering does not state its own version anywhere a machine can read it:
`fr.jar` carries no manifest, `com.genir` holds no version constant, and the mod
ships no readme, because it is an install patch rather than a mod folder with a
`mod_info.json`. So the version above is not self-reported; it was established by
hashing `fr.jar` against the release assets, and the SHA-256 is what actually
identifies the bytes these citations were read from.

Releases are published at
[Halke1986/starsector-render](https://github.com/Halke1986/starsector-render/releases),
which ships `fast-rendering-<version>.zip` with `fr.jar` inside. To identify an
arbitrary install, hash its jar and compare:

```
sha256sum "<starsector>/starsector-core/fr.jar"
```

Sizes alone separate neighbouring releases (`v0.7.1b` is 530139 bytes, `v0.7.2`
is 548847), so a size mismatch is a fast first check before hashing.

Every Fast Rendering fact below was first read on `v0.7.1b` and re-read on
`v0.7.2`. Nothing behavioural changed between them: the rewrite list, the CPU
modelview handling, and the readable `glGet*` surface are identical, and only
line numbers moved. That is one data point on stability, not a promise.

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

It lives at `starsector-core\fr.jar` and patches `starfarer.api.jar` in place,
leaving the original beside it as `starfarer.api.vanilla.jar`. There is no folder
under `mods\`, so searching `mods\` for it finds nothing and its presence is not
declared in any `mod_info.json`. Presence of `starfarer.api.vanilla.jar` in a
core directory is the install's own tell that it has been patched.

### It rewrites GL class references in every jar

Its classloader rewrites constant-pool class entries, so a reference compiled
against LWJGL resolves to the bridge at runtime. `org/lwjgl/opengl/GL11` becomes
`com/genir/renderer/bridge/GL11`, and the same holds for `GL13`, `GL14`, `GL15`,
`GL20`, `GL30`-`GL33`, `GL40`-`GL44`, `Display`, `GLContext`, `GLSync`,
`SharedDrawable`, and `java/net/URLClassLoader`
(`ScriptTransformations.transformations`,
`starsector-core/fr/com/genir/renderer/loaders/ScriptTransformations.java:11`; the
app-side list is narrower, `.../loaders/AppClassLoader.java:24`).

This applies to KM jars. It is blunt and total: there is no opt-out, no
annotation, and no per-mod exclusion. A KM jar compiles against real LWJGL and
links fine, then binds to the bridge at runtime, so a bridge gap surfaces as a
`NoSuchMethodError` from inside a render pass rather than as a build failure.

Two useful consequences:

- `org.lwjgl.util.glu.GLU` is **not** on the rewrite list, so `gluUnProject` and
  friends keep working as plain matrix arithmetic under both renderers.
- The rewrite is the cheapest detection there is. Because a KM jar's own class
  constants are rewritten too, `GL11.class.getName()` reports
  `com.genir.renderer.bridge.GL11` under Fast Rendering. That needs no reflection
  and no `Class.forName`, and it tests the condition that actually matters: that
  this jar's GL references were redirected.

### It tracks the modelview on the CPU

This is the fact that breaks naive GL code, and it is a design choice, not a bug.

`VertexInterceptor.glVertex3f` multiplies each vertex by
`TransformManager.getCPUModelView()` before submitting it
(`.../bridge/context/VertexInterceptor.java:108`), and in that mode
`TransformManager.setCPUModelView()` loads **identity** into GL
(`.../bridge/context/TransformManager.java:25`).

So while Fast Rendering is in CPU mode, GL's modelview does not describe what is
being drawn. Reading `GL_MODELVIEW_MATRIX` back would return identity while the
real transform sits in a Java object. A crash on the read is the polite failure;
a pass-through implementation returning identity would be the impolite one,
because it yields silently wrong coordinates instead.

The matrix is reachable, publicly: `ContextManager.getThreadContext()` is public
static (`.../bridge/context/ContextManager.java:16`), `Context.transformManager`
is a public final field (`.../bridge/context/Context.java:32`), and
`getCPUModelView()` is public (`.../bridge/context/TransformManager.java:44`).

Two cautions on using it. It returns the live mutable matrix, not a copy, so
callers must copy before holding it. And it returns identity when Fast Rendering
has instead pushed the matrix to the GPU
(`.../bridge/context/TransformManager.java:44-49`), so identity means "this read
is not usable", not "no transform". That is safe to lean on because a real
campaign-UI pass is never identity (see
[The modelview around a map render](#the-modelview-around-a-map-render)).

None of this is published API. It is mod internals, and a genir refactor can
break any of it, so code reading it should fail safe rather than assume.

### What the GL11 bridge can and cannot read back

Drawing is broadly covered. Reading state back is not: the bridge's entire
`glGet*` surface is `glGetInteger(int)` and `glGetInteger(int, IntBuffer)`,
`glGetString`, `glGetFloat(int)`, `glGetError`, `glGetTexLevelParameteri`, and
two `glGetTexImage` overloads (`.../bridge/GL11.java:1208-1393`).

There is **no buffer-taking `glGetFloat`** at all, so `GL_MODELVIEW_MATRIX` and
`GL_PROJECTION_MATRIX` cannot be read through the bridge in any form.
`glGetInteger(int, IntBuffer)` is bridged and delegates to real GL, so
`GL_VIEWPORT` reads work unchanged under both renderers
(`.../bridge/GL11.java:1248`).

Matrix reads are best avoided outright rather than worked around: the campaign
UI's projection is derivable arithmetically (below), and the modelview is
available from `TransformManager`.

### Its reach goes past the GL bridge

`fr.jar` also ships patched copies of core game classes under `com.fs.*`, not just
the GL bridge, and they shadow the game's own copies in `starfarer_obf.jar` /
`fs.common_obf.jar`. There were 17 on `v0.7.2`, reaching well past rendering:
`Version`, `BaseGameState`, `combat/CombatEngine`, `combat/CombatState`,
`combat/ai/admiral/G`, `graphics/TextureLoader`, `graphics/LayeredRenderer`,
`loading/SpecStore`, `loading/ScriptStore`, `loading/ResourceLoaderState`,
`campaign/save/B`, `campaign/rules/oOOO`, and
`api/impl/combat/threat/RoilingSwarmEffect` among them.

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
