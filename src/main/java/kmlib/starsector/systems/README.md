# KMLib Star Systems

What the sector's star systems are,
read and never changed:
the whole set and one system,
what tells two of them apart,
what a single pass may read of them,
which ones are moving,
and the ways into a system the engine does not model.

Everything here is a read.
A caller that must account for every system in a modded sector needs more than the ID vanilla answers with,
and that is the fact the package is shaped around.

## Index

- [Two altitudes](#two-altitudes)
- [What tells two systems apart](#what-tells-two-systems-apart)
- [One pass, one walk](#one-pass-one-walk)
- [Remembering something per system](#remembering-something-per-system)
- [Systems that move](#systems-that-move)
- [Ways in the engine does not model](#ways-in-the-engine-does-not-model)
- [Ids that reach in from outside](#ids-that-reach-in-from-outside)
- [What is not here](#what-is-not-here)

## Two altitudes

[`SectorStarSystems`](SectorStarSystems.java)
answers about the sector's whole set:
where the systems sit (`collectHyperspacePositions`),
which one the player is in (`getPlayerStarSystem`),
and how to reach one by ID or by key
(`indexById`, `indexByKey`, `indexHeldSystemsById`, `findSystemById`).
[`StarSystems`](StarSystems.java)
answers about one system the caller already holds:
what to call it (`readDisplayName`),
what is in it
(`getStars`, `readMarkets`, `readMarketsUnlistedByEconomy`, `findNearestMarket`, `findTaggedEntity`, `getCentremostStar`),
and whether it can be reached at all (`isReachable`).

The split is by when a caller asks rather than by what the answer is about.
A pass resolves the sector's layout once and then asks about systems many times over,
so the two are reached at different moments by different code.
Kept in one class they read as a grab-bag that any new system read could be added to,
with no rule saying which.

Every read that traverses the system list reports it through [`SectorWalkCounters`](../SectorWalkCounters.java),
so a caller's profiling row states the walks it caused without having asked for any of them to be counted.

## What tells two systems apart

`StarSystemAPI#getId` is not unique.
A live heavily modded sector reports 502 systems under 498 distinct IDs,
and three of the four collisions are vanilla content rather than anything a mod added.
The colliding systems share their name as well,
so neither arm alone separates them.
Anything keyed on the ID keeps one of a colliding set and drops the rest,
which is a system missing from every structure built on that map with nothing anywhere saying so.

[`SystemKey`](SystemKey.java) is the triple that does separate them:
the system's own ID,
plus the IDs of its centre and its hyperspace anchor.

- The **anchor arm** is load-bearing.
  An anchor's ID is minted by the engine per system and persisted in the save as entity identity,
  so it is both distinct and stable across reloads.
- The **centre arm** would not do alone.
  A centre ID may be a literal its creator passed to `initStar`,
  and two authors can pick the same one.
- The **ID arm** is the collision itself.
  It is kept because it is what a person and every external input call the system by,
  and because it is the arm that is never absent.

The three are held as fields and compared as such rather than composed into a string,
so no separator has to be trusted never to occur inside an entity ID -
a composition where `a + b` and `ab + absent` can meet reintroduces the very collision it was meant to break.
Any arm may be absent,
held as the empty string,
so equality compares three present values.

One shape tells nothing apart:
the key whose every arm is absent,
read off a system the sector states nothing at all about.
Two such keys are equal while standing for two systems,
so `hasStatedArm` is asked before keying anything on one.

## One pass, one walk

[`SectorPassIndex`](SectorPassIndex.java)
is one pass's whole reading of a sector:
which systems it holds (`readSystemsByKey`, `readSystemsById`) and the colonies in each (`readColoniesIn`, `readColoniesById`).

Readers are handed this instead of the `SectorAPI`,
and that substitution is the point.
A reader given a sector is a reader that can still walk a system again;
one given this cannot,
so the one-walk-per-pass rule is enforced by what a reader is able to reach
rather than by everyone remembering it.

A rebuild addresses its systems both ways at once -
its cells by key,
its readers holding a bare ID by ID -
and its budget is one traversal for the whole of it.
So the ID index is projected off the systems the key index already holds,
through `indexHeldSystemsById`,
rather than off a traversal of its own.
The complete index is always the one a coarser address is taken from,
never the reverse.

## Remembering something per system

[`SystemKeyedMemo`](SystemKeyedMemo.java)
holds one value per system for the life of a pass,
and states the two rules every such memo owes so that none restates them:

- Keyed on the whole key rather than the ID,
  so two systems sharing an ID are two entries rather than one being handed the other's value.
- A system with no stated arm is resolved afresh on every ask rather than pooled under the blank key.
  The repeat costs what a later ask would have saved,
  which is the honest price of a system the sector states nothing about.

The colony memo behind `readColoniesIn` is one of these.
A caller adding a per-system memo of its own takes this type
rather than writing the two rules again.

## Systems that move

[`SystemMotionTracker`](SystemMotionTracker.java)
reports which systems shifted across hyperspace between polls:
[`MotionTracker`](../../math/motion/MotionTracker.java)
bound to `SystemKey` and to the floor a shift must clear to read as motion rather than float noise.

It takes positions the caller has already read rather than the sector they came off,
so a caller sharing one reading of the sector hands over what that reading holds instead of this walking the system list a second time for the same answer.
Which systems are in scope is the caller's alone,
so the tracker stays agnostic to any one feature's membership rule.

## Ways in the engine does not model

Whether a system can be reached is a question about hyperspace,
but the answer can turn on a single mod's entity.
Random Assortment of Things moves fleets through an Abyssal Fracture by a manual hyperspace transition rather than a jump point,
so a system entered only that way holds no jump point
and may carry the cut-off tag while being perfectly reachable.

[`SystemAccessRoute`](SystemAccessRoute.java)
is that exception stated by whoever knows the mod,
and [`SystemAccessRoutes`](SystemAccessRoutes.java)
is the register a mod fills at load.
The reachability read consults them and names no mod.

A collection rather than one slot,
which is what separates this from an [extension point](../../extensions/README.md):
work taken over whole admits one implementation,
but access is not taken over.
Two mods can each add a way in,
both true at once,
and keeping only the last registered would drop one mod's routes because another loaded after it.
A route answering false leaves the question exactly where it found it,
which is what lets an install carry several without them having to agree about anything.

A route also vouches that the mod marks the system on the hyperspace map,
since a mod carrying fleets somewhere shows the player
where by a marker of its own making that no scan of the game's star anchors can see.

## Ids that reach in from outside

The key is internal to code holding systems.
Anything addressed from outside stays on the vanilla ID,
that being the only arm a person or a data file can write:
an override table,
a persisted preference,
a console argument,
a log line naming a system.

Those reads answer about the first system carrying the ID,
documented as such.
`indexById` states in the log which system a repeated ID left out,
because a collision is a fact about the install worth saying out loud -
and that line is what turns the next report of something missing into a one-line diagnosis.

## What is not here

- Who claims a system -
  [`claims/`](claims/)
  holds vanilla's claim behind a port,
  and the scored contest behind a second one for callers that must justify a claim
  rather than merely colour by it.
- What a colony is -
  [`markets/colonies/`](../markets/colonies/)
  owns the colony set the per-pass index memoises per system.
- The motion arithmetic -
  [`math/motion`](../../math/motion/)
  holds the tracker and the floor comparison bound here.
- Which systems the sector map marks with a star -
  [`map/`](../map/).
