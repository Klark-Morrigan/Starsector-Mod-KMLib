package kmlib.testfixtures.starsector.ui.widgets.lists;

import kmlib.starsector.ui.widgets.lists.SelectableListItem;

/**
 * Test fixture: one row of a picker list, standing in for whatever a consuming mod ranks and
 * spotlights in its own sidebar. It implements {@link SelectableListItem} through its own
 * components - the seam a consumer is expected to declare its item type against - and nothing the
 * list package declares, so the sort model is exercised over a type it has no way to open and the
 * picker draws a caller's own type without anything being mapped into a library value. Carries two
 * numerics beside the name so {@link AnomalySortMode} has distinct keys to rank and flip on.
 *
 * <p>It decides for itself whether a row reads back, off a value of its own rather than off either
 * ranked numeric - a consumer's dim rule is its own business, and running the picker's tone split
 * against a rule the sort cannot see is what proves the picker asks the seam rather than the metric.
 *
 * @param itemId          the ID a pick reports, kept apart from the label so a suite can tell an
 *                        id-resolved lit row from a label-matched one
 * @param displayName     the label a row draws, null standing in for a name that did not resolve
 * @param crestSpritePath the crest path the row draws beside the label, null standing in for none
 * @param severity        one numeric a fixture mode ranks on
 * @param radius          the other, so a mode switch visibly reorders rather than relabels
 * @param isDimmed        whether this row reads back, stated outright so a suite picks the state it
 *                        is asserting on rather than deriving it
 */
public record Anomaly(
    String itemId,
    String displayName,
    String crestSpritePath,
    int severity,
    int radius,
    boolean isDimmed) implements SelectableListItem {

    /**
     * An anomaly that reads at full strength - what almost every case lists, since the tone split is
     * asserted by two suites and assumed by the rest.
     *
     * @param itemId          the ID a pick reports
     * @param displayName     the label a row draws
     * @param crestSpritePath the crest path, null standing in for none
     * @param severity        one numeric a fixture mode ranks on
     * @param radius          the other
     */
    public Anomaly(
            String itemId,
            String displayName,
            String crestSpritePath,
            int severity,
            int radius) {

        this(itemId, displayName, crestSpritePath, severity, radius, false);
    }

    /**
     * An anomaly listed for the sort suites, which rank on the name and the numerics and never read
     * an ID or a crest: the name doubles as the ID and no crest is carried, so those suites name
     * only what they assert on.
     *
     * @param displayName the label, also standing in as the ID
     * @param severity    one numeric a fixture mode ranks on
     * @param radius      the other
     */
    public Anomaly(String displayName, int severity, int radius) {
        this(displayName, displayName, null, severity, radius);
    }
}
