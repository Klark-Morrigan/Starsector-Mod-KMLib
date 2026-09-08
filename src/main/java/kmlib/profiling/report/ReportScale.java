package kmlib.profiling.report;

import kmlib.profiling.ProfileSection;
import kmlib.profiling.snapshot.ProfileNode;
import kmlib.profiling.snapshot.ProfileOriginTree;
import kmlib.time.Timings;

import java.util.List;

/**
 * What one origin's totals are divided by: nothing, or the frames they were
 * spread over.
 *
 * <p>A total says how much a session did, which is a fact about how long the
 * player left the map open. What a reader is after is what one frame spends, and
 * that is the total over the beat that ran once per frame - one number, on the
 * same scale whether it was captured over four seconds or four minutes.
 *
 * <p>Per origin rather than per capture, because two games are two sets of
 * frames: dividing one game's rows by another's frame count would report a cost
 * no frame ever had.
 *
 * <p>Only the totals divide. A minimum, a maximum and an average are per call
 * already, and a call is not a frame.
 */
final class ReportScale {

    /** Every total as it was captured: what a request that named no beat asks for. */
    static final ReportScale PER_CAPTURE = new ReportScale(0L);

    private final long frames;

    private ReportScale(long frames) {
        this.frames = frames;
    }

    /**
     * @param originTree the origin whose rows are being written
     * @param request    what was asked of the capture
     * @return what this origin's totals divide by - as captured where no beat was
     *         named, and as captured where the beat named never ran here, since a
     *         capture holding no frame of it cannot say what one cost
     */
    static ReportScale resolveScale(ProfileOriginTree originTree, ProfileReportRequest request) {

        if (!request.hasFrameBeat()) {
            return PER_CAPTURE;
        }
        var frames = countCalls(originTree.getRoots(), request.getFrameBeat());

        return frames == 0 ? PER_CAPTURE : new ReportScale(frames);
    }

    boolean isPerFrame() {
        return frames > 0;
    }

    long getFrames() {
        return frames;
    }

    /**
     * @param amount what a row counted over the whole capture
     * @return how much of it fell in one frame, or the amount itself where
     *         nothing is being divided - written whole there, since a count of
     *         things is a whole number until a frame is what it is spread over
     */
    String formatCount(long amount) {

        return isPerFrame()
            ? ReportFormats.formatScaledCount((double) amount / frames)
            : Long.toString(amount);
    }

    String formatMillis(long nanos) {

        var millis = Timings.convertNanosToMillis(nanos);

        return ReportFormats.formatMillis(isPerFrame() ? millis / frames : millis);
    }

    // Every row of that section, wherever it sits: a beat opened under two
    // parents is still one beat per frame, and counting only the roots would
    // divide by however many of them happened to be roots.
    private static long countCalls(List<ProfileNode> nodes, ProfileSection section) {

        var calls = 0L;

        for (var node : nodes) {

            if (node.getSection() == section) {
                calls += node.getTiming().getCount();
            }
            calls += countCalls(node.getChildren(), section);
        }
        return calls;
    }
}
