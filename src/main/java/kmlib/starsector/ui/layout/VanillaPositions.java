package kmlib.starsector.ui.layout;

import com.fs.starfarer.api.ui.PositionAPI;

import kmlib.math.geometry.Rectangle;

/**
 * Reads an engine-assigned {@link PositionAPI} into the substrate-independent {@link Rectangle} the
 * rest of the KM UI hit-tests and draws with, so a widget the vanilla layout placed can be handled
 * as plain geometry. The conversion is kept here rather than on {@link Rectangle}, which stays free
 * of any engine coupling.
 */
public final class VanillaPositions {
    private VanillaPositions() {
    }

    /**
     * Converts {@code position}'s origin and size into a rectangle. Both are already in the
     * position's own UI coordinates, so this is a field read with no coordinate conversion - it
     * exists only so the four accessors are not unpacked by hand at each call.
     *
     * @param position the engine-assigned position to read
     * @return the position's origin and size as a rectangle
     */
    public static Rectangle toRectangle(PositionAPI position) {
        return new Rectangle(
            position.getX(),
            position.getY(),
            position.getWidth(),
            position.getHeight());
    }
}
