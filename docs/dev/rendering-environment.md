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
| Starsector | `0.98a-RC8` | - |
| Fast Rendering | `v0.8.9` | `fr.jar` SHA-256 `e669b6dd6b9c1fc4e34b44b40e9e03c19374d8dc46e7820b9e3a4813cbc3208f`, 716583 bytes; `fr.agent.jar` SHA-256 `a6bd66743a95104f07a8bb367356881fcfae7549d26d765d296c094ea687590d`, 14841 bytes |

That row is the release the citations below were read out of.
An install is patched by dropping the release zip's jars into `starsector-core\`,
so a matching hash means the recorded artifact and the running one are the same file.

Check both jars,
and check `fr.agent.jar` by hash only.
Its size sat at 15558 bytes across `v0.8.5rc1`,
`v0.8.6`,
`v0.8.7` and `v0.8.8` alike
(15561 on `v0.8.7rc1`)
while its contents changed in every one of them,
so a size *match* there is no evidence.
`v0.8.9` is the first release in that run to move it,
to 14841,
because it merged two transformation tables into one class -
but treat that as a coincidence of this release rather than a habit.
That jar is where the bridge package name and the rewrite tables live,
which is the half of the patch KM's detection depends on.

Ten claims on this page are release-specific rather than true of every build still in the field,
and each is flagged where it appears:

- **Per-mod exclusion exists**,
  from `v0.8.5rc1`.
  Earlier releases rewrite every mod jar without exception;
  `v0.8.5rc1` hard-codes a class-name prefix that is skipped.
  Its second half -
  a separate skip of VRAM Optimizer's *plugin* during resource loading -
  existed on `v0.8.5rc1` alone and is gone from `v0.8.6`.
- **Stall detection is armed at the end of game initialisation**,
  from `v0.8.7`.
  Through `v0.8.7rc1` it was armed on the first combat frame instead,
  which made a stalling read on the campaign map survive any session that never entered a battle.
  This is the delta on this page with the most teeth for KM code.
- **`GL_CURRENT_PROGRAM` is answered from `AttribTracker`** on `v0.8.5rc1` through `v0.8.9` and on `v0.8.3` and earlier,
  and from `ShaderTracker` on `v0.8.4` alone.
- **`glIsTexture` is an inline read**,
  from `v0.8.4`.
  On `v0.8.3` and earlier it stalls.
- **`glGetTexLevelParameteri` is an inline read** for three pnames,
  from `v0.8.5rc1`.
  Earlier releases always stall on it.
- **`glGetTexParameteri` exists at all**,
  from `v0.8.7`.
  Calling it on an earlier release is a `NoSuchMethodError` from inside a render pass;
  calling it on `v0.8.7` or later stalls,
  since it has no inline path.
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
- **Display lists are callable from mod code**,
  from `v0.8.9`.
  Through `v0.8.8` `glGenLists` / `glNewList` / `glEndList` / `glCallList` were deliberately unreachable,
  under `*_restricted` names.
- **The game's own classes get the full GL rewrite**,
  from `v0.8.9`.
  Through `v0.8.8` `com.fs.*` and `sound.*` had only `GL11`,
  `GL14`,
  `Display` and `GLContext` redirected.

Nothing KMLib binds to has moved or changed signature through `v0.8.9`.
`GLCommand`,
`GLGetter`,
`TransformManager` and `ContextManager` are byte-identical across `v0.8.5rc1` to `v0.8.9`;
`Context.transformManager`,
`Context.exec` and `Executor.execute(GLCommand)` kept their names and signatures
while the code around them turned over -
`Context` and `Executor` were both reworked in `v0.8.9` without touching either.
`VertexInterceptor` and `MatrixStack` were both rewritten in this span without touching the two facts KM reads off them
(the CPU-modelview multiply and the row-major field order).

The table that decides the bridge package name is the exception,
and it broke its own run in `v0.8.9`:
`ScriptTransformations` was byte-identical from `v0.8.5rc1` to `v0.8.8`,
then `v0.8.9` merged it with `ObfTransformations` into a single `Transformations` class
holding one named map per rewrite family,
and repointed every `GLnn` entry at the new facade package.
Neither the merge nor the repoint is in the release notes.
KM detection spans it because it matches the `com.genir.renderer.` prefix rather than a whole class name,
which is the case this tolerance was built for.

Fast Rendering names itself in three places,
and from `v0.8.6` **all three agree with the release**.
`v0.8.5rc1` is the one build in the field where all three lie:
each says `v0.8.4`,
because the three files carrying them shipped byte-identical to the `v0.8.4` release.
So a self-report is a lower bound on the release rather than the release,
and the jar hash is what settles it.

