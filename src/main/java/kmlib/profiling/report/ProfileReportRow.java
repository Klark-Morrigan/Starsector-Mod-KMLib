package kmlib.profiling.report;

import kmlib.profiling.snapshot.ProfileNode;

/**
 * One line of a rendered capture: the row it reports, what it is called there,
 * and how deep it sits.
 *
 * <p>The name is the view's rather than the section's, because the same row is
 * called different things by different readings of one capture: under a tree it
 * is the section's own name, indented under the row it ran inside, while a
 * listing that has taken it out of the tree has to name the whole path or the
 * reader cannot tell two rows of one section apart.
 *
 * <p>The depth is kept even where a view prints no indent, since what is written
 * about a row - what its worst call did, what its loop ran - is written under it
 * and has to sit further in than the row it belongs to.
 */
final class ProfileReportRow {

    private final ProfileNode node;
    private final String name;
    private final int depth;

    ProfileReportRow(ProfileNode node, String name, int depth) {
        this.node = node;
        this.name = name;
        this.depth = depth;
    }

    ProfileNode getNode() {
        return node;
    }

    String getName() {
        return name;
    }

    int getDepth() {
        return depth;
    }
}
