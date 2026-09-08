package kmlib.profiling.report;

import kmlib.profiling.ProfileCounter;
import kmlib.profiling.ProfileSection;
import kmlib.text.KmlibStrings;

/**
 * What is being asked of a capture: which reading of it, over which rows, how
 * many of them, and against what.
 *
 * <p>One value rather than four arguments, because they are asked together and
 * mean nothing apart: "the twenty worst rows of my own layer, per frame" is one
 * question, and a reader that took them as four would let a caller state half a
 * question.
 *
 * <p>Immutable and composed by narrowing: each call returns the request it
 * describes rather than changing the one it was asked of, so a caller holding a
 * default cannot have it narrowed underneath them.
 *
 * <p>The reading is named by how a capture is being read - as the tree it was
 * measured as, as a listing worst first, or as what a counter says - rather than
 * by naming a view type, since which types exist is this package's business and
 * what is being asked for is the caller's.
 */
public final class ProfileReportRequest {

    /** Every row, whatever it is called: the namespace nothing is dropped by. */
    public static final String EVERY_NAMESPACE = "";

    /** However many there are: the count nothing is cut by. */
    public static final int EVERY_ROW = 0;

    private final ProfileView view;
    private final String namespace;
    private final int topRows;

    // Null where nothing is being divided by. A sentinel section would have to be
    // registered to stand for "no beat", which would put a row nobody opens in
    // the registry every reader shares - a worse trade than one field that reads
    // through hasFrameBeat.
    private final ProfileSection frameBeat;

    private ProfileReportRequest(
            ProfileView view,
            String namespace,
            int topRows,
            ProfileSection frameBeat) {

        this.view = view;
        this.namespace = namespace;
        this.topRows = topRows;
        this.frameBeat = frameBeat;
    }

    /**
     * The capture as it was measured: every row under the row it ran inside.
     *
     * @return the request so far, over every row of every namespace
     */
    public static ProfileReportRequest showTree() {
        return showEverythingIn(new TreeProfileView());
    }

    /**
     * The capture as a listing, worst self time first, each row named by its
     * whole path.
     *
     * @return the request so far, over every row of every namespace
     */
    public static ProfileReportRequest showRowsBySelfTime() {
        return showEverythingIn(FlatProfileView.sortBySelfTime());
    }

    /**
     * Only the rows that counted {@code counter}, most of it in one call first.
     *
     * <p>Stated over a counter rather than over a name, because what a counter
     * means is the caller's: profiling knows that a row counted something and
     * not that the something was a walk of a sector.
     *
     * @param counter what the rows are being read for
     * @return the request so far, over every row of every namespace
     */
    public static ProfileReportRequest showRowsCounting(ProfileCounter counter) {
        return showEverythingIn(FlatProfileView.sortByCounter(counter));
    }

    /**
     * Narrows to the rows whose section name starts with {@code namespace},
     * keeping the rows they ran inside so their share of a frame can still be
     * read against the whole of it.
     *
     * @param namespace what a row has to be called to be kept; blank keeps
     *                  everything
     * @return the request so narrowed
     */
    public ProfileReportRequest limitToNamespace(String namespace) {

        var kept = KmlibStrings.hasText(namespace) ? namespace : EVERY_NAMESPACE;

        return new ProfileReportRequest(view, kept, topRows, frameBeat);
    }

    /**
     * Narrows to the {@code topRows} rows this reading ranks highest, plus the
     * rows they ran inside.
     *
     * @param topRows how many rows to keep; nothing positive keeps them all
     * @return the request so narrowed
     */
    public ProfileReportRequest limitToTopRows(int topRows) {

        var kept = Math.max(EVERY_ROW, topRows);

        return new ProfileReportRequest(view, namespace, kept, frameBeat);
    }

    /**
     * Reports what a row costs per call of {@code frameBeat} rather than what it
     * came to over the whole capture.
     *
     * <p>What a reader wants of a frame is one number: a total over four minutes
     * of play says the map was drawn a lot, while the same total over the beat
     * that ran once per frame says what a frame spends.
     *
     * @param frameBeat the section whose calls a frame is counted by
     * @return the request so scaled
     */
    public ProfileReportRequest divideByFramesOf(ProfileSection frameBeat) {
        return new ProfileReportRequest(view, namespace, topRows, frameBeat);
    }

    ProfileView getView() {
        return view;
    }

    String getNamespace() {
        return namespace;
    }

    int getTopRows() {
        return topRows;
    }

    /**
     * @return whether a frame beat was named to divide by; where it was not,
     *         every row is reported as it was captured
     */
    boolean hasFrameBeat() {
        return frameBeat != null;
    }

    ProfileSection getFrameBeat() {
        return frameBeat;
    }

    private static ProfileReportRequest showEverythingIn(ProfileView view) {
        return new ProfileReportRequest(view, EVERY_NAMESPACE, EVERY_ROW, null);
    }
}
