package kmlib.starsector.ui.compatibility;

import kmlib.testfixtures.starsector.ui.coreui.CoreUiReachFailures;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Covers the half of the raise that decides whether to attempt one at all.
 *
 * <p>Standing the panel up needs a running game, so what is pinned is the gate in front of it: a
 * screen that is not showing and a widget tree that cannot be walked both answer no, and neither
 * reaches for the game to find that out. The gate failing open would put a panel in a tree nothing
 * is drawing, which reports success and shows the player nothing.
 */
final class CompatibilityNoticePanelTest {

    @Nested
    class RaiseNoticeOnScreen {

        @Test
        void standsNoNoticeWhereNoScreenIsShowing() {

            assertThat(CompatibilityNoticePanel.raiseNoticeOnScreen(() -> null))
                .isNull();
        }

        @Test
        void standsNoNoticeWhereTheScreenCannotBeReached() {

            // A reach that raises means the widget tree cannot be walked at all. Failing closed is
            // what keeps that from becoming a panel nobody can see or dismiss.
            assertThat(CompatibilityNoticePanel.raiseNoticeOnScreen(ScreenReaches::throwCannotWalkTree))
                .isNull();
        }

        @Test
        void throwsNothingWhereTheScreenCannotBeReached() {

            assertThatCode(() ->
                    CompatibilityNoticePanel.raiseNoticeOnScreen(ScreenReaches::throwCannotWalkTree))
                .doesNotThrowAnyException();
        }

        @Test
        void standsNoNoticeWhereTheGamesOwnFailureComesBackChecked() {
            // How the reach hands on a game method that threw: wrapped, checked and undeclared, so a
            // gate catching unchecked failures alone would let it out of the frame.
            assertThat(CompatibilityNoticePanel.raiseNoticeOnScreen(
                    CoreUiReachFailures::throwWrappedGameFailure))
                .isNull();
        }

        @Test
        void standsNoNoticeWhereAMemberNoLongerLinks() {

            assertThat(CompatibilityNoticePanel.raiseNoticeOnScreen(CoreUiReachFailures::throwUnlinkedMember))
                .isNull();
        }
    }

    // The reach into the widget tree as it behaves on a build that renamed what it walks through.
    private static final class ScreenReaches {

        private static Object throwCannotWalkTree() {

            throw new IllegalStateException("The widget tree could not be walked");
        }
    }
}
