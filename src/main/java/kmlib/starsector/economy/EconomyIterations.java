package kmlib.starsector.economy;

import com.fs.starfarer.api.SettingsAPI;

/**
 * Where an economy tick falls within its month.
 *
 * <p>The economy steps a fixed number of times a month, the {@code economyIterPerMonth} setting, numbering the steps
 * from zero each month. The last step runs in the same frame as the month end, ahead of it, so it is the last moment a
 * listener can write to the month's report before the game totals and pays it - vanilla's month-end stipends book on it
 * for that reason.
 */
public final class EconomyIterations {

    private static final String ITERATIONS_PER_MONTH_KEY = "economyIterPerMonth";

    private EconomyIterations() {
    }

    /**
     * @param settings  the game settings, which carry the month's step count
     * @param iterationIndex the step's index within its month, as the economy tick reports it
     * @return whether the step is the month's last
     */
    public static boolean isLastIterationOfMonth(SettingsAPI settings, int iterationIndex) {

        var lastIterationIndex = (int) settings.getFloat(ITERATIONS_PER_MONTH_KEY) - 1;

        return iterationIndex == lastIterationIndex;
    }
}
