package kmlib.starsector.ui.render.gl.controls;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.render.gl.UiBoxes;
import kmlib.starsector.ui.render.gl.UiElementPaint;
import kmlib.starsector.ui.render.gl.UiFill;
import kmlib.starsector.ui.widgets.BoxBorder;

import java.awt.Color;

/**
 * A single framed button that reads as on or off: when on it carries a lit wash, under the pointer a
 * hovered one, for a moment after a press a light over both, always with a border. The consumer owns the
 * on/off state, hit-tests the button through its
 * {@link Rectangle#containsPoint}, and draws the On/Off caption; this owns only the fill and
 * frame, so the button's geometry is just the {@code bounds} it is handed.
 *
 * <p>All draws touch the GL surface (over {@link UiFill#renderQuad} and {@link UiBoxes}), so
 * like the other raw-draw helpers this is run only in-engine - there is no geometry to compute
 * here beyond the bounds the consumer supplies.
 */
public final class ToggleButton {
    // The lit state is a wash over the frame, not a second opaque block, so it reads as a
    // highlight consistent with the radio and tab accents.
    private static final float ON_FILL_ALPHA_MULT = 0.30f;
    private static final float OUTLINE_THICKNESS = 1f;

    private ToggleButton() {
    }

    /**
     * Frames {@code bounds}, washes its interior when {@code isOn}, and lifts it by however far the
     * pointer has carried it. Every draw fades by {@code opacity}.
     *
     * <p>The hover wash stacks over the lit one rather than replacing it, the two answering different
     * questions: the lit wash says what the button is set to and the hover says where the pointer is,
     * so a lit button under the pointer stands above both an unlit one and a lit one left alone.
     *
     * <p>The press light goes over both of them, for the same reason and one step further: it says what the
     * player just did, which has to read whatever the button was already showing - a press laid under the
     * lit wash would be visible on an off button and swallowed on an on one, so the same click would answer
     * differently depending on the state it was flipping.
     *
     * @param bounds      the button's footprint, in UI coordinates
     * @param isOn        whether to draw the lit wash
     * @param frameColour the outline colour
     * @param onColour    the lit-wash colour
     * @param cellPaints  the wash and press light each cell takes; a toggle has the one cell
     * @param opacity     overall alpha, 0..1
     */
    public static void render(
            Rectangle bounds,
            boolean isOn,
            Color frameColour,
            Color onColour,
            CellPaintSources cellPaints,
            float opacity) {

        UiFill.renderQuad(bounds, cellPaints.hoverWashes().resolveSingleCellWashPaint());

        if (isOn) {
            UiFill.renderQuad(
                bounds,
                new UiElementPaint(onColour, opacity * ON_FILL_ALPHA_MULT));
        }

        UiFill.renderQuad(bounds, cellPaints.pressLights().resolveSingleCellLightPaint());

        UiBoxes.renderBorder(
            bounds,
            new BoxBorder(OUTLINE_THICKNESS),
            new UiElementPaint(frameColour, opacity));
    }
}
