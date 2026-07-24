package kmlib.math.geometry;

/**
 * An axis-aligned rectangle given by its lower-left corner and size. Immutable and free of any
 * UI or engine coupling, so it serves equally as a laid-out UI box (UI origin is bottom-left,
 * which matches {@code x}/{@code y} being the lower-left corner) and as a plain geometric
 * bound. A rectangle laid out for drawing hit-tests a point with no conversion.
 */
public record Rectangle(float x, float y, float width, float height) {

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
