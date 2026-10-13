package kmlib.starsector.strings;

import kmlib.math.percentages.Percentages;

import java.math.BigDecimal;

/**
 * Formats figures the way Starsector's own copy shows them. A tooltip that routes its template through
 * {@link StarsectorStrings#format(String, String, Object...)} uses these to shape the figure for a {@code %s} slot.
 */
public final class StarsectorFormat {

    private StarsectorFormat() {
    }

    /**
     * Formats a {@code [0, 1]} fraction as a whole percent with a trailing percent sign: {@code 0.01f} returns
     * {@code "1%"} and {@code 0.20f} returns {@code "20%"}. Rounds as
     * {@link Percentages#computeWholePercent(float)} does.
     *
     * @param fraction the fraction to format, e.g. {@code 0.20f} for 20%
     * @return the rendered whole-percent string, never {@code null}
     */
    public static String formatPercent(float fraction) {

        return Percentages.computeWholePercent(fraction) + "%";
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
