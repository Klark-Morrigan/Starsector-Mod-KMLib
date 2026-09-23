package kmlib.starsector.ui.compatibility;

import kmlib.starsector.compatibility.CompatibilityFailures;
import kmlib.testfixtures.starsector.compatibility.CompatibilityFailureFixture;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers what the on-screen reporter does when it cannot report: the case that runs on every
 * install where nothing is broken, and the one that decides whether a failure survives to be shown
 * the other way.
 *
 * <p>The raise itself needs a running game to stand a panel in, so what is pinned here is the
 * record either side of it. A reporter that took a failure it then could not show would lose the
 * modal entirely - the dialog would find nothing waiting when the player returned to the campaign,
 * and the only trace left would be the log.
 *
 * <p>The reach for a screen is supplied rather than taken live. The live one walks the game's own
 * widget tree and reports a map screen it cannot find, and a case about the record either side of
 * a raise is not asking it to.
 */
final class ScreenCompatibilityNoticesTest {

    // A screen that is not showing, which is every frame the player is not on a map.
    private static final Supplier<Object> NO_SCREEN = () -> null;

    private final CompatibilityFailures failures = new CompatibilityFailures();

    @Nested
    class ShowPendingFailureOnScreen {

        @Test
        void showsNothingWhereNothingWasRecorded() {

            assertThat(ScreenCompatibilityNotices.showPendingFailureOnScreen(failures, NO_SCREEN))
                .isFalse();
        }

        @Test
        void leavesTheFailureOnTheRecordWhereNoScreenCouldBeFound() {

            // No screen, so no panel. The failure has to still be there: the campaign's dialog is
            // what reports it from here, and it reads the same record.
            recordOneFailure();

            var wasShown = ScreenCompatibilityNotices.showPendingFailureOnScreen(failures, NO_SCREEN);

            assertThat(wasShown)
                .isFalse();
            assertThat(failures.hasUnreported())
                .isTrue();
        }

        @Test
        void leavesTheFailureOnTheRecordWhereTheScreenCannotBeReached() {

            // A widget tree that cannot be walked is not a reason to lose a report, only a reason
            // to show it the other way.
            recordOneFailure();

            var wasShown = ScreenCompatibilityNotices.showPendingFailureOnScreen(
                failures,
                ScreenCompatibilityNoticesTest::throwCannotWalkTree);

            assertThat(wasShown)
                .isFalse();
            assertThat(failures.hasUnreported())
                .isTrue();
        }

        @Test
        void reachesForNoScreenWhereNothingIsPending() {

            // The healthy path, taken on every frame of every session that has nothing wrong with
            // it: one empty check on the record, and no walk of the widget tree at all. A reach
            // that throws is what proves it was never made.
            var wasShown = ScreenCompatibilityNotices.showPendingFailureOnScreen(
                failures,
                ScreenCompatibilityNoticesTest::throwCannotWalkTree);

            assertThat(wasShown)
                .isFalse();
        }
    }

    // The reach into the widget tree as it behaves on a build that renamed what it walks through.
    private static Object throwCannotWalkTree() {

        throw new IllegalStateException("The widget tree could not be walked");
    }

    // One failure waiting to be reported, which is what puts the reporter past its first check.
    private void recordOneFailure() {

        failures.recordOnce(
            CompatibilityFailureFixture.FAST_RENDERING_SUBJECT_KEY,
            CompatibilityFailureFixture.MAP_OVERLAY_CONSUMER,
            recordedAs -> CompatibilityFailureFixture.createFailure());
    }
}
