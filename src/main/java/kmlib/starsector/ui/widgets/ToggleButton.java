package kmlib.starsector.ui.widgets;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.render.UiBoxes;
import kmlib.starsector.ui.render.UiFill;

import java.awt.Color;

/**
 * A single framed button that reads as on or off: when on it carries a lit wash, always with a
 * border. The consumer owns the on/off state, hit-tests the button through its
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
     * Frames {@code bounds} and, when {@code isOn}, washes its interior. Both draws fade by
     * {@code opacity}.
     *
     * @param bounds     the button's footprint, in UI coordinates
     * @param isOn       whether to draw the lit wash
     * @param frameColor the outline colour
     * @param onColor    the lit-wash colour
     * @param opacity    overall alpha, 0..1
     */
    public static void render(Rectangle bounds, boolean isOn, Color frameColor, Color onColor,
            float opacity) {
        if (isOn) {
            UiFill.renderQuad(bounds.x(), bounds.y(), bounds.width(), bounds.height(), onColor,
                    opacity * ON_FILL_ALPHA_MULT);
        }
        UiBoxes.renderBorder(bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                OUTLINE_THICKNESS, frameColor, opacity);
    }
}
