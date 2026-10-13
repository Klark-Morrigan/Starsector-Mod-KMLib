package kmlib.starsector.strings;

import java.math.BigDecimal;

/**
 * Number-to-string helpers for Starsector mod copy. Lives next to
 * {@link StarsectorStrings} because the typical caller is a tooltip /
 * description paint that already routes its template through
 * {@code StarsectorStrings.format(...)} and just needs the numeric
 * argument shaped to match the {@code %s} slot.
 *
 * <p>A mod's settings usually store a percentage as a {@code [0, 1]}
 * fraction ({@code 0.20f} for 20%) so the arithmetic reads naturally,
 * while the player-facing copy wants {@code "20%"}. Doing such
 * conversions in one place keeps every KM* mod's tooltips rendering a
 * figure the same way.
 *
 * <p>The class is final and has no public constructor: it is a pure
 * static utility, mirroring the {@link StarsectorStrings} shape.
 */
public final class StarsectorFormat {

    private static final float PERCENT_PER_FRACTION = 100f;

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

    /**
     * Formats a figure with no trailing zeros: {@code 2f} returns {@code "2"} rather than {@code "2.0"}, while
     * {@code 1.5f} keeps its decimal. A setting that ships whole reads cleanly, and one retuned to a fraction still
     * reads honestly. Never in scientific notation, however large the figure.
     *
     * @param value the figure to format
     * @return the rendered figure, never {@code null}
     */
    public static String formatTrimmedNumber(float value) {

        return new BigDecimal(Float.toString(value))
            .stripTrailingZeros()
            .toPlainString();
    }
}
