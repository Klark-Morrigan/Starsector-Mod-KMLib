# KMLib UI Map Icons

Part of [KMLib UI](../../README.md).

The one write into the sector map widget's draw order.
An overlay that rides on a terrain entity is painted where the widget seeded that entity's icon,
and the widget seeds the map's own nebula icons after every real entity,
so the overlay sits under the fog on every open with no published call to move it.
[`MapIconReseater`](MapIconReseater.java) is the script that moves it anyway,
over the [rule](MapIconReseatDecision.java) that says when a move is owed.

## Index

- [How an icon is lifted](#how-an-icon-is-lifted)
- [Acting on a reading, not an event](#acting-on-a-reading-not-an-event)
- [Waiting for the drop, not for the next advance](#waiting-for-the-drop-not-for-the-next-advance)
- [A stand-down is the open's](#a-stand-down-is-the-opens)
- [What the log says](#what-the-log-says)
- [What is not here](#what-is-not-here)

## How an icon is lifted

The widget keeps one icon per entity in an insertion-ordered map and walks the terrain-tagged ones in that order.
Each rendered frame it rebuilds its candidate list from the location's entities,
appends its synthetic nebulae after all of them,
and evicts any icon whose entity is missing from that list.
A re-added entity therefore re-enters at the tail,
after the nebulae.

So a lift is a removal from the location followed by a put-back,
with a frame rendered between them.
The types here are named for that pair rather than for what it achieves:
a *reseat* is the removal and the put-back,
which is how the icon takes a new seat at the tail,
and a *lift* is what one is for -
the icon ending up past the nebulae that were seeded after it.
The prose says lift wherever the effect is the point.

Which entity, which maps matter and where an icon currently sits are all ports the consuming mod wires in:
a rule about the first two would be a guess about somebody's content,
and reading the third here would make a package that writes depend on the [probes](../probes/) that only read.
The placement port is asked *about* an entity rather than standing for one,
so it cannot be pointed at a different entity from the one moved.

## Acting on a reading, not an event

The decision reads where the icon sits on every advance a map is up and lifts whenever it reads buried.
The obvious trigger - the edge into "a map is showing", the map being seeded per open - is a proxy,
and a proxy is only as good as its every occurrence being observed.
An earlier rule armed on that edge passed every case written for it and failed in play the first time an open went unobserved,
leaving the icon buried for the rest of the session with nothing able to notice.
Reading the placement makes the lift self-correcting:
whatever re-seeded the map,
and whatever was missed,
the next advance sees a buried icon and lifts it.

The map read stays as a scope rather than a trigger.
It says which map's ordering the caller cares about,
so nothing is moved for a screen the caller has no interest in.

## Waiting for the drop, not for the next advance

An advance is not a frame.
The campaign advances its scripts once per rendered frame ordinarily and several times per frame under its speed-up,
which a player can leave toggled on with a map open.
A removal and a put-back on consecutive advances then land in one frame,
the widget never renders without the icon,
nothing is evicted,
and the lift is repeated until the attempt bound abandons it -
which is how every layer ended up under the nebulae for whole sessions,
on every open,
unmoved by a save reload or a Starscape toggle.

The put-back therefore waits until the placement reads unplaceable,
which is the widget having dropped the icon,
and that reading is the same walk every map-open advance already pays for.
The wait ends at once when the map goes down,
and after `MAX_ADVANCES_DETACHED` advances regardless,
so a map that has stopped rendering cannot keep the entity out.
The bound is well above any speed-up multiplier a player sets,
a put-back that came too early being a lift that did nothing.

## A stand-down is the open's

Reading the state means the move can be attempted against a build where it no longer works,
where an event-keyed rule would have fired once and stopped.
`MAX_ATTEMPTS` bounds that:
lifts that do not clear the fog are counted until the icon is next seen clear,
and past the bound the decision stands down.
That is the same graceful direction the whole lever fails in,
rather than an entity flickering out of its location for as long as a map is up.

The stand-down lasts for the rest of the map open and no longer.
The next open is a fresh widget with a fresh seeding,
owed a fresh try,
and a stand-down that outlived its cause was indistinguishable from outside from the lever having stopped working.
Everything an open accrues - the lifts spent, the stand-down, whether the icon was seen clear, the run of unplaceable readings -
is one holder replaced whole when the map goes down.

## What the log says

All of it on this library's own logger,
which `KmlibLunaSettings` binds to KMLib's verbosity field,
so following a layering problem means turning KMLib's verbosity up rather than the consuming mod's.
Every line starts `Map icon reseat:`.

At DEBUG:

- each move, as the entity is detached and reattached;
- the map opening and closing, each carrying the count of lifts since the icon was last seen clear,
  and the closing line whether it was seen clear at all while the map was up.

At WARN, each said once per session,
since a failure that does not heal recurs on every open and the first line says all of it:

- a map that has stayed up for `UNPLACEABLE_ADVANCES_BEFORE_DISAGREEMENT` advances with the entity in its location and no icon placeable for it -
  the map read and the placement read disagreeing about what is on screen,
  a state no single reading shows;
- the stand-down,
  with the [readings](ReseatReading.java) that led to it,
  oldest first,
  since "did not clear" alone cannot say whether the lifts were never observed or observed and undone.

A fault in the moves themselves is an ERROR, once per session, with the cause;
later advances keep trying.

## What is not here

Where an icon sits is read by the [layering probe](../probes/MapIconLayeringProbe.java) and described by the [order trace](../probes/MapIconOrderTrace.java),
both in `map/probes`,
which read the live widget tree and never touch it.
Which entity is lifted,
and that it is a terrain drawn over the map's nebulae,
is the consuming mod's.
