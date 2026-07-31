package kmlib.settings;

/**
 * An enum option that a LunaLib Radio setting stores by its display label. LunaLib persists a
 * Radio field's value as the selected option's label string, so mapping that string back to
 * the enum constant is the same lookup for every such setting. Implementing this lets an enum
 * reuse {@link LabeledChoices#fromLabel} instead of hand-rolling the match loop.
 *
 * <p>Because the label is the persisted value, it is a stored key wearing the costume of a
 * caption, and is frozen once shipped for the reason a field id is: reword one and the string
 * an existing save holds resolves to nothing, so the setting silently reverts to its default
 * for every player who had picked that option. Changing the wording is a migration, not a
 * caption edit.
 */
public interface LabeledChoice {
    /**
     * @return the display label LunaLib stores for this option; the value
     *         {@link LabeledChoices#fromLabel} matches against
     */
    String getLabel();
}