The agent logs a release at startup,
before anything else runs:
`Agent.premain` writes `Fast Rendering: v0.8.9` at INFO,
followed by the SHA-256 of `starfarer_obf.jar`
(`starsector-core/fr.agent/com/genir/renderer/agent/Agent.java:21-24`).
The line appeared in `v0.8.4`;
earlier releases log nothing.
That checksum is of the *game's* jar,
not Fast Rendering's,
so it identifies the Starsector build being patched and says nothing about which patch is doing it.

`fr.jar` carries a version constant from `v0.8.2`,
`com.genir.renderer.Version.getVersion()`
(`starsector-core/fr/com/genir/renderer/Version.java:7-8`),
returning a release string -
`"v0.8.9"` on `v0.8.9`.
Nothing in either jar calls it,
so it names the artifact rather than the session,
which is what a build wants:
KMLib's `build.gradle` loads it straight out of the jar to report
which Fast Rendering the bridge adapter was type-checked against.
The banner is therefore only as honest as the constant:
correct from `v0.8.6`,
and a KMLib build bound against `v0.8.5rc1` reports `Fast Rendering: v0.8.4`.

The third is a display string in the shadowed copy of the game's own version class,
`"Starsector 0.98a-RC8 FR8.9"`
(`starsector-core/fr/com/fs/starfarer/Version.java:28`, `:36`),
which is what puts `FR8.9` on the launcher and the main menu.
Its shape is `FR` followed by the release tag with its leading `v0.` dropped,
patch letters and release-candidate suffixes included,
so `v0.7.1` reads `FR7.1`,
`v0.7.1b` reads `FR7.1b` and `v0.8.7rc1` reads `FR8.7rc1`.
It tracked the release exactly except at `v0.8.5rc1`,
which reads `FR8.4`.
It is the only identifier readable without opening a jar and so the one to ask a player for,
with that single ambiguity:
`FR8.4` is either `v0.8.4` or `v0.8.5rc1`.

None of the three proves the bytes,
and -
as `v0.8.5rc1` demonstrates -
a rebuild can carry all of them unchanged.
The SHA-256 is what identifies what these citations were read from.

