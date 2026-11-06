# KMLib UI Map Probes

Part of [KMLib UI](../../README.md).

The live reads into the map's widget tree,
over [`CoreUiTree`](../../coreui/CoreUiTree.java)'s by-name reach:
which widget the map is,
where the maps that are not it stand,
what tooltip is up over it,
and what it paints over what.
Beside the reads sits one report about the map's render hook.

## Index

- [The map the player opened](#the-map-the-player-opened)
- [The maps a mod embeds](#the-maps-a-mod-embeds)
- [The shown tooltip](#the-shown-tooltip)
- [Traces](#traces)
- [Shared walks](#shared-walks)
- [A pass with no vanilla map](#a-pass-with-no-vanilla-map)

## The map the player opened

[`ShownMapTab`](ShownMapTab.java) answers which widget the map is, wherever the game is showing one:
the `M` screen's tab, recognised by being a `SectorMapAPI`,
or the intel screen's lit map visor.
Why the tab is recognised by what it is rather than by its ID is in the hub's
[Two hosts, one map widget](../../README.md#two-hosts-one-map-widget).

The [surface bounds](MapSurfaceBounds.java) pick the map out of that tab by shape,
as a [surface area](MapSurfaceArea.java) carrying the chrome drawn with it.

## The maps a mod embeds

The [finder](EmbeddedMapFinder.java) answers where the maps which are not that tab stand,
as [one value per map](EmbeddedMap.java) carrying the chain it hangs under.

The value answers the box the map is currently drawn in, live at each ask and never kept:
a panel a mod slides on and off screen has moved by the time a held box is read.
For a caller whose subject is the widget rather than what shows of it,
it also answers the map as a component,
which answers where the box read does not, since a map drawn to nothing is a widget still.
Off that same ancestry it answers the widget its owner docked:
the outermost thing the map was wrapped in, or the map itself where it was wrapped in nothing,
so no caller has to index into a chain and be right about which end of it is which.

## The shown tooltip

[`VanillaMapTooltipProbe`](VanillaMapTooltipProbe.java) hands the shown tooltip back rather than a yes or no,
so a consumer can step aside for it or draw over it.
It searches under a root the caller supplies rather than the core tab,
since a docked map surface is no tab at all,
and the frames one is up are frames where no tab is.
What a vanilla tooltip is stays the probe's;
which surfaces exist is the caller's.

## Traces

Three traces each hand a line back for a consumer to log as its own:

- the [widget trace](MapTabWidgetTrace.java) describes what the cursor is inside;
- the [host trace](EmbeddedMapHostTrace.java) names whoever owns an embedded map, off the finder's walk;
- the [icon-order trace](MapIconOrderTrace.java) describes which terrain the widget paints over which.

The [layering probe](MapIconLayeringProbe.java) answers the icon order for one entity, as a value to act on.

## Shared walks

Both icon reads reach the icon map through one shared [reader](MapWidgetIcons.java),
so the walk and the one warning about it stopping working exist once.

The two probes that want the first widget under a root answering for something -
that icon reader and the tooltip probe -
descend through one [subtree search](SubtreeSearch.java).
The walks that collect rather than stop do not,
each carrying something different down with it:
a full ancestry, a parent and a depth, a capped running set.
Folding them together would need a visit result and a context object to save five lines of skeleton apiece.

## A pass with no vanilla map

[`ForeignMapPassWarning`](ForeignMapPassWarning.java) says once a session, in the caller's own words,
that the sector map's render hook was driven with no vanilla map drawn,
a map still fading out after the player left it counting as drawn.
It appends the host trace and the stack of the pass,
the two things a caller cannot know from inside it.
