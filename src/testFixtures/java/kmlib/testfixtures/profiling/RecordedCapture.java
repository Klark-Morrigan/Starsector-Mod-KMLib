package kmlib.testfixtures.profiling;

import kmlib.profiling.ActiveProfiler;
import kmlib.profiling.SilentProfiler;
import kmlib.profiling.recording.RecordingProfiler;
import kmlib.profiling.snapshot.ProfileNode;
import kmlib.profiling.snapshot.ProfileOriginTree;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * What a profiler recorded while some work ran through the process-wide holder, and the rows
 * found in it by name.
 *
 * <p>Published as a fixture variant so every KM mod pins what its passes record through one double, in the way
 * {@code LogAppenderFake} serves the game-log seam. The holder is process-wide and a binding left
 * behind follows the next case into a capture it never asked for, so binding, running and
 * restoring the silent default belong to {@link #recordWhile} rather than to each caller.
 *
 * <p>Rows are found by section name rather than taken as "the first root": what runs before a
 * scope opens - posing a pass, reading the sector - counts onto the reserved row, which then heads
 * the capture, and a case reading the first root would be reading that.
 */
public final class RecordedCapture {

    private final List<ProfileOriginTree> originTrees;

    private RecordedCapture(List<ProfileOriginTree> originTrees) {
        this.originTrees = originTrees;
    }

    /**
     * Runs {@code work} with a recording profiler bound, timing against the system clock.
     *
     * @param work what to run while recording
     * @return what was recorded
     */
    public static RecordedCapture recordWhile(Runnable work) {
        return recordWhile(new RecordingProfiler(), work);
    }

    /**
     * Runs {@code work} with {@code profiler} bound, for a caller that supplies its own clock or
     * level.
     *
     * @param profiler the profiler to bind while the work runs
     * @param work     what to run while recording
     * @return what was recorded
     */
    public static RecordedCapture recordWhile(RecordingProfiler profiler, Runnable work) {

        ActiveProfiler.bindProfiler(profiler);
        try {
            work.run();
        } finally {
            ActiveProfiler.bindProfiler(SilentProfiler.INSTANCE);
        }
        return new RecordedCapture(profiler.snapshot());
    }

    /**
     * @return the whole capture, one tree per origin, in first-record order
     */
    public List<ProfileOriginTree> getOriginTrees() {
        return originTrees;
    }

    /**
     * @param sectionName the name of the section whose row is wanted
     * @return the row that section's calls landed on, wherever in the capture it sits
     * @throws NoSuchElementException where nothing of that name was recorded
     */
    public ProfileNode findNode(String sectionName) {

        for (var originTree : originTrees) {
            var found = findNodeIn(originTree.getRoots(), sectionName);
            if (found != null) {
                return found;
            }
        }
        throw new NoSuchElementException("No row recorded for section '" + sectionName + "'");
    }

    /**
     * @param sectionName the name of the section asked about
     * @return whether any call of it was recorded at all
     */
    public boolean hasNode(String sectionName) {

        for (var originTree : originTrees) {
            if (findNodeIn(originTree.getRoots(), sectionName) != null) {
                return true;
            }
        }
        return false;
    }

    // Depth first, since a row may sit under whatever the work opened above it.
    private static ProfileNode findNodeIn(List<ProfileNode> nodes, String sectionName) {

        for (var node : nodes) {
            if (node.getSection().getName().equals(sectionName)) {
                return node;
            }
            var found = findNodeIn(node.getChildren(), sectionName);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
