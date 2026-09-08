package kmlib.starsector.ui.widgets.tabs;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.controls.Control;
import kmlib.starsector.ui.text.ImageSpan;
import kmlib.starsector.ui.widgets.tabs.style.TabStyle;

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
}
