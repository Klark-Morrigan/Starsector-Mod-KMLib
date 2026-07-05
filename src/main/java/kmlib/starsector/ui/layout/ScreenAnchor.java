package kmlib.starsector.ui.layout;

/**
 * One of the nine reference points a UI box can pin to on screen: the four corners, the four
 * edge midpoints, and the centre. Each carries the fraction of the free space its box takes up
 * along each axis - 0 hugs the low edge (left / bottom), 1 the high edge (right / top), 0.5
 * centres - which is all {@link BoxPlacement} needs to place the box. Keeping the fractions on
 * the anchor means a caller names the corner it wants and never restates the arithmetic.
 */
public enum ScreenAnchor {
    TOP_LEFT(0f, 1f),
    TOP_CENTER(0.5f, 1f),
    TOP_RIGHT(1f, 1f),
    LEFT_CENTER(0f, 0.5f),
    CENTER(0.5f, 0.5f),
    RIGHT_CENTER(1f, 0.5f),
    BOTTOM_LEFT(0f, 0f),
    BOTTOM_CENTER(0.5f, 0f),
    BOTTOM_RIGHT(1f, 0f);

    private final float horizontalFraction;
    private final float verticalFraction;

    ScreenAnchor(float horizontalFraction, float verticalFraction) {
        this.horizontalFraction = horizontalFraction;
        this.verticalFraction = verticalFraction;
    }

    public float getHorizontalFraction() {
        return horizontalFraction;
    }

    public float getVerticalFraction() {
        return verticalFraction;
    }
}
