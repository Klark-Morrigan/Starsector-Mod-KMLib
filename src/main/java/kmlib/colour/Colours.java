package kmlib.colour;

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
public final class Colours {

    // The inclusive maximum of an 8-bit colour channel, one source for both forms: the
    // float normalizes channels to 0..1 for GL, the int clamps a scaled alpha back into
    // Color's constructor range.
    private static final int MAX_CHANNEL_VALUE = 255;
    private static final float MAX_CHANNEL = MAX_CHANNEL_VALUE;

    private Colours() {
    }

    /**
     * Converts {@code colour} to normalized {r, g, b, a} float components in
     * [0, 1], with the alpha further scaled by {@code alphaMult}.
     *
     * <p>Lets a caller hand a single {@link Color} - a literal, a palette
     * entry, or a live faction colour - straight to a {@code glColor4f} call
     * without per-channel float bookkeeping, and fade a whole render pass by
     * passing its viewport alpha as {@code alphaMult}.
     *
     * @param colour    the source colour; its own alpha is honoured
     * @param alphaMult extra alpha scale (e.g. a map/viewport fade); 1 keeps
     *                  the colour's own alpha
     * @return {r, g, b, a}, each in [0, 1]
     */
    public static float[] getGlComponents(Color colour, float alphaMult) {
        return new float[] {
            colour.getRed() / MAX_CHANNEL,
            colour.getGreen() / MAX_CHANNEL,
            colour.getBlue() / MAX_CHANNEL,
            colour.getAlpha() / MAX_CHANNEL * alphaMult,
        };
    }

    /**
     * A copy of {@code colour} with its own alpha scaled by {@code alphaMult} - the AWT
     * {@link Color} counterpart of {@link #getGlComponents}, for a consumer that needs a
     * faded {@link Color} object (e.g. a LazyLib DrawableString's base colour) rather
     * than GL float components. The red, green, and blue channels are unchanged; the
     * scaled alpha is rounded to the nearest channel value and clamped into range, so an
     * {@code alphaMult} above 1 saturates instead of throwing from {@link Color}'s
     * constructor.
     *
     * @param colour    the source colour; its own alpha is honoured
     * @param alphaMult extra alpha scale (e.g. a map/viewport fade); 1 keeps the
     *                  colour's own alpha
     * @return a colour with the same RGB and the scaled, clamped alpha
     */
    public static Color scaleAlpha(Color colour, float alphaMult) {
        return new Color(
            colour.getRed(),
            colour.getGreen(),
            colour.getBlue(),
            roundToChannel(colour.getAlpha() * alphaMult));
    }

