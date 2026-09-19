package kmlib.starsector.ui.widgets.tabs.style;

import kmlib.starsector.ui.font.TextFace;

/**
 * How a tab strip is sized and painted, carried as one injected value: which chrome it wears, its
 * dimensions, its colour scheme, how it presents a bound key, and how its labels are lettered. Tab look
 * is a look, not a law: two panels can share the whole layout and still want their tabs sized or shaded
 * differently - one floating free with room to breathe, another crowded against a neighbour's chrome - so
 * the whole description travels with the call rather than living as constants every strip inherits alike.
 *
 * <p>One value rather than a dimensions/paint pair split by which tier reads it, so a strip's geometry
 * and its paint cannot disagree. The cost is that a caller laying out without drawing still supplies the
 * paint, and one drawing pre-laid tabs still supplies the band height; that is the cheaper side of the
 * trade. Substrate-independent throughout - {@link TabPalette} is AWT colours and unit fractions,
 * {@link HotkeyStyle} a colour and two dimensions, {@link TextFace} a font and size, and
 * {@link TextHalo} a colour, a radius, and a fraction - so the layout may measure against this value and
 * the renderer may paint from it.
 *
 * <p>Every dimension is UI-coordinate pixels and content-space: it measures the tab surface itself, not
 * any border a host strokes around the panel that carries it. A bordered box grows outward around its
 * content, so a framed panel stands its border taller than the band height given here.
 *
 * @param chrome             which surface the row's paint is laid onto - the map's seamless strip or the
 *                           intel screen's raised buttons; read only at paint time, every other field
 *                           meaning the same thing under either
 * @param headerBandHeight   how tall the band carrying a panel's tabs stands; a non-positive value
 *                           collapses the band to nothing rather than inverting it, leaving the panel its
 *                           body alone
 * @param tabBox             the box each tab stands in within that band - a stated width, height and
 *                           neighbour channel, or {@link TabBox#SNAPPED} for tabs sized to their own
 *                           labels and abutting. It sits beside the band height because the two answer
 *                           together: the band is the room the row is given, this is what the row does
 *                           with it
 * @param palette            the strip's chrome accent, per-state resting looks, and interaction lifts
 * @param hotkey             how a tab presents the key it is bound to - the key's colour and whether it
 *                           is underlined
 * @param face               the font and size the tab labels are measured and drawn in
 * @param textHalo           whether the labels stand inside a ring of themselves, and how wide it is; it
 *                           sits beside the face because the two answer together - a small bitmap face
 *                           wants the ring a smooth one drawn at size reads muddier for - and it costs no
 *                           width, so nothing the layout measures moves with it
 * @param pixelFaceSharpness how hard a hard-edged face reads, 0 fully interpolated and 1 fully unfiltered
 *                           (see {@link kmlib.starsector.ui.render.gl.GlyphAtlasFilter}); unread by a face
 *                           that carries its own antialiasing. Per style rather than one setting for the
 *                           screen, because the right amount is whatever matches the chrome this row stands
 *                           beside, and two rows on one screen stand beside different chrome
 */
public record TabStyle(
    TabChrome chrome,
    float headerBandHeight,
    TabBox tabBox,
    TabPalette palette,
    HotkeyStyle hotkey,
    TextFace face,
    TextHalo textHalo,
    float pixelFaceSharpness) {

    /**
     * The baseline band height: room enough for the larger tab face with a little slack above and below
     * it. The one place the baseline dimension is written down, so a caller wanting a number off the
     * baseline reads it from here rather than from a parallel constant that could drift from it. A bare
     * dimension rather than a whole baseline style, because the colours a style carries resolve from the
     * live palette and so cannot be frozen into a constant.
     */
    public static final float DEFAULT_HEADER_BAND_HEIGHT = 19f;

    /**
     * Clamps the band to a floor of zero, so a caller handed a negative height lays out a bandless panel
     * instead of a tab row that hangs above its own top edge.
     */
    public TabStyle {
        headerBandHeight = Math.max(0f, headerBandHeight);
    }

    /**
     * How tall this style's tabs stand: its box's own height where it states one, otherwise the whole band.
     *
     * <p>Asked of the style rather than of the box, because the answer needs both the box and the band and
     * this is the one value carrying the pair. A layout placing the tabs and a paint pass clipping around
     * them read the same number here rather than each combining the two, which is how the row a fold wipes
     * and the row a chrome paints stay the same row.
     *
     * @return the height every tab in this style's band stands at
     */
    public float resolveTabHeight() {
        return tabBox.resolveTabHeight(headerBandHeight);
    }

    /**
     * This style standing in a band of a given height, everything else about it carried over. What a
     * caller wants where a look is chosen for one thing and the room it stands in is another's to state -
     * a panel's band button wearing its own chrome inside the band the panel was given - so the two are
     * not left to be kept agreeing by hand.
     *
     * @param bandHeight the band the style is to stand in
     * @return this style at that band height
     */
    public TabStyle withHeaderBandHeight(float bandHeight) {
        return rebuildAt(bandHeight, tabBox);
    }

    /**
     * This style standing its tabs in a different box, everything else about it carried over. What a
     * caller wants where a row is to be drawn in another row's chrome and colours at a width of its own -
     * a panel's band button wearing the tabs' look while sizing to the image it carries rather than to
     * the fixed box a layer name needs.
     *
     * @param tabBox the box the tabs are to stand in
     * @return this style over that box
     */
    public TabStyle withTabBox(TabBox tabBox) {
        return rebuildAt(headerBandHeight, tabBox);
    }

    // This style with the band and the box replaced, which is the whole of what either refinement above
    // varies. One rebuild rather than two, since a record of eight components restated twice is a
    // component eventually carried over wrongly in one of them.
    private TabStyle rebuildAt(float headerBandHeight, TabBox tabBox) {
        return new TabStyle(
            chrome,
            headerBandHeight,
            tabBox,
            palette,
            hotkey,
            face,
            textHalo,
            pixelFaceSharpness);
    }
}
