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
     * <p>Appended on first use, so a capture where every root named its origin
     * carries no empty reserved group - and one that lost a root has that group
     * stated among the rest rather than always present and usually empty.
     *
     * @param groups the groups of one capture, in first-opened order
     * @param origin the origin a root is being opened under
     * @return the group that capture keeps for the origin
     */
    static ProfileOriginAccumulator resolveOriginIn(
            List<ProfileOriginAccumulator> groups,
            ProfileOrigin origin) {

        return IdentityLookup.resolveByKey(
            groups,
            ProfileOriginAccumulator::getOrigin,
            origin,
            ProfileOriginAccumulator::new);
    }

    ProfileOrigin getOrigin() {
        return origin;
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
