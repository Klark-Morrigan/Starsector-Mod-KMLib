package kmlib.starsector.ui.widgets.tabs;

import kmlib.starsector.ui.font.TextFace;

/**
 * How a tab strip is sized and painted, carried as one injected value: which chrome it wears, its
 * dimensions, its colour scheme, how it presents a bound key, and the face its labels draw in. Tab look
 * is a look, not a law: two panels can share the whole layout and still want their tabs sized or shaded
 * differently - one floating free with room to breathe, another crowded against a neighbour's chrome - so
 * the whole description travels with the call rather than living as constants every strip inherits alike.
 *
 * <p>One value rather than a dimensions/paint pair split by which tier reads it, so a strip's geometry
 * and its paint cannot disagree. The cost is that a caller laying out without drawing still supplies the
 * paint, and one drawing pre-laid tabs still supplies the band height; that is the cheaper side of the
 * trade. Substrate-independent throughout - {@link TabPalette} is AWT colours and unit fractions,
 * {@link HotkeyStyle} a colour and two dimensions, and {@link TextFace} a font and size - so the layout
 * may measure against this value and the renderer may paint from it.
 *
 * <p>Every dimension is UI-coordinate pixels and content-space: it measures the tab surface itself, not
 * any border a host strokes around the panel that carries it. A bordered box grows outward around its
 * content, so a framed panel stands its border taller than the band height given here.
 *
 * @param chrome           which surface the row's paint is laid onto - the map's seamless strip or the
 *                         intel screen's raised buttons; read only at paint time, every other field
 *                         meaning the same thing under either
 * @param headerBandHeight how tall the band carrying a panel's tabs stands, and so the height every tab
 *                         in it shares; a non-positive value collapses the band to nothing rather than
 *                         inverting it, leaving the panel its body alone
 * @param palette          the strip's chrome accent, per-state resting looks, and interaction lifts
 * @param hotkey           how a tab presents the key it is bound to - the key's colour and whether it
 *                         is underlined
 * @param face             the font and size the tab labels are measured and drawn in
 */
public record TabStyle(
    TabChrome chrome,
    float headerBandHeight,
    TabPalette palette,
    HotkeyStyle hotkey,
    TextFace face) {
        
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
}
