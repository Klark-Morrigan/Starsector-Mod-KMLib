package kmlib.math.geometry;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * One edge of a rectangular box's border: its top, right, bottom, or left side. A subset of these names
 * which sides of a box are framed, leaving the rest open - so a box drawn flush against another need not
 * double the frame on the shared sides, both when stroking that side and when reserving the space it would
 * occupy.
 */
public enum BoxEdge {
    TOP,
    RIGHT,
    BOTTOM,
    LEFT;

    /** All four edges - the full frame, and the default when a caller omits none. */
    public static final Set<BoxEdge> ALL = Collections.unmodifiableSet(EnumSet.allOf(BoxEdge.class));
}
