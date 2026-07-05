package kmlib.settings;

/**
 * An enum option that a LunaLib Radio setting stores by its display label. LunaLib persists a
 * Radio field's value as the selected option's label string, so mapping that string back to
 * the enum constant is the same lookup for every such setting. Implementing this lets an enum
 * reuse {@link LabeledChoices#fromLabel} instead of hand-rolling the match loop.
 */
public interface LabeledChoice {
    /**
     * @return the display label LunaLib stores for this option; the value
     *         {@link LabeledChoices#fromLabel} matches against
     */
    String getLabel();
}