Releases are published at [Halke1986/starsector-render](https://github.com/Halke1986/starsector-render/releases),
which ships `fast-rendering-<version>.zip`.
To identify an arbitrary install,
hash its jar and compare:

```bash
sha256sum "<starsector>/starsector-core/fr.jar"
```

Sizes still separate neighbouring releases
(`v0.7.6` is 632557 bytes, `v0.7.7` is 632640, `v0.8.0` is 617425, `v0.8.1` is 631624, `v0.8.2` is 635887, `v0.8.3` is 634576, `v0.8.4rc1` is 632459, `v0.8.4` is 639820, `v0.8.5rc1` is 642412, `v0.8.6` is 660831, `v0.8.7rc1` is 666557, `v0.8.7` is 668949, `v0.8.8` is 669864, `v0.8.9` is 716583),
so a size mismatch is a fast first check before hashing.
Treat only the mismatch as informative:
`v0.7.6` and `v0.7.7` are 83 bytes apart,
close enough that a size *match* is weak evidence and hashing is what settles it.
Release candidates are published as ordinary releases and get installed as such,
so `rc` builds are part of the set an install can be -
and on `v0.8.5rc1` the size is the *only* cheap discriminator against `v0.8.4`,
since all three self-reports agree with it.

Names below are release-specific,
and a rename is not announced.
Three moves so far have invalidated citations wholesale,
none mentioned in its release notes.
`v0.7.4` moved the whole GL bridge from `com.genir.renderer.bridge` to `com.genir.renderer.bridge.commands`,
and the command interfaces to `com.genir.renderer.bridge.interfaces`.
`v0.8.0` moved the bytecode-rewriting machinery out of `fr.jar` entirely:
the package `com.genir.renderer.loaders` no longer exists,
and its transformation tables now sit in a second jar,
`fr.agent.jar`,
under `com.genir.renderer.agent`.
`v0.8.9` split the bridge in two:
the implementations stayed in `com.genir.renderer.bridge.commands`,
and a new `com.genir.renderer.bridge.opengl` became what GL references actually rewrite to.
The older move is the one that looks like the newer one and is not:
`v0.7.4` relocated the classes,
while `v0.8.9` left them where they were and put a layer in front.
So a `bridge.commands.GL11` frame means "at or after `v0.7.4`" and nothing more,
and only a `bridge.opengl` frame dates a trace to `v0.8.9` or later.
Every KM fact that names a bridge type -
the detection string,
the compile-only stubs -
is therefore a fact about one range of releases,
and the hash is what says which.

The bridge's *members* have been stabler than its files.
Nothing KMLib names has moved or changed signature since `v0.7.4`,
while the code around them turns over steadily:
`context/BufferPool` was rewritten in `v0.8.0`,
`commands/GL11`'s `glGetTexImage` gained a compressed-texture path in `v0.8.3`,
and `v0.8.4` repacked `context/VertexInterceptor`'s vertex arrays,
added `stall/TextureTracker`,
moved program tracking from `AttribTracker` to `ShaderTracker`,
and deleted `commands/ARBVertexBufferObject` and `stall/BufferManager`.
`v0.8.5rc1` moved texture loading off the startup path into a new `context/TextureManager` and a new `overrides/loading/textures/` package,
deleting `overrides/loading/DDSCache` and relocating `TextureBuilder`,
`TextureData` and `TextureLoader` into it;
deleted `commands/DisplayUtil` and stopped redirecting `org/lwjgl/util/Display` to it;
made most of `commands/Display` call real LWJGL inline instead of deferring;
and re-signatured `context/Context`'s constructor from `Context(boolean)` to `Context(Context parent)`,
so auxiliary contexts now share the main context's texture and shader trackers.
`v0.8.6` added `async/AsyncException`,
deleted `overrides/PathUtil`,
and rewrote `stall/TextureTracker` to record each texture name's bound *target*
rather than a plain "is bound" flag.
`v0.8.7rc1` added `interfaces/DebugString` and changed the frame's argument packing (below).
`v0.8.7` added `overrides/loading/textures/Blacklist` and moved the stall detector's arming point out of `overrides/CombatEngine` (below).
`v0.8.9` added the whole `bridge/opengl` facade package,
re-signatured `context/Context`'s constructor again -
`Context(Context parent)` became a no-arg constructor for the main context and `Context(Context, SharedDrawable)` for auxiliary ones,
with `shutdown()` renamed `destroy()` and a new `restoreCurrent()` -
renamed `ContextManager.destroyMainContext` / `destroyAuxContext` to `removeMainContext` / `removeAuxContext`,
rewrote `Executor`'s exception plumbing around a nested `ExceptionHandler` and an `isStateCorrupted` flag,
moved `commands/GL11`'s compressed-texture read into a new `context/TextureReadManager` that owns its own FBO and shared drawable,
and brought back a `stall/BufferManager` -
the name `v0.8.4` deleted,
now holding an unrelated mapped-buffer shim (below).

So byte-identity of the tree is the wrong thing to check on an upgrade;
the six members KMLib mirrors are -
`ContextManager.getThreadContext`,
`Context.transformManager`,
`Context.exec`,
`Executor.execute(GLCommand)`,
`GLCommand.run` and `TransformManager.getCPUModelView` -
and the real-jar build leg is what checks them.

## How to re-verify

Every claim below carries a `file:line` into the decompiled sources cache at `<starsector>\.sources-cache\`,
whose layout mirrors the relative JAR path under the game install.
Regenerate it with the `/jar-search` command.

Fast Rendering occupies two cache roots,
not one:
`starsector-core\fr\` for the bridge and the shadowed game classes,
and `starsector-core\fr.agent\` for the bytecode rewriting.
A glob of `fr.jar` alone misses half of it;
use `fr*.jar`.

The cache is keyed on the jar's path,
not its contents,
and a Fast Rendering upgrade replaces `fr.jar` in place.
So an upgraded install keeps serving the previous release's sources until the extraction is forced,
and every read off it describes a jar that is no longer there.
Re-extract with `-Force` after any upgrade,
and check `com/genir/renderer/Version.java` against the table above before trusting a line number.

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

It lives at `starsector-core\fr.jar` beside a second jar,
`fr.agent.jar`,
its own launchers `fr.bat` and `fr.noterminal.bat`,
and its own JVM argument file,
`fr.vmparams`.
There is no folder under `mods\`,
so searching `mods\` for it finds nothing and its presence is not declared in any `mod_info.json`.

Nothing on disk is rewritten.
`fr.vmparams` sets `-javaagent:fr.agent.jar`,
so the patch is a JVM instrumentation agent,
applied to bytes on their way into the JVM;
the game's own jars are read unmodified.
Two consequences worth holding onto:
the install is only patched when the player launches `fr.bat` rather than `starsector.exe`,
so the same install runs stock or patched depending on which one was double-clicked,
and a `starfarer.api.vanilla.jar` sitting in a core directory is somebody else's doing,
not Fast Rendering's.

The two jars split the work,
and which one a fact lives in matters when reading a stack trace.
`fr.agent.jar` is small and holds only the rewriting:
an agent entry point (`.../agent/Agent.java:21-24`),
the `ClassFileTransformer` that picks a rewrite table per class,
and the tables themselves.
`fr.jar` holds everything the rewritten code then resolves to -
the GL bridge,
the shadowed game classes -
and is what a KM build binds against.

`fr.bat` also passes `-javaagent:PatchLibAgent.jar` when that file is present,
so a PatchLib-based mod and Fast Rendering coexist as two ordinary agents on the same JVM.
Fast Rendering's transformer sees every class either agent's loaders pull in,
so PatchLib's view of the game classes is the rewritten one.

This arrangement is new in `v0.8.0`.
Through `v0.7.7` the patch was instead the system classloader
(`-Djava.system.class.loader=com.genir.renderer.loaders.AppClassLoader`),
which loaded and rewrote classes itself
and had a bespoke path to route an agent's classes back through its own transformer.
`com.genir.renderer.loaders` no longer exists,
so a stack frame naming it is from `v0.7.7` or earlier.

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
(`Transformations.opengl`, `starsector-core/fr.agent/com/genir/renderer/agent/Transformations.java:12`).

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
anything left that is loaded by a loader which is neither the system loader nor the agent's own takes the mod transformer
(`.../agent/ClassTransformer.java:35-65`).

The game transformer stacks four tables:
the same full GL list the mods get,
janino's `JavaSourceClassLoader` swapped for a plain `ClassLoader`,
the obfuscation aliases,
and a rewrite of obfuscated names that collide with Java keywords -
`class.do` to `class_do` and so on,
which is why a decompile shows types like `com/fs/starfarer/util/return`
(`.../agent/ClassTransformer.java:20`, `.../agent/IllegalTransformations.java:12-23`).

Two things about the game's share of this are `v0.8.9` changes.
Through `v0.8.8` it was a *narrower* GL rewrite -
`GL11`,
`GL14`,
`Display` and `GLContext` only -
so the game reached real LWJGL for everything else while mods did not.
It now gets the same list as mods.
And through `v0.8.8` it also renamed the display-list calls
`glGenLists` / `glNewList` / `glEndList` / `glCallList` to `*_restricted` variants,
which is how the game was steered off display lists;
`v0.8.9` dropped that rename,
implemented all four under their real names in `bridge.commands`,
and exposed them through the facade,
so display lists are now available to the game and to mods alike.
Through `v0.8.4` the game's table also redirected `org/lwjgl/util/Display` to a `commands/DisplayUtil` shim;
`v0.8.5rc1` dropped both.

This applies to KM jars,
and no KM code can turn it off:
a KM jar compiles against real LWJGL and links fine,
then binds to the bridge at runtime,
so a bridge gap surfaces from inside a render pass rather than as a build failure -
as an `UnsupportedOperationException` from `v0.8.9`,
and as a `NoSuchMethodError` through `v0.8.8`.

There is no opt-out a mod can *request* -
no annotation,
no manifest entry,
no setting.
What `v0.8.5rc1` introduced is an opt-out genir grants:
classes whose binary name starts with `DeCell.VOpt.Commons.Rendering.` get no transformer at all
(`.../agent/ClassTransformer.java:61-63`).
Earlier releases have no such branch,
and that one has been byte-identical since.
`v0.8.5rc1` also had `overrides/loading/ResourceLoader` skip mods whose plugin class starts with `DeCell.VOpt`;
that half is gone from `v0.8.6`,
so the exclusion is now purely a bytecode-rewriting one and the mod's plugin loads like any other.

VRAM Optimizer is the mod on the far side of that exclusion,
and Fast Rendering reaches back into it by name rather than the other way round:
`DDSIntegration` looks up `DeCell.VOpt.Commons.Rendering.TextureLoading.UploadDDSTexture` and falls back to an older `...Rendering.Textures` entry point when that is absent
(`.../overrides/loading/textures/DDSIntegration.java:260`, `:268`).
Both are reflective lookups through the script classloader,
so neither mod is a build-time dependency of the other,
and a missing method is logged rather than fatal.

That matters to KM code less for the one mod named than for the shape it establishes,
because an excluded jar is a *mixed* state this page's model does not otherwise admit:
its GL references stay pointed at real LWJGL while the game around it runs on the bridge.
Detection stays correct through it -
the check below asks what *this* jar's `GL11` resolved to,
which is exactly the question an exclusion changes the answer to -
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

`VertexInterceptor.glVertex3f` multiplies each vertex by `TransformManager.getCPUModelView()` before submitting it
(`.../bridge/context/VertexInterceptor.java:135-139`),
and in that mode `TransformManager.setCPUMode()` loads **identity** into GL
(`.../bridge/context/TransformManager.java:30-37`).

So while Fast Rendering is in CPU mode,
GL's modelview does not describe what is being drawn.
Reading `GL_MODELVIEW_MATRIX` back would return identity
while the real transform sits in a Java object.
A crash on the read is the polite failure;
a pass-through implementation returning identity would be the impolite one,
because it yields silently wrong coordinates instead.

The matrix is reachable,
publicly:
`ContextManager.getThreadContext()` is public static
(`.../bridge/context/ContextManager.java:20`),
`Context.transformManager` is a public final field (`.../bridge/context/Context.java:50`),
and `getCPUModelView()` is public
(`.../bridge/context/TransformManager.java:49`).
Only the modelview is tracked this way.
`TransformManager` mirrors GL's matrix stack only while the matrix mode is `GL_MODELVIEW`
and delegates to real GL otherwise
(`.../bridge/context/TransformManager.java:146-148`),
so there is no CPU projection matrix to read and the campaign's ortho goes straight to GL.

`getThreadContext()` returns `null` more often than "unregistered thread" suggests.
From `v0.8.5rc1` the main context is created lazily and cleared on shutdown
(`.../bridge/context/ContextManager.java:16-18`, `:26-36`),
so the same call on the same thread answers `null` before the renderer is up
and again after it is torn down;
earlier releases built it in a static initialiser and it was never null on the main thread.
Any read of it has to null-check regardless of release.

Four cautions on using it.
The first is where,
not what,
and it dwarfs the rest:
the matrix cannot be read correctly from the calling thread at all,
because the bridge mutates it on a separate render thread a step behind.
That is its own section below
([It defers every GL call to a render thread](#it-defers-every-gl-call-to-a-render-thread));
the remaining three assume the read already runs there.

It returns the live mutable matrix,
not a copy,
so callers must copy before holding it.
And it returns identity when Fast Rendering has instead pushed the matrix to the GPU
(`.../bridge/context/TransformManager.java:49-53`),
so identity means "this read is not usable",
not "no transform".
That is safe to lean on because a real campaign-UI pass is never identity
(see [The modelview around a map render](#the-modelview-around-a-map-render)).

The third is the quiet one:
**its `Matrix4f` fields are row-major**,
transposed from the convention LWJGL's own `Matrix4f` uses.
`VertexInterceptor.glVertex3f` takes the translation from `m03/m13/m23`
(`.../bridge/context/VertexInterceptor.java:137-139`),
and `MatrixStack` writes it there too
(`.../bridge/context/MatrixStack.java:53-56`),
so the first index is the row.
LWJGL's own methods read the same fields as `m<col><row>` and put a translation in `m30/m31/m32`.

So `getCPUModelView().store(buffer)` yields the matrix **transposed** relative to what GL and `gluUnProject` expect;
`storeTranspose` is what gives the column-major layout.
That is not a correction but the same conversion Fast Rendering itself does when it hands the matrix to GL
(`TransformManager.setGPUMode`, `.../bridge/context/TransformManager.java:39-44`),
and it reads back with `loadTranspose` on the way in
(`MatrixStack.glLoadMatrix`, `.../bridge/context/MatrixStack.java:150-154`).

This is worth care because it fails silently:
a transposed modelview is still sixteen plausible floats,
so nothing throws and a map overlay just resolves the wrong point.
For a translate-only map pass the pan lands in slots 3/7 instead of 12/13.

None of this is published API.
It is mod internals,
and a genir refactor can break any of it,
so code reading it should fail safe rather than assume.

### It defers every GL call to a render thread

The reason a naive `getCPUModelView()` read is not just transposed but flatly wrong:
it reads the matrix from the wrong thread,
at the wrong time.

The bridge is a deferred,
double-buffered renderer.
A `GL11.glTranslatef` on the game thread does not touch the modelview -
it appends a command to a frame buffer
(`.../bridge/commands/GL11.java:400-403`),
and `Executor.execute` only records it
(`.../bridge/context/Executor.java:40-49`).
The command that actually mutates `TransformManager` runs later,
when the frame is replayed on a dedicated single-thread executor named `FR-Render`
(`.../bridge/context/Executor.java:33`, `:113-143`, `:166-179`).
The same holds for `glPushMatrix`,
`glPopMatrix`,
`glLoadIdentity`,
`glScalef` and the rest:
all enqueue,
none mutate inline.

So `TransformManager` is render-thread state,
mutated roughly a frame behind the game thread that enqueues the calls.
A KM overlay's `renderOnMap` runs on the game thread;
reading `getCPUModelView()` directly from there samples whatever unrelated transform the render thread happens to be replaying at that instant,
and reads it field-by-field while that thread writes it -
a torn read of a matrix that was never the map's.
The identity guard cannot catch it,
because the sample is a real non-identity transform belonging to some other draw.
The failure is a confident wrong point every frame,
not an absent one.

This is the asymmetry that makes the viewport safe but the modelview not.
The viewport read (`glGetInteger(int, IntBuffer)`) is answered synchronously from `context.attribTracker` on the calling thread
(`.../bridge/commands/GL11.java:1271-1292`),
so it is caller-side state and reads true from anywhere.
`TransformManager` is executor-side state,
so it does not.

A synchronous readback reads the right matrix but is a trap of its own.
The executor can run a read in-band and return it -
`Executor.get(GLGetter)` submits a callback and blocks for the result
(`.../bridge/context/Executor.java:81-88`),
which runs it on the render thread at the caller's own stream position,
exactly where the modelview is the map's.
But `get` routes through `Executor.wait`,
which swaps frames and blocks until the queue drains -
a **pipeline stall** -
and the bridge actively punishes stalling.
`Executor.wait` calls `StallDetector.detectStall` (`.../bridge/context/Executor.java:92`),
which counts stalled frames and **throws `RuntimeException("Asynchronous pipeline stall")`
once a caller stalls on 30 of any 60 frames**
(`.../bridge/context/stall/StallDetector.java:17-40`).
A per-frame synchronous read on the open map hits 60 of 60
and takes the game down within about a second.
So a synchronous read is not an option for anything drawn every frame.

Counting starts once the game is up,
and from `v0.8.7` "up" means **the end of game initialisation**:
the detector is armed in `ResourceLoader.initEpilogue`,
immediately after every mod's `onApplicationLoad` has run
and the same flag Fast Rendering treats as "game initialised" is set
(`.../overrides/loading/ResourceLoader.java:187-188`).
So arming happens before the main menu is ever drawn,
and a per-frame stalling read on the campaign map is fatal from the first time the map is opened.

That arming point is release-specific and the change was not announced.
Through `v0.8.7rc1` the detector was armed on the first **combat frame** rendered instead,
from the shadowed `com/fs/starfarer/combat/CombatEngine.render`,
which made stalls free in the launcher,
during loading and anywhere in the campaign layer -
a stalling map overlay could be exercised for hours and only die on the first battle.
Two consequences.
A clean test on `v0.8.7rc1` or earlier proves nothing about a stalling read;
and a stall crash that now lands on the sector map with no battle in the session is the expected shape on `v0.8.7` and later,
not a new bug.

The viewport read escapes this only because it never stalls:
the bridge answers `GL_VIEWPORT` inline from `attribTracker` and returns before reaching `exec.wait`
(`.../bridge/commands/GL11.java:1271-1292`).
The modelview has no such inline path.

The fix is to read **one frame late,
without stalling**.
`Executor.execute(GLCommand)` enqueues a command and returns immediately -
no `wait`,
no stall
(`.../bridge/context/Executor.java:40-49`).
So each frame the reader enqueues a fire-and-forget command that,
when the render thread replays it at the caller's stream position,
copies `getCPUModelView()` (transpose included) into a holder the reader owns;
and it returns the copy a *prior* frame's command left there.
The holder is published across the two threads through an `AtomicReference`,
which also makes the cross-thread read tear-free.
The result is a frame or two stale -
the render thread runs a frame behind and a frame's copy is only guaranteed complete a frame later,
so a read sees the frame-before-last's value or last's.
That is invisible for a still map,
since the cursor moves but the transform does not,
and trails by a frame or two of pan velocity while panning -
and costs zero stalls.

This costs KMLib three mirrored members in its compile-only bridge stubs
(`Context.exec`, `Executor.execute`, `GLCommand`) beyond the modelview read itself -
see `build.gradle` and `src/bridgestubs/java`.
`GLCommand` is the one that moved:
it lives in `com.genir.renderer.bridge.interfaces` from `v0.7.4`,
having been `com.genir.renderer.bridge.context.commands` before,
and its method is `run(Context, float[], int)` -
the float array carries the packed arguments of the `execute` overloads that take them
(`.../bridge/context/Executor.java:51-79`),
and a command that reads nothing from it ignores both parameters.

Ignoring them is also what makes the stub immune to the one change in this area.
How those arguments are packed was re-laid-out in `v0.8.7rc1`:
each command now gets a fixed four-float slot at `i * 4`
(`.../bridge/context/Executor.java:171-184`),
where earlier releases walked a variable stride whose length sat in the first float of each record.
The *signature* did not change,
so a command that reads neither parameter -
KMLib's copy command -
spans both conventions,
while anything that had decoded the old packing would now read the wrong floats silently.

One hazard the fire-and-forget hop does not remove:
a command that **throws** on the render thread is captured
and re-thrown on the game thread at the next frame swap,
wrapped in a `RuntimeException`
(`.../bridge/context/Executor.java:111-141`, `:156-164`).
Since `swapFrames` runs every frame,
"fire and forget" means the exception is deferred,
not swallowed -
so the copy command has to be total,
and KMLib's holds no logic beyond the copy for that reason.

### What the GL11 bridge can and cannot read back

Drawing is broadly covered.
Reading state back is not:
the bridge's entire *implemented* `glGet*` surface is `glGetInteger(int)` and `glGetInteger(int, IntBuffer)`,
`glGetString`,
`glGetFloat(int)`,
`glGetError`,
`glGetTexLevelParameteri`,
`glGetTexParameteri` and two `glGetTexImage` overloads,
plus the two `glIs*` predicates
(`.../bridge/commands/GL11.java:1228-1480`).

Read "implemented" strictly from `v0.8.9`,
because the facade changed what *declared* means.
`bridge.opengl.GL11` declares LWJGL's whole `glGet*` surface,
including a buffer-taking `glGetFloat(int, FloatBuffer)`
(`.../bridge/opengl/GL11.java:389`),
and every entry point outside the list above throws `UnsupportedOperationException`.
So the presence of a method on the class KM code now binds to says nothing about whether it works,
and the list above is still the whole of what does.

There is **no working buffer-taking `glGetFloat`**,
so `GL_MODELVIEW_MATRIX` and `GL_PROJECTION_MATRIX` cannot be read through the bridge in any form.
Through `v0.8.8` that attempt was a `NoSuchMethodError`;
from `v0.8.9` it is an `UnsupportedOperationException`,
thrown from the facade before any GL work is attempted.
`glGetInteger(int, IntBuffer)` is bridged,
so `GL_VIEWPORT` reads work unchanged under both renderers
(`.../bridge/commands/GL11.java:1271-1292`).

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
  array buffer binding,
  **current program**,
  framebuffer binding,
  and vertex array binding
  (`.../bridge/commands/GL11.java:1228-1256`).
  The current program comes from `AttribTracker` on `v0.8.5rc1` through `v0.8.9` and on `v0.8.3` and earlier,
  and from `ShaderTracker` on `v0.8.4`;
  inline on all of them,
  so only a stack trace tells them apart.
- `glGetInteger(int, IntBuffer)` for `GL_VIEWPORT` only.
- `glIsEnabled` for stencil test,
  alpha test,
  texture 2D,
  blend,
  lighting,
  and **scissor test**
  (`.../bridge/commands/GL11.java:1445-1475`).
- `glGetFloat(int)` for `GL_LINE_WIDTH`
  (`.../bridge/commands/GL11.java:1319-1334`).
- `glGetString` for `GL_EXTENSIONS`,
  which is captured every frame rather than on demand -
  and is handed back with `GL_ARB_vertex_buffer_object` **edited out**,
  so a capability probe under this renderer reports that extension missing whatever the driver says
  (`.../bridge/commands/GL11.java:1294-1304`).
- `glIsTexture`,
  from `v0.8.4`
  (`.../bridge/commands/GL11.java:1477-1480`, `.../bridge/context/stall/TextureTracker.java:71-90`).
  `TextureTracker` keeps a caller-side record of which target each texture name was last bound to,
  and the answer comes from that;
  the real GL call still runs,
  deferred,
  purely to assert the two agree.
  On `v0.8.3` and earlier the same call is an `exec.get` and stalls.
- `glGetTexLevelParameteri` for `GL_TEXTURE_WIDTH`,
  `GL_TEXTURE_HEIGHT` and `GL_TEXTURE_INTERNAL_FORMAT`,
  from `v0.8.5rc1`
  (`.../bridge/commands/GL11.java:1392-1410`, `.../bridge/context/stall/TextureTracker.java:108-148`).
  `TextureTracker` caches those three per texture name as level 0 is uploaded,
  and the read is inline only when the texture currently bound **to that target** has an entry
  and the pname is one of the three;
  everything else -
  any other pname,
  an unseen texture,
  a non-zero level -
  still falls through to `exec.get` and stalls.
  Mip levels above 30000 are the exception and the trap:
  they short-circuit to `0` without touching GL or the cache at all,
  which makes that a read to distrust rather than rely on.

Inline only after one stall.
Four `glGetInteger(int)` pnames and three `glGetString` names go through a learn-on-first-miss cache
(`.../bridge/context/stall/StateCache.java:26-56`):
the first read misses,
stalls through `exec.get`,
and registers the pname;
every later read is inline and at most a frame stale.
The refill is part of the per-frame context update,
which runs on the render thread inside the `Display.update` command -
so "per frame" here means per presented frame,
not per game tick
(`.../bridge/context/Context.java:87-100`, `.../bridge/commands/Display.java:61-70`).

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
and each addition is somebody's crash being fixed:
`GL_CURRENT_PROGRAM` went inline in `v0.7.4`,
`GL_SCISSOR_TEST` in `v0.7.5`,
`glIsTexture` in `v0.8.4`,
`glGetTexLevelParameteri`'s three size pnames in `v0.8.5rc1`,
each to stop a per-frame reader from tripping the stall detector.
`v0.8.7` added `glGetTexParameteri`,
which is the counter-example worth holding onto:
it closed a `NoSuchMethodError` without adding an inline path,
so it always stalls
(`.../bridge/commands/GL11.java:1412-1422`).
A call being *present* in the bridge is therefore not evidence that it is cheap -
and from `v0.8.9` it is not even evidence that it is implemented,
since the facade declares the whole LWJGL surface and throws on most of it.

`v0.8.9` added no new inline `glGet*` answer,
but it did remove a stall elsewhere,
by the same reasoning and worth knowing for the shape:
a mapped buffer range is now served from a CPU-side scratch buffer instead of a synchronous readback.
`glMapBufferRange` hands back a plain `ByteBuffer` the caller fills,
and `glUnmapBuffer` flushes it with `glBufferSubData`,
so neither call waits on the render thread
(`.../bridge/context/stall/BufferManager.java:18-55`).
It is deliberately partial:
an access mask outside write/invalidate,
a target with nothing bound,
or a second overlapping map all return `null`
and fall back to the stalling path.
This is the release's `BoxUtil` fix,
and it reuses the class name `v0.8.4` deleted for something unrelated,
so a `stall/BufferManager` frame means `v0.8.3` or earlier,
or `v0.8.9` or later,
and the two are not the same code.

Two consequences for KM code.
Whether a given read is fatal depends on the Fast Rendering version,
so "it worked on my install" proves less than it looks -
and `v0.8.5rc1` makes that worse,
since it is indistinguishable from `v0.8.4` in a log or on the main menu
while answering two of these reads differently.
And the scissor **box** is still not shadowed -
only the enable flag is -
so the current clip rectangle remains unreadable without a stall,
which is why clip composition is the caller's job
(`kmlib.starsector.ui.render.gl.UiScissor`) and why the one place KMLib does read the box back -
the clip a map hover is diagnosed against,
in `MapCursorRead.describeRead` -
is gated off under this renderer rather than merely used sparingly.

`glGetInteger(int, IntBuffer)` switches on exactly one pname -
`2978`,
which is `GL_VIEWPORT` -
answering it from `attribTracker.getViewport()` and returning before the executor is reached;
every other pname falls past the switch to `context.exec.wait`
(`.../bridge/commands/GL11.java:1271-1292`).
`GL_SCISSOR_BOX` is `3088`,
so it takes that second path and stalls.

The neighbouring pair is what makes this easy to get wrong from memory:
the scissor **enable flag** is inline while the scissor **box** is not,
so a clip read written as "is it on,
and what is it" is half safe and half fatal.
Two lines,
two different answers.

Matrix reads are best avoided outright rather than worked around:
the campaign UI's projection is derivable arithmetically (below),
and the modelview is available from `TransformManager` -
though only through the render thread,
never by reading it back through GL
(see [It defers every GL call to a render thread](#it-defers-every-gl-call-to-a-render-thread)).

### Its reach goes past the GL bridge

`fr.jar` also ships patched copies of core game classes under `com.fs.*` (and `sound.*`),
not just the GL bridge,
and they shadow the game's own copies in `starfarer_obf.jar` / `fs.common_obf.jar`.
There are 21 on `v0.8.9`,
unchanged in membership since `v0.8.1` -
though their contents are not stable at all:
`graphics/TextureLoader` was rewritten in `v0.8.5rc1` to load DDS textures on demand rather than at startup,
`loading/ResourceLoaderState` and `loading/scripts/ScriptStore` both changed in `v0.8.6`,
and `Version` and `loading/ResourceLoaderState` both changed again in `v0.8.9`.
Membership holding says nothing about behaviour holding.
They reach well past rendering:
`Version`,
`BaseGameState`,
`combat/CombatEngine`,
`combat/CombatState`,
`combat/entities/Ship`,
`combat/ai/admiral/G`,
`graphics/TextureLoader`,
`graphics/LayeredRenderer`,
`loading/SpecStore`,
`loading/ScriptStore`,
`loading/LoadingUtils`,
`loading/ResourceLoaderState`,
`campaign/save/B`,
`campaign/rules/oOOO`,
and `api/impl/combat/threat/RoilingSwarmEffect` among them.
The set grows:
`combat/entities/Ship` and the `combat/E/o0OO` bounds class arrived in `v0.7.5`,
`sound/C` in `v0.7.6`,
`loading/LoadingUtils` in `v0.8.1`.

These are compiled against readable aliases -
`proxy.com.fs.graphics.Sprite`,
`proxy.com.fs.starfarer.combat.collision.Bounds` and the like -
which the obfuscation table maps onto the game's obfuscated names as each class loads
(`Transformations.obfuscation`, `starsector-core/fr.agent/com/genir/renderer/agent/Transformations.java:13`).
That table was its own class,
`ObfTransformations`,
through `v0.8.8`;
`v0.8.9` folded it into `Transformations` as one map among five.
So a name that looks like a public game type in a decompile of `fr.jar` may be an alias for an obfuscated one,
and that table is where to resolve it.
The table is not stable either:
`v0.8.1` renamed the `com.fs.util.ResourceLoader` alias family to `com.fs.util.FileLoader`,
and `v0.8.4` dropped the `com.fs.starfarer.campaign.rules.Rules` family outright,
so an alias that resolves on one release may not exist on the next.
It also carries method-name aliases,
not just class names,
which is why entries like `ProgressBar_render` sit in the same map.

So when behaviour differs under Fast Rendering,
the bridge is not the only place to look,
and a decompile of the game's own jar is not necessarily what is executing.
List the current set with:

```plaintext
Glob "**/com/fs/**/*.java" in <starsector>/.sources-cache/starsector-core/fr
```

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
