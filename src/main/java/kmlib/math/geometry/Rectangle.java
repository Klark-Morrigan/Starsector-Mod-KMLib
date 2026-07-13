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
        return pointX >= x && pointX <= x + width
                && pointY >= y && pointY <= y + height;
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
