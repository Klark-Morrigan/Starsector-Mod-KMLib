package kmlib.profiling;

import java.util.ArrayList;
import java.util.List;

/**
 * The mutable node {@link RecordingProfiler} adds to - one section under one
 * parent - together with the nodes opened inside it, in first-opened order.
 *
 * <p>Kept apart from the {@link ProfileNode} a snapshot hands out: what a
 * reader is given must not change while it is being read, and what the profiler
 * adds to must stay cheap enough for a path that runs every frame.
 */
final class ProfileNodeAccumulator {

    private final ProfileSection section;
    private final List<ProfileNodeAccumulator> children = new ArrayList<>();

    private long count;
    private long totalNanos;
    private long minNanos;
    private long maxNanos;

    ProfileNodeAccumulator(ProfileSection section) {
        this.section = section;
    }

    /**
     * Finds the node {@code section} holds among {@code siblings}, appending
     * one the first time that section is opened there.
     *
     * <p>Static and taking the list, because the roots of the tree are siblings
     * with no node above them and are resolved by the same rule as a node's
     * children.
     *
     * @param siblings the nodes at one level, in first-opened order
     * @param section  the section being opened
     * @return the node that level keeps for the section
     */
    static ProfileNodeAccumulator resolveNodeIn(
            List<ProfileNodeAccumulator> siblings,
            ProfileSection section) {

        // An indexed identity scan rather than a map lookup: a node holds a
        // handful of children, a section is a registered value, and this runs on
        // every open - so no name is hashed and no iterator allocated.
        for (var index = 0; index < siblings.size(); index++) {
            var sibling = siblings.get(index);
            if (sibling.section == section) {
                return sibling;
            }
        }
        var opened = new ProfileNodeAccumulator(section);
        siblings.add(opened);
        return opened;
    }

    ProfileSection getSection() {
        return section;
    }

    ProfileNodeAccumulator resolveChildNode(ProfileSection childSection) {
        return resolveNodeIn(children, childSection);
    }

    void addSpan(long elapsedNanos) {

        // The first span sets both bounds rather than being folded into
        // sentinels the snapshot would then have to undo. A node with no span -
        // opened while the snapshot was taken, or left open by a caller - then
        // reads as the zeroes it holds, with no rule about what a row means.
        if (count == 0) {
            minNanos = elapsedNanos;
            maxNanos = elapsedNanos;
        } else {
            minNanos = Math.min(minNanos, elapsedNanos);
            maxNanos = Math.max(maxNanos, elapsedNanos);
        }
        count++;
        totalNanos += elapsedNanos;
    }

    /**
     * @return this node and everything under it, copied into the immutable form
     *         a snapshot is read from
     */
    ProfileNode buildNode() {

        var childNodes = new ArrayList<ProfileNode>(children.size());

        for (var child : children) {
            childNodes.add(child.buildNode());
        }
        return new ProfileNode(section, count, totalNanos, minNanos, maxNanos, childNodes);
    }
}
