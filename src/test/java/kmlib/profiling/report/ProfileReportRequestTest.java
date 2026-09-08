package kmlib.profiling.report;

import kmlib.profiling.ProfileSection;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins how a request narrows: a namespace nobody stated keeps every row, a row count that asks for
 * none keeps them all, a frame beat is only divided by once one is named, and narrowing answers a
 * new request rather than changing the one it was asked of.
 */
final class ProfileReportRequestTest {

    private static final String NAMESPACE = "politicalMap.";
    private static final String BLANK_NAMESPACE = "   ";
    private static final String FRAME_BEAT_SECTION = "mapLayer.prepare";

    private static final int TWENTY_ROWS = 20;
    private static final int FEWER_THAN_NONE = -5;

    @Nested
    class LimitToNamespace {

        @Test
        void keepsTheNamespaceItWasGiven() {

            var request = ProfileReportRequest.showTree().limitToNamespace(NAMESPACE);

            assertThat(request.getNamespace())
                .isEqualTo(NAMESPACE);
        }

        @Test
        void keepsEveryRowWhereTheNamespaceIsBlank() {
            // A player who typed namespace= and nothing after it asked for no narrowing, which is
            // a different thing from asking for the rows named after a run of spaces.
            var request = ProfileReportRequest.showTree().limitToNamespace(BLANK_NAMESPACE);

            assertThat(request.getNamespace())
                .isEqualTo(ProfileReportRequest.EVERY_NAMESPACE);
        }

        @Test
        void answersANewRequestRatherThanChangingTheOneItWasAskedOf() {
            // A caller holding a default must not have it narrowed underneath them by whoever they
            // handed it to.
            var everything = ProfileReportRequest.showTree();
            var narrowed = everything.limitToNamespace(NAMESPACE).limitToTopRows(TWENTY_ROWS);

            assertThat(everything.getNamespace())
                .isEqualTo(ProfileReportRequest.EVERY_NAMESPACE);
            assertThat(everything.getTopRows())
                .isEqualTo(ProfileReportRequest.EVERY_ROW);
            assertThat(narrowed.getNamespace())
                .isEqualTo(NAMESPACE);
        }
    }

    @Nested
    class LimitToTopRows {

        @Test
        void keepsTheCountItWasGiven() {

            var request = ProfileReportRequest.showTree().limitToTopRows(TWENTY_ROWS);

            assertThat(request.getTopRows())
                .isEqualTo(TWENTY_ROWS);
        }

        @Test
        void keepsEveryRowWhereFewerThanNoneWereAskedFor() {
            // Nothing positive asks for no rows at all, which is never what a reader meant: a
            // report of nothing answers no question, so it is read as no narrowing.
            var request = ProfileReportRequest.showTree().limitToTopRows(FEWER_THAN_NONE);

            assertThat(request.getTopRows())
                .isEqualTo(ProfileReportRequest.EVERY_ROW);
        }
    }

    @Nested
    class HasFrameBeat {

        @Test
        void reportsNoBeatUntilOneIsNamed() {

            assertThat(ProfileReportRequest.showTree().hasFrameBeat())
                .isFalse();
        }

        @Test
        void reportsTheBeatThatWasNamed() {

            var beat = ProfileSection.registerSection(FRAME_BEAT_SECTION);
            var request = ProfileReportRequest.showTree().divideByFramesOf(beat);

            assertThat(request.hasFrameBeat())
                .isTrue();
            assertThat(request.getFrameBeat())
                .isSameAs(beat);
        }
    }
}
