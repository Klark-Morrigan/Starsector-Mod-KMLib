package kmlib.settings;

/**
 * Maps a stored LunaLib Radio label back to its enum constant. One place for the match loop
 * every {@link LabeledChoice} enum would otherwise repeat: scan the constants for the one
 * whose label matches, and fall back when none do (an unset setting, or a label left behind
 * by an option that no longer exists).
 */
public final class LabeledChoices {
    private LabeledChoices() {
    }

    /**
     * Finds the option whose label equals {@code label}.
     *
     * @param options  the enum's constants, e.g. from {@code values()}
     * @param label    the stored label to match, may be {@code null}
     * @param fallback the option returned when none match
     * @param <T>      the labelled enum type
     * @return the matching option, or {@code fallback} when none has that label
     */
    public static <T extends LabeledChoice> T fromLabel(T[] options, String label, T fallback) {
        for (var option : options) {
            if (option.getLabel().equals(label)) {
                return option;
            }
        }
        return fallback;
    }
}
