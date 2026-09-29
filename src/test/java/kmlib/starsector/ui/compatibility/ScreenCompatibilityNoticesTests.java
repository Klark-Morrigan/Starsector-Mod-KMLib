package kmlib.starsector.ui.compatibility;

import kmlib.starsector.compatibility.CompatibilityFailures;
import kmlib.testfixtures.starsector.compatibility.CompatibilityFailureFixture;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.function.BooleanSupplier;

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
 * <p>The read of the screen is supplied rather than taken live. The live one walks the game's own
 * widget tree and reports a map screen it cannot find, and a case about the record either side of
 * a raise is not asking it to. A tree that cannot be walked reads as no screen, which that read
 * pins for itself.
 */
final class ScreenCompatibilityNoticesTests {

    // A screen that is not showing, which is every frame the player is not on a map.
    private static final BooleanSupplier NO_SCREEN = () -> false;

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
        void reachesForNoScreenWhereNothingIsPending() {

            // The healthy path, taken on every frame of every session that has nothing wrong with
            // it: one empty check on the record, and no walk of the widget tree at all. A read that
            // throws is what proves it was never taken.
            var wasShown = ScreenCompatibilityNotices.showPendingFailureOnScreen(
                failures,
                ScreenCompatibilityNoticesTests::throwIfAsked);

            assertThat(wasShown)
                .isFalse();
        }
    }

    // A read of the screen that must not be taken.
    private static boolean throwIfAsked() {

        throw new IllegalStateException("The screen was read.");
    }

    // One failure waiting to be reported, which is what puts the reporter past its first check.
    private void recordOneFailure() {

        failures.recordOnce(
            CompatibilityFailureFixture.FAST_RENDERING_SUBJECT_KEY,
            CompatibilityFailureFixture.MAP_OVERLAY_CONSUMER,
            recordedAs -> CompatibilityFailureFixture.createFailure());
    }
}
