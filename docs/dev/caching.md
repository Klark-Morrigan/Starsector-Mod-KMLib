# Caching in KMLib

Every cache KMLib holds and what keeps it honest: what each one keys on, how long
it lives, and what invalidates it. KMLib caches only two kinds of thing - the
results of loading a font asset, and pure derivations over immutable inputs -
because everything else it exposes is a read of live campaign state that the game
may change between two frames.

Consumer repositories (KMU, KMO, ...) should link here rather than restate any of
it. KMU's own caches, which are a different problem (derived draw state over a
live sector), are documented in
[KMU's caching notes](https://github.com/Klark-Morrigan/Starsector-Mod-KMU/blob/master/docs/dev/caching.md).

## Index

- [Why anything is cached](#why-anything-is-cached)
- [The caches](#the-caches)
  - [Font faces](#font-faces)
  - [Glyph runs](#glyph-runs)
  - [Label line wraps](#label-line-wraps)
- [Invalidation primitives](#invalidation-primitives)
- [What is deliberately not cached](#what-is-deliberately-not-cached)
- [Rules](#rules)
- [Related reading](#related-reading)

## Why anything is cached

KM UI draws in immediate mode: a render pass runs from scratch every frame the
screen is open, and there is no retained scene graph to hold results between
frames. Two costs make that untenable without caching.

Loading a bitmap font parses a `.fnt` descriptor and uploads its atlas. Minting a
line of text builds a GL vertex buffer for the glyph run. Doing either per frame
would spend a per-frame cost on data that cannot change while the game runs, so
both are done once and kept.

Everything cached here is derived from files on disk or from arguments the caller
supplies. Nothing is derived from the campaign, so nothing here can go stale - a
cached value that disagreed with the sector is a shape that does not occur in this
library. That is the whole reason these caches need no invalidation and hold no
revision counters.

## The caches

| Cache | Keyed on | Lives for | Miss costs |
| --- | --- | --- | --- |
| [`LazyFontCache`](../../src/main/java/kmlib/starsector/ui/font/LazyFontCache.java) | the `.fnt` path | the process | a descriptor parse + atlas upload |
| [`DrawableStringCache`](../../src/main/java/kmlib/starsector/ui/font/DrawableStringCache.java) | `(face, size, text)` | the process | a GL vertex buffer mint |
| [`FontLabelLengthEstimator`](../../src/main/java/kmlib/starsector/ui/label/FontLabelLengthEstimator.java) | a line count | its own instance | an exhaustive wrap search |

### Font faces

`LazyFontCache` maps a `.fnt` path to its loaded `LazyFont`. The path comes from
[`StarsectorFont.resolvePath()`](../../src/main/java/kmlib/starsector/ui/font/StarsectorFont.java),
so the key space is the enum: a fixed handful of atlases, all shipped with the
game and none of them writable at runtime. There is no eviction and no
invalidation, because there is no event that could make a loaded atlas wrong.

It also caches *failures*. A face that will not load (a missing or malformed
descriptor) has its path remembered and its exception logged once, then returns
null on every later ask without retrying. Callers sit inside per-frame render
loops, so a retried failure would re-throw and re-log at frame rate; remembering
the failure is what turns a broken asset into one log line and a silently
text-free widget rather than a flooded log.

Both the hit and the miss path can return null. Every caller treats null as "draw
the chrome without the text" rather than as an error - a font that will not load
must not take a panel down with it.

### Glyph runs

`DrawableStringCache` maps `(TextFace, String)` - a face, a size, and the exact
line - to the `LazyFont.DrawableString` that draws it. It is the one home for that
minting, so two surfaces drawing the same word in the same face share one GL
buffer instead of each holding its own.

The base colour is deliberately outside the key. A caller re-sets the colour (and
its opacity) on the returned drawable before each draw, which is exactly what
lets one buffer serve every frame of a fade, a hover wash, or a selected-state
recolour without minting a variant per shade.

**The key space is the caller's responsibility.** Nothing is ever evicted, so
every distinct string drawn through it is a GL buffer held until the process ends.
That is correct for the text KM UI actually draws - control labels, tab names,
tooltip rows, faction names - which is a small set of strings repeated every frame.
It is the wrong shape for text that varies per frame:
[`DebugHud`](../../src/main/java/kmlib/starsector/ui/debug/DebugHud.java) pushes
live values (positions, counters) whose every distinct rendering mints a buffer
that is then never drawn again. That is tolerable only because the HUD is a
developer surface, off in normal play, and bounded by one session. New callers
drawing changing values should treat that as the exception, not the pattern.

### Label line wraps

`FontLabelLengthEstimator` memoises, per instance, the balanced wrap it computed
for a given line count. The search is exhaustive over word-boundary splits, and a
label is asked for several line counts as a fitter searches for a size that fits,
often re-asking the same count.

A count the text cannot fill (fewer words than lines) has no wrap, and that
absence is memoised too - `computeIfAbsent` cannot store a null, so the miss is
recorded explicitly through `containsKey`. Without that, exactly the counts that
are cheapest to reject would be the ones re-derived on every ask.

The memo is per instance, and an estimator is built for one text with one measurer,
so it dies with the pass that made it. Nothing static, nothing to invalidate.

## Invalidation primitives

KMLib holds no cache that needs invalidating, but it owns the primitive consumers
invalidate *with*:
[`Fingerprints.compute(IntSupplier...)`](../../src/main/java/kmlib/math/hashing/Fingerprints.java)
folds N monotonic revision counters into one order-sensitive int. A consumer keeps
the fingerprint its cached state was built against and rebuilds when the current
one differs, so a cache with many independent inputs still costs one int compare
per frame to check.

The convention it assumes: a producer of live state exposes a counter it bumps on
change, never a boolean "dirty" flag. A counter composes (many sources fold into
one number), survives being read by several consumers at different cadences, and
cannot be cleared by whichever consumer happens to look first.

[`RevisionMemo`](../../src/main/java/kmlib/starsector/ui/widgets/lists/RevisionMemo.java)
is the other half of that convention, and it is a primitive rather than a cache of
this library's own for the same reason: the consumer supplies both the revision and
the walk, so what is held and when it goes stale is entirely the consumer's
declaration. It exists here because one part of the key is not the consumer's to
get right by luck - the memo holds the sector it was built against, weakly, so a
save reloaded in the same session recomputes rather than serving the previous save's
value under a revision that happens to match.

## What is deliberately not cached

The Starsector-facing wrappers - `Markets`, `StarSystems`, `FactionFlags`,
`SectorMemoryAccess`, `StarsectorSprites` and their neighbours - are pass-through
reads. They resolve against the live sector on every call and hold nothing.

That is a deliberate stance, not an omission. The engine owns that state and
mutates it on its own schedule: a market changes hands, a memory flag is set by a
script, a faction's relationship moves. A library-level cache in front of those
reads would have no way to learn it had gone stale, and every consumer would
inherit a wrong answer it could not see. Caching derived campaign state is the
consumer's job, because only the consumer knows which changes it must react to -
see KMU's refresh signals for what that looks like in practice.

## Rules

- Cache a load or a pure derivation. Do not cache a live campaign read.
- A process-lifetime cache needs a key space that is bounded by the code, not by
  the data. If a caller can produce unbounded distinct keys, it owns that decision
  and should say so where it draws.
- Fail soft and remember the failure: a resource that will not load returns null,
  logs once, and never retries in a per-frame path.
- Keep per-draw variation (colour, opacity, anchor) out of the key, and re-apply
  it to the cached value at draw time.
- A cached GL buffer has an owner. `DrawableStringCache` owns what it mints and
  never disposes it; anything a consumer must dispose (a rebuilt label) must be
  minted by that consumer, not fetched from here.

## Related reading

- [Rendering environment](rendering-environment.md) - what the GL substrate does
  and does not guarantee, including under Fast Rendering.
- [`starsector/ui/font/`](../../src/main/java/kmlib/starsector/ui/font/) - the
  face enum, the two caches, and the width-measurement port.
- [`starsector/ui/label/`](../../src/main/java/kmlib/starsector/ui/label/) - the
  estimator that memoises wraps, and the fitter that asks it repeatedly.
