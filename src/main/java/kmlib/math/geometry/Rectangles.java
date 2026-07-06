package kmlib.math.geometry;

import java.util.List;
import java.util.function.Function;

/**
 * Operations over sequences of {@link Rectangle}s. Holds the one hit-test every laid-out row of
 * boxes needs - which box, if any, a point falls in - so a tab strip, a radio row, or any other
 * abutting-box widget hit-tests through a single tested routine rather than re-rolling the loop.
 */
public final class Rectangles {
    /** {@link #findIndexContaining} returns this when no rectangle contains the point. */
    public static final int NONE = -1;

    private Rectangles() {
    }

    /**
     * The index of the first rectangle in {@code rectangles} containing {@code (pointX, pointY)},
     * or {@link #NONE} when none does. Boxes that abut share an edge; since
     * {@link Rectangle#containsPoint} counts edges as inside, a point on a shared edge resolves to
     * the earlier box.
     *
     * @param rectangles the boxes to test, in order
     * @param pointX     the point's x
     * @param pointY     the point's y
     * @return the first containing box's index, or {@link #NONE}
     */
    public static int findIndexContaining(List<Rectangle> rectangles, float pointX, float pointY) {
        return findIndexContaining(rectangles, Function.identity(), pointX, pointY);
    }

    /**
     * The index of the first item in {@code items} whose {@code boundsOf} rectangle contains
     * {@code (pointX, pointY)}, or {@link #NONE} when none does. The bounds extractor lets a widget
     * hit-test its own laid-out elements (a tab, a segment) without first copying out a plain
     * rectangle list.
     *
     * @param items    the elements to test, in order
     * @param boundsOf maps each element to the rectangle to test
     * @param pointX   the point's x
     * @param pointY   the point's y
     * @param <T>      the element type
     * @return the first matching element's index, or {@link #NONE}
     */
    public static <T> int findIndexContaining(List<T> items, Function<T, Rectangle> boundsOf,
            float pointX, float pointY) {
        for (var index = 0; index < items.size(); index++) {
            if (boundsOf.apply(items.get(index)).containsPoint(pointX, pointY)) {
                return index;
            }
        }
        return NONE;
    }
}
