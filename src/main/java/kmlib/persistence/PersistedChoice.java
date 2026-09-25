package kmlib.persistence;

/**
 * An option that is stored by a save-stable key of its own rather than by its name or position, so
 * the choice a save holds resolves back to the option whatever the option is later called or
 * wherever it is later listed.
 *
 * <p>The key is frozen once shipped: rename one and the string an existing save holds resolves to
 * nothing, so the choice silently reverts to its fallback for everyone who had picked that option.
 * Where the key is stored is the caller's; nothing here reaches a save.
 *
 * <p>The counterpart of {@link kmlib.settings.LabeledChoice}, which a LunaLib radio stores by its
 * display label: a label is a key LunaLib chose, this is a key the option owns.
 */
public interface PersistedChoice {

    /**
     * @return the save-stable key this option persists under; the value
     *         {@link PersistedChoices#fromKey} matches against
     */
    String persistenceKey();
}
