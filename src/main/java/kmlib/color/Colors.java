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

    // The inclusive maximum of an 8-bit colour channel, one source for both forms: the
    // float normalizes channels to 0..1 for GL, the int clamps a scaled alpha back into
    // Color's constructor range.
    private static final int MAX_CHANNEL_VALUE = 255;
    private static final float MAX_CHANNEL = MAX_CHANNEL_VALUE;

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

    /**
     * A copy of {@code color} with its own alpha scaled by {@code alphaMult} - the AWT
     * {@link Color} counterpart of {@link #getGlComponents}, for a consumer that needs a
     * faded {@link Color} object (e.g. a LazyLib DrawableString's base colour) rather
     * than GL float components. The red, green, and blue channels are unchanged; the
     * scaled alpha is rounded to the nearest channel value and clamped into range, so an
     * {@code alphaMult} above 1 saturates instead of throwing from {@link Color}'s
     * constructor.
     *
     * @param color     the source colour; its own alpha is honoured
     * @param alphaMult extra alpha scale (e.g. a map/viewport fade); 1 keeps the
     *                  colour's own alpha
     * @return a colour with the same RGB and the scaled, clamped alpha
     */
    public static Color scaleAlpha(Color color, float alphaMult) {
        return new Color(
            color.getRed(),
            color.getGreen(),
            color.getBlue(),
            roundToChannel(color.getAlpha() * alphaMult));
    }

    /**
     * A copy of {@code color} with its red, green, and blue channels scaled toward black by
     * {@code factor}, its own alpha kept - so a caller can sink a fill to a darker, more
     * recessive shade of the same hue without touching its transparency. A factor of 1 leaves
     * the colour unchanged, 0 returns black, and values between darken proportionally. Each
     * scaled channel is rounded to the nearest channel value and clamped into range, so a
     * factor above 1 (a brighten) saturates at white instead of throwing from {@link Color}'s
     * constructor.
     *
     * @param color  the source colour; its own alpha is preserved
     * @param factor the fraction of each RGB channel to keep; 1 leaves the colour unchanged,
     *               0 returns black
     * @return a colour with each RGB channel scaled by {@code factor} and the original alpha
     */
    public static Color darken(Color color, float factor) {
        return new Color(
            scaleChannel(color.getRed(), factor),
            scaleChannel(color.getGreen(), factor),
            scaleChannel(color.getBlue(), factor),
            color.getAlpha());
    }

    /**
     * Lerps {@code base}'s red, green, and blue toward {@code target}'s by {@code amount}, keeping
     * {@code base}'s own alpha - so a caller can wash a fill toward a brighter shade (e.g. a tab fill
     * toward white on hover) as a pure brightness shift without also changing its transparency. An
     * amount of 0 returns {@code base}'s RGB, 1 returns {@code target}'s, and values between
     * interpolate; each channel is rounded and clamped, so an out-of-range amount saturates rather
     * than throwing from {@link Color}'s constructor.
     *
     * @param base   the colour to wash from; its own alpha is preserved
     * @param target the colour to wash toward; its alpha is ignored
     * @param amount the fraction to move each RGB channel from base toward target
     * @return base's RGB moved toward target by amount, with base's original alpha
     */
    public static Color blendRgbTowards(Color base, Color target, float amount) {
        return new Color(
            lerpChannel(base.getRed(), target.getRed(), amount),
            lerpChannel(base.getGreen(), target.getGreen(), amount),
            lerpChannel(base.getBlue(), target.getBlue(), amount),
            base.getAlpha());
    }

    // Scales one 0-255 channel by the factor.
    private static int scaleChannel(int channel, float factor) {
        return roundToChannel(channel * factor);
    }

    // Lerps one 0-255 channel from -> to by t.
    private static int lerpChannel(int from, int to, float t) {
        return roundToChannel(from + (to - from) * t);
    }

    // Rounds a computed channel value to the nearest int and clamps it into the 0-255 range, so a
    // scaled or lerped channel saturates rather than overflowing Color's constructor. The one place
    // every channel transform funnels its result through, so the round-then-clamp rule lives once.
    private static int roundToChannel(float value) {
        return Math.max(0, Math.min(MAX_CHANNEL_VALUE, Math.round(value)));
    }
}
