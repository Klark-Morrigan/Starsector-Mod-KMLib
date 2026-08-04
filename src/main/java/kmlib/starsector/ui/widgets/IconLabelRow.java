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
 * ranking number - or, in the same slot, a small filled direction triangle a sort selector draws
 * ({@link #computeDirectionTriangleBox}) since the body font renders no up/down glyph. Either
 * right-aligns to the row's right inset, so a stack of rows reads as a value column even though each
 * row measures its own value width. A row with no trailing value lays its label the whole way to the
 * right inset; the width measurement only reserves the value and its gap when the row has one.
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

    // What the row's three columns are worth - the insets off either end and the gaps parting the
    // label from each flank - taken from the one spec every three-column row reads, so a stack that
    // mixes this row with another shape starts every label at the same offset.
    private static final RowColumnSpec COLUMNS = RowColumnSpec.CONTROL_ROW;

    // How far the icon square is inset off the row's top and bottom edges, so the crest reads as a
    // framed pip inside the row rather than filling its full height. This one is the row's own: it
    // sizes the icon within the row rather than parting one column from the next.
    private static final float ICON_VERTICAL_INSET = 2f;

    // A trailing direction triangle sized off the row height (as the icon is), so a stack of rows
    // shows even triangles. Kept narrower and shorter than a full row so it reads as a compact marker
    // in the trailing slot rather than a block filling it.
    private static final float TRIANGLE_WIDTH_FRACTION = 0.45f;
    private static final float TRIANGLE_HEIGHT_FRACTION = 0.3f;

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
        return new Rectangle(
            row.x() + COLUMNS.leadingPadding(),
            row.y() + ICON_VERTICAL_INSET,
            side,
            side);
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
            return row.x() + COLUMNS.leadingPadding();
        }
        return row.x()
            + COLUMNS.leadingPadding()
            + computeIconSide(row.height())
            + COLUMNS.leadingLabelGap();
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
        return row.x()
            + row.width()
            - COLUMNS.trailingPadding();
    }

    /**
     * The width the trailing direction triangle occupies for a given row height - the same value the
     * layout reserves as the row's trailing width and the renderer sizes the triangle box to, so the
     * reserved slot and the drawn triangle cannot disagree. Derived from the row height so a stack of
     * rows shows even triangles.
     *
     * @param rowHeight the option row's height
     * @return the triangle slot's width, in UI coordinates
     */
    public static float computeDirectionTriangleSlotWidth(float rowHeight) {
        return rowHeight * TRIANGLE_WIDTH_FRACTION;
    }

    /**
     * The box a trailing direction triangle fills: right-aligned to the same trailing inset a text
     * value anchors to, vertically centred in the row, and sized off the row height. A stack of rows
     * lines its triangles into the same right-hand column a value column would occupy.
     *
     * @param row the option row's footprint
     * @return the triangle's box, at the row's trailing edge
     */
    public static Rectangle computeDirectionTriangleBox(Rectangle row) {

        var width = computeDirectionTriangleSlotWidth(row.height());
        var height = row.height() * TRIANGLE_HEIGHT_FRACTION;
        var rightEdge = computeTrailingAnchorX(row);
        var bottom = row.computeCenterY() - height / 2f;

        return new Rectangle(
            rightEdge - width,
            bottom,
            width,
            height);
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
    public static float measureRowWidth(
            float rowHeight,
            float labelWidth,
            boolean hasIcon,
            float trailingWidth) {

        var iconExtent = hasIcon
            ? computeIconSide(rowHeight) + COLUMNS.leadingLabelGap()
            : 0f;

        var trailingExtent = trailingWidth > 0f
            ? COLUMNS.labelTrailingGap() + trailingWidth
            : 0f;

        return COLUMNS.leadingPadding()
            + iconExtent
            + labelWidth
            + trailingExtent
            + COLUMNS.trailingPadding();
    }

    // The icon square's side for a given row height: the row height less the inset off each of the
    // top and bottom edges. One source for the size, so the box placement and the width measurement
    // cannot disagree on how large the icon is.
    private static float computeIconSide(float rowHeight) {
        return rowHeight - 2f * ICON_VERTICAL_INSET;
    }
}
