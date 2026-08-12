# KMLib Geometry

Part of [KMLib](../../../../../../README.md).

Game-agnostic 2D geometry: the value types a shape is stated in, the passes that reshape
a polygon, and the point/line arithmetic underneath both. Nothing here names the
Starsector API - the map layers that consume it live in the mods, not in this package.

## Index

- [Two conventions](#two-conventions)
- [Shapes and values](#shapes-and-values)
- [Polygon passes](#polygon-passes)
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
- **`PolygonRegions`** - signed area, point-in-ring, and the interior spans a line crosses.
- **`Rings`** - ring hygiene: which vertices survive a dedup, and where each half of a
  surviving corner came from.
- **`EdgeRings`** - chains loose edges back into rings, welding shared vertices, so a set
  of adjacent pieces yields the outline around them with the seams dropped.
- **`Disks`** - clips a convex polygon against a `Disk`, in both senses from one walk:
  `intersectWithDisk` keeps what is inside, `subtractDisk` keeps what is outside as
  disjoint convex pieces, and `subtractDiskWithLabels` names what lies across each edge of
  those pieces.

## Point, line and span arithmetic

`Points` holds the vector arithmetic every measure above is built from - distances,
bearings, projections onto an axis, unit vectors. Most take either four loose doubles or
two `{x, y}` arrays. `Lines` and `Spans` work on intervals along a line: what a line
crosses, and the longest run of it left clear by a set of obstacles. `PixelGrid` snaps to
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
