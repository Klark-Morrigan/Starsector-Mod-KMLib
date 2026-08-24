package kmlib.starsector.ui.map.probes;

import kmlib.starsector.ui.coreui.CoreUiTree;

import java.util.function.Function;

/**
 * The search this package's probes make when they want the first widget under a root that answers
 * for something, and nothing else about the tree.
 *
 * <p>Extracted because two probes were making it identically - one looking for the widget that
 * carries the map's icons, one for the widget showing a tooltip - and the part they were copying is
 * the part that is easy to get subtly wrong: descend only through the children hop, stop at the
 * depth bound, and treat a component that answers nothing as a leaf rather than a failure. Both were
 * also expressing the same bound in opposite directions, one counting up and one counting down,
 * which is the sort of drift a shared walk removes rather than documents.
 *
 * <p>The other walks in this package are deliberately not built on this. They collect rather than
 * stop at the first answer, and each carries something different down with it - a full ancestry, a
 * parent and a depth, a running set with a cap of its own. Folding those together needs a visit
 * result to signal "skip this subtree" or "stop now" and a context object to carry the rest, and the
 * skeleton it would save them is about five lines each. What the abstraction costs in reading is
 * more than the copying costs, so they stay as they are.
 *
 * <p>What a caller supplies is a read rather than a predicate, so the thing found comes back with
 * the answer instead of being looked up a second time by whoever asked. A read that wants to be
 * called for every component it meets - one keeping a tally, or recording what it saw on the way -
 * gets that for free, since it runs once per node whether or not it answers.
 *
 * <p>Nothing here is guarded. Every hop it takes is {@link CoreUiTree}'s forgiving one, which
 * answers null rather than raising, but a caller's own read can raise and that is left to travel: a
 * probe knows what a failed read means for what it draws, and this does not.
 */
final class SubtreeSearch {

    // Where a walk starts counting. Named rather than passed by callers, so the bound below is
    // stated against one origin instead of each caller choosing its own and meaning something
    // slightly different by the same number.
    private static final int ROOT_DEPTH = 0;

    private SubtreeSearch() {
    }

    /**
     * The first answer any component in this subtree gives, searching depth-first from the root
     * itself downwards.
     *
     * <p>Depth-first and root-first is what makes "first" mean the outermost answer on the earliest
     * branch, which is what a caller looking for one widget among many wants: the tab's own answer
     * before its content's, and a container's before its children's.
     *
     * @param <T>         what the caller is looking for
     * @param root        the widget to search from, or null when the caller has none
     * @param readValueOf the read to try on each component, answering null for one that does not
     *                    have what is wanted - which is most of a widget tree
     * @return the first non-null answer, or null when no component in the subtree gave one
     */
    static <T> T findFirstUnder(Object root, Function<Object, T> readValueOf) {
        return findFirstUnder(root, readValueOf, ROOT_DEPTH);
    }

    // Carries the depth down so a tree that is malformed - or whose parent and child answer as each
    // other's children - cannot walk until the stack gives out in the middle of a frame.
    private static <T> T findFirstUnder(Object component, Function<Object, T> readValueOf, int depth) {

        if (component == null || depth > ProbeLimits.MAX_SEARCH_DEPTH) {
            return null;
        }
        var value = readValueOf.apply(component);

        if (value != null) {
            return value;
        }
        for (var child : CoreUiTree.readChildrenOf(component)) {

            var foundInChild = findFirstUnder(child, readValueOf, depth + 1);

            if (foundInChild != null) {
                return foundInChild;
            }
        }
        return null;
    }
}
