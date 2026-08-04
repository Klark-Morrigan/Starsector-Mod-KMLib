package kmlib.starsector.ui.widgets.lists;

/**
 * Test fixture: one row of a picker list, standing in for whatever a consuming mod ranks and
 * spotlights in its own sidebar. It implements {@link SelectableListItem} through its own
 * components - the seam a consumer is expected to declare its item type against - and nothing else
 * this package declares, so the sort model is still exercised over a type it has no way to open and
 * the picker draws a caller's own type without anything being mapped into a library value. Carries
 * two numerics beside the name so {@link AnomalySortMode} has distinct keys to rank and flip on.
 *
 * @param itemId          the id a pick reports, kept apart from the label so a suite can tell an
 *                        id-resolved lit row from a label-matched one
 * @param displayName     the label a row draws, null standing in for a name that did not resolve
 * @param crestSpritePath the crest path the row draws beside the label, null standing in for none
 * @param severity        one numeric a fixture mode ranks on
 * @param radius          the other, so a mode switch visibly reorders rather than relabels
 */
record Anomaly(
    String itemId,
    String displayName,
    String crestSpritePath,
    int severity,
    int radius) implements SelectableListItem {

    /**
     * An anomaly listed for the sort suites, which rank on the name and the numerics and never read
     * an id or a crest: the name doubles as the id and no crest is carried, so those suites name
     * only what they assert on.
     *
     * @param displayName the label, also standing in as the id
     * @param severity    one numeric a fixture mode ranks on
     * @param radius      the other
     */
    Anomaly(String displayName, int severity, int radius) {
        this(displayName, displayName, null, severity, radius);
    }
}
