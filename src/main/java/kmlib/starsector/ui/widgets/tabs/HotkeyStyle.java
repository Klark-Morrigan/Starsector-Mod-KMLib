package kmlib.starsector.ui.widgets.tabs;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.colour.StarsectorUiColour;

import java.awt.Color;

/**
 * How a tab presents the key it is bound to: the colour the key glyph reads in, and whether it is
 * underlined. Vanilla is of two minds about the emphasis - the sector map's own Sector/System tabs
 * underline the bracketed letter while the intel screen's raised buttons leave theirs bare - so the
 * emphasis travels with a tab's look rather than sitting as a constant in whichever renderer draws it,
 * and a panel placed beside either can match its neighbour.
 *
 * <p>Only the key carries this. Its delimiters are part of the display string the layout measures and
 * snaps a tab to, so they stay with {@link VanillaTabStrip}; an emphasis costs no width and needs no such
 * reach into the layout.
 *
 * <p>The key colour lives here rather than among the tab's other colour roles because it is the one that
 * does not answer to a tab's state: a bound key reads the same whether its tab is resting, selected, or
 * hovered. Substrate-independent like the rest of this package - an AWT colour and plain dimensions - so
 * the value travels inside the {@link TabStyle} a renderer paints from.
 *
 * @param keyColour          the colour the key glyph paints in, apart from its label-coloured delimiters
 * @param isKeyUnderlined    whether a line is drawn beneath the key; false leaves the dimensions below
 *                           unread
 * @param underlineThickness how tall the underline stands, in UI pixels; floored at zero, since a
 *                           negative thickness would invert the quad rather than thin the line
 * @param underlineGap       the clearance between the key's drawn box and the top of the underline, in
 *                           UI pixels. Measured from the box bottom, which sits a descender below the
 *                           glyph's baseline: a key is a capital, so that band is empty and a negative
 *                           gap legitimately lifts the underline up into it, closer to the baseline
 */
public record HotkeyStyle(
        Color keyColour,
        boolean isKeyUnderlined,
        float underlineThickness,
        float underlineGap) {

    // A hairline sitting just below the key's baseline, matching the sector map's Sector/System tabs.
    // The gap is negative because it is measured from the drawn box's bottom, which hangs a descender's
    // depth below the baseline that an all-capitals key never reaches into. Both values are eyeballed
    // against the real tabs rather than derived - the font exposes no baseline per drawn run.
    private static final float VANILLA_UNDERLINE_THICKNESS = 1f;
    private static final float VANILLA_UNDERLINE_GAP = -1f;

    /**
     * Floors the rule's thickness at zero, so a caller handed a negative one draws no rule instead of a
     * quad that grows upward through the glyph it should sit under.
     */
    public HotkeyStyle {
        underlineThickness = Math.max(0f, underlineThickness);
    }

    /**
     * The plain-key vanilla convention: the key lit in the highlight gold and left bare, the way the
     * intel screen's raised buttons present theirs. Resolves the gold through {@link
     * StarsectorUiColour} on each call, so it tracks a live palette change.
     *
     * @return a hotkey look with no underline
     */
    public static HotkeyStyle createPlain() {
        return new HotkeyStyle(
            StarsectorUiColour.VANILLA_HIGHLIGHT_GOLD.resolve(),
            false,
            0f,
            0f);
    }

    /**
     * The underlined-key vanilla convention: the key lit in the highlight gold and underlined, the way the
     * sector map's own Sector/System tabs present theirs. Resolves the gold through {@link
     * StarsectorUiColour} on each call, so it tracks a live palette change.
     *
     * @return a hotkey look drawing a hairline under the key
     */
    public static HotkeyStyle createUnderlined() {
        return new HotkeyStyle(
            StarsectorUiColour.VANILLA_HIGHLIGHT_GOLD.resolve(),
            true,
            VANILLA_UNDERLINE_THICKNESS,
            VANILLA_UNDERLINE_GAP);
    }

    /**
     * Places the underline under a drawn key: it spans the key's own width and stands its styled
     * thickness, the styled gap below the key's box. The style owns the placement so every renderer
     * drawing a key under this look puts the line in the same spot, rather than each deriving its own
     * offset from the same two numbers. Callers draw the result only for an underlined style; a plain
     * one has no line to place.
     *
     * @param keyBox the key glyph's drawn box, in UI coordinates (origin bottom-left)
     * @return the underline's box, in the same coordinates
     */
    public Rectangle computeUnderlineBox(Rectangle keyBox) {
        return new Rectangle(
            keyBox.x(),
            keyBox.y() - underlineGap - underlineThickness,
            keyBox.width(),
            underlineThickness);
    }
}
