package kmlib.starsector.ui.widgets;

import kmlib.starsector.ui.widgets.scroll.ScrollbarThickness;

/**
 * The two chrome dimensions a panel is laid out around: the {@link BoxBorder} framing its footprint, and
 * the {@link ScrollbarThickness} its body's bar draws at. Bundles them so a panel's chrome travels as one
 * value rather than as two arguments threaded side by side through every layout that frames a box over a
 * scrollable body.
 *
 * <p>They belong together because they are the same question asked at two edges: how much of the panel is
 * spent on chrome rather than on content. Both are room a layout has to reserve before a control is placed
 * - the border around the box, the bar's gutter inside it - and a panel is never laid out knowing one
 * without the other. It is sizes only: what the chrome looks like is a host's to hand to the paint pass,
 * and nothing here names a colour.
 *
 * @param border             the frame around the panel's footprint; an open edge reserves no room
 * @param scrollbarThickness how wide the body's scrollbar draws, and so the gutter its body keeps clear
 */
public record PanelChrome(
    BoxBorder border,
    ScrollbarThickness scrollbarThickness) {
}
