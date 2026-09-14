package kmlib.starsector.ui.widgets.lists;

import kmlib.starsector.ui.text.TextSpan;

import java.awt.Color;
import java.util.Comparator;
import java.util.List;

/**
 * Test fixture: a set of sort modes declared outside this package - the stand-in for whatever a
 * consuming mod ranks its picker list by. An enum, since that is the shape a consumer is expected to
 * reach for. Alpha runs ascending by default while the numeric modes run descending, so a suite
 * can tell a mode's own default direction from the active sort's live one.
 *
 * <p>The four shapes a trailing value can take are one mode apiece, so a suite can tell them apart:
 * {@link #ALPHA} leaves the seam's no-runs default, {@link #SEVERITY} answers a run that came out
 * blank (the value a consumer assembled from parts and found nothing to put in), {@link #RADIUS}
 * writes one filled run in the tone the row itself takes, and {@link #SPREAD} writes a joined range
 * mixing shades of its own with the offered one.
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
        Comparator.comparingInt(Anomaly::severity)) {

        // The came-out-blank mode: a run that is present but carries nothing, which is what a
        // consumer composing a value from parts hands back when none of them had anything to say.
        // The row must not be able to tell it from the mode that declares no value at all.
        @Override
        public List<TextSpan> resolveTrailingRuns(Anomaly anomaly, Color defaultColour) {
            return List.of(TextSpan.createBlank(defaultColour));
        }
    },

    RADIUS(
        "radius",
        "Radius",
        SortDirection.DESCENDING,
        Comparator.comparingInt(Anomaly::radius)) {

        // The plain-value mode: one filled run in whatever tone the row itself took, so a suite can
        // tell a drawn value apart from the unfilled slot the two valueless modes leave.
        @Override
        public List<TextSpan> resolveTrailingRuns(Anomaly anomaly, Color defaultColour) {
            return List.of(new TextSpan(String.valueOf(anomaly.radius()), defaultColour));
        }
    },

    SPREAD(
        "spread",
        "Spread",
        SortDirection.DESCENDING,
        Comparator.comparingInt(anomaly -> anomaly.radius() - anomaly.severity())) {

        // The multi-run mode: the two ends of the spread parted by a separator, each end in a shade of
        // its own and the separator in the offered one, so a suite can tell a value that carries its
        // own colours from one that takes the row's and can watch the receded override win over both.
        // The last two runs butt against the ones before them, so the range reads as one value - which
        // is also what lets a suite pin that receding a value keeps its spacing.
        @Override
        public List<TextSpan> resolveTrailingRuns(Anomaly anomaly, Color defaultColour) {
            return List.of(
                new TextSpan(String.valueOf(anomaly.severity()), LOW_END_COLOUR),
                new TextSpan(RANGE_SEPARATOR, defaultColour).joinsPreviousRun(),
                new TextSpan(String.valueOf(anomaly.radius()), HIGH_END_COLOUR).joinsPreviousRun());
        }
    };

    /** What parts {@link #SPREAD}'s two ends, named so a suite spells the drawn value once. */
    static final String RANGE_SEPARATOR = "-";

    // The shades SPREAD's two ends draw in - two arbitrary, distinguishable literals, since what a
    // suite reads off them is only that the mode's own colours reached the slot rather than the row's.
    static final Color HIGH_END_COLOUR = Color.BLUE;
    static final Color LOW_END_COLOUR = Color.RED;

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