    /**
     * A copy of {@code colour} with its red, green, and blue channels scaled toward black by
     * {@code factor}, its own alpha kept - so a caller can sink a fill to a darker, more
     * recessive shade of the same hue without touching its transparency. A factor of 1 leaves
     * the colour unchanged, 0 returns black, and values between darken proportionally. Each
     * scaled channel is rounded to the nearest channel value and clamped into range, so a
     * factor above 1 (a brighten) saturates at white instead of throwing from {@link Color}'s
     * constructor.
     *
     * @param colour the source colour; its own alpha is preserved
     * @param factor the fraction of each RGB channel to keep; 1 leaves the colour unchanged,
     *               0 returns black
     * @return a colour with each RGB channel scaled by {@code factor} and the original alpha
     */
    public static Color darken(Color colour, float factor) {
        return new Color(
            scaleChannel(colour.getRed(), factor),
            scaleChannel(colour.getGreen(), factor),
            scaleChannel(colour.getBlue(), factor),
            colour.getAlpha());
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

    /**
     * Lerps every channel of {@code base} toward {@code target}, alpha included - the whole-colour
     * counterpart of {@link #blendRgbTowards}, for a caller whose two ends differ in how solid they are
     * and not only in hue. A surface fading in from unpainted is the case that needs it: an alpha kept
     * from the base would hold a transparent start transparent the whole way, so the fade would never
     * appear at all.
     *
     * <p>Kept apart from the RGB-only blend rather than replacing it, because the two answer different
     * questions. A wash over a fill that is already a surface is a pure brightness shift, and carrying an
     * alpha across there would change how solid the element is as a side effect of brightening it.
     *
     * <p>Each channel is rounded and clamped, so an out-of-range amount saturates rather than throwing
     * from {@link Color}'s constructor.
     *
     * @param base   the colour to move from
     * @param target the colour to move toward, its alpha included
     * @param amount the fraction to move each channel from base toward target
     * @return base moved toward target by amount on all four channels
     */
    public static Color blendTowards(Color base, Color target, float amount) {
        return new Color(
            lerpChannel(base.getRed(), target.getRed(), amount),
            lerpChannel(base.getGreen(), target.getGreen(), amount),
            lerpChannel(base.getBlue(), target.getBlue(), amount),
            lerpChannel(base.getAlpha(), target.getAlpha(), amount));
    }

    /**
     * The opaque colour a translucent {@code colour} composites to over {@code backdrop} - what the eye
     * already sees where that colour is drawn over that backdrop, stated as one shade that needs neither.
     * For a caller whose element must carry its own surface rather than borrow one, and for reproducing
     * a shade some other renderer arrives at by drawing one colour over another.
     *
     * <p>The backdrop is taken as opaque, since the result is: compositing over a see-through backdrop
     * yields something still see-through, which is not a surface. A fully opaque {@code colour} returns
     * itself, and a fully transparent one returns the backdrop.
     *
     * @param colour   the source colour; its alpha is what it is composited by, and is spent here
     * @param backdrop the surface it is composited over; read as opaque, its own alpha ignored
     * @return the opaque colour of the two composited, one over the other
     */
    public static Color flattenOnto(Color colour, Color backdrop) {

        var sourceWeight = colour.getAlpha() / MAX_CHANNEL;

        return new Color(
            lerpChannel(backdrop.getRed(), colour.getRed(), sourceWeight),
            lerpChannel(backdrop.getGreen(), colour.getGreen(), sourceWeight),
            lerpChannel(backdrop.getBlue(), colour.getBlue(), sourceWeight),
            MAX_CHANNEL_VALUE);
    }

    /**
     * {@code base} with {@code overlay} added onto it - the additive blend, where light is piled on rather
     * than replaced, so the result is at least as bright as the base on every channel and saturates at
     * white. For reproducing a shade another renderer arrives at by drawing a glow pass over a fill, which
     * no amount of interpolating between the two colours can produce.
     *
     * <p>The overlay's own alpha scales what it contributes, exactly as it does in the blend this
     * mirrors, and {@code overlayWeight} scales it again for a caller fading the pass in and out.
     * {@code base} keeps its alpha, being the surface the light lands on.
     *
     * @param base          the colour the light is added to; its alpha is kept
     * @param overlay       the light being added; its own alpha scales its contribution
     * @param overlayWeight how much of the overlay to add, 0 adding nothing and 1 adding it in full
     * @return the base brightened by the overlay, clamped at white
     */
    public static Color addOverlay(Color base, Color overlay, float overlayWeight) {

        var addedWeight = overlay.getAlpha() / MAX_CHANNEL * overlayWeight;

        return new Color(
            addChannel(base.getRed(), overlay.getRed(), addedWeight),
            addChannel(base.getGreen(), overlay.getGreen(), addedWeight),
            addChannel(base.getBlue(), overlay.getBlue(), addedWeight),
            base.getAlpha());
    }

    /**
     * {@code base} with {@code light} laid over it as a surface of its own: the channels added exactly as
     * {@link #addOverlay} adds them, and the result made as solid as the light landing on it. The
     * difference from that method is the whole point - light falling on a see-through surface is still
     * light, so it has to be visible, where {@code addOverlay} keeps the base's alpha and would brighten
     * the channels of something that goes on being drawn at nothing.
     *
     * <p>For a chrome whose resting state is an unpainted surface: light landing on it is what paints it,
     * and taking that back off leaves it unpainted again. A base already solid is untouched on that
     * channel, so a caller whose surfaces are all opaque cannot tell the two methods apart.
     *
     * @param base        the surface the light is added to
     * @param light       the light being added; its own alpha scales its contribution and is the ceiling
     *                    the base's own alpha is carried toward
     * @param lightWeight how much of the light to add, 0 adding nothing and 1 adding it in full
     * @return the base brightened and made solid by that much light, clamped at white
     */
    public static Color addLight(Color base, Color light, float lightWeight) {

        // The channels are the additive blend's, taken from it rather than spelt out again, so the two
        // methods cannot come to brighten the same light differently; the alpha is the whole of what this
        // one adds.
        var brightened = addOverlay(base, light, lightWeight);

        return new Color(
            brightened.getRed(),
            brightened.getGreen(),
            brightened.getBlue(),
            raiseAlphaToward(base.getAlpha(), light.getAlpha(), lightWeight));
    }

    // Adds a weighted share of one 0-255 channel onto another, saturating rather than wrapping.
    private static int addChannel(int base, int added, float addedWeight) {
        return roundToChannel(base + added * addedWeight);
    }

    // How solid a surface becomes under light: carried that fraction of the way toward the light's own
    // solidity, and never below where it started. Light can only ever add - a dim light falling on a solid
    // surface leaves it solid rather than eating a hole in it.
    private static int raiseAlphaToward(int baseAlpha, int lightAlpha, float lightWeight) {
        return roundToChannel(baseAlpha + Math.max(0, lightAlpha - baseAlpha) * lightWeight);
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
