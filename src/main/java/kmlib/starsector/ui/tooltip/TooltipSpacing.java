package kmlib.starsector.ui.tooltip;

/**
 * The spacing vanilla lays its own tooltips and intel panels out on, so a box drawn beside the game's sits its text the
 * same way. The game repeats these as local literals in every panel it builds, so they are written down once here
 * rather than once per caller.
 */
public final class TooltipSpacing {

    /** The height vanilla gives a button inside a tooltip, its intel Delete button among them. */
    public static final float BUTTON_HEIGHT = 20f;

    /** Vanilla's {@code opad}: the gap above a heading or a paragraph that opens a new block. */
    public static final float OUTER_PAD = 10f;

    /** Vanilla's {@code pad}: the gap between rows of one block, and above a tooltip's first paragraph. */
    public static final float SMALL_PAD = 3f;

    private TooltipSpacing() {
    }
}
