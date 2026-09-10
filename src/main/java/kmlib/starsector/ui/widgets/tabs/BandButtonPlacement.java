package kmlib.starsector.ui.widgets.tabs;

import kmlib.colour.Colours;
import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.controls.Control;
import kmlib.starsector.ui.text.ImageSpan;
import kmlib.starsector.ui.widgets.tabs.style.TabStyle;

import java.awt.Color;

/**
 * A laid-out band button: the control the layout placed, and the look it was placed at. The style travels
 * beside it for the reason the border travels on {@link TabPanelPlacement} - the pass that paints the
 * button wears the very look the layout measured it against, rather than reading the same source a second
 * time and hoping the two agree. Read a different face, and the word drawn is wider than the box snapped
 * to it.
 *
 * <p>The style carried here is the one the layout actually used, band height included, so it is what a
 * paint pass clips and centres against rather than the style its caller handed in.
 *
 * @param control the laid-out one-cell control - its bounds the button's box, its single segment the hit
 *                target
 * @param style   the chrome, box, palette and face it was laid at and is painted in
 * @param icon    the image drawn into the button's box in place of a word, or null for a button showing
 *                its label
 */
public record BandButtonPlacement(
    Control control,
    TabStyle style,
    ImageSpan icon) {

    // A band button is never the cell a panel is showing: it opens something rather than selecting
    // anything, so it is laid with no selection at all. Stated here because the look channel asks the
    // question and there is no index to answer it with.
    private static final boolean NEVER_SELECTED = false;

    /**
     * The box the icon is drawn into: the tab this button stands as, hung from the band's top. Not the
     * button's whole bounds, which is the band it stands in - a chrome whose tabs are shorter than their
     * band keeps the difference for the line they stand on, and an image drawn over that would sit a
     * pixel low and cover the rule.
     *
     * @return the icon's rectangle in UI coordinates
     */
    public Rectangle computeIconBox() {

        var bounds = control.bounds();
        var tabHeight = style.resolveTabHeight();

        return new Rectangle(
            bounds.x(),
            bounds.y() + bounds.height() - tabHeight,
            bounds.width(),
            tabHeight);
    }

    /**
     * Whether the point lands on the button. Geometry alone: whether the panel is presenting its band at
     * all is the panel's live fold, which this value knows nothing about.
     *
     * @param pointX the point's x in UI coordinates, the coordinates the placement is laid out in
     * @param pointY the point's y in UI coordinates
     * @return whether the point is on the button
     */
    public boolean containsPoint(float pointX, float pointY) {
        return control.bounds().containsPoint(pointX, pointY);
    }

    /**
     * The colour the mark is multiplied by at this point of the button's fade, and the whole of how this
     * control answers the pointer: the image fills its box, so what shows the pointer is the mark itself
     * rather than a margin of chrome around it - the fill beneath an image is covered by the very thing it
     * would be lighting for.
     *
     * <p>The palette's mark shade rather than its label's, which is not the same reading and is the point:
     * a word is parted from its lit self by role while its fill brightens underneath, and the mark covers
     * that fill - so it is lit rather than recoloured, and the brightening lands on the mark itself. The
     * palette composes it; see {@link kmlib.starsector.ui.widgets.tabs.style.TabHover#computeMarkLight}.
     *
     * <p>Multiplied over whatever the image states rather than replacing it, so an asset authored in its
     * own colours is washed rather than repainted, and one stating no tint takes the shade whole.
     *
     * @param hoverFraction how far the button has travelled into being pointed at, 0 fully off and 1 fully
     *                      on
     * @return the colour to multiply the image by
     */
    public Color resolveIconTint(float hoverFraction) {

        var markShade = style.palette().resolveMarkTintAtHoverFraction(NEVER_SELECTED, hoverFraction);

        // Whether "as authored" is white is the image's own answer, so a button carrying one asks it
        // rather than spelling the no-op multiply a second time; a button carrying none is the shade
        // itself, there being nothing for it to wash.
        return icon == null
            ? markShade
            : Colours.multiplyBy(icon.resolveDrawnTint(), markShade);
    }
}
