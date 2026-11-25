# Rendering Environment

Facts about the GL substrate KM mods draw through:
how the game sets up its matrices,
and how the Fast Rendering mod changes what GL calls mean.
None of it is recoverable from KM source.
It was read out of decompiled game and mod jars,
and it is the context behind why KMLib's map and UI code is shaped the way it is.

Consumer repositories (KMU, KMO, ...) should link here rather than restate any of it.

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
  - [It uploads VRAM Optimizer's textures lazily](#it-uploads-vram-optimizers-textures-lazily)
- [The campaign UI's GL setup](#the-campaign-uis-gl-setup)
  - [Projection, viewport, and two coordinate spaces](#projection-viewport-and-two-coordinate-spaces)
  - [The modelview around a map render](#the-modelview-around-a-map-render)
  - [Map pan and zoom state](#map-pan-and-zoom-state)
- [Traps](#traps)

## Versions this was verified against

Everything here was read out of these exact artifacts.
Treat the page as unproven against anything else.

| What | Version | Identity |
| --- | --- | --- |
| Starsector | `0.98.5a-RC9` under the Fast Rendering section; `0.98a-RC8` under the campaign UI's GL setup | - |
| LWJGL (bundled with the game) | `2.9.3` | `org.lwjgl.Sys.getVersion()`; Linux x64 native `liblwjgl64.so` SHA-256 `4622966cceee1c13df9e293fbae8fa402d1be84b3e1ade7256eca57892392f08` |
| Fast Rendering | `v1.0.4rc1` | `fr.jar` SHA-256 `3c2ca02e6b96fa73c4e34d5237bb0e1ec789a80e356ec93d18a28f25ee706d00`, 872049 bytes |

The Fast Rendering row is the release the citations below were read out of.
The campaign UI's GL setup was read on the older game build and has not been read again since.

A Windows install ships `native\windows` only.
The Linux native,
which a Linux machine needs to load Fast Rendering's bridge outside the game -
`FastRenderingBridgeIntegrationTests` does,
on `kmlib-runner` -
comes from the official LWJGL artefact:
[`lwjgl-platform-2.9.3-natives-linux.jar`](https://repo1.maven.org/maven2/org/lwjgl/lwjgl/lwjgl-platform/2.9.3/lwjgl-platform-2.9.3-natives-linux.jar),
SHA-1 `b1eafe80093381c56415731e1d64279e6140bcd0`.
It is kept at `starsector-core\native\linux\` beside the Windows natives,
the layout the Linux game uses.
It links against `libX11`, `libXext`, `libXcursor`, `libXrandr`, `libXxf86vm` and the JDK's `libjawt`,
and `libjawt` in turn against the JDK's `libawt_xawt`,
which adds `libXrender`, `libXtst` and `libXi`.
A machine loading it needs all eight X libraries installed,
and a full JDK rather than a headless runtime, which lacks `libjawt`.
An install is patched by dropping the release zip's jars into `starsector-core\`,
so a matching hash means the recorded artifact and the running one are the same file.

From `v1.0.0rc1` the patch is one jar.
Through `v0.9.8` the agent shipped beside it as `fr.agent.jar`,
and an install of those releases is checked by both hashes,
`fr.agent.jar`'s by hash only.
Its size held at 15558 bytes across four releases whose contents all differed,
moved by three bytes for a release string in `v0.8.10rc1`,
and grew tenfold in `v0.9.0rc1` when it took on an ASM jar and the bytecode patcher (below).
So a size match can hide a rewrite and a size move can be a banner.
The agent is where the bridge package name,
the rewrite tables and the game-class patches live,
which is the half of the patch KM's detection depends on.

Eighteen claims on this page are release-specific rather than true of every build still in the field,
and each is flagged where it appears:

- **Per-mod exclusion exists**,
  from `v0.8.5rc1`.
  Earlier releases rewrite every mod jar without exception.
- **Stall detection is armed at the end of game initialisation**,
  from `v0.8.7`.
  Through `v0.8.7rc1` it was armed on the first combat frame instead,
  which made a stalling read on the campaign map survive any session that never entered a battle.
  This is the delta on this page with the most teeth for KM code.
  From `v0.9.0rc1` the arming call sits in `overrides/loading/ResourceLoaderState` rather than `overrides/loading/ResourceLoader`,
  and by `v1.0.4rc1` it runs when the title screen first comes up rather than at the end of `initEpilogue`;
  both are after every mod's `onApplicationLoad`.
- **GL references rewrite to `com.genir.renderer.bridge.opengl`**,
  from `v0.8.9`.
  Through `v0.8.8` they rewrote to `com.genir.renderer.bridge.commands`,
  which is where the implementations still live;
  `bridge.opengl` is a facade in front of them.
- **A GL entry point the bridge does not implement throws `UnsupportedOperationException`**,
  from `v0.8.9`.
  Through `v0.8.8` the same call was a `NoSuchMethodError`,
  because the method simply was not there.
  Both land mid-render; only the exception type and the stack trace differ.
- **`Context.exec` is declared as the `bridge.context.executor.Executor` interface**,
  from `v0.9.0`.
  Through `v0.9.0rc2` it was the class `bridge.context.Executor`.
  A field links by its type,
  so a jar compiled against either shape fails to link the field on the other.
- **Game classes are patched by merging methods into them**,
  from `v0.9.0rc1`.
  Through `v0.8.10` `fr.jar` shadowed whole copies of 21 game classes instead.
- **`GL_SCISSOR_BOX` is an inline read**,
  from `v0.9.0rc2`.
  On `v0.9.0rc1` and every earlier release it stalls.
- **A facade throw names the method it refuses**,
  from `v0.9.0`.
  Through `v0.9.0rc2` the `GL11` to `GL20` facades threw a bare `UnsupportedOperationException`,
  so a bare one in a trace dates it.
- **The render-thread modelview lives in `MatrixManager`, reached as `Context.matrixManager`**,
  from `v0.9.1rc1`.
  Through `v0.9.0` the same class was `TransformManager`,
  reached as `Context.transformManager`.
  The release notes do not mention the rename.
- **`glGetFloat(GL_MODELVIEW_MATRIX, FloatBuffer)` answers inline**,
  from `v0.9.1rc1`.
  Through `v0.9.0` the read failed mid-render,
  as an `UnsupportedOperationException` from `v0.8.9` and a `NoSuchMethodError` before it,
  so the modelview could not be read back through GL at all.
- **The bridge's matrices are its own `bridge.context.Matrix`**,
  from `v0.9.2rc1`.
  Through `v0.9.1rc1` they were LWJGL's `org.lwjgl.util.vector.Matrix4f`,
  so `MatrixManager.getCPUModelView` changed its return type,
  and a caller linked against the old one fails to link.
- **`glMultMatrix` composes in GL's order**,
  except on `v0.9.2rc1` through `v0.9.3`,
  which multiplied the other way round.
  A modelview read after a `glMultMatrix` on a non-identity matrix answers the wrong matrix there,
  with nothing thrown.
- **VRAM Optimizer's lazy texture window closes before any mod's `onApplicationLoad`**,
  from `v0.9.4`.
  Through `v0.9.3` it closed after them,
  so every `onApplicationLoad` fell inside it.
- **Display lists are refused**,
  from `v0.9.6rc1`.
  `glNewList`, `glEndList`, `glCallList` and `glGenLists` throw `UnsupportedOperationException`,
  and the game's own display-list cache is replaced by a no-op stub.
  Through `v0.9.5` the bridge recorded lists and replayed them.
- **The agent ships inside `fr.jar`**,
  from `v1.0.0rc1`,
  under `com.genir.renderer.agent.transformations`,
  with the rewrite tables as instance fields of `agent.transformations.symbols.Transformations`.
  Through `v0.9.8` it was the separate `fr.agent.jar`,
  with the tables as static fields of `agent.Transformations`.
- **The launcher tag swaps `v1.` for `FR`**,
  from `v1.0.0rc1`,
  so `FR0.4rc1` is `v1.0.4rc1`.
  Through `v0.9.8` it swapped `v0.`,
  so `FR9.6rc1` is `v0.9.6rc1`.
- **`GL_VIEWPORT` and `GL_SCISSOR_BOX` answers are checked against real GL**,
  from `v1.0.0`.
  The read still answers inline,
  and also queues a render-thread comparison that ends the session on a mismatch.
- **`glGetInteger(int, IntBuffer)` throws for every pname but `GL_VIEWPORT` and `GL_SCISSOR_BOX`**,
  from `v1.0.1rc1`.
  Through `v1.0.0` the others fell through to `exec.wait` and stalled.

KMLib binds to no bridge member.
It reads the modelview through plain `GL11.glGetFloat`,
which the bridge serves from `v0.9.1rc1`,
and it guards that read so an earlier release costs the reading rather than the render pass
(see [It defers every GL call to a render thread](#it-defers-every-gl-call-to-a-render-thread)).
A mod that read the render-thread matrix directly would have been broken three times in three releases.
`Context.exec` changed type in `v0.9.0` from the class `bridge.context.Executor`
to the interface `bridge.context.executor.Executor`
(implemented by `AsyncExecutor`; `SyncExecutor` and `SyncBatchExecutor` sit beside it unused),
with `execute(GLCommand)` unchanged on it.
`Context.transformManager` and `TransformManager.getCPUModelView` are gone in `v0.9.1rc1`,
renamed to `Context.matrixManager` and `MatrixManager.getCPUModelView`
with the same signature and the same body.
`v0.9.2rc1` then changed that method's return type from LWJGL's `Matrix4f` to the bridge's own `Matrix`.
The classes around them have been rewritten repeatedly -
`Context` in `v0.8.9`, `v0.9.0`, `v0.9.1rc1`, `v0.9.2rc1` and `v0.9.6rc1`,
`VertexInterceptor` and `MatrixStack` more than once.

The table that decides the bridge package name has been reshaped without notice -
`ScriptTransformations` through `v0.8.8`,
`Transformations` with one map per rewrite family from `v0.8.9`,
three more maps in `v0.9.0rc1`,
and from `v1.0.0rc1` instance fields on `agent.transformations.symbols.Transformations`,
built from an analysis of the game's classes -
and KM detection spans all of it by matching the `com.genir.renderer.` prefix rather than a whole class name,
which is the case this tolerance was built for.

Fast Rendering names itself in three places,
and **all three agree with each other** from `v0.9.0rc1`.
They do not always agree with the release.
Two builds in the field lie:
`v0.8.5rc1` says `v0.8.4` in all three,
because the files carrying them shipped byte-identical to the `v0.8.4` release,
and `v0.9.2` says `v0.9.2rc2`,
because its version constant shipped unchanged from that release candidate.
So a self-report is a lower bound on the release rather than the release,
and the jar hash is what settles it.

The agent logs a release at startup,
before anything else runs:
`Agent.premain` writes `Fast Rendering: v1.0.4rc1` at INFO,
followed by the SHA-256 of `starfarer_obf.jar`
(`starsector-core/fr/com/genir/renderer/agent/Agent.java:21-24`).
The line appeared in `v0.8.4`;
earlier releases log nothing.
Through `v0.8.10` the agent carried the release as a literal of its own;
from `v0.9.0rc1` it reads `fr.jar`'s version constant (below).
From `v0.9.6` it also sets the system property `fr.version` to the same string (`:22`).
That checksum is of the *game's* jar,
not Fast Rendering's,
so it identifies the Starsector build being patched and says nothing about which patch is doing it.

`fr.jar` carries a version constant from `v0.8.2`,
`com.genir.renderer.Version.getVersion()`
(`starsector-core/fr/com/genir/renderer/Version.java:7-8`),
returning a release string -
`"v1.0.4rc1"` on `v1.0.4rc1`.
From `v0.9.0rc1` the agent's startup line and the banner below are both built from it,
so the three self-reports are one value.
KMLib reads it at runtime,
by name through a method handle
(`FastRendering.readInstalledVersion`),
to name the installed release when it reports a refused modelview read.
That report is therefore only as honest as the constant:
`v0.8.5rc1` reports itself as `v0.8.4`,
and `v0.9.2` as `v0.9.2rc2`.
Neither misreport crosses the release the modelview read needs,
so neither can turn a served read into a reported refusal.

The third is the display string on the launcher and the main menu,
`"Starsector 0.98.5a-RC9 FR0.4rc1"`.
From `v0.9.0rc1` the agent makes it by rewriting one constant inside the game's own version class as it loads,
replacing the game version literal with that literal plus the release tag
(`starsector-core/fr/com/genir/renderer/agent/transformations/constants/ConstantClassTransformer.java:61-64`);
through `v0.8.10` the string sat in a shadowed copy of that class inside `fr.jar`.
The tag is the version constant with its leading `v1.` swapped for `FR`,
and through `v0.9.8` its leading `v0.`.
So the digits after `FR` restart at `1.0`:
`FR9.6rc1` is `v0.9.6rc1`,
and `FR0.4rc1` is `v1.0.4rc1`.
Two things follow.
The tag is derived from the version constant,
so the two cannot disagree with each other,
though both can be stale together,
as on `v0.9.2`;
and the match is on exact game version literals -
`0.98a-RC8` and `0.98.5a-RC3` to `RC9` on `v1.0.4rc1` -
so on any other game build the banner is simply absent,
which says nothing about whether the patch is running.
The shape keeps patch letters and release-candidate suffixes -
`FR7.1b`,
`FR8.7rc1` -
and what follows the dot is a counter,
so `FR8.10` is later than `FR8.9` and is not `FR8.1`.
It is the only identifier readable without opening a jar and so the one to ask a player for,
with two ambiguities:
`FR8.4` is either `v0.8.4` or `v0.8.5rc1`,
and `FR9.2rc2` is either `v0.9.2rc2` or `v0.9.2`.

None of the three proves the bytes,
and -
as `v0.8.5rc1` demonstrates -
a rebuild can carry all of them unchanged.
The SHA-256 is what identifies what these citations were read from.

Releases are published at [Halke1986/starsector-render](https://github.com/Halke1986/starsector-render/releases),
which ships `fast-rendering-<version>.zip`,
and from `v1.0.1rc1` `fast_rendering_v<version>.zip`.
A version can be tagged there and never released:
`v1.0.3` has a tag and changelog entry but no release or zip.
To identify an arbitrary install,
hash its jar and compare:

```bash
sha256sum "<starsector>/starsector-core/fr.jar"
```

Sizes still separate neighbouring releases
(`v0.8.4` is 639820 bytes, `v0.8.5rc1` is 642412, `v0.8.6` is 660831, `v0.8.7rc1` is 666557, `v0.8.7` is 668949, `v0.8.8` is 669864, `v0.8.9` is 716583, `v0.8.10rc1` is 716402, `v0.8.10rc2` is 716913, `v0.8.10rc3` is 719220, `v0.8.10` is 718990, `v0.9.0rc1` is 534841, `v0.9.0rc2` is 539066, `v0.9.0` is 562976, `v0.9.1rc1` is 584591, `v0.9.2rc1` is 574298, `v0.9.2rc2` is 573060, `v0.9.2` is 573440, `v0.9.3` is 574251, `v0.9.4` is 583305, `v0.9.5` is 585921, `v0.9.6rc1` is 570102, `v0.9.6rc2` is 569385, `v0.9.6rc3` is 592570, `v0.9.6rc4` is 592570, `v0.9.6` is 592450, `v0.9.7rc1` is 596723, `v0.9.7rc2` is 602167, `v0.9.7` is 606883, `v0.9.8rc1` is 618674, `v0.9.8` is 618670, `v1.0.0rc1` is 849420, `v1.0.0` is 850204, `v1.0.1rc1` is 861456, `v1.0.1` is 875270, `v1.0.2rc1` is 859095, `v1.0.2` is 862971, `v1.0.4rc1` is 872049),
so a size mismatch is a fast first check before hashing.
Only the mismatch is informative:
neighbours can sit within a few hundred bytes of each other,
`v0.9.6rc3` and `v0.9.6rc4` share a size outright,
and the figure does not climb steadily -
`v0.9.0rc1` is 184 KB smaller than `v0.8.10`,
the shadowed game classes having left the jar,
and `v1.0.0rc1` is 231 KB larger than `v0.9.8`,
the agent having moved in.
`v1.0.1` is larger than both its neighbours because its jar carries stale classes its tag does not have:
both copies of the buffer pools, an old `ArrayPool` and a `ParticleRenderer2`.
Release candidates are installed like any other release,
and on `v0.8.5rc1` and `v0.9.2` the size is the only cheap discriminator against the release each names itself as.

Names below are release-specific,
and a rename is not announced.
Every move so far has invalidated citations,
and the release notes have named a moved jar at most, never a moved package.
`v0.7.4` moved the whole GL bridge from `com.genir.renderer.bridge` to `com.genir.renderer.bridge.commands`,
and the command interfaces to `com.genir.renderer.bridge.interfaces`.
`v0.8.0` moved the rewriting out of `fr.jar` into `fr.agent.jar` (`com.genir.renderer.agent`).
`v0.8.9` split the bridge in two:
the implementations stayed in `com.genir.renderer.bridge.commands`,
and a new `com.genir.renderer.bridge.opengl` became what GL references actually rewrite to.
`v0.9.0` moved `Executor` out of `com.genir.renderer.bridge.context` into `com.genir.renderer.bridge.context.executor`,
as an interface over a new `AsyncExecutor`;
and `v0.9.0rc1`,
one release earlier,
emptied `fr.jar` of every shadowed game class (below).
`v0.9.1rc1` renamed `bridge.context.TransformManager` to `bridge.context.MatrixManager`,
and the `Context` field that holds it from `transformManager` to `matrixManager`.
`v0.9.2rc1` swapped LWJGL's `Matrix4f` under every bridge matrix for its own `bridge.context.Matrix`.
`v0.9.6rc1` deleted the bridge's display-list machinery:
`bridge.context.ListManager` and the `Recordable` and `GLCommandClient` interfaces.
`v1.0.0rc1` folded the agent into `fr.jar`
and split `com.genir.renderer.agent` into `agent.transformations.bytecode`,
`.constants` and `.symbols`,
renaming `BytecodeFileTransformer` to `BytecodeClassTransformer`
and `ConstantFileTransformer` to `ConstantClassTransformer`;
it also moved the buffer pools into `bridge.pool`.
The older move is the one that looks like the newer one and is not:
`v0.7.4` relocated the classes,
while `v0.8.9` left them where they were and put a layer in front.
So a `bridge.commands.GL11` frame means "at or after `v0.7.4`" and nothing more,
and only a `bridge.opengl` frame dates a trace to `v0.8.9` or later.
A fact that names a bridge type is therefore a fact about one range of releases,
and the hash is what says which.

KM code names two things in `fr.jar`,
and both have outlived every move:
the `com.genir.renderer.` prefix detection matches on (below),
and the `com.genir.renderer.Version` class a report reads the installed release from.
Everything else KM needs from the renderer goes through LWJGL's own entry points,
which the bridge is rewritten onto and so cannot move out from under a caller.

## How to re-verify

Every claim below carries a `file:line` into the decompiled sources cache at `<starsector>\.sources-cache\`,
whose layout mirrors the relative JAR path under the game install.
Regenerate it with the `/jar-search` command.

Fast Rendering occupies one cache root from `v1.0.0rc1`,
`starsector-core\fr\`,
for the bridge, the override donors and the agent alike.
Through `v0.9.8` the agent had a second root,
`starsector-core\fr.agent\`,
and a glob of `fr*.jar` was needed to cover both.

The cache stamps each jar's length and write time,
so an upgraded `fr.jar` is extracted again the next time it is searched.
Still check `com/genir/renderer/Version.java` against the table above before trusting a line number.
A few of the bridge's command records are declared under names holding a `$`,
such as `Sprite$render` inside `Sprite`;
CFR's whole-jar run drops their bodies,
so the cache decompiles each one alone into a file of its own,
named like `Sprite$Sprite$render.java`, beside its outer class.

Re-verify rather than trust this page.
Line numbers drift on any re-decompile even when nothing changed,
so the surrounding symbol names are the durable part of a citation
and the line number is only a hint.

The two version dependencies rot differently,
and it is worth knowing which is which before relying on a fact:

- **Starsector facts** (the campaign UI's GL setup) are stable across patch releases
  but sit behind obfuscated names that are renamed every build.
  Expect the symbols to move,
  not the behaviour.
- **Fast Rendering facts** are the volatile half.
  They describe mod internals that are not published API,
  carry no compatibility promise,
  and can change in any release.
  A hash mismatch means every Fast Rendering claim below is unverified until re-read.

## Fast Rendering

Fast Rendering (`com.genir.renderer`, by genir) replaces the game's GL calls with a batching,
deferred renderer.
It is widely installed,
so KM code that touches GL has to work under it and under stock LWJGL both.

### It is an install patch, not a mod

It lives at `starsector-core\fr.jar`,
beside its own launchers `fr.bat` and,
from `v1.0.4rc1`,
`fr.exe`,
and its own JVM argument file,
`fr.vmparams`.
Through `v0.9.8` a second jar,
`fr.agent.jar`,
sat beside it,
and through `v1.0.2` a second launcher,
`fr.noterminal.bat`.
There is no folder under `mods\`,
so searching `mods\` for it finds nothing and its presence is not declared in any `mod_info.json`.

Nothing on disk is rewritten.
`fr.vmparams` sets `-javaagent:fr.jar`,
and through `v0.9.8` `-javaagent:fr.agent.jar`,
so the patch is a JVM instrumentation agent,
applied to bytes on their way into the JVM;
the game's own jars are read unmodified.
Two consequences worth holding onto:
the install is only patched when the player launches `fr.bat` rather than `starsector.exe`,
so the same install runs stock or patched depending on which one was double-clicked,
and a `starfarer.api.vanilla.jar` sitting in a core directory is somebody else's doing,
not Fast Rendering's.

The patch splits its work in two,
and which half a fact lives in matters when reading a stack trace.
`com.genir.renderer.agent` holds the rewriting:
an agent entry point that loads its transformers through a class loader over a jar embedded in `fr.jar`
(`starsector-core/fr/com/genir/renderer/agent/Agent.java:19-34`,
`.../agent/transformations/TransformerAdder.java:21-28`),
a `ConstantClassTransformer` that picks a constant-pool rewrite table per class,
the tables themselves,
and - from `v0.9.0rc1` - a `BytecodeClassTransformer` built on that embedded ASM jar
that patches methods into named game classes as they load (below).
The rest of `fr.jar` is everything the rewritten code then resolves to -
the GL bridge and the `overrides` the patches are copied from.
Through `v0.9.8` the first half was `fr.agent.jar`,
with the two transformers named `ConstantFileTransformer` and `BytecodeFileTransformer`.

On `v0.9.6rc1` `fr.bat` also passed `-javaagent:PatchLibAgent.jar` when that file was present,
so a PatchLib-based mod and Fast Rendering coexisted as two ordinary agents on the same JVM.
By `v1.0.2` `fr.bat` only runs `java @fr.vmparams`,
and `fr.vmparams` names no second agent.

Through `v0.7.7` the patch was a system classloader instead,
`com.genir.renderer.loaders.AppClassLoader`,
so a stack frame naming that package is from `v0.7.7` or earlier.

### It rewrites GL class references in every jar

Its agent rewrites constant-pool class entries,
so a reference compiled against LWJGL resolves to the bridge at runtime.
`org/lwjgl/opengl/GL11` becomes `com/genir/renderer/bridge/opengl/GL11`,
and the same holds for `GL13`,
`GL14`,
`GL15`,
`GL20`,
`GL30`-`GL33`,
and `GL40`-`GL44`.
`Display`,
`GLContext`,
`GLSync` and `SharedDrawable` are the exceptions:
they rewrite to `com/genir/renderer/bridge/commands/` and have no facade
(`Transformations.opengl`, `starsector-core/fr/com/genir/renderer/agent/transformations/symbols/Transformations.java:14`).
From `v0.9.6rc1` the game's display-list cache,
`com/fs/graphics/util/GLListManager`,
is also replaced by a no-op stub in `fr.jar`
(see [It defers every GL call to a render thread](#it-defers-every-gl-call-to-a-render-thread)).

`bridge.opengl` is new in `v0.8.9` and is a facade,
not a reimplementation.
Each of its methods either forwards to the `bridge.commands` class of the same name
or throws `UnsupportedOperationException`
(`starsector-core/fr/com/genir/renderer/bridge/opengl/GL11.java`).
Through `v0.8.8` the rewrite pointed straight at `bridge.commands`,
which declared only the entry points it implemented,
so an unimplemented call failed to *link*.
Now every entry point on LWJGL's own surface exists and the unimplemented ones fail when *called*.
For KM code that is a change of symptom rather than of capability:
the same reads are unavailable,
and the exception is `UnsupportedOperationException` instead of `NoSuchMethodError`.

The same table is applied to mod jars and to the game,
and which transformer a class gets is decided by package prefix first,
classloader second.
`com.fs.`,
`sound.` and `zzz.com.fs.` take the game transformer;
`org.lwjgl.util.glu.` and `com.thoughtworks.xstream.` take their own;
`com.genir.renderer.` outside the agent takes the override-renaming one;
anything left that is loaded by a loader which is neither the system loader nor the agent's own takes the mod transformer
(`starsector-core/fr/com/genir/renderer/agent/transformations/constants/ConstantClassTransformer.java:47-90`;
the class was `ConstantFileTransformer` through `v0.9.8` and `ClassTransformer` through `v0.8.10`).
Four game classes are then singled out by name:
`com.fs.starfarer.Version` for the banner (above);
`BaseGameState` and `combat.CombatState`,
whose `Thread` and `Display` references are repointed at `overrides/Sync` -
the `v0.9.0` frame pacing,
a sleep to the frame deadline in place of a busy wait
(`:68-70`, `starsector-core/fr/com/genir/renderer/overrides/Sync.java:77-95`);
and from `v1.0.4rc1` `combat.entities.terrain.Planet`,
whose `org/lwjgl/util/glu/Sphere` is repointed at `overrides/render/Sphere`,
the planet renderer that release sped up (`:65-67`).

The game transformer applies three tables:
the same full GL list the mods get,
janino's `JavaSourceClassLoader` swapped for a plain `ClassLoader`,
and the debug overrides,
empty on a release build
(`.../agent/transformations/symbols/CommonTransformers.java:15`).
A separate transformer rewrites obfuscated names that collide with Java keywords -
`class.do` to `class_do` and so on,
which is why a decompile shows types like `com/fs/starfarer/util/return`
(`.../agent/transformations/constants/IllegalClassTransformer.java:15`, `:29-38`,
`.../agent/IllegalTransformations.java:10-24`).
The obfuscation aliases go the other way,
into the donors rather than the game (below).

There is no opt-out a mod can *request*.
What `v0.8.5rc1` introduced is one genir grants by name:
classes under `DeCell.VOpt.Commons.Rendering.` (VRAM Optimizer) get no transformer at all
(`.../agent/transformations/constants/ConstantClassTransformer.java:83-85`),
and `v0.8.10rc3` added a second named case pointing the other way,
two FarsightDrive renderer classes whose `glGetError` is renamed to `glDrainErrors`,
a bridge-only entry point that returns `0` at once and drains the real error queue on the render thread
(`:86-88`, `starsector-core/fr/com/genir/renderer/bridge/commands/GL11.java:1211-1223`),
so a class on that list never sees a GL error.

The exclusion matters to KM code for the shape it establishes:
an excluded jar is a *mixed* state,
its GL references pointed at real LWJGL while the game around it runs on the bridge.
Detection stays correct through it -
the check below asks what *this* jar's `GL11` resolved to -
but "Fast Rendering is installed" and "my calls go through Fast Rendering" stop being the same fact,
and only the second one is worth acting on.

Two useful consequences:

- `org.lwjgl.util.glu.GLU` is **not** redirected:
  a mod's call to `gluUnProject` still lands on the real GLU class,
  which is plain matrix arithmetic and touches no GL,
  so it behaves identically under both renderers.
  (GLU's own body is transformed as it loads, but only to point its internal GL calls at the bridge.)
- The rewrite is the cheapest detection there is.
  Because a KM jar's own class constants are rewritten too,
  `GL11.class.getName()` reports a `com.genir.renderer.` name under Fast Rendering.
  That needs no reflection and no `Class.forName`,
  and it tests the condition that actually matters:
  that this jar's GL references were redirected.
  Match the **package prefix**,
  not the whole name -
  the exact name was `com.genir.renderer.bridge.GL11` through `v0.7.3`,
  `com.genir.renderer.bridge.commands.GL11` from `v0.7.4`,
  and `com.genir.renderer.bridge.opengl.GL11` from `v0.8.9`,
  and a full-name comparison silently reports "stock" on the releases it does not know,
  which is the worst of the three answers
  because it routes callers into GL reads the bridge cannot serve.

`KMLib_ModPlugin.onApplicationLoad` runs that detection once and logs the answer at INFO as `Active GL renderer resolved; fastRendering=<true|false>`.
Which stack is underneath is a standing condition on everything below -
what a GL hint does,
what a state read answers -
so any rendering report gathered from a log is read under it,
and two reports from different stacks are otherwise indistinguishable.
Unconditional and at load,
rather than left to the one binding that reports its own choice
(`ModelviewMatrixReaders.selectForActiveRenderer`, at INFO):
that line appears only in a session that read the map transform for a hover,
and it names the reader it picked rather than the renderer it picked it for.

### It tracks the modelview on the CPU

This is the fact that breaks naive GL code,
and it is a design choice,
not a bug.

`VertexInterceptor.glVertex3f` multiplies each vertex by `MatrixManager.getCPUModelView()` before submitting it
(`starsector-core/fr/com/genir/renderer/bridge/context/VertexInterceptor.java:115-119`),
and in that mode `MatrixManager.setCPUMode()` loads **identity** into GL
(`.../bridge/context/MatrixManager.java:26-38`).
Through `v0.9.0` the class was `TransformManager`,
identical apart from its name.

So while Fast Rendering is in CPU mode,
GL's modelview does not describe what is being drawn.
Reading the real driver's `GL_MODELVIEW_MATRIX` would return identity
while the real transform sits in a Java object.
A crash on the read is the polite failure;
a pass-through implementation returning identity would be the impolite one,
because it yields silently wrong coordinates instead.
From `v0.9.1rc1` the bridge does neither:
it answers the read from a caller-side copy of the matrix
(see [What the GL11 bridge can and cannot read back](#what-the-gl11-bridge-can-and-cannot-read-back)).

The render-thread matrix is reachable,
publicly:
`ContextManager.getThreadContext()` is public static
(`.../bridge/context/ContextManager.java:17-25`),
`Context.matrixManager` is a public final field (`.../bridge/context/Context.java:48`),
and `getCPUModelView()` is public
(`.../bridge/context/MatrixManager.java:56-61`).
Only the modelview is drawn from the CPU.
`MatrixManager` keeps a projection stack beside the modelview one,
but hands every call to real GL as well unless the mode is `GL_MODELVIEW` in CPU mode
(`shouldDelegate`, `.../bridge/context/MatrixManager.java:13-15`, `:164-166`),
so the projection that draws is GL's and the campaign's ortho goes straight to GL.

`getThreadContext()` returns `null` more often than "unregistered thread" suggests.
The main context is created by the bridged `Display.create` and cleared by `Display.destroy`
(`.../bridge/context/ContextManager.java:27-42`, `.../bridge/commands/Display.java:24`, `:68`),
so the same call on the same thread answers `null` before the window is up
and again after it is torn down.

Five cautions on reading `getCPUModelView()` directly.
Only the last applies to the `glGetFloat` read from `v0.9.1rc1`,
which answers on the calling thread, copies, and is already column-major.
The first is where,
not what,
and it dwarfs the rest:
the matrix cannot be read correctly from the calling thread at all,
because the bridge mutates it on a separate render thread a step behind.
That is its own section below
([It defers every GL call to a render thread](#it-defers-every-gl-call-to-a-render-thread));
the remaining four assume the read already runs there.

It returns the live mutable matrix,
not a copy,
so callers must copy before holding it.
And it returns identity when Fast Rendering has instead pushed the matrix to the GPU
(`.../bridge/context/MatrixManager.java:56-61`),
so identity means "this read is not usable",
not "no transform".
That is safe to lean on because a real campaign-UI pass is never identity
(see [The modelview around a map render](#the-modelview-around-a-map-render)).

The third is the quiet one:
**its matrix fields are row-major**,
transposed from what GL expects.
`VertexInterceptor.glVertex3f` takes the translation from `m03/m13/m23`
(`.../bridge/context/VertexInterceptor.java:117-119`),
and `Matrix.translate` writes it there too
(`.../bridge/context/Matrix.java:45-50`),
so the first index is the row.
The type is the bridge's own `bridge.context.Matrix` from `v0.9.2rc1`.
Through `v0.9.1rc1` it was LWJGL's `Matrix4f`,
whose own methods read the same fields as `m<col><row>` and put a translation in `m30/m31/m32`,
so on those releases the field names themselves mislead.

So `getCPUModelView().store(buffer)` yields the matrix **transposed** relative to what GL and `gluUnProject` expect;
`storeTranspose` is what gives the column-major layout.
That is not a correction but the same conversion Fast Rendering itself does when it hands the matrix to GL
(`MatrixManager.setGPUMode`, `.../bridge/context/MatrixManager.java:40-54`),
and it reads back with `loadTranspose` on the way in
(`MatrixStack.glLoadMatrix`, `.../bridge/context/MatrixStack.java:69-71`, `.../bridge/context/Matrix.java:202-219`).

This is worth care because it fails silently:
a transposed modelview is still sixteen plausible floats,
so nothing throws and a map overlay just resolves the wrong point.
For a translate-only map pass the pan lands in slots 3/7 instead of 12/13.

The last is the order a matrix is composed in,
and it holds for `glGetFloat` too,
because the caller-side copy shares the same `Matrix` code.
`Matrix.mul(right)` computes `right × this`
(`.../bridge/context/Matrix.java:148-181`).
`glMultMatrix` uses it the right way round,
`current × M`,
from `v0.9.4`
(`.../bridge/context/MatrixStack.java:62-67`);
on `v0.9.2rc1` through `v0.9.3` it composed `M × current`.
`glOrtho` still composes `P × current`
(`Matrix.ortho`, `.../bridge/context/Matrix.java:130-146`),
where GL composes `current × P`.
Both agree with GL on an identity matrix,
so they bite only on a matrix already carrying a transform,
and then the read answers what the bridge draws rather than what stock GL would.
The campaign UI is clear of both:
its ortho goes to the projection matrix,
which no read serves,
and the map's modelview is translation alone
(see [The campaign UI's GL setup](#the-campaign-uis-gl-setup)).

None of this is published API.
It is mod internals,
and a genir refactor can break any of it -
`v0.9.0`, `v0.9.1rc1` and `v0.9.2rc1` each moved a member a direct reader would bind to.
KMLib reads none of it.
It asks `glGetFloat`,
and from `v0.9.1rc1` the bridge does the transpose itself before answering.

### It defers every GL call to a render thread

The reason a naive `getCPUModelView()` read is not just transposed but flatly wrong:
it reads the matrix from the wrong thread,
at the wrong time.

The bridge is a deferred,
double-buffered renderer.
A `GL11.glTranslatef` on the game thread does not touch the render-thread modelview -
it appends a command to a frame buffer
(`.../bridge/commands/GL11.java:328-332`),
and `Executor.execute` only records it
(`.../bridge/context/executor/AsyncExecutor.java:43-51`).
The command that actually mutates `MatrixManager` runs later,
when the frame is replayed on a dedicated single-thread executor named `FR-Render`
(`.../bridge/context/executor/AsyncExecutor.java:36`, `:95-133`).
`Executor` is an interface from `v0.9.0`,
`AsyncExecutor` the implementation the context constructs
(`.../bridge/context/Context.java:52`).
The same holds for `glPushMatrix`,
`glPopMatrix`,
`glLoadIdentity`,
`glScalef` and the rest:
all enqueue,
none mutate `MatrixManager` inline.

So `MatrixManager` is render-thread state,
mutated roughly a frame behind the game thread that enqueues the calls.
A KM overlay's `renderOnMap` runs on the game thread;
reading `getCPUModelView()` directly from there samples whatever unrelated transform the render thread happens to be replaying at that instant,
and reads it field-by-field while that thread writes it -
a torn read of a matrix that was never the map's.
The identity guard cannot catch it,
because the sample is a real non-identity transform belonging to some other draw.
The failure is a confident wrong point every frame,
not an absent one.

This is the asymmetry that makes the viewport safe but `MatrixManager` not.
The viewport read (`glGetInteger(int, IntBuffer)`) is answered synchronously from `context.attribTracker` on the calling thread
(`.../bridge/commands/GL11.java:994-1004`),
so it is caller-side state and reads true from anywhere.
`MatrixManager` is executor-side state,
so it does not.

From `v0.9.1rc1` the modelview gets the viewport's treatment.
A second copy of the stack,
`MatrixTracker`
(`Context.matrixTracker`, `.../bridge/context/Context.java:56`),
is updated inline on the calling thread by the same calls,
before they enqueue
(`.../bridge/commands/GL11.java:287-387`, `.../bridge/context/stall/MatrixTracker.java:15-83`).
It keeps a modelview and a projection stack,
switched by its own copy of the matrix mode (`:23-29`);
only the modelview is ever read from it.

Display lists leave it nothing to miss,
and how depends on the release.
From `v0.9.6rc1` the bridge refuses them:
`glNewList`, `glEndList`, `glCallList` and `glGenLists` throw `UnsupportedOperationException`
(`.../bridge/opengl/GL11.java:45-47`, `:413-415`, `:833-839`),
and `bridge.commands.GL11` no longer has them.
The game's own display-list cache,
`com.fs.graphics.util.GLListManager`,
is replaced by a stub whose `callList` always misses and whose `beginList` records nothing
(`starsector-core/fr/com/genir/renderer/overrides/render/GLListManager.java:8-28`),
so every game pass that would have replayed a list draws immediately,
through the tracked calls above.
Through `v0.9.5` lists were served:
while one was being compiled the tracker's updates were recorded into a caller-side list instead of applied,
and `glCallList` replayed that list on the calling thread.
A mod that compiles a display list of its own works through `v0.9.5`
and fails mid-render from `v0.9.6rc1`.

It has no CPU or GPU mode,
so it never answers identity in place of a transform.
It is what `glGetFloat(GL_MODELVIEW_MATRIX, FloatBuffer)` answers from.

A synchronous readback reads the right matrix but is a trap of its own.
The executor can run a read in-band and return it -
`Executor.get(GLGetter)` submits a callback and blocks for the result
(`.../bridge/context/executor/AsyncExecutor.java:58-63`),
which runs it on the render thread at the caller's own stream position,
exactly where the modelview is the map's.
But `get` routes through `Executor.wait`,
which swaps frames and blocks until the queue drains -
a **pipeline stall** -
and the bridge actively punishes stalling.
`Executor.wait` calls `StallDetector.detectStall` (`.../bridge/context/executor/AsyncExecutor.java:71`),
which counts stalled frames and **throws `RuntimeException("Asynchronous pipeline stall")`
once a caller stalls on 30 of any 60 frames**
(`:73`, `.../bridge/context/stall/StallDetector.java:7-8`, `:19-43`).
A per-frame synchronous read on the open map hits 60 of 60
and takes the game down within about a second.
So a synchronous read is not an option for anything drawn every frame.

Counting starts once the game is up,
and from `v0.8.7` "up" means **the end of game initialisation**.
The detector is armed in `ResourceLoaderState.initFastRendering`,
beside the flag Fast Rendering treats as "game initialised"
(`.../overrides/loading/ResourceLoaderState.java:125-135`),
which the title screen runs when it first comes up
(`.../overrides/TitleScreenState.java:12-15`),
after every mod's `onApplicationLoad` has run.
On `v0.9.6rc1` the same two lines closed `initEpilogue`, a step earlier.
So arming happens before a campaign can be entered,
and a per-frame stalling read on the campaign map is fatal from the first time the map is opened.
Through `v0.8.7rc1` the detector was armed on the first **combat frame** instead,
so a stalling map overlay could run for hours there and die on the first battle;
a stall crash on the sector map with no battle in the session is the expected shape from `v0.8.7`,
not a new bug.

A freeze leaves no such trace,
and from `v0.9.1rc1` there is a switch for one.
`-Dcom.genir.renderer.watchdog=<seconds>` in `fr.vmparams` starts an `FR-Watchdog` thread
that logs every thread's stack at INFO on that period
(`.../debug/Watchdog.java:17-28`, `:42-63`).
It is started at the top of game loading
(`.../overrides/loading/ResourceLoaderState.java:98`),
and the release ships it at `0`,
which is off.

The viewport read escapes the detector only because it never stalls:
the bridge answers `GL_VIEWPORT` inline from `attribTracker` and returns before reaching `exec.wait`
(`.../bridge/commands/GL11.java:994-1004`).
From `v0.9.1rc1` the modelview has the same inline path,
through `glGetFloat` and `MatrixTracker` (above),
and that read is current to the call.
Through `v0.9.0` it had none.
The only stall-free route then was a command enqueued through `Executor.execute`,
which returns without waiting,
copying the matrix on the render thread for a later frame to read.
[The upstream report](https://github.com/Halke1986/starsector-render/issues/11) records that workaround and what it cost.

KMLib does not ship it.
It reads through `glGetFloat` under either renderer,
and under Fast Rendering takes the read under a guard
(`FastRenderingModelviewMatrixReader`):
a release that refuses it costs the reading for the session rather than the render pass,
and the player is told once,
through the [compatibility channel](../../src/main/java/kmlib/starsector/compatibility/README.md),
which release the read needs.

### What the GL11 bridge can and cannot read back

Drawing is broadly covered.
Reading state back is not:
the bridge's entire *implemented* `glGet*` surface on `GL11` is `glGetInteger(int)` and `glGetInteger(int, IntBuffer)`,
`glGetString`,
`glGetFloat(int)`,
`glGetFloat(int, FloatBuffer)` (from `v0.9.1rc1`, for one pname),
`glGetError`,
`glGetTexLevelParameteri`,
`glGetTexParameteri` and two `glGetTexImage` overloads,
plus the two `glIs*` predicates and `glDrainErrors`,
which is not an LWJGL entry point at all
(`.../bridge/commands/GL11.java:943-1223`).

Read "implemented" strictly from `v0.8.9`,
because the facade changed what *declared* means.
`bridge.opengl.GL11` declares LWJGL's whole `glGet*` surface,
and every entry point outside the list above throws `UnsupportedOperationException` -
from `v0.9.0` with a message naming the method and its parameter types,
where through `v0.9.0rc2` the `GL11` to `GL20` facades threw it bare
and only the top stack frame said which call it was.
So the presence of a method on the class KM code now binds to says nothing about whether it works,
and the list above is still the whole of what does.

The buffer-taking `glGetFloat` serves **`GL_MODELVIEW_MATRIX` and nothing else**,
from `v0.9.1rc1`
(`.../bridge/commands/GL11.java:1060-1071`, `.../bridge/opengl/GL11.java:389-391`).
It answers inline from `MatrixTracker`
(see [It defers every GL call to a render thread](#it-defers-every-gl-call-to-a-render-thread)),
writing with `storeTranspose`,
so the buffer holds the column-major layout GL and `gluUnProject` expect.
It writes through a duplicate of the buffer,
so the caller's position does not move,
as with LWJGL's own read.
Every other pname throws `UnsupportedOperationException` from the implementation rather than the facade,
with the pname as a number in the message -
`GL_PROJECTION_MATRIX` included.
`glGetDouble` still throws in both forms,
so the double-precision read is no way round that.
Through `v0.9.0` the facade threw for every pname,
and through `v0.8.8` the method did not exist.

`glGetInteger(int, IntBuffer)` is bridged for **`GL_VIEWPORT` and `GL_SCISSOR_BOX` alone**,
so `GL_VIEWPORT` reads work unchanged under both renderers.
From `v1.0.1rc1` every other pname throws `UnsupportedOperationException`,
with the pname as a number in the message;
through `v1.0.0` the others fell through to `exec.wait` and stalled
(`.../bridge/commands/GL11.java:994-1015`).
It writes only the four ints a rectangle fills,
through a duplicate of the buffer,
and leaves the rest of a larger buffer alone.

From `v1.0.0` both answers come with a check.
The read answers from `attribTracker`'s copy of the rectangle,
and also queues a command that reads real GL on the render thread
and throws `AssertionError` if the two disagree
(`.../bridge/context/stall/AttribTracker.java:246-323`, `.../debug/Assert.java:31-35`).
The check is always on.
A throw on the render thread is re-raised on the calling thread at its next frame swap or blocking read,
so a disagreement ends the session a frame after the read that caused it.
The copies start at `0, 0, 0, 0`
and change only through a bridged `glViewport` or `glScissor`,
or a `glPopAttrib` restoring one
(`.../bridge/context/stall/AttribState.java:25`, `:33`, `:134`, `:138`, `:237`, `:244`).
So a read is safe once the game has set the rectangle through the bridge,
which the campaign UI's 2D entry does for the viewport every frame
(see [The campaign UI's GL setup](#the-campaign-uis-gl-setup)),
and fatal before that.

A second axis matters as much as which reads exist:
whether a read is answered inline or by stalling the pipeline.
There are three answers,
not two.
Reads served from state the bridge shadows on the caller's side are inline;
a handful of driver-level properties stall on their first read and are inline afterwards;
everything else routes through `exec.get` / `exec.wait` every time,
which is the stall the detector counts.

Always inline:

- `glGetInteger(int)` for the texture binding,
  matrix mode,
  active texture,
  **current program**,
  the blend equations and blend factors (from `v1.0.1rc1`),
  the unpack alignment (from `v1.0.4rc1`),
  array buffer binding,
  element array buffer binding (from `v0.9.0rc2`),
  framebuffer binding,
  vertex array binding,
  uniform buffer binding (from `v1.0.1rc1`)
  and shader storage buffer binding
  (`.../bridge/commands/GL11.java:943-992`).
- `glGetInteger(int, IntBuffer)` for `GL_VIEWPORT`,
  and from `v0.9.0rc2` for `GL_SCISSOR_BOX`,
  each checked against GL as above
  (`:994-1015`, `.../bridge/context/stall/AttribTracker.java:246-323`).
- `glGetFloat(int, FloatBuffer)` for `GL_MODELVIEW_MATRIX`,
  from `v0.9.1rc1`
  (`.../bridge/commands/GL11.java:1060-1071`, `.../bridge/context/stall/MatrixTracker.java:15-17`).
- `glIsEnabled` for stencil test,
  alpha test,
  texture 2D,
  blend,
  lighting,
  **scissor test**,
  and from `v1.0.4rc1` face culling,
  the one of them checked against GL the same way
  (`.../bridge/commands/GL11.java:1183-1204`, `.../bridge/context/stall/AttribTracker.java:57-112`).
- `glGetFloat(int)` for `GL_LINE_WIDTH`
  (`.../bridge/commands/GL11.java:1043-1058`).
- `glGetString` for `GL_EXTENSIONS`,
  which is captured every frame rather than on demand -
  and is handed back with `GL_ARB_vertex_buffer_object` **edited out**,
  so a capability probe under this renderer reports that extension missing whatever the driver says
  (`.../bridge/commands/GL11.java:1018-1041`).
- `glIsTexture`,
  from `v0.8.4`
  (`.../bridge/commands/GL11.java:1206-1209`, `.../bridge/context/stall/TextureTracker.java:73-117`).
  `TextureTracker` keeps a caller-side record of which target each texture name was last bound to,
  and the answer comes from that;
  the real GL call still runs,
  deferred,
  purely to assert the two agree.
- `glGetTexLevelParameteri` for `GL_TEXTURE_WIDTH`,
  `GL_TEXTURE_HEIGHT` and `GL_TEXTURE_INTERNAL_FORMAT`,
  from `v0.8.5rc1`
  (`.../bridge/commands/GL11.java:1131-1148`, `.../bridge/context/stall/TextureTracker.java:132-223`).
  `TextureTracker` caches those three per texture name as level 0 is uploaded,
  and the read is inline only when the texture currently bound **to that target** has an entry
  and the pname is one of the three;
  everything else -
  any other pname,
  an unseen texture,
  a non-zero level -
  still falls through to `exec.get` and stalls.
  Mip levels above 30000 are the exception and the trap:
  they short-circuit to `0` without touching GL or the cache at all
  (`.../bridge/commands/GL11.java:1132-1134`),
  which makes that a read to distrust rather than rely on.
- `glIsBuffer`,
  `glIsFramebuffer` and `glIsRenderbuffer`,
  from `v0.9.0`
  (`.../bridge/context/stall/BufferTracker.java:158-285`).
  A `BufferTracker` records every buffer,
  framebuffer and renderbuffer name bound on the caller's side,
  and the answer comes from that with the real call deferred as an assertion -
  the `glIsTexture` shape over again.

Inline only after one stall.
Four `glGetInteger(int)` pnames and three `glGetString` names go through a learn-on-first-miss cache
(`.../bridge/context/stall/StateCache.java:18-56`):
the first read misses,
stalls through `exec.get`,
and registers the pname;
every later read is inline and at most a frame stale.
The refill is part of the per-frame context update,
which runs on the render thread inside the `Display.update` command -
so "per frame" here means per presented frame,
not per game tick
(`.../bridge/context/Context.java:94-107`, `.../bridge/commands/Display.java:94-133`).

The four integer pnames are the **vendor VRAM queries** -
`34812` (`GL_TEXTURE_FREE_MEMORY_ATI`) and `36935`-`36937`
(the `NVX_gpu_memory_info` dedicated / total-available / current-available triple) -
and the three strings are `GL_VENDOR`,
`GL_RENDERER` and `GL_VERSION`.
That split explains the design:
the strings are constants that only need fetching once,
while free VRAM genuinely changes,
so a per-frame refresh is the point rather than an optimisation,
and a reader gets last frame's figure instead of stalling the pipeline for this one.

For KM code:
these are safe in a per-frame path *provided* the first read is not itself in one,
and a stall trace naming one of them is a first-use event rather than a recurring cost.
A VRAM reading taken under this renderer is also never current to the frame it is drawn on,
which matters for anything that displays it.

The set grows release by release,
each addition somebody's per-frame reader being saved from the stall detector,
so on an older release any of the reads above may still stall.
`v0.8.7` added `glGetTexParameteri`,
which is the counter-example worth holding onto:
it closed a `NoSuchMethodError` without adding an inline path,
so it always stalls
(`.../bridge/commands/GL11.java:1150-1160`).
`v0.9.0` added four more of the same kind -
`glGetBufferParameteri` on `GL15`,
`glGetActiveUniform` and `glGetActiveAttrib` on `GL20`,
`glGetProgramBinary` on `GL41` -
every one a round trip through the executor.
A call being *present* in the bridge is therefore not evidence that it is cheap -
and from `v0.8.9` it is not even evidence that it is implemented,
since the facade declares the whole LWJGL surface and throws on most of it.

`v0.8.9` removed a stall by the same reasoning outside `glGet*`:
`glMapBufferRange` / `glUnmapBuffer` are served from a CPU-side scratch buffer
(`.../bridge/context/stall/BufferManager.java:25-54`),
partially -
an unusual access mask or an overlapping map falls back to the stalling path.
`v0.8.10rc3` removed one stall for one mod:
`glDrainErrors` answers `0` inline and drains the real error queue on the render thread
(`.../bridge/commands/GL11.java:1211-1223`),
and the agent renames `glGetError` to it inside two FarsightDrive renderer classes and nowhere else
(see [It rewrites GL class references in every jar](#it-rewrites-gl-class-references-in-every-jar)).
A KM call to `glGetError` is still an `exec.get` and stalls
(`:1119-1129`).

Two consequences for KM code.
Whether a given read is fatal depends on the Fast Rendering version,
so "it worked on my install" proves less than it looks -
and `v0.8.5rc1` makes that worse,
since it is indistinguishable from `v0.8.4` in a log or on the main menu
while answering two of these reads differently.
The scissor **box** is the clearest case:
through `v0.9.0rc1` only the enable flag was shadowed,
so "is the clip on,
and what is it" was half safe and half fatal -
two lines,
two different answers;
from `v0.9.0rc2` `glGetInteger(int, IntBuffer)` answers `GL_SCISSOR_BOX` inline beside `GL_VIEWPORT`
(`.../bridge/commands/GL11.java:994-1015`, `.../bridge/context/stall/AttribTracker.java:286-323`),
every other pname falling through to `exec.wait` through `v1.0.0` and throwing from `v1.0.1rc1`.
The modelview read spans three answers across the field:
`NoSuchMethodError` through `v0.8.8`,
`UnsupportedOperationException` from `v0.8.9` through `v0.9.0`,
and an inline read from `v0.9.1rc1`.
Clip composition stays the caller's job either way
(`kmlib.starsector.ui.render.gl.UiScissor`),
and the one place KMLib reads the box back -
the clip a map hover is diagnosed against,
in `MapCursorRead.describeRead` -
stays gated off under this renderer,
because the gate cannot see which release is underneath
and a diagnostic must not be able to end the session it was turned on to explain.
From `v1.0.0` the box read is checked against GL as well,
which is one more way for it to do that.

The projection read is best avoided outright:
the campaign UI's projection is derivable arithmetically (below),
and no release serves `GL_PROJECTION_MATRIX`.
The modelview is an ordinary `glGetFloat` from `v0.9.1rc1`.
Through `v0.9.0` it is available only from the render-thread matrix,
never by reading it back through GL
(see [It defers every GL call to a render thread](#it-defers-every-gl-call-to-a-render-thread)).

### Its reach goes past the GL bridge

Fast Rendering also changes the game's own classes,
not just what GL calls resolve to,
and the way it does so changed in `v0.9.0rc1`.

From `v0.9.0rc1` the agent **patches game classes as they load**.
For each of 31 named classes it removes or renames particular methods on the game's class
and then copies every method of a donor class from `fr.jar`'s `overrides` package into it
(`starsector-core/fr/com/genir/renderer/agent/transformations/bytecode/BytecodeClassTransformer.java:49-183`,
`.../bytecode/BytecodeTransformer.java:30-66`),
with ASM from a jar embedded in `fr.jar`.
A renamed method keeps the vanilla body under a `_vanilla` suffix for the donor to call;
a removed one is replaced outright.
The donors are compiled against readable names,
and the `overrides` map renames them onto the game's as they are copied
(`Transformations.overrides`, `.../agent/transformations/symbols/Transformations.java:21`).
By their readable names, the classes patched this way are:

- graphics: `TextureLoader`, `Sprite`, `util.GLListManager`,
  and the particles `BaseParticle`, `SmoothParticle`, `NebulaParticle`, `GenericTextureParticle`,
  `NegativeParticle` and `DynamicParticleGroup`;
- combat and its effects: `combat.CombatEngine`, `combat.CombatState`,
  `combat.entities.ContrailParticle`, `renderers.fx.DetailedSmokeParticle`
  and `api.impl.combat.threat.RoilingSwarmEffect`;
- loading: `loading.scripts.ScriptStore`, `loading.SpecStore`, `loading.ResourceLoaderState`
  and `loading.LoadingUtils`;
- the rest: `util.Tesselator`, `title.TitleScreenState` and `settings.StarfarerSettings`;
- and ten found by analysing the game's obfuscated classes rather than by name:
  `Expression`, `ProgressBar`, `DeploymentManager`, `Bounds`,
  `SoundStore`, `FileLoader`, `HullSpecStore`, `WeaponSpecStore`, `FontRenderer`
  and `StarfarerSettings$SettingsAPIImpl`.

Through `v0.9.8` the switch was `BytecodeFileTransformer` in `fr.agent.jar`,
patching 25 classes on `v0.9.0rc1` and growing since.

The `Sprite` patch replaces `render(float, float)`,
`renderNoBind` and `renderRegion`
(`.../bytecode/BytecodeClassTransformer.java:115-119`)
with one bridge command per sprite
in place of the twenty-odd GL calls the vanilla body issues
(`starsector-core/fr/com/genir/renderer/overrides/render/Sprite.java:43-112`).
Its matrix push,
transforms and pop run on the render-thread `MatrixManager` alone,
inside that command,
so `MatrixTracker` never sees them;
being balanced,
they leave nothing for it to miss
(`.../overrides/render/Sprite$Sprite$render.java:49-72`).
The particle patches batch each group into one draw and make no matrix call at all:
each particle's corners are worked out from its centre,
angle and size inside the draw command,
which reads neither copy of the modelview
(`.../overrides/render/particle/ParticleRenderer$ParticleRenderer$render.java:31-41`, `:93`).
Through `v0.9.8` they pushed,
loaded identity and popped through the bridge's own `GL11`,
which the tracker saw.
Three more are changed by constant rewriting alone -
`Version` for the banner,
`BaseGameState` for the frame pacing
and `Planet` for the sphere (above) -
and `combat/CombatState` gets the pacing rewrite on top of its patch.

Through `v0.8.10` the same reach was had by **shadowing**:
`fr.jar` shipped whole patched copies of 21 game classes under `com.fs.*` and `sound.*`,
ahead of the game's own copies in `starfarer_obf.jar` / `fs.common_obf.jar` on the classpath.
That is why a `v0.9.0rc1` jar is 184 KB smaller than a `v0.8.10` one,
and why a decompile of `fr.jar` from `v0.9.0rc1` on has no `com/fs` tree at all:
the overrides are no longer complete classes,
and a frame in a trace that names a game class is running a mix of the game's methods and genir's.

The donors are compiled against readable aliases -
`com.fs.graphics.TextureHandler`,
`com.fs.util.FileLoader` and the like -
which the obfuscation table maps onto the game's obfuscated names as each class loads
(`Transformations.obfuscation`, `.../agent/transformations/symbols/Transformations.java:20`).
From `v1.0.0rc1` that table is built at startup by analysing the game's classes,
rather than written out by hand for one game build,
which is what lets one release patch several.
So a name that looks like a public game type in a decompile of `fr.jar` may be an alias for an obfuscated one,
and that table is where to resolve it;
it carries method and field aliases too,
which is why entries like `ProgressBar_render` sit in the same map,
and an alias that resolves on one release may not exist on the next.

So when behaviour differs under Fast Rendering,
the bridge is not the only place to look,
and a decompile of the game's own jar is not necessarily what is executing.
The patch set is the `switch` in the agent's `applyTransform`;
list the donors with:

```plaintext
Glob "**/overrides/**/*.java" in <starsector>/.sources-cache/starsector-core/fr
```

### It uploads VRAM Optimizer's textures lazily

With the VRAM Optimizer mod enabled,
Fast Rendering loads every image that mod has converted from its DDS cache
(`starsector-core/fr/com/genir/renderer/overrides/loading/textures/DDSIntegration.java:44-51`, `:134-203`).
Such a texture is not uploaded when it loads.
`DDSIntegration.commitTexture` binds the texture name it is given
and registers it with the `TextureManager` as unloaded
(`:61-113`).
The first bridged `glBindTexture` of that name uploads it
(`.../bridge/context/TextureManager.java:55-65`).
Without VRAM Optimizer the path never runs,
and every texture uploads as it loads.

The laziness has an end.
`ResourceLoaderState.initMain` sets `GameState.assetLoadingFinished`
once the game's own specs have loaded,
and from `v0.9.4` it does so before any mod's `onApplicationLoad` runs
(`.../overrides/loading/ResourceLoaderState.java:105-108`, `:205-227`).
After that a DDS texture uploads as it loads too
(`.../bridge/context/TextureManager.java:38-42`).
So the window is the game's own asset loading,
and no `onApplicationLoad` falls inside it.
Through `v0.9.3` the moment came after the loop,
so every `onApplicationLoad` fell inside the window.
On `v0.9.6rc1` the moment was an `assetLoadingFinished` call in `initEpilogue`,
where `v1.0.4rc1` sets a flag in `initMain`.

A texture can be dropped inside that window without ever uploading.
A bridged call that writes image data -
`glTexImage1D`,
`glTexImage2D`,
`glCompressedTexImage2D`,
`glCopyTexImage2D` or `glTexStorage2D` -
calls `TextureManager.textureModified`
(`.../bridge/commands/GL11.java:670`, `:687`, `:704`, `:799`; `.../GL13.java:34`; `.../GL42.java:39`)
on whichever texture the calling thread has bound,
as `attribTracker` records it (`.../GL11.java:668`).
That marks it as not managed
and throws its loader away
(`.../bridge/context/TextureManager.java:83-93`).
A lazy texture is still bound after its own registration,
so a later upload made without binding a texture first lands on it.
From then on a bind does nothing,
and the texture never gets an image.

A text atlas dropped this way draws every glyph as a solid quad in the text colour.
KMLib's map label face was dropped like this when it loaded in `onApplicationLoad` on `v0.9.3` and earlier,
by a call never identified among a player's mods.
`v0.9.4`'s notes list a workaround for that LazyFont symptom,
and moving `assetLoadingFinished` ahead of the mods is the change that matches it.
The notes say the underlying issue remains,
and the code agrees:
the drop itself is unchanged,
only the window it can happen in is shorter.
So a texture is loaded after the window on every release:
`KMLib_ModPlugin` loads its font faces on the first game load.

The log tells the paths apart
(`.../overrides/loading/textures/TextureLoader.java:98`, `.../bridge/context/TextureManager.java:39`, `:44`, `:63`):

- `Loading image [path]` is a texture that uploaded as it loaded,
  not through VRAM Optimizer.
- `Loading DDS texture  [path]`,
  with two spaces and no count,
  is a DDS texture that uploaded as it loaded:
  one registered after the window,
  or under a name already marked as not managed.
- `Loading DDS texture n/N [path]` is a lazy texture uploading on its first bind.
  The count is how many have uploaded so far out of how many were registered.
- **No line at all** for a texture that is drawn means it was registered and then dropped.

GL state cannot confirm an upload.
`glGetTexLevelParameteri` answers the width,
height and format from `TextureTracker`
(see [What the GL11 bridge can and cannot read back](#what-the-gl11-bridge-can-and-cannot-read-back)),
and `commitTexture` fills that cache at registration
(`.../overrides/loading/textures/DDSIntegration.java:112`).
A dropped texture reports its full size.

## The campaign UI's GL setup

### Projection, viewport, and two coordinate spaces

`CampaignState.render` enters 2D mode through `com.fs.graphics.util.B` immediately before rendering the screen panel that holds the whole UI tree,
including the map
(`starsector-core/starfarer_obf/com/fs/starfarer/campaign/CampaignState.java:1582-1587`).
That helper does `glLoadIdentity()` then `glOrtho(0, width, 0, height, -6000, 6000)`,
and sets the viewport to the full window scaled by `Display.getPixelScaleFactor()`
(`starsector-core/fs.common_obf/com/fs/graphics/util/B.java:130-145`).

The width and height it passes come from two obfuscated `StarfarerSettings` statics
(`.../campaign/CampaignState.java:1551-1552`),
which are exactly what the public `SettingsAPI.getScreenWidth()` and `getScreenHeight()` return
(`.../settings/StarfarerSettings.java:1905-1911`).
Read that pair of files together to re-confirm it;
the obfuscated names themselves are not reproduced here,
since they are non-ASCII and change every game build.

So the projection is a known,
axis-aligned ortho in UI virtual units and never needs to be read back from GL:
`Global.getSettings().getScreenWidth()` and `getScreenHeight()` reconstruct it.

The depth range is `-6000..6000`.
The caller passes `1000`,
but the helper overwrites that argument (`.../util/B.java:131`).
It makes no difference to unprojected x/y,
since an axis-aligned ortho has no shear.

Two coordinate spaces follow,
and mixing them is the failure mode:

- **UI virtual units** span `0..getScreenWidth()`.
  This is what the ortho and all UI widget positions are in.
- **Physical pixels** span `0..getScreenWidth() * getPixelScaleFactor()`.
  This is what `GL_VIEWPORT` reports and what `org.lwjgl.input.Mouse.getX/getY` return (bottom-left origin).

`gluUnProject` reconciles them when handed the real viewport,
which is why the viewport is worth reading from GL rather than deriving.
`getScreenWidthPixels()` and `getScreenHeightPixels()` are the API-side equivalents
(`.../settings/StarfarerSettings.java:1913-1919`).

KM code reaches all four of those through [`VanillaScreen`](../../src/main/java/kmlib/starsector/ui/screen/VanillaScreen.java)
rather than through `SettingsAPI` directly,
and converts between the two spaces through [`ScreenAxis`](../../src/main/java/kmlib/starsector/ui/screen/ScreenAxis.java),
which holds one axis's two lengths together.
That is what makes the mix-up this section warns about -
invisible at a pixel scale of 1,
and therefore on the machine the code is written on -
impossible to express rather than merely discouraged:
a pixel length cannot be obtained apart from the UI length it belongs with.

### The modelview around a map render

The campaign UI's base modelview is `identity + translate(0.01, 0.01)` (`.../util/B.java:142-143`).

The sector map widget adds exactly one modelview change around the terrain render:
`glTranslatef((int)centerX, (int)centerY, 0)`,
where the centre is the widget's own screen position
(`starsector-core/starfarer_obf/com/fs/starfarer/coreui/A/H.java:432-433`).
Pan is not a matrix translation of its own:
the map panel sits in a scroller,
so scrolling moves the widget's position and the translate follows.

So the whole world-to-screen transform for a map overlay is that translation plus the per-vertex `factor` the game hands to `renderOnMap`,
and no part of it is a rotation or a shear.
A real map pass therefore never has an identity modelview,
which is what makes identity usable as an error signal.

### Map pan and zoom state

`com.fs.starfarer.campaign.CampaignUIPersistentData` implements `DoNotObfuscate`,
so its name is stable across game builds,
and it carries the sector map's live pan and zoom:
`hyperMapCoordinates`,
`hyperMapZoom`,
and the in-system equivalents
(`.../campaign/CampaignUIPersistentData.java:29-35`).
They are written during the map's own pass (`.../coreui/A/G.java:334-336`),
not only on close,
so they are live rather than last-session values.

Reconstructing a screen transform from them is a trap,
though,
and was rejected for KMLib's map transform:
inverting them needs the map panel's position and size,
which live on `coreui.A.G` and `coreui.A.H`.
Those names are obfuscated and not stable across game builds,
unlike `CampaignUIPersistentData` itself.

## Traps

- **`com.fs.starfarer.prototype` is dead demo code.** It contains its own GL setup with a hardcoded `glOrtho(0, 1024, 0, 768, ...)`
  (`.../prototype/super/A.java:36-39`, `.../prototype/super/Object.java:36`).
  It looks like the game's real projection and is not;
  it is a standalone LWJGL demo with its own `main`.
  The real path is `CampaignState.render` (above).
- **A grep for `glOrtho` finds the wrong answer first.** The hits in `com.fs.graphics` are the live ones;
  check the caller before believing any of them.
- **A GL call that compiles is not a GL call that resolves.** Under Fast Rendering the binding changes after compile,
  so the bridge's surface,
  not LWJGL's,
  is what a KM jar actually gets.
