package kmlib.color;

import java.awt.Color;

/**
 * Colour conversions for raw rendering.
 *
 * <p>Pure math over {@link Color}: no rendering API, so it stays testable and
 * free of any GL dependency. Bridges AWT's 0-255 integer channels to the
 * normalized 0..1 float components a {@code glColor4f}-style call wants,
 * folding in an alpha multiplier so a faded layer can dim a whole palette
 * with one factor.
 */
public final class Colors {

    private static final float MAX_CHANNEL = 255f;

    private Colors() {
    }

    /**
     * Converts {@code color} to normalized {r, g, b, a} float components in
     * [0, 1], with the alpha further scaled by {@code alphaMult}.
     *
     * <p>Lets a caller hand a single {@link Color} - a literal, a palette
     * entry, or a live faction colour - straight to a {@code glColor4f} call
     * without per-channel float bookkeeping, and fade a whole render pass by
     * passing its viewport alpha as {@code alphaMult}.
     *
     * @param color     the source colour; its own alpha is honoured
     * @param alphaMult extra alpha scale (e.g. a map/viewport fade); 1 keeps
     *                  the colour's own alpha
     * @return {r, g, b, a}, each in [0, 1]
     */
    public static float[] getGlComponents(Color color, float alphaMult) {
        return new float[] {
                color.getRed() / MAX_CHANNEL,
                color.getGreen() / MAX_CHANNEL,
                color.getBlue() / MAX_CHANNEL,
                color.getAlpha() / MAX_CHANNEL * alphaMult,
        };
    }
}
