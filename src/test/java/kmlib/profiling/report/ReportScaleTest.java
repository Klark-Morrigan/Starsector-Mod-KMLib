package kmlib.profiling.report;

import kmlib.profiling.ProfileOrigin;
import kmlib.profiling.ProfileSection;
import kmlib.profiling.snapshot.BudgetBreach;
import kmlib.profiling.snapshot.DurationBuckets;
import kmlib.profiling.snapshot.ProfileIterations;
import kmlib.profiling.snapshot.ProfileNode;
import kmlib.profiling.snapshot.ProfileOriginTree;
import kmlib.profiling.snapshot.ProfileTiming;
import kmlib.profiling.snapshot.WorstCall;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link ReportScale}: a frame is every call of the named beat wherever it sits in the trees,
 * a request naming no beat and a capture the named beat never ran in are both reported as captured,
 * and what is divided reads as a fraction of a frame while what is not stays whole.
 */
final class ReportScaleTest {

    private static final String FRAME_BEAT_SECTION = "mapLayer.prepare";
    private static final String REBUILD_SECTION = "politicalMap.rebuild";
    private static final String UNOPENED_SECTION = "mapLayer.tooltip";

    private static final String ORIGIN_LABEL = "MN-6220 - Marat";

    // Two calls of the beat, so a total divided by them is a figure the row never reported.
    private static final long FRAMES = 2;
    private static final long ONE_CALL = 1;

    private static final long THREE_MILLIS_IN_NANOS = 3_000_000;

    // Five of something over those two frames, which is the fraction a whole number could not say.
    private static final long FIVE_ITEMS = 5;

    @Nested
    class ResolveScale {

        @Test
        void countsEveryCallOfTheBeatWhereverItSits() {
            // A beat opened under two parents is still one beat per frame, so counting only the
            // roots would divide by however many of them happened to be roots.
            var scale = ReportScale.resolveScale(
                originTreeOf(
                    beatNode(ONE_CALL),
                    nodeOf(REBUILD_SECTION, beatNode(ONE_CALL))),
                dividedByTheBeat());

            assertThat(scale.getFrames())
                .isEqualTo(FRAMES);
        }

        @Test
        void reportsAsCapturedWhereNoBeatWasNamed() {

            var scale = ReportScale.resolveScale(
                originTreeOf(beatNode(FRAMES)), ProfileReportRequest.showTree());

            assertThat(scale.isPerFrame())
                .isFalse();
        }

        @Test
        void reportsAsCapturedWhereTheNamedBeatNeverRan() {
            // A capture holding no frame of the beat cannot say what one cost, and dividing by
            // nothing would report a figure no frame ever had.
            var scale = ReportScale.resolveScale(
                originTreeOf(beatNode(FRAMES)),
                ProfileReportRequest.showTree().divideByFramesOf(
                    ProfileSection.registerSection(UNOPENED_SECTION)));

            assertThat(scale.isPerFrame())
                .isFalse();
        }
    }

    @Nested
    class FormatCount {

        @Test
        void writesAWholeNumberWhereNothingIsBeingDivided() {

            assertThat(ReportScale.PER_CAPTURE.formatCount(FIVE_ITEMS))
                .isEqualTo("5");
        }

        @Test
        void writesAFractionOfAFrameWhereTheTotalsAreDivided() {
            // Five over two frames is 2.5 of them a frame: rounded to a whole number it would read
            // as two, and a pass that walks the sector every other frame has to read as something
            // other than nothing.
            assertThat(perFrameScale().formatCount(FIVE_ITEMS))
                .isEqualTo("2.50");
        }
    }

    @Nested
    class FormatMillis {

        @Test
        void writesTheDurationItselfWhereNothingIsBeingDivided() {

            assertThat(ReportScale.PER_CAPTURE.formatMillis(THREE_MILLIS_IN_NANOS))
                .isEqualTo("3.000");
        }

        @Test
        void writesWhatOneFrameSpentWhereTheTotalsAreDivided() {

            assertThat(perFrameScale().formatMillis(THREE_MILLIS_IN_NANOS))
                .isEqualTo("1.500");
        }
    }

    private static ReportScale perFrameScale() {

        return ReportScale.resolveScale(originTreeOf(beatNode(FRAMES)), dividedByTheBeat());
    }

    private static ProfileReportRequest dividedByTheBeat() {

        return ProfileReportRequest.showTree().divideByFramesOf(
            ProfileSection.registerSection(FRAME_BEAT_SECTION));
    }

    private static ProfileOriginTree originTreeOf(ProfileNode... roots) {

        return new ProfileOriginTree(
            ProfileOrigin.registerOrigin(ORIGIN_LABEL), List.of(roots));
    }

    private static ProfileNode beatNode(long calls) {

        return new ProfileNode(
            ProfileSection.registerSection(FRAME_BEAT_SECTION),
            new ProfileTiming(
                calls,
                THREE_MILLIS_IN_NANOS,
                THREE_MILLIS_IN_NANOS,
                THREE_MILLIS_IN_NANOS,
                DurationBuckets.NO_CALLS),
            WorstCall.NO_CALL,
            BudgetBreach.NO_BREACH,
            ProfileIterations.NO_ITERATIONS,
            List.of(),
            List.of());
    }

    private static ProfileNode nodeOf(String name, ProfileNode... children) {

        return new ProfileNode(
            ProfileSection.registerSection(name),
            new ProfileTiming(
                ONE_CALL,
                THREE_MILLIS_IN_NANOS,
                THREE_MILLIS_IN_NANOS,
                THREE_MILLIS_IN_NANOS,
                DurationBuckets.NO_CALLS),
            WorstCall.NO_CALL,
            BudgetBreach.NO_BREACH,
            ProfileIterations.NO_ITERATIONS,
            List.of(),
            List.of(children));
    }
}
