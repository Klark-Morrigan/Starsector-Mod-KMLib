package kmlib.profiling.snapshot;

import kmlib.profiling.ProfileOrigin;

import java.util.List;

/**
 * One origin's share of a capture: the label, and the sections opened as roots
 * under it.
 *
 * <p>A capture is a list of these rather than one list of roots, because the
 * same section under two games is two facts. Grouping at the top is what lets a
 * reader take a maximum back to the save it happened in, and what keeps a
 * second game loaded in one session from reading as a discontinuity in the
 * first.
 *
 * <p>Immutable, like everything else a reader is handed: what is being read
 * must not change while it is being read.
 */
public final class ProfileOriginTree {

    private final ProfileOrigin origin;
    private final List<ProfileNode> roots;

    public ProfileOriginTree(ProfileOrigin origin, List<ProfileNode> roots) {
        this.origin = origin;
        this.roots = List.copyOf(roots);
    }

    public ProfileOrigin getOrigin() {
        return origin;
    }

    /**
     * @return the sections opened with nothing else open under this origin, in
     *         the order they were first opened
     */
    public List<ProfileNode> getRoots() {
        return roots;
    }
}
