package kmlib.starsector.ui.layout;

import kmlib.math.geometry.Rectangle;

/**
 * Places a fixed-size box on screen at a {@link ScreenAnchor}, keeping a uniform margin from
 * whichever edges the anchor pulls it toward. Pure geometry in UI coordinates (origin
 * bottom-left): the box slides across the free space left after the margins by the anchor's
 * fractions, so a corner anchor sits margin-in from two edges and a centre anchor lands
 * mid-screen along that axis.
 */
public final class BoxPlacement {
    private BoxPlacement() {
    }

    /**
     * Lays out a {@code boxWidth} by {@code boxHeight} box within a {@code screenWidth} by
     * {@code screenHeight} screen at {@code anchor}, inset by {@code margin} from the edges the
     * anchor pulls toward.
     *
     * @param screenWidth  screen width, in UI units
     * @param screenHeight screen height, in UI units
     * @param boxWidth     box width, in UI units
     * @param boxHeight    box height, in UI units
     * @param margin       gap kept between the box and each edge its anchor pulls it toward
     * @param anchor       the corner, edge midpoint, or centre to pin the box to
     * @return the placed box, its {@code x}/{@code y} at the lower-left corner
     */
    public static Rectangle placeBox(
            float screenWidth,
            float screenHeight,
            float boxWidth,
            float boxHeight,
            float margin,
            ScreenAnchor anchor) {
                
        var boxX = margin
            + anchor.getHorizontalFraction() * (screenWidth - boxWidth - 2f * margin);
        var boxY = margin
            + anchor.getVerticalFraction() * (screenHeight - boxHeight - 2f * margin);

        return new Rectangle(boxX, boxY, boxWidth, boxHeight);
    }
}
