package kmlib.profiling.recording;

import kmlib.profiling.ProfileOrigin;
import kmlib.profiling.ProfileSection;
import kmlib.profiling.snapshot.ProfileNode;
import kmlib.profiling.snapshot.ProfileOriginTree;

import java.util.ArrayList;
import java.util.List;

/**
 * The mutable group {@link RecordingProfiler} files a root under - one origin
 * and the roots opened beneath it, in first-seen order.
 *
 * <p>Kept apart from the {@link ProfileOriginTree} a snapshot hands out for the
 * reason a node is: what a reader holds must not change under it, and what the
 * profiler adds to must stay cheap enough for a path that opens a root every
 * frame.
 */
final class ProfileOriginAccumulator {

    private final ProfileOrigin origin;
    private final List<ProfileNodeAccumulator> roots = new ArrayList<>();

    private ProfileOriginAccumulator(ProfileOrigin origin) {
        this.origin = origin;
    }

    /**
     * Finds the group {@code origin} holds among {@code groups}, appending one
     * the first time a root is opened under that origin.
     *
     * <p>Appended on first use rather than made up front, so a capture where
     * every root named its origin carries no empty reserved group, and a
     * capture that lost one has it stated first among its groups.
     *
     * @param groups the groups of one capture, in first-opened order
     * @param origin the origin a root is being opened under
     * @return the group that capture keeps for the origin
     */
    static ProfileOriginAccumulator resolveOriginIn(
            List<ProfileOriginAccumulator> groups,
            ProfileOrigin origin) {

        // An indexed identity scan, as everywhere else on this path: an origin
        // is a registered value and a session holds one or two of them, so no
        // label is hashed and no iterator allocated.
        for (var index = 0; index < groups.size(); index++) {
            var group = groups.get(index);
            if (group.origin == origin) {
                return group;
            }
        }
        var opened = new ProfileOriginAccumulator(origin);
        groups.add(opened);
        return opened;
    }

    ProfileNodeAccumulator resolveRootNode(ProfileSection section) {
        return ProfileNodeAccumulator.resolveNodeIn(roots, section);
    }

    /**
     * @return this origin and everything under it, copied into the immutable
     *         form a snapshot is read from
     */
    ProfileOriginTree buildTree() {

        var rootNodes = new ArrayList<ProfileNode>(roots.size());

        for (var root : roots) {
            rootNodes.add(root.buildNode());
        }
        return new ProfileOriginTree(origin, rootNodes);
    }
}
