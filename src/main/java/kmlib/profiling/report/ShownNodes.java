package kmlib.profiling.report;

import kmlib.profiling.snapshot.ProfileNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Which rows of one origin's trees survive a request.
 *
 * <p>A set of nodes rather than a pruned tree, because a node's self time is its
 * total less its children's: rebuilding a tree without the rows a request
 * dropped would hand every surviving parent the time of the children it lost and
 * report a row as costing what it never did.
 *
 * <p>Two narrowings, in one place because they compose the same way. A namespace
 * keeps what it names and everything beneath it; a count keeps the few rows the
 * reading ranks highest. Both then keep every row those rows ran inside, which
 * is what lets a consumer read their own layer against the frame it cost - a
 * finding with no parent above it is a number with nothing to be judged against.
 *
 * <p>Held by identity: a node is one section at one place in one tree, and two
 * rows of one section must not collapse into each other.
 */
final class ShownNodes {

    private ShownNodes() {
        // utility class, no instances.
    }

    /**
     * @param roots   the origin's roots
     * @param request what is being asked of them
     * @return the nodes to write, the rows they ran inside included
     */
    static Set<ProfileNode> selectShownNodes(
            List<ProfileNode> roots,
            ProfileReportRequest request) {

        var parentsByNode = new IdentityHashMap<ProfileNode, ProfileNode>();
        var candidates = new ArrayList<ProfileNode>();

        collectCandidates(roots, null, request, false, parentsByNode, candidates);
        candidates.sort(request.getView().resolveRanking());

        var shownNodes = Collections.<ProfileNode>newSetFromMap(new IdentityHashMap<>());
        var keptCount = countKept(candidates.size(), request.getTopRows());

        for (var index = 0; index < keptCount; index++) {
            addWithAncestors(candidates.get(index), parentsByNode, shownNodes);
        }
        return shownNodes;
    }

    // Every row the request could keep, and the way back up from each. A row
    // under a named row is itself under the namespace, whatever it is called:
    // what a kept row is made of is part of reading it.
    private static void collectCandidates(
            List<ProfileNode> nodes,
            ProfileNode parent,
            ProfileReportRequest request,
            boolean isUnderNamespace,
            Map<ProfileNode, ProfileNode> parentsByNode,
            List<ProfileNode> candidates) {

        for (var node : nodes) {

            parentsByNode.put(node, parent);
            var isNamed = isUnderNamespace || isNamedUnder(node, request.getNamespace());

            if (isNamed && request.getView().isWorthShowing(node)) {
                candidates.add(node);
            }
            collectCandidates(
                node.getChildren(), node, request, isNamed, parentsByNode, candidates);
        }
    }

    // Up to the root, stopping at the first row already kept: everything above
    // that one is kept as well, since a row is only ever added with its whole
    // way up.
    private static void addWithAncestors(
            ProfileNode node,
            Map<ProfileNode, ProfileNode> parentsByNode,
            Set<ProfileNode> shownNodes) {

        for (var kept = node; kept != null; kept = parentsByNode.get(kept)) {

            if (!shownNodes.add(kept)) {
                return;
            }
        }
    }

    private static int countKept(int candidateCount, int topRows) {

        return topRows == ProfileReportRequest.EVERY_ROW
            ? candidateCount
            : Math.min(topRows, candidateCount);
    }

    // By prefix, which is what a namespace is in a dotted section name. A prefix
    // rather than a whole leading name, so asking for one section's rows and
    // asking for a family of them are the same question asked with more or less
    // of the name.
    private static boolean isNamedUnder(ProfileNode node, String namespace) {

        return namespace.equals(ProfileReportRequest.EVERY_NAMESPACE)
            || node.getSection().getName().startsWith(namespace);
    }
}
