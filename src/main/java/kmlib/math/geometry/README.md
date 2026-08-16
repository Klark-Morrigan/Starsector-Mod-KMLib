# KMLib Geometry

Part of [KMLib](../../../../../../README.md).

Game-agnostic 2D geometry: the value types a shape is stated in, the passes that reshape
a polygon, and the point/line arithmetic underneath both. Nothing here names the
Starsector API - the map layers that consume it live in the mods, not in this package.

## Index

- [Two conventions](#two-conventions)
- [Shapes and values](#shapes-and-values)
- [Polygon passes](#polygon-passes)
- [Laying something out along a ring](#laying-something-out-along-a-ring)
- [Giving a polyline girth](#giving-a-polyline-girth)
- [Point, line and span arithmetic](#point-line-and-span-arithmetic)
- [Partitioning](#partitioning)
- [One home for the degenerate thresholds](#one-home-for-the-degenerate-thresholds)
- [Why a disk carries its own segment count](#why-a-disk-carries-its-own-segment-count)

## Two conventions

A **point** is a `double[] {x, y}`. A **ring** is a `List<double[]>` in winding order with
no repeated closing vertex - the edge from the last vertex back to the first is implied.
Counter-clockwise winding gives positive signed area, and the passes preserve the winding
they are handed.

## Shapes and values

| Type | Holds |
| --- | --- |
| `Rectangle`, `Rectangles` | an axis-aligned box, and lookup across a collection of them |
| `Segment`, `DirectedLine`, `HalfPlane`, `BoxEdge` | a finite edge, an infinite oriented line, a clip side, a box's side |
| `Disk` | a centre, a radius, and the segment count it is approximated at |
| `CornerRounding` | the radius, segment count and bevel threshold a rounding pass uses |
| `LabelledPolygon` | a ring whose every edge carries an int naming what lies across it |
| `RingRegion` | one filled region: an outer ring plus the holes cut from it |
| `RingPath` | a ring walked as a path: one direction, one start, positions as arc length |
| `RingStretch` | a stretch of a path, as the arc lengths it opens and closes at |
| `PrincipalAxis` | the fitted major/minor axes of a point cloud |
| `RegionChord` | a chord across a region, with the span it clears |

`LabelledPolygon` is what lets a clip answer "what is across this edge" rather than only
"where is this edge". A pass that cuts it stamps the new edge with a caller-chosen label
while every surviving edge keeps its own, so a consumer can tell a real border from a
seam between two pieces of the same shape.

## Polygon passes

- **`PolygonOffsets`** - moves edges inward: per-edge segments (`offsetEdgesInward`), a
  half-plane inset of a convex ring (`insetConvexPolygon`), a selective inset that pulls
  only flagged edges (`insetSelectedEdges`), and a miter inset that keeps reflex corners a
  half-plane clip would shear off (`insetPolygonByMiter`). `removeReversedLoops` splices
  out the folds a self-crossing inset leaves.
- **`PolygonSmoothing`** - `removeSpikes` drops needles and cusps too thin to round, then
  `roundCorners` arcs or chamfers what remains. Run in that order.
- **`PolygonRegions`** - signed area, point-in-ring, distance to the boundary, and the
  interior spans a line crosses. The point tests split by question: `isPointInsideRing`
  says which side of the boundary a point is on and is arbitrary on it,
  `computeDistanceToBoundary` says how far off it is and is exact there.
- **`Rings`** - ring hygiene: which vertices survive a dedup, and where each half of a
  surviving corner came from.
- **`EdgeRings`** - chains loose edges back into rings, welding shared vertices, so a set
  of adjacent pieces yields the outline around them with the seams dropped.
- **`Disks`** - clips a convex polygon against a `Disk`, in both senses from one walk:
  `intersectWithDisk` keeps what is inside, `subtractDisk` keeps what is outside as
  disjoint convex pieces, and `subtractDiskWithLabels` names what lies across each edge of
  those pieces.

## Laying something out along a ring

A ring says nothing about where a walk of it begins or which way it goes: the winding is
whatever the pass that built it produced, and the first vertex is wherever that pass
started. `RingPath` settles both, plus the third thing a layout needs - a position stated
as one number rather than as an edge and a fraction of it.

- **Direction** - clockwise (negative signed area), whatever winding arrived.
- **Start** - the ring's top centre: the highest crossing of the vertical line through a
  caller-supplied anchor, falling back to the ring's topmost corner when that line misses
  the ring entirely.
- **Position** - arc length from that start, wrapping past the perimeter, so a layout that
  runs off the end continues round instead of having to be split.

`traceInsetRing` traces an inset of the ring rather than the ring itself, because the inset
carries a fact the caller cannot see: where a shape is narrower than twice the inset, what
comes back is not obviously wrong to look at. A square inset past half its width returns a
smaller square, correctly wound and self-intersecting nowhere, built entirely of backwards
edges - so the check is a measurement (each corner must stand its inset distance off the
original ring, by `PolygonRegions.computeDistanceToBoundary`) rather than a winding or
vertex-count test.

What it does with a corner that fails is carve, not refuse: the stretch from the corner
before it to the corner after it is subtracted from what `findClearArcs` offers, and the rest
of the ring stands. A shape pinched in one place is the ordinary case, and giving up its whole
outline for one narrow spot loses ring that was several times wide enough. The refusal
survives as what the carve leaves - a ring overrun everywhere fails at every corner, so every
stretch goes and `findStretchesHoldingItsInset` comes back empty. That pair is what a caller
choosing between insets asks: `hasStretchHoldingItsInset` to pick a ring, the list itself to
fall back on when its own keep-outs left nothing. The carve reaches the failing corner's neighbours
deliberately: clearance is sampled at corners, and a point midway along an edge can stand
nearer the ring than either end of it. `collectPointsBetween` returns a stretch as a polyline
including the corners it turns at - a stretch spanning a corner bends, and whatever gives it
girth has to bend with it.

A layout rarely has the whole ring to itself, so `findClearArcs` answers which stretches of the
path a set of keep-out shapes - and the path's own carved stretches - leave free, as arc-length
intervals in the path's own frame. Both are subtracted in the one answer, since a stretch with no
room for the inset is as unusable as a stretch something else covers, and a caller taking the
longest of what is left would otherwise have to remember there were two reasons a stretch might be
missing. The
answer is intervals rather than geometry because a caller measuring its layout in distances
already reads the path that way, and what it then makes of them - one interval or several,
resized or not - is its own. Every shape handed over is tested, whoever it belongs to, and a shape
covering several edges in a row comes back as one covered stretch rather than as a gap at every
corner it crosses. The intervals never wrap - a shape over the start leaves the pieces before and
after it stated separately, which is what "the layout begins at the start" means.
`Segment.computeBandCorners` builds one such shape from a centreline and a girth, so what is
drawn as a band and what keeps clear of it are one rectangle rather than two derivations of it.

Each stretch is a `RingStretch`, a named start and end rather than a bare pair of distances: the
two are not interchangeable, and a caller reading them the wrong way round lays every layout
backwards and compiles cleanly. An end past the perimeter is expected - that is how a stretch
crossing the start is stated, and `collectPointsBetween` walks one as it stands.

Two answers sit on top of the carve, both opt-in so a caller pays only for what it asks:

- `fuseStretchAcrossStart` reads the pair of stretches meeting at the path's start as the one
  stretch they are. The carve deliberately does not wrap, so a caller choosing between stretches
  - the longest, the first that fits - would otherwise judge the run through the origin on
  whichever half happened to be bigger.
- `placeSpanNearestStart` settles where along a stretch a layout of a given length sits: **as
  near the path's start as the stretch allows**. That start is the one position every path
  shares, so a layout sitting wherever the room happened to open costs a reader the landmark to
  read it from. Nearness is measured on the layout's *start*, since a layout is ordered from
  there - pulled in by its middle it would straddle the landmark and put its middle where its
  opening belongs. One clamp says it all: start at the path's start, pulled into
  `[stretchStart, stretchEnd - spanLength]` the shortest way round, ties taking the stretch's
  start so an opposite-lying stretch places the same way every call.

## Giving a polyline girth

`PolylineBands.strokeToTriangles` is what gives it that girth: a centreline plus a width
becomes a band of triangles, `{x, y}` points with every three one triangle - so a stretch
picked off a `RingPath` strokes by handing `collectPointsBetween`'s polyline straight over.

Triangles rather than a wide GL line, which has no join handling at all and whose width is a
screen quantity where a band along world geometry is a world one. The joins are the whole of
the work: a corner mitres, carrying both rails on to where they cross, and falls back to a
bevel in two cases that are asked separately because they answer different halves of it.

- A **spike** - a turn sharp enough that the outer miter stands farther from the corner than
  `miterSpikeLimit` half-widths - bevels the outer rail only. The inner rail still meets at a
  point and needs no bevel.
- A miter **outrunning its segments** bevels both rails. The reach is what makes this its own
  case: a miter reaches back along both segments it joins, so a segment short enough for the
  reaches at its two ends to meet has its rails cross and the band folds into a bowtie. A
  corner claims half of each adjacent segment; an end of the polyline claims none of its own,
  since a straight cap reaches back nothing, leaving the whole of that segment to the corner
  at the far end.

A bevelled rail falls back to the plain offset of its own segment, never to the centreline
point. A rail brought in to the centreline necks the band to half its width at that corner,
so a centreline turning every few widths - a traced outline, say - comes out strung with
necks; and since both ends of a segment offset perpendicular to that same segment, the quad
between them is its rectangle however short the segment or sharp its corners. What the
fallback costs instead is the two quads lying over each other across the inside of the turn.

The band comes out gap-free and, where the centreline's turns leave room for the width asked
of them, without stacking its pieces - which matters because a translucent band draws every
overlap as a brighter patch. Joins are made within one stroke, so two bands stroked separately
butt at their shared end: exact along a straight stretch, a small open wedge where that end
lands on a corner.

A band drawn in pieces - different colours along its length - therefore uses
`strokeSpansToTriangles`, which takes the centreline as its consecutive stretches and hands
back each stretch's own triangles out of one stroke. A boundary between two pieces is then a
point the band turns at like any other, rather than two square ends and the wedge between them,
and only the band's two outer ends are left open. A stretch covering no distance comes back
empty rather than dropped, so the answers stay readable by position.

## Point, line and span arithmetic

`Points` holds the vector arithmetic every measure above is built from - distances,
bearings, projections onto an axis, unit vectors. Most take either four loose doubles or
two `{x, y}` arrays. `Lines` and `Spans` work on intervals along a line: what a line
crosses, and the longest run of it left clear by a set of obstacles. `Segments` answers the
same questions bounded by two endpoints rather than running on without end - whether two
spans really cross, and how far a point lies from the nearest place on one. `PixelGrid` snaps to
a pixel lattice.

## Partitioning

`VoronoiCellBuilder` partitions the plane around a set of sites into one convex cell each,
every cell bounded to a maximum reach from its site. `buildLabelledCell` returns each edge
tagged with the neighbouring site's index, or `BOUND_EDGE` where the cell met its radius
bound instead of a neighbour.

## One home for the degenerate thresholds

`Limits` holds the minimums below which a shape stops enclosing real area:
`MIN_VERTICES_TO_ENCLOSE_AREA`, `MIN_EDGE_LENGTH`, `MIN_AXIS_VECTOR_LENGTH`. Two routines
asking "is this degenerate?" against the same fact must agree, so the fact lives once.

Note that `Rings.isSamePoint` dedups at `MIN_EDGE_LENGTH` - the same threshold below which
an edge has no computable direction. Any pass that dedups first therefore cannot then meet
an edge too short to offset.

## Why a disk carries its own segment count

A disk is approximated as a regular polygon, not treated as a true circle, so its rim is a
chain of chords. Two clips of the "same" disk only agree if they approximate it
identically: a keep-out and the reach bound it was cut from land on the same chords when
they share a count, and leave a seam when they do not. Holding the count with the centre
and radius in one `Disk` means a caller cannot vary one between two calls it meant to keep
aligned.
