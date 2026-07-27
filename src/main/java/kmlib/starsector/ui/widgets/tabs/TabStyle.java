package kmlib.starsector.ui.widgets.tabs;

/**
 * The dimensions a tab strip is laid out and painted to, carried as one injected value. Tab geometry is a
 * look, not a law: two panels can share the whole layout and still want their tabs sized differently -
 * one floating free with room to breathe, another crowded against a neighbour's chrome - so the numbers
 * travel with the call rather than living as constants every strip inherits alike. Substrate-independent,
 * like the rest of this package: it is measured against by the layout and read by whichever renderer
 * paints the row.
 *
 * <p>Every dimension is UI-coordinate pixels and content-space: it measures the tab surface itself, not
 * any border a host strokes around the panel that carries it. A bordered box grows outward around its
 * content, so a framed panel stands its border taller than the band height given here.
 *
 * @param headerBandHeight how tall the band carrying a panel's tabs stands, and so the height every tab
 *                         in it shares; a non-positive value collapses the band to nothing rather than
 *                         inverting it, leaving the panel its body alone
 */
public record TabStyle(
        float headerBandHeight) {
    /**
     * The baseline style: room enough for the larger tab face with a little slack above and below it. The
     * value to pass where a caller has no reason to differ, so an unstyled strip is an explicit choice
     * rather than an accident of omission, and the one place the baseline dimensions are written down - a
     * caller wanting a number off the baseline reads it from here rather than from a parallel constant that
     * could drift from it.
     */
    public static final TabStyle DEFAULT = new TabStyle(19f);

    /**
     * Clamps the band to a floor of zero, so a caller handed a negative height lays out a bandless panel
     * instead of a tab row that hangs above its own top edge.
     */
    public TabStyle {
        headerBandHeight = Math.max(0f, headerBandHeight);
    }
}
