package kmlib.starsector.ui.render.gl.controls;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.render.gl.UiBoxes;
import kmlib.starsector.ui.render.gl.UiElementPaint;
import kmlib.starsector.ui.render.gl.UiFill;
import kmlib.starsector.ui.widgets.BoxBorder;

import java.awt.Color;

/**
 * A single framed button that reads as on or off: when on it carries a lit wash, under the pointer a
 * hovered one, always with a border. The consumer owns the on/off state, hit-tests the button through its
 * {@link Rectangle#containsPoint}, and draws the On/Off caption; this owns only the fill and
 * frame, so the button's geometry is just the {@code bounds} it is handed.
 *
 * <p>All draws touch the GL surface (over {@link UiFill#renderQuad} and {@link UiBoxes}), so
 * like the other raw-draw helpers this is exercised in-engine rather than in unit tests - there
 * is no geometry to compute here beyond the bounds the consumer supplies.
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
     * @param bounds      the button's footprint, in UI coordinates
     * @param isOn        whether to draw the lit wash
     * @param frameColour the outline colour
     * @param onColour    the lit-wash colour
     * @param hoverWashes the wash each cell takes under the pointer; a toggle has the one
     * @param opacity     overall alpha, 0..1
     */
    public static void render(
            Rectangle bounds,
            boolean isOn,
            Color frameColour,
            Color onColour,
            CellHoverWashSource hoverWashes,
            float opacity) {

        UiFill.renderQuad(bounds, hoverWashes.resolveWashPaintAt(ControlSpec.SINGLE_CELL));

        if (isOn) {
            UiFill.renderQuad(
                bounds,
                new UiElementPaint(onColour, opacity * ON_FILL_ALPHA_MULT));
        }

        UiBoxes.renderBorder(
            bounds,
            new BoxBorder(OUTLINE_THICKNESS),
            new UiElementPaint(frameColour, opacity));
    }
}
