package kmlib.starsector.ui.widgets;

/**
 * Which way a {@link RadioRow}'s segments flow. Horizontal lays them left to right - the default a
 * compact option pair (Short/Full) reads best in; vertical stacks them top to bottom, so a longer
 * list of options (a view selector) reads as a column rather than a wide strip. The alignment picks
 * how the row is split into segments and how the dividers between them are drawn; the hit-test is
 * agnostic to it, since it runs over the already-split segment rectangles.
 */
public enum RadioAlignment {
    /** Segments run left to right, split by width; dividers are vertical rules between columns. */
    HORIZONTAL,

    /** Segments stack top to bottom, split by height; dividers are horizontal rules between rows. */
    VERTICAL
}
