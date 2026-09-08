package kmlib.profiling.report;

import kmlib.profiling.snapshot.ProfileNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * The capture as it was measured: every row under the row it ran inside, in the
 * order the sections were first opened.
 *
 * <p>What a reader opens first, because it is the only reading that says what a
 * total is made of. Nothing is dropped for having nothing to say - a row that
 * cost nothing is still part of what its parent cost - and the ranking is only
 * consulted where a caller asked for the few rows that matter, which it answers
 * by self time, the same question a listing answers.
 */
final class TreeProfileView implements ProfileView {

    @Override
    public boolean isWorthShowing(ProfileNode node) {
        return true;
    }

    @Override
    public Comparator<ProfileNode> resolveRanking() {
        return ProfileView.orderBySelfTimeDescending();
    }

    @Override
    public List<ProfileReportRow> presentRows(
            List<ProfileNode> roots,
            Set<ProfileNode> shownNodes) {

        var rows = new ArrayList<ProfileReportRow>();

        appendRows(roots, shownNodes, 0, rows);
        return rows;
    }

    // Depth-first, so a child is written under the row it ran inside rather than
    // after everything at its own level. A row that is not shown takes its
    // subtree with it: what survives a request keeps every ancestor of every row
    // it kept, so a node nobody kept has nothing kept beneath it either, and
    // writing one anyway would indent a row under a parent that is not there.
    private static void appendRows(
            List<ProfileNode> nodes,
            Set<ProfileNode> shownNodes,
            int depth,
            List<ProfileReportRow> rows) {

        for (var node : nodes) {

            if (!shownNodes.contains(node)) {
                continue;
            }
            rows.add(new ProfileReportRow(node, node.getSection().getName(), depth));
            appendRows(node.getChildren(), shownNodes, depth + 1, rows);
        }
    }
}
