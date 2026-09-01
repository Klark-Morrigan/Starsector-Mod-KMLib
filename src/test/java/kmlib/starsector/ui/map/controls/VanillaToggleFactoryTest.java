package kmlib.starsector.ui.map.controls;

import kmlib.testfixtures.starsector.ui.map.controls.MapFilterButtonFake;
import kmlib.testfixtures.starsector.ui.map.controls.MapFilterRowFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the write into somebody else's widget: that a row of the shape the game builds gets one more
 * button standing at the end of it, reporting where it was told to and nowhere else - and that every
 * row which is not that shape gets nothing at all.
 *
 * <p>The redirect is pinned from both ends, because a button that reported to both would look right
 * and would rewrite the player's map filters on every click of it. So the callback hearing the click
 * is only half the claim; the row no longer hearing it is the other.
 *
 * <p>The refusals are pinned one shape at a time because they are what the signature matching is
 * for. The two members this reaches for carry the same regenerated name in the game, so nothing but
 * their shapes tells them apart - and a row where a shape fits twice, or not at all, is one where
 * the recognition has stopped picking out what it was written to pick out. Guessing there would be a
 * coin toss made inside another party's widget, so every one of those cases has to come back empty
 * rather than nearly right.
 */
class VanillaToggleFactoryTest {

    private static final String LABEL = "Map layers";

    // What the game lays its own M-screen toggles at. Stated so the append is asked for at a size a
    // row really uses rather than at a placeholder.
    private static final float BUTTON_WIDTH = 120f;
    private static final float BUTTON_HEIGHT = 25f;

    private static final Runnable DOES_NOTHING = () -> {
    };

    @Nested
    class AppendToggle {

        @Test
        void appendToggleStandsOneMoreButtonAtTheEndOfTheRow() {
            // The ordinary case on either screen, and the one thing the whole match is for: a row
            // the game built, with a button on the end of it that the game did not.
            var rowFake = new MapFilterRowFake("Starscape", "Fuel range");

            var button = VanillaToggleFactory.appendToggle(
                new MapFilterRow(rowFake), LABEL, BUTTON_WIDTH, BUTTON_HEIGHT, DOES_NOTHING);

            assertThat(button)
                .isNotNull();
            assertThat(rowFake.getChildrenCopy())
                .hasSize(3)
                .last().isSameAs(button);
        }

        @Test
        void appendToggleSendsTheButtonsClicksToTheCallerAndNotToTheRow() {
            // Both halves of the redirect. The row answers a click by rewriting all eight of the
            // game's filter settings from its own toggles, so a button still reporting there would
            // spend every click writing the player's map filters back over themselves.
            var rowFake = new MapFilterRowFake("Starscape");
            var clickCount = new AtomicInteger();

            var button = (MapFilterButtonFake) VanillaToggleFactory.appendToggle(
                new MapFilterRow(rowFake), LABEL, BUTTON_WIDTH, BUTTON_HEIGHT,
                clickCount::incrementAndGet);
            button.click();

            assertThat(clickCount.get())
                .isEqualTo(1);
            assertThat(rowFake.countClicksHeard())
                .isZero();
        }

        @Test
        void appendToggleRefusesARowThatBuildsNoButtons() {
            // What a game build that reworked the row into some other shape looks like from here.
            // Nothing is reached for once the first member is missing, the button's own shape being
            // what the rest of the match is read off.
            assertThat(VanillaToggleFactory.appendToggle(
                new MapFilterRow(new BareRowFake()), LABEL, BUTTON_WIDTH, BUTTON_HEIGHT,
                DOES_NOTHING))
                .isNull();
        }

        @Test
        void appendToggleRefusesARowWhereTwoMembersLookLikeItsButtonFactory() {
            // Two candidates say the recognition no longer picks out one member, which is the same
            // news as none: picking either would be a guess about what the game does with it.
            assertThat(VanillaToggleFactory.appendToggle(
                new MapFilterRow(new TwinFactoryRowFake()), LABEL, BUTTON_WIDTH, BUTTON_HEIGHT,
                DOES_NOTHING))
                .isNull();
        }

        @Test
        void appendToggleRefusesARowWithNowhereToStandAButton() {
            // A row that builds buttons and lays out none of them. Refused rather than half-done:
            // the button would exist, belong to nothing, and draw nowhere.
            assertThat(VanillaToggleFactory.appendToggle(
                new MapFilterRow(new UnappendableRowFake()), LABEL, BUTTON_WIDTH, BUTTON_HEIGHT,
                DOES_NOTHING))
                .isNull();
        }

        @Test
        void appendToggleRefusesARowWhereTwoMembersLookLikeItsAppender() {
            // The second member matched, and ambiguous for the same reason and with the same answer
            // as the first.
            assertThat(VanillaToggleFactory.appendToggle(
                new MapFilterRow(new TwinAppenderRowFake()), LABEL, BUTTON_WIDTH, BUTTON_HEIGHT,
                DOES_NOTHING))
                .isNull();
        }

        @Test
        void appendToggleRefusesARowWhoseButtonsCannotBeRedirected() {
            // The match reaches the button and stops there. A button whose clicks cannot be taken
            // off the row is worse than no button: it would draw as a control of ours and act as one
            // of the game's.
            assertThat(VanillaToggleFactory.appendToggle(
                new MapFilterRow(new UndivertibleButtonRowFake()), LABEL, BUTTON_WIDTH,
                BUTTON_HEIGHT, DOES_NOTHING))
                .isNull();
        }

        @Test
        void appendToggleRefusesARowThatBuildsNothingWhenAsked() {
            // A row of the right shape that answers the call with nothing, which is a different
            // failure from any shape mismatch and has to be survived before the button is used.
            assertThat(VanillaToggleFactory.appendToggle(
                new MapFilterRow(new ButtonlessRowFake()), LABEL, BUTTON_WIDTH, BUTTON_HEIGHT,
                DOES_NOTHING))
                .isNull();
        }
    }

    /** A row offering nothing that looks like a way to build one of its buttons. */
    private static final class BareRowFake {
    }

    /** A row offering two members that both look like its button factory. */
    private static final class TwinFactoryRowFake {

        private MapFilterButtonFake o00000(String buttonLabel, Object shortcut) {
            return new MapFilterButtonFake(null);
        }

        private MapFilterButtonFake o00001(String buttonLabel, Object modifier) {
            return new MapFilterButtonFake(null);
        }
    }

    /** A row that builds buttons and offers nothing that looks like a way to lay one out. */
    private static final class UnappendableRowFake {

        private MapFilterButtonFake o00000(String buttonLabel, Object shortcut) {
            return new MapFilterButtonFake(null);
        }
    }

    /** A row offering two members that both look like its appender. */
    private static final class TwinAppenderRowFake {

        private MapFilterButtonFake o00000(String buttonLabel, Object shortcut) {
            return new MapFilterButtonFake(null);
        }

        private void o00001(MapFilterButtonFake button, float width, float height) {
        }

        private void o00002(MapFilterButtonFake button, float x, float y) {
        }
    }

    /** A row whose buttons offer nothing that looks like a way to redirect their clicks. */
    private static final class UndivertibleButtonRowFake {

        private String o00000(String buttonLabel, Object shortcut) {
            return buttonLabel;
        }

        private void o00001(String button, float width, float height) {
        }
    }

    /** A row of the shape this recognises that answers with no button when asked for one. */
    private static final class ButtonlessRowFake {

        private MapFilterButtonFake o00000(String buttonLabel, Object shortcut) {
            return null;
        }

        private void o00001(MapFilterButtonFake button, float width, float height) {
        }
    }
}
