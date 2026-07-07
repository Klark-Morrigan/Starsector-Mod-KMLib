package kmlib.starsector.ui.render;

import java.awt.Color;

/**
 * The resolved paint of one drawable UI element: its colour and opacity, baked from the
 * settings once at build time. Groups the two values that always travel together for a
 * fill or a stroke, so the draw pass can ask a single element whether it is worth
 * emitting rather than juggling a loose colour/alpha pair per element.
 *
 * <p>A null colour is a "no colour" choice and a zero (or negative) opacity a fully
 * transparent element; either way {@link #isHidden} reports it shows nothing, so the
 * draw pass can skip it - emitting its run would only rasterise pixels the blend
 * discards. Geometry lives on the caller's own element record, not here, so a hidden
 * element can still take part in whatever the caller derives from its shape.
 */
public record UiElementPaint(Color color, float alpha) {
    public boolean isHidden() {
        return color == null || alpha <= 0f;
    }
}
