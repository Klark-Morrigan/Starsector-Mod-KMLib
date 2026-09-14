package kmlib.starsector.ui.controls;

/**
 * What a click on a radio's already-lit segment does. A standard radio treats re-picking the lit
 * option as inert, but two hosts need the re-pick to mean something: a picker that clears to nothing
 * on a second click, and a selector that acts again on the same option (to flip a sub-state such as a
 * sort direction). Naming the three intents lets a host say which it wants at the call site rather
 * than encode it as a bare boolean whose meaning a reader must recover from the input listener.
 *
 * <p>Mechanically there are only two branches the input listener takes: swallow the re-pick, or fire
 * the action on the lit segment. {@link #firesOnReselect()} folds the three intents onto that split -
 * {@link #INERT} swallows, {@link #DESELECT} and {@link #REFIRE} both fire - so the listener reads
 * one predicate and never learns what the fired action then means. The {@code DESELECT} versus {@code
 * REFIRE} distinction is for the host that supplies the action (one turns its control off, the other
 * re-runs it on the lit option) and for the reader; the widget layer treats the two the same.
 */
public enum ReselectBehaviour {
    /** Re-picking the lit segment does nothing - the standard always-selected radio (an option pair). */
    INERT,

    /** Re-picking the lit segment fires the action to turn the control off (a deselectable picker). */
    DESELECT,

    /** Re-picking the lit segment fires the action to act again on that option (e.g. flip a sort direction). */
    REFIRE;

    /**
     * Whether a click on the already-lit segment fires the control's action. True for {@link
     * #DESELECT} and {@link #REFIRE} - both want the re-pick to reach the action, differing only in
     * what the action then does - and false for {@link #INERT}, whose re-pick is swallowed.
     *
     * @return true when the lit segment is an actionable hit, false when re-picking it is inert
     */
    public boolean firesOnReselect() {
        return this != INERT;
    }
}
