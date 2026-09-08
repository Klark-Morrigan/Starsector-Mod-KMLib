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
 *
 * <p>One selection is one instance, holding what the walk gathers, so the
 * recursion carries only what changes as it descends - which node it is under,
 * and whether the namespace has already been matched above.
 */
final class ShownNodes {

    private final ProfileReportRequest request;
    private final Map<ProfileNode, ProfileNode> parentsByNode = new IdentityHashMap<>();
    private final List<ProfileNode> candidates = new ArrayList<>();

    private ShownNodes(ProfileReportRequest request) {
        this.request = request;
    }

    /**
     * @param roots   the origin's roots
     * @param request what is being asked of them
     * @return the nodes to write, the rows they ran inside included
     */
    static Set<ProfileNode> selectShownNodes(
            List<ProfileNode> roots,
            ProfileReportRequest request) {

        return new ShownNodes(request).select(roots);
    }

    private Set<ProfileNode> select(List<ProfileNode> roots) {

        collectCandidates(roots, null, false);
        candidates.sort(request.getView().resolveRanking());

        var shownNodes = Collections.<ProfileNode>newSetFromMap(new IdentityHashMap<>());

        for (var index = 0; index < countKept(); index++) {
            addWithAncestors(candidates.get(index), shownNodes);
        }
        return shownNodes;
    }

    // Every row the request could keep, and the way back up from each. A row
    // under a named row is itself under the namespace, whatever it is called:
    // what a kept row is made of is part of reading it.
    private void collectCandidates(
            List<ProfileNode> nodes,
            ProfileNode parent,
            boolean isUnderNamespace) {

        for (var node : nodes) {

            parentsByNode.put(node, parent);
            var isNamed = isUnderNamespace || isNamedUnderTheNamespace(node);

            if (isNamed && request.getView().isWorthShowing(node)) {
                candidates.add(node);
            }
            collectCandidates(node.getChildren(), node, isNamed);
        }
    }

    // Up to the root, stopping at the first row already kept: everything above
    // that one is kept as well, since a row is only ever added with its whole
    // way up.
    private void addWithAncestors(ProfileNode node, Set<ProfileNode> shownNodes) {

        for (var kept = node; kept != null; kept = parentsByNode.get(kept)) {

            if (!shownNodes.add(kept)) {
                return;
            }
        }
    }

    private int countKept() {

        return request.getTopRows() == ProfileReportRequest.EVERY_ROW
            ? candidates.size()
            : Math.min(request.getTopRows(), candidates.size());
    }

    // By prefix, which is what a namespace is in a dotted section name. A prefix
    // rather than a whole leading name, so asking for one section's rows and
    // asking for a family of them are the same question asked with more or less
    // of the name.
    private boolean isNamedUnderTheNamespace(ProfileNode node) {

        var namespace = request.getNamespace();

        return namespace.equals(ProfileReportRequest.EVERY_NAMESPACE)
            || node.getSection().getName().startsWith(namespace);
    }
}
