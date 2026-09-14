package kmlib.profiling.report;

import kmlib.profiling.ProfileCounter;
import kmlib.profiling.snapshot.ProfileNode;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * One reading of a capture: which of its rows have anything to say, which of
 * them matter most, and how they are laid out.
 *
 * <p>Three questions rather than one rendering, because a caller asking for the
 * few rows that matter has to be answered before the layout is decided: what
 * "the worst twenty" means is the view's, and cutting a laid-out tree at twenty
 * lines would answer a different question.
 *
 * <p>Views are not what a caller names - a request is
 * ({@link ProfileReportRequest}) - so nothing outside this package states one.
 */
interface ProfileView {

    /**
     * Worst self time first: what a reader hunting for where the time went sorts
     * by, since a row's own cost is the one it can be optimised on.
     */
    static Comparator<ProfileNode> orderBySelfTimeDescending() {
        return Comparator.comparingLong(ProfileNode::getSelfNanos).reversed();
    }

    /**
     * Most of {@code counter} in one call first, then most in all.
     *
     * <p>Per call rather than in total, because a bound is broken by one call:
     * the row that walked the sector three times in a single call is the one to
     * look at, not the row that walked it once on each of a thousand frames.
     */
    static Comparator<ProfileNode> orderByCountPerCallDescending(ProfileCounter counter) {

        return Comparator
            .comparingLong((ProfileNode node) -> readMaxPerCall(node, counter))
            .thenComparingLong(node -> readTotal(node, counter))
            .reversed();
    }

    /**
     * @param node the row being considered
     * @return whether this reading has anything to say about it at all - a
     *         reading of what walked has nothing to say about a row that never
     *         did
     */
    boolean isWorthShowing(ProfileNode node);

    /**
     * @return how this reading ranks two rows when a caller has asked for only
     *         the few that matter
     */
    Comparator<ProfileNode> resolveRanking();

    /**
     * Lays the shown rows out in this reading's order, naming each as this
     * reading names it.
     *
     * @param roots      the origin's roots, walked for the structure the names
     *                   and depths are taken from
     * @param shownNodes which rows survived the request - held by identity, a
     *                   node being one place in one tree
     * @return the rows to write, in the order they are written
     */
    List<ProfileReportRow> presentRows(List<ProfileNode> roots, Set<ProfileNode> shownNodes);

    // A row that never counted it ranks as nothing rather than as an error: the
    // reading that cares whether it counted has already dropped it.
    private static long readMaxPerCall(ProfileNode node, ProfileCounter counter) {

        var count = node.findCount(counter);

        return count == null ? 0L : count.getMaxPerCall();
    }

    private static long readTotal(ProfileNode node, ProfileCounter counter) {

        var count = node.findCount(counter);

        return count == null ? 0L : count.getTotals().getTotal();
    }
}
