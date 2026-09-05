package kmlib.profiling;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins what a snapshot row derives rather than receives: self time is the total less what ran
 * inside it, and a row whose children outlast it - a scope still open when the snapshot was taken -
 * reports no self time rather than a negative duration.
 */
final class ProfileNodeTest {

    private static final long PARENT_TOTAL_NANOS = 100L;
    private static final long CHILD_TOTAL_NANOS = 30L;

    @Nested
    class GetSelfNanos {

        @Test
        void reportsTheTotalLessEveryChildTotal() {

            var parent = new ProfileNode(
                ProfileSection.registerSection("test.profileNode.parent"),
                1,
                PARENT_TOTAL_NANOS,
                PARENT_TOTAL_NANOS,
                PARENT_TOTAL_NANOS,
                List.of(),
                List.of(
                    childNode("test.profileNode.firstChild", CHILD_TOTAL_NANOS),
                    childNode("test.profileNode.secondChild", CHILD_TOTAL_NANOS)));

            assertThat(parent.getSelfNanos())
                .isEqualTo(40L);
        }

        @Test
        void reportsNoSelfTimeWhereTheChildrenOutweighTheTotal() {
            // The shape a scope left open leaves behind: its children closed and it did not, so it
            // holds no span of its own to take theirs out of.
            var unclosed = new ProfileNode(
                ProfileSection.registerSection("test.profileNode.unclosed"),
                0,
                0,
                0,
                0,
                List.of(),
                List.of(childNode("test.profileNode.closedChild", CHILD_TOTAL_NANOS)));

            assertThat(unclosed.getSelfNanos())
                .isEqualTo(0L);
        }
    }

    private static ProfileNode childNode(String name, long totalNanos) {
        return new ProfileNode(
            ProfileSection.registerSection(name),
            1,
            totalNanos,
            totalNanos,
            totalNanos,
            List.of(),
            List.of());
    }
}
