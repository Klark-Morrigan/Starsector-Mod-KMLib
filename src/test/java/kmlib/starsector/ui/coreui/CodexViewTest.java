package kmlib.starsector.ui.coreui;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins what a caller standing an overlay down for the codex depends on, none of it visible from the
 * rule's own shape.
 *
 * <p>That the answer comes off the state's own report and not off anything shaped like a dialog,
 * since the codex is raised outside the core UI entirely - which is what makes it invisible to the
 * modal rule beside it and this class necessary at all.
 *
 * <p>That every way the read can fail lands on "no codex". The caller only ever stands something
 * down on this answer, so a state that cannot be asked has to leave it drawing rather than take it
 * off a screen the player is looking at.
 *
 * <p>The live resolution is exercised only as far as it goes with no game running: the app driver
 * holds no state until the game has begun, so what a test JVM can assert is that the absence reads
 * as no codex. That the driver hands back the campaign's state, and that the state answers this
 * accessor, are in-game facts.
 */
class CodexViewTest {

    @Nested
    class IsCodexShowingOn {

        @Test
        void isCodexShowingOnIsTrueForAStateReportingTheCodexUp() {

            assertThat(CodexView.isCodexShowingOn(new CodexShowingStateFake(true)))
                .isTrue();
        }

        @Test
        void isCodexShowingOnIsFalseForAStateReportingNoCodex() {

            assertThat(CodexView.isCodexShowingOn(new CodexShowingStateFake(false)))
                .isFalse();
        }

        @Test
        void isCodexShowingOnIsFalseForAStateThatDoesNotAnswerForACodex() {
            // The shape the rule must survive rather than one it can rule out: the accessor belongs
            // to the states that can raise a codex, so a state not carrying it is a different state
            // and not a broken read.
            assertThat(CodexView.isCodexShowingOn(new Object()))
                .isFalse();
        }

        @Test
        void isCodexShowingOnIsFalseWhenTheStateRaisesOnBeingAsked() {
            // A hop that resolves and then throws is the other way the read fails, and it lands on
            // the same answer as a shape carrying no accessor - the caller has no way to act on the
            // difference, and this runs from a render pass every frame.
            assertThat(CodexView.isCodexShowingOn(new UnreadableCodexStateFake()))
                .isFalse();
        }

        @Test
        void isCodexShowingOnIsFalseWhenThereIsNoState() {

            assertThat(CodexView.isCodexShowingOn(null))
                .isFalse();
        }
    }

    @Nested
    class IsCodexShowing {

        @Test
        void isCodexShowingIsFalseBeforeTheGameHasStoodUpAState() {

            assertThat(CodexView.isCodexShowing())
                .isFalse();
        }
    }

    // Stands for an app state that can raise a codex and reports whether one is up. Local rather
    // than a shipped fixture because a consuming mod takes this answer as a supplied read, so
    // nothing outside this suite ever stands an app state up.
    public static final class CodexShowingStateFake {

        private final boolean isShowingCodex;

        CodexShowingStateFake(boolean isShowingCodex) {
            this.isShowingCodex = isShowingCodex;
        }

        public boolean isShowingCodex() {
            return isShowingCodex;
        }
    }

    // Stands for a state whose accessor is there and fails when called - the half a game build can
    // break on its own, as against a state carrying no such accessor at all.
    public static final class UnreadableCodexStateFake {

        public boolean isShowingCodex() {
            throw new IllegalStateException("codex state cannot be read");
        }
    }
}
