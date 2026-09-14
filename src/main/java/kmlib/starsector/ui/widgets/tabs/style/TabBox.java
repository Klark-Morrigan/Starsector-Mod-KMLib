package kmlib.starsector.ui.widgets.tabs.style;

/**
 * The box a tab stands in: a fixed width and height with a channel parting it from its neighbour, or
 * {@link #SNAPPED} for a row that sizes each tab to its own label and abuts them.
 *
 * <p>Which of the two a chrome wants is not a preference but a fact about what it is copying. A row of
 * fixed boxes is geometry the chrome owns: a renamed tab moves nothing, a label too long for its box
 * overruns rather than widening it, and the row spans the same width whatever it says. A snapped row is
 * geometry its text owns. The engine's own map tabs are the first kind and its stacked body rows the
 * second, so the choice travels with the chrome rather than being settled once for every tab row.
 *
 * <p>The height is the tab's own, not the band's. A band may stand taller than the tab it carries - the
 * engine's map row reserves a pixel under its tabs for the line they stand on - so a chrome states the
 * box it wants and the layout hangs it from the band's top, leaving whatever is left below.
 *
 * @param width        the width every tab takes, whatever its label measures; non-positive means the row
 *                     is snapped to its labels instead
 * @param height       how tall each tab stands inside its band; non-positive fills the band
 * @param neighbourGap the empty channel between two neighbouring tabs, taken between them rather than
 *                     out of either
 */
public record TabBox(
    float width,
    float height,
    float neighbourGap) {

    /**
     * A row that sizes each tab to its own label, fills its band top to bottom, and abuts its tabs so
     * their seams can be ruled. What every tab row was before a chrome could state a box of its own.
     */
    public static final TabBox SNAPPED = new TabBox(0f, 0f, 0f);

    /**
     * Floors every dimension at zero, so a caller handed a negative box lays a snapped row or a collapsed
     * tab rather than one drawn backwards through its neighbour.
     */
    public TabBox {
        width = Math.max(0f, width);
        height = Math.max(0f, height);
        neighbourGap = Math.max(0f, neighbourGap);
    }

    /**
     * Whether this box states a width of its own, which is what decides whether the row measures its
     * labels at all.
     *
     * @return true when every tab takes this box's width, false when each snaps to its label
     */
    public boolean isFixedWidth() {
        return width > 0f;
    }

    /**
     * Whether a channel stands between neighbouring tabs, which is what decides whether the row has any
     * seam to rule: a rule marks where two tabs meet, and parted tabs never do.
     *
     * @return true when neighbours are parted by a channel, false when they abut
     */
    public boolean isParted() {
        return neighbourGap > 0f;
    }

    /**
     * How tall a tab stands in a band of {@code bandHeight}: this box's height where it states one,
     * otherwise the whole band. Answered here rather than at each caller so the layout that places a tab
     * and any chrome that paints inside it cannot read the band differently.
     *
     * @param bandHeight the band the row was laid into
     * @return the tab's height, never taller than the band
     */
    public float resolveTabHeight(float bandHeight) {
        return height > 0f
            ? Math.min(height, bandHeight)
            : bandHeight;
    }
}
