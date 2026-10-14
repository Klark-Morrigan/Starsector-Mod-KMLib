package kmlib.math.geometry;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

/**
 * A hash grid of square cells that files items under every cell their extent touches, so a
 * question about what lies near a place, or which items might meet, only looks where an answer
 * can be.
 *
 * <p>What it is for is the step before an exact test. Comparing every item against every other,
 * or every query against every item, costs the square of the set for an answer that is nearly
 * always no. Filing each item by its box first means a query reads only the cells round it, and
 * a pairing only reads items that share a cell.
 *
 * <p>The cells are a coarse filter, never the answer. An item is filed by its bounding box, so
 * one filed beside a query can still be farther from it than the radius asked, and two items
 * sharing a cell need not touch. The exact test stays the caller's.
 *
 * <p>Answers come back in filing order - the order items were first filed in - however the cells
 * happen to be stored, so the same filings always give the same answer. An item is told apart
 * from another by {@code equals}: filing one item under several extents, or into several cells,
 * still answers it once.
 *
 * @param <T> the item type
 */
public final class UniformGrid<T> {

    // The fewest items measureAdaptiveCellSide divides an extent among. With none, the extent is
    // still one cell rather than a division by zero.
    private static final int MIN_DIVIDED_ITEM_COUNT = 1;

    private final double cellSide;

    private final Map<T, Integer> filingOrderByItem = new HashMap<>();

    // Per cell, the filing orders of the items filed under it. A set, so an item filed into one
    // cell more than once is held there once. Unordered, since every answer is sorted by filing
    // order on the way out.
    private final Map<GridCell, Set<Integer>> filingOrdersByCell = new HashMap<>();

    // Every distinct item in the order it was first filed. An item's position here is its filing
    // order, which is what the cells hold and what every answer is sorted by.
    private final List<T> items = new ArrayList<>();

    /**
     * @param cellSide the side of every square cell, in the same units as the coordinates filed
     * @throws IllegalArgumentException if {@code cellSide} is not positive (a cell of no width
     *         has no extent to file anything under)
     */
    public UniformGrid(double cellSide) {

        // Negated rather than "<= 0" so a NaN side, which compares false both ways, is refused too.
        if (!(cellSide > 0)) {
            throw new IllegalArgumentException("Grid cell side must be positive, was " + cellSide);
        }

        this.cellSide = cellSide;
    }

    /**
     * Files an item under every cell a box touches.
     *
     * <p>A cell holds its lower edge and not its upper one, so a box ending exactly on a cell
     * edge also touches the cell beyond it. That errs toward one extra candidate rather than
     * toward missing an item that touches the edge.
     *
     * @param bounds the item's extent
     * @param item   the item to file
     */
    public void fileBounds(Bounds bounds, T item) {

        var filingOrder = resolveFilingOrder(item);

        visitCellsTouching(bounds, cell -> filingOrdersByCell
            .computeIfAbsent(cell, filed -> new HashSet<>())
            .add(filingOrder));
    }

    /**
     * Files an item under the one cell a point falls in.
     *
     * @param x    the point's x
     * @param y    the point's y
     * @param item the item to file
     */
    public void filePoint(double x, double y, T item) {
        fileBounds(new Bounds(x, y, x, y), item);
    }

    /**
     * Files an item under every cell a segment's bounding box touches.
     *
     * <p>The box rather than the cells the segment actually crosses: a diagonal segment is filed
     * in cells it only passes beside. That costs a few extra candidates and spares walking the
     * segment cell by cell.
     *
     * @param from the segment's one end, as {x, y}
     * @param to   the segment's other end, as {x, y}
     * @param item the item to file
     */
    public void fileSegment(double[] from, double[] to, T item) {
        fileBounds(Bounds.computeEnclosingBounds(List.of(from, to)), item);
    }

    /**
     * Every pair of distinct items filed under at least one shared cell - the candidates an exact
     * pairwise test needs to look at, and no others.
     *
     * <p>Each unordered pair comes back once however many cells the two share. Within a pair the
     * earlier-filed item is first, and pairs are ordered by their first item's filing order, then
     * by their second's.
     *
     * @return the candidate pairs; empty when no cell holds two items
     */
    public List<ItemPair<T>> listCandidatePairs() {

        var pairKeys = new TreeSet<PairKey>(
            Comparator.comparingInt(PairKey::firstOrder).thenComparingInt(PairKey::secondOrder));

        for (var filingOrders : filingOrdersByCell.values()) {

            var cellOrders = new ArrayList<>(filingOrders);

            for (var one = 0; one < cellOrders.size(); one++) {
                for (var other = one + 1; other < cellOrders.size(); other++) {

                    var oneOrder = cellOrders.get(one);
                    var otherOrder = cellOrders.get(other);

                    pairKeys.add(new PairKey(
                        Math.min(oneOrder, otherOrder),
                        Math.max(oneOrder, otherOrder)));
                }
            }
        }

        var pairs = new ArrayList<ItemPair<T>>(pairKeys.size());

        for (var pairKey : pairKeys) {
            pairs.add(new ItemPair<>(items.get(pairKey.firstOrder()), items.get(pairKey.secondOrder())));
        }

        return pairs;
    }

