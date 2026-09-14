package kmlib.starsector.ui.widgets.tabs;

/**
 * One stretch of a tab's text, drawn as a piece in its own right because it is coloured differently from
 * its neighbours: a run of ordinary label text, or the bound key, which lights and may carry an
 * underline. A tab's text is a list of these in reading order, so a pass that paints them walks one list
 * rather than branching on where the key ended up.
 *
 * @param text the characters of this run, never blank - a run with nothing in it is left out rather than
 *             carried as an empty piece for a caller to skip
 * @param role what this run is, which is what decides how it is painted
 */
public record TabTextRun(
    String text,
    Role role) {

    /**
     * What a run of a tab's text is. Two roles rather than three: a bound key's delimiters are ordinary
     * label text that happens to sit either side of it, so nothing about painting them differs from the
     * label they were appended to.
     */
    public enum Role {

        /** Ordinary text, painted in the tab's label colour. */
        LABEL,

        /** The bound key, painted in the hotkey colour and carrying the emphasis its style asks for. */
        KEY
    }
}
