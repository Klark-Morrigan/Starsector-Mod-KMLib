package kmlib.starsector.strings;

/**
 * Number-to-string helpers for Starsector mod copy. Lives next to
 * {@link StarsectorStrings} because the typical caller is a tooltip /
 * description paint that already routes its template through
 * {@code StarsectorStrings.format(...)} and just needs the numeric
 * argument shaped to match the {@code %s} slot.
 *
 * <p>The helpers exist for one reason: a mod's {@code KmoSettings}-style
 * configuration object usually stores percentages as {@code [0, 1]}
 * fractions ({@code 0.20f} for 20%) so the math sites read naturally,
 * but the player-facing copy wants a {@code "20%"} string. Doing that
 * conversion in one place keeps the truncate-vs-round choice consistent
 * across every tooltip in the KM* mod family.
 *
 * <p>The class is final and has no public constructor: it is a pure
 * static utility, mirroring the {@link StarsectorStrings} shape.
 */
public final class StarsectorFormat {

    private StarsectorFormat() {
    }

    /**
     * Formats a {@code [0, 1]} fraction as a whole-percent string with
     * a trailing percent sign. For example, {@code 0.01f} returns
     * {@code "1%"} and {@code 0.20f} returns {@code "20%"}.
     *
     * <p>Truncates (via {@code (int)}) rather than rounding: settings
     * defaults across the KM* mod family ship at one significant figure,
     * so a stray rounding bump from {@code 0.014f} -> {@code "1%"} to
     * {@code "2%"} would mislead the player on a value the modder
     * picked deliberately. A future caller that needs rounded output
     * adds a sibling helper rather than changing the contract here.
     *
     * @param fraction the fraction to format, e.g. {@code 0.20f} for 20%.
     * @return the rendered whole-percent string, never {@code null}.
     */
    public static String formatPercent(float fraction) {
        return ((int) (fraction * PERCENT_PER_FRACTION)) + "%";
    }

    private static final float PERCENT_PER_FRACTION = 100f;
}