    /**
     * Every item filed under a cell that the square of a radius around a point touches.
     *
     * <p>A coarse answer: it holds every item whose box comes within the radius, and may hold
     * items farther off that share a touched cell. A radius of zero reads the point's own cell.
     *
     * @param x      the point's x
     * @param y      the point's y
     * @param radius how far from the point to reach, along each axis; zero or more
     * @return the items, each once, in filing order; empty when nothing is filed nearby
     * @throws IllegalArgumentException if {@code radius} is negative (a square of negative size
     *         touches no cell)
     */
    public List<T> listItemsNear(double x, double y, double radius) {

        if (radius < 0) {
            throw new IllegalArgumentException("Search radius must not be negative, was " + radius);
        }

        // A bit per filing order rather than a sorted set: the orders are small and dense, so the
        // bits already read back in filing order.
        var filingOrders = new BitSet();

        visitCellsTouching(new Bounds(x - radius, y - radius, x + radius, y + radius), cell -> {

            var cellOrders = filingOrdersByCell.get(cell);
            if (cellOrders != null) {
                cellOrders.forEach(filingOrders::set);
            }
        });

        var near = new ArrayList<T>(filingOrders.cardinality());

        filingOrders.stream().forEach(filingOrder -> near.add(items.get(filingOrder)));

        return near;
    }

    /**
     * A cell side that splits a set's extent into about as many cells as it has items.
     *
     * <p>About one item per cell keeps each cell's share small without a sea of empty cells. The
     * longer side of the extent is the one divided, so a long thin set still gets cells of a
     * useful size across its length. The side never falls below {@link Limits#MIN_EDGE_LENGTH},
     * since a set whose items all sit at one point has no spread to divide.
     *
     * @param bounds    the box the whole set fits inside
     * @param itemCount how many items will be filed; fewer than one reads as one, so the extent
     *                  is a single cell
     * @return the cell side, positive
     */
    public static double measureAdaptiveCellSide(Bounds bounds, int itemCount) {

        var span = Math.max(bounds.maxX() - bounds.minX(), bounds.maxY() - bounds.minY());
        var divisor = Math.sqrt(Math.max(itemCount, MIN_DIVIDED_ITEM_COUNT));

        return Math.max(span / divisor, Limits.MIN_EDGE_LENGTH);
    }

    // Which column or row a coordinate falls in. Floor rather than truncation, so the cell
    // either side of zero is a cell of its own instead of the two sharing index zero.
    private long locateCellIndex(double coordinate) {
        return (long) Math.floor(coordinate / cellSide);
    }

    // The item's filing order, registering it on first sight.
    private int resolveFilingOrder(T item) {

        var known = filingOrderByItem.get(item);
        if (known != null) {
            return known;
        }

        var filingOrder = items.size();

        items.add(item);
        filingOrderByItem.put(item, filingOrder);

        return filingOrder;
    }

    // Every cell a box touches. The one walk filing and asking share, so a query reads exactly
    // the cells a box of its extent would have been filed under.
    private void visitCellsTouching(Bounds bounds, Consumer<GridCell> visitor) {

        var fromColumn = locateCellIndex(bounds.minX());
        var toColumn = locateCellIndex(bounds.maxX());
        var fromRow = locateCellIndex(bounds.minY());
        var toRow = locateCellIndex(bounds.maxY());

        for (var column = fromColumn; column <= toColumn; column++) {
            for (var row = fromRow; row <= toRow; row++) {
                visitor.accept(new GridCell(column, row));
            }
        }
    }

    // One cell, keyed by its column and row as they are rather than packed into one number, so
    // no two cells can share a key however far from the origin they lie.
    private record GridCell(
        long column,
        long row) {
    }

    /**
     * Two distinct items that share a cell, the earlier-filed one first.
     *
     * @param first  the item filed first
     * @param second the item filed after it
     * @param <T>    the item type
     */
    public record ItemPair<T>(
        T first,
        T second) {
    }

    // One candidate pair by the two items' filing orders, the smaller first.
    private record PairKey(
        int firstOrder,
        int secondOrder) {
    }
}
