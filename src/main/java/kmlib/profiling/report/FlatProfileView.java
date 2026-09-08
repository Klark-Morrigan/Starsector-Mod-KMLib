package kmlib.profiling.report;

import kmlib.profiling.ProfileCounter;
import kmlib.profiling.snapshot.ProfileNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The capture as a listing: every row out of the tree and in one order, worst
 * first.
 *
 * <p>What a reader opens once they know a frame is too slow and want to know
 * which row to open next. A row is named by its whole path here, since two rows
 * of one section only differ by where they ran and a listing has taken that
 * away.
 *
 * <p>One class for the two listings that exist, because they differ only in what
 * they rank by and in which rows they have anything to say about: the same
 * laying out serves "where did the time go" and "what walked the sector".
 */
final class FlatProfileView implements ProfileView {

    // The tree written into one name. A separator a section name cannot contain,
    // so a path reads back as the rows it was composed of.
    private static final String PATH_SEPARATOR = "/";

    private static final String NO_PARENT_PATH = "";

    // A listing has taken the rows out of the tree, so nothing is indented and
    // what is written under a row sits one level in from the left.
    private static final int LISTED_DEPTH = 0;

    private final Predicate<ProfileNode> isWorthShowing;
    private final Comparator<ProfileNode> ranking;

    private FlatProfileView(Predicate<ProfileNode> isWorthShowing, Comparator<ProfileNode> ranking) {
        this.isWorthShowing = isWorthShowing;
        this.ranking = ranking;
    }

    @Override
    public boolean isWorthShowing(ProfileNode node) {
        return isWorthShowing.test(node);
    }

    @Override
    public Comparator<ProfileNode> resolveRanking() {
        return ranking;
    }

    @Override
    public List<ProfileReportRow> presentRows(
            List<ProfileNode> roots,
            Set<ProfileNode> shownNodes) {

        var rows = new ArrayList<ProfileReportRow>();

        appendRows(roots, NO_PARENT_PATH, shownNodes, rows);
        rows.sort(Comparator.comparing(ProfileReportRow::getNode, ranking));
        return rows;
    }

    /**
     * Every row, worst self time first - where the time went.
     */
    static FlatProfileView sortBySelfTime() {
        return new FlatProfileView(node -> true, ProfileView.orderBySelfTimeDescending());
    }

    /**
     * Only the rows that counted {@code counter}, most of it in one call first.
     *
     * <p>Only those rows, because a listing of what walked is unreadable beside
     * the rows that never walked: the question is which pass went looking for the
     * sector, and a row that never did is not an answer at zero.
     */
    static FlatProfileView sortByCounter(ProfileCounter counter) {

        return new FlatProfileView(
            node -> node.findCount(counter) != null,
            ProfileView.orderByCountPerCallDescending(counter));
    }

    // Walked rather than taken from the shown set directly, because a path is
    // only known from above: a node holds its own name and its children, and
    // what it ran inside is the walk that reached it.
    private static void appendRows(
            List<ProfileNode> nodes,
            String parentPath,
            Set<ProfileNode> shownNodes,
            List<ProfileReportRow> rows) {

        for (var node : nodes) {

            var path = parentPath.isEmpty()
                ? node.getSection().getName()
                : parentPath + PATH_SEPARATOR + node.getSection().getName();

            if (shownNodes.contains(node)) {
                rows.add(new ProfileReportRow(node, path, LISTED_DEPTH));
            }
            appendRows(node.getChildren(), path, shownNodes, rows);
        }
    }
}
