package kmlib.starsector.ui.widgets;

import kmlib.math.geometry.Rectangle;

/**
 * The geometry of one option row that carries an optional small leading image, a label to its right,
 * and an optional value flush against the row's right edge - the internal layout of a single cell in a
 * picker whose options read as a three-column table (leading icon, label, trailing value).
 * Substrate-independent: it places the icon square, the label's left anchor, and the trailing value's
 * right anchor and renders nothing, so a GL or a UI-API renderer draws the icon against
 * {@link #computeIconBox} while the consumer draws the label at {@link #computeLabelAnchorX} and the
 * value at {@link #computeTrailingAnchorX}. All read the same geometry, so the drawn icon, label, and
 * value cannot drift.
 *
 * <p>The trailing value is an optional right-aligned text - the picker uses it to show each bloc's
 * ranking number, and the same slot carries a sort selector's direction glyph. It right-aligns to the
 * row's right inset, so a stack of rows reads as a value column even though each row measures its own
 * value width. A row with no trailing value lays its label the whole way to the right inset; the width
 * measurement only reserves the value and its gap when the row has one.
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

    // The least gap kept between the label and a trailing value, so a snug row's name and value do
    // not touch. The width measurement reserves it, so the label region always clears the value.
    private static final float LABEL_TRAILING_GAP = 6f;

    // The inset off the row's right edge the measurement reserves past the label (or past the
    // trailing value when the row has one), so the widest name or value still clears the frame on the
    // trailing side. A trailing value right-aligns to this inset.
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
     * The x a right-aligned trailing value anchors to: the row's right edge less the trailing inset,
     * so the value clears the frame and a stack of rows aligns its values into a right-hand column.
     * The consumer draws the value right-anchored to this x; the width measurement reserves room for
     * it, so it never overlaps the label.
     *
     * @param row the option row's footprint
     * @return the trailing value's right-anchor x, in UI coordinates
     */
    public static float computeTrailingAnchorX(Rectangle row) {
        return row.x() + row.width() - TRAILING_PADDING;
    }

    /**
     * How wide the row must be to hold its icon and label without clipping, with no trailing value:
     * the leading inset, the icon and its gap when present, the label, and the trailing inset. A host
     * sizes the list to the widest row this reports, so the widest name (with or without a crest)
     * still clears the frame.
     *
     * @param rowHeight  the option row's height (the icon side derives from it)
     * @param labelWidth the label's measured rendered width
     * @param hasIcon    whether this option contributes an icon and its gap
     * @return the row's required width, in UI coordinates
     */
    public static float measureRowWidth(float rowHeight, float labelWidth, boolean hasIcon) {
        return measureRowWidth(rowHeight, labelWidth, hasIcon, 0f);
    }

    /**
     * How wide the row must be to hold its icon, label, and trailing value without clipping: the
     * leading inset, the icon and its gap when present, the label, the value and its gap when present,
     * and the trailing inset. Sizing every row to hold its own value and taking the widest gives a
     * column wide enough that each row's label clears its right-aligned value, since the widest row's
     * width bounds every row's label region.
     *
     * @param rowHeight     the option row's height (the icon side derives from it)
     * @param labelWidth    the label's measured rendered width
     * @param hasIcon       whether this option contributes an icon and its gap
     * @param trailingWidth the trailing value's measured rendered width, or 0 for a row with no value
     * @return the row's required width, in UI coordinates
     */
    public static float measureRowWidth(float rowHeight, float labelWidth, boolean hasIcon,
            float trailingWidth) {
        var iconExtent = hasIcon ? computeIconSide(rowHeight) + ICON_LABEL_GAP : 0f;
        var trailingExtent = trailingWidth > 0f ? LABEL_TRAILING_GAP + trailingWidth : 0f;
        return LEADING_PADDING + iconExtent + labelWidth + trailingExtent + TRAILING_PADDING;
    }

    // The icon square's side for a given row height: the row height less the inset off each of the
    // top and bottom edges. One source for the size, so the box placement and the width measurement
    // cannot disagree on how large the icon is.
    private static float computeIconSide(float rowHeight) {
        return rowHeight - 2f * ICON_VERTICAL_INSET;
    }
}
