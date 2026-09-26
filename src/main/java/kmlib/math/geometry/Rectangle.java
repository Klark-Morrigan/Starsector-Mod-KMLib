package kmlib.math.geometry;

import org.lwjgl.util.vector.Vector2f;

import java.util.List;

/**
 * An axis-aligned rectangle given by its lower-left corner and size. Immutable and free of any
 * UI or engine coupling, so it serves equally as a laid-out UI box (UI origin is bottom-left,
 * which matches {@code x}/{@code y} being the lower-left corner) and as a plain geometric
 * bound. A rectangle laid out for drawing hit-tests a point with no conversion.
 */
public record Rectangle(
    float x,
    float y,
    float width,
    float height) {

    /**
     * Measures the box a list of vectors fits inside - {@link Bounds#computeEnclosingBounds} stated
     * as a corner and a size, for points laid out in the float coordinates the game's UI works in.
     *
     * @param points the points to enclose; must be non-empty
     * @return the smallest rectangle containing every point, its corner at the lowest x and y
     * @throws IllegalArgumentException if {@code points} is empty (a box is undefined with nothing
     *         to enclose)
     */
    public static Rectangle computeEnclosingRectangle(List<Vector2f> points) {

        var bounds = Bounds.computeEnclosingBounds(points, point -> point.x, point -> point.y);

        return new Rectangle(
            (float) bounds.minX(),
            (float) bounds.minY(),
            (float) (bounds.maxX() - bounds.minX()),
            (float) (bounds.maxY() - bounds.minY()));
    }

    /**
     * @return whether {@code (pointX, pointY)} lies within this rectangle, edges inclusive
     */
    public boolean containsPoint(float pointX, float pointY) {

        return pointX >= x
            && pointX <= x + width
            && pointY >= y
            && pointY <= y + height;
    }

    /**
     * Intersects this rectangle with {@code other} into the region common to both. A non-overlap
     * yields a zero-extent rectangle (extents are floored at zero rather than allowed to go negative)
     * at the near corner, so composing a clip with a bound it lies outside of covers nothing rather
     * than inverting. Lets one clip be narrowed to stay inside another - a scrolling list's viewport
     * confined to the shrinking box of a collapsing panel - without either enclosing the other.
     *
     * @param other the rectangle to narrow this one against
     * @return the overlapping region, with zero-floored extents
     */
    public Rectangle intersectWith(Rectangle other) {

        var left = Math.max(x, other.x);
        var bottom = Math.max(y, other.y);
        var right = Math.min(x + width, other.x + other.width);
        var top = Math.min(y + height, other.y + other.height);

        return new Rectangle(
            left,
            bottom,
            Math.max(0f, right - left),
            Math.max(0f, top - bottom));
    }

    /**
     * Whether this rectangle and {@code other} share any area at all - the question behind "is this
     * box on that surface", asked wherever a box placed by somebody else has to be told apart from
     * one placed off the surface entirely.
     *
     * <p>Stated over {@link #intersectWith} rather than as its own corner arithmetic, so the two
     * cannot disagree about what an overlap is.
     *
     * <p>Touching edges do not overlap, the shared region then having no area. A box abutting the
     * edge of what it is tested against occupies none of it, and answering otherwise would report a
     * sliver nobody can see as present.
     *
     * @param other the rectangle to test against
     * @return whether the two share an area of non-zero extent on both axes
     */
    public boolean overlapsBox(Rectangle other) {

        var overlap = intersectWith(other);
        return overlap.width() > 0f && overlap.height() > 0f;
    }

    /**
     * Unions this rectangle with {@code other} into the smallest rectangle enclosing both. A widget
     * painted as several rects sitting outside one another has no single box of its own; this is how
     * one is composed, so a clip, a bound, or a backdrop covering the whole of it is stated once from
     * the pieces rather than re-derived from corner arithmetic at each site. Unlike
     * {@link #intersectWith} the result is a superset of both inputs, so it can cover screen neither
     * input does - the gap between two disjoint pieces is inside the union.
     *
     * @param other the rectangle to widen this one to include
     * @return the smallest rectangle containing both
     */
    public Rectangle unionWith(Rectangle other) {

        var left = Math.min(x, other.x);
        var bottom = Math.min(y, other.y);
        var right = Math.max(x + width, other.x + other.width);
        var top = Math.max(y + height, other.y + other.height);

        return new Rectangle(left, bottom, right - left, top - bottom);
    }

    /**
     * This rectangle pulled inward by {@code inset} on all four sides, holding its centre - the box left
     * inside a frame of that thickness, so an element drawn within one is placed from the box it is
     * framed by rather than from four corner offsets spelt out at the draw site. Extents floor at zero, so
     * a box too small to hold its own inset collapses at its centre rather than inverting into a
     * rectangle drawn back-to-front across whatever it was inside.
     *
     * @param inset how far to pull each side inward
     * @return the inner box, with zero-floored extents
     */
    public Rectangle computeInsetBox(float inset) {

        var innerWidth = Math.max(0f, width - 2f * inset);
        var innerHeight = Math.max(0f, height - 2f * inset);

        return new Rectangle(
            computeCenterX() - innerWidth / 2f,
            computeCenterY() - innerHeight / 2f,
            innerWidth,
            innerHeight);
    }

    /**
     * @return the x of the rectangle's horizontal centre, for placing a centred element
     */
    public float computeCenterX() {
        return x + width / 2f;
    }

    /**
     * @return the y of the rectangle's vertical centre, for placing a vertically centred element
     */
    public float computeCenterY() {
        return y + height / 2f;
    }
}
