package kmlib.starsector.ui.widgets.lists;

import java.util.Comparator;

/**
 * Test fixture: a set of sort modes declared outside this package - the stand-in for whatever a
 * consuming mod ranks its picker list by. An enum, since that is the shape a consumer is expected to
 * reach for. Alpha runs ascending by default while the two numeric modes run descending, so a suite
 * can tell a mode's own default direction from the active sort's live one. Only {@link #RADIUS}
 * declares a trailing value; the other two leave the seam's blank default, so a suite can tell a mode
 * that writes a number onto its rows from one that shows none.
 *
 * <p>Each label is a plain literal, which is what the seam asks for: a consumer resolves its own
 * strings and hands drawn text over, so a fixture has nothing to resolve against.
 */
enum AnomalySortMode implements ListSortMode<Anomaly> {
    ALPHA(
        "alpha",
        "Alpha",
        SortDirection.ASCENDING,
        Comparator.comparing(Anomaly::displayName, String.CASE_INSENSITIVE_ORDER)),
    SEVERITY(
        "severity",
        "Severity",
        SortDirection.DESCENDING,
        Comparator.comparingInt(Anomaly::severity)),
    RADIUS(
        "radius",
        "Radius",
        SortDirection.DESCENDING,
        Comparator.comparingInt(Anomaly::radius)) {

        // The one mode that writes its number onto the rows, so a suite can tell a drawn trailing
        // value apart from the blank the other two leave.
        @Override
        public String resolveTrailingValue(Anomaly anomaly) {
            return String.valueOf(anomaly.radius());
        }
    };

    private final String persistenceKey;
    private final String labelText;
    private final SortDirection defaultDirection;

    // The mode's key low-to-high; comparator(direction) runs it as asked, forward or reversed.
    private final Comparator<Anomaly> ascendingOrder;

    AnomalySortMode(
            String persistenceKey,
            String labelText,
            SortDirection defaultDirection,
            Comparator<Anomaly> ascendingOrder) {

        this.persistenceKey = persistenceKey;
        this.labelText = labelText;
        this.defaultDirection = defaultDirection;
        this.ascendingOrder = ascendingOrder;
    }

    @Override
    public String persistenceKey() {
        return persistenceKey;
    }

    @Override
    public String resolveLabelText() {
        return labelText;
    }

    @Override
    public SortDirection defaultDirection() {
        return defaultDirection;
    }

    @Override
    public Comparator<Anomaly> comparator(SortDirection direction) {
        return direction == SortDirection.ASCENDING
            ? ascendingOrder
            : ascendingOrder.reversed();
    }
}
