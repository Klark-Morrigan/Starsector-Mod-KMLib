package kmlib.starsector.ui.render.gl;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * One edge of a rectangular box's border: its top, right, bottom, or left side. A renderer can stroke a
 * subset of these to frame only some sides of a box, leaving the omitted sides open - so a box drawn flush
 * against another's edge need not double the frame there.
 */
public enum BoxEdge {
    TOP,
    RIGHT,
    BOTTOM,
    LEFT;

    /** All four edges - the full frame, and the default when a caller omits none. */
    public static final Set<BoxEdge> ALL = Collections.unmodifiableSet(EnumSet.allOf(BoxEdge.class));
}
