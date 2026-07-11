package kmlib.starsector.ui.widgets;

import kmlib.math.geometry.Rectangle;

/**
 * The geometry of one option row that carries an optional small leading image and a label to its
 * right - the internal layout of a single cell in a picker whose options each show an icon beside
 * their name. Substrate-independent: it places the icon square and the label's left anchor and
 * renders nothing, so a GL or a UI-API renderer draws the icon against {@link #computeIconBox} while
 * the consumer draws the text at {@link #computeLabelAnchorX}. Both read the same geometry, so the
 * drawn icon and the drawn label cannot drift.
 *
 * <p>The image is described here only as "an icon" - a small square drawn flush-left in the row. A
 * faction crest is the case this exists for on the political map, but the widget is agnostic to what
 * the image is: any small image the consumer can supply per option renders the same way. An option
 * with no image lays its label at the row's left inset instead, so an icon-less row reads as a plain
 * text row.
 *
 * <p>The icon square is sized to the row height (less a small inset off the top and bottom edges) so
 * it scales with the row rather than a fixed pixel size, keeping a stack of rows visually even. The
 * raw-GL paint of the whole vertical list - chrome plus each row's icon - lives in
 * {@link kmlib.starsector.ui.render.gl.IconRadioListRenderer}.
 */
public final class IconLabelRow {
    // The left inset both the icon and (icon-less) label start from, so content clears the row's
    // frame and divider rather than touching it.
    private static final float LEADING_PADDING = 4f;

    // How far the icon square is inset off the row's top and bottom edges, so the crest reads as a
    // framed pip inside the row rather than filling its full height.
    private static final float ICON_VERTICAL_INSET = 2f;

    // The gap between the icon's right edge and the label's left anchor, matching the checkbox's
    // box-to-label gap so icon rows and checkbox rows space their text alike.
    private static final float ICON_LABEL_GAP = 6f;

    // The inset off the row's right edge the measurement reserves past the label, so the widest
    // name still clears the frame on the trailing side.
    private static final float TRAILING_PADDING = 4f;

    private IconLabelRow() {
    }

    /**
     * The icon square for the row: a box of side {@code rowHeight - 2 * inset}, inset off the top and
     * bottom edges and flush against the row's left padding, vertically centred in the row. Sized off
     * the row height so a stack of rows shows equal icons.
     *
     * @param row the option row's footprint (one segment of the vertical list)
     * @return the square the icon draws into, at the row's left
     */
    public static Rectangle computeIconBox(Rectangle row) {
        var side = computeIconSide(row.height());
        return new Rectangle(row.x() + LEADING_PADDING, row.y() + ICON_VERTICAL_INSET, side, side);
    }

    /**
     * The x the row's label starts from (a left-anchored text): past the icon and its gap when the
     * option has an icon, or at the row's left inset when it does not, so an icon-less option reads as
     * a plain text row.
     *
     * @param row     the option row's footprint
     * @param hasIcon whether this option draws a leading icon
     * @return the label's left-anchor x, in UI coordinates
     */
    public static float computeLabelAnchorX(Rectangle row, boolean hasIcon) {
        if (!hasIcon) {
            return row.x() + LEADING_PADDING;
        }
        return row.x() + LEADING_PADDING + computeIconSide(row.height()) + ICON_LABEL_GAP;
    }

    /**
     * How wide the row must be to hold its icon and label without clipping: the leading inset, the
     * icon and its gap when present, the label, and the trailing inset. A host sizes the list to the
     * widest row this reports, so the widest name (with or without a crest) still clears the frame.
     *
     * @param rowHeight  the option row's height (the icon side derives from it)
     * @param labelWidth the label's measured rendered width
     * @param hasIcon    whether this option contributes an icon and its gap
     * @return the row's required width, in UI coordinates
     */
    public static float measureRowWidth(float rowHeight, float labelWidth, boolean hasIcon) {
        var iconExtent = hasIcon ? computeIconSide(rowHeight) + ICON_LABEL_GAP : 0f;
        return LEADING_PADDING + iconExtent + labelWidth + TRAILING_PADDING;
    }

    // The icon square's side for a given row height: the row height less the inset off each of the
    // top and bottom edges. One source for the size, so the box placement and the width measurement
    // cannot disagree on how large the icon is.
    private static float computeIconSide(float rowHeight) {
        return rowHeight - 2f * ICON_VERTICAL_INSET;
    }
}
