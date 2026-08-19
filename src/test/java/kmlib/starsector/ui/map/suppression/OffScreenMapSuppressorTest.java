package kmlib.starsector.ui.map.suppression;

import com.fs.starfarer.api.ui.UIComponentAPI;

import kmlib.math.geometry.Rectangle;
import kmlib.testfixtures.starsector.ui.coreui.CoreUiWidgetFake;
import kmlib.testfixtures.starsector.ui.layout.PositionFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins when a map widget is switched off and when it is handed back, which between them are the
 * whole of what this script does to somebody else's screen.
 *
 * <p>The pairing is what the cases are built around: every state that suppresses has a counterpart
 * that restores, because a widget left at zero is a map the player has lost rather than a map that
 * costs nothing. The caller withdrawing its permission is one such counterpart, and so is the widget
 * simply sliding back on screen.
 *
 * <p>The value handed back is pinned as the one the widget was found at rather than as anything this
 * script chose, since the field belongs to whoever built the widget.
 */
class OffScreenMapSuppressorTest {

    // Advancing an EveryFrameScript from a test says nothing about elapsed time - this script reads
    // no clock - so the value only has to be one the engine could plausibly pass.
    private static final float ONE_FRAME = 0.016f;

    private static final float FULLY_DRAWN = 1f;

    // A widget its owner draws at something other than full, which is what makes "put back what it
    // was at" distinguishable from "put back one".
    private static final float PARTLY_FADED = 0.6f;

    private static final Rectangle OFF_SCREEN_BOX = new Rectangle(-400f, 100f, 200f, 150f);
    private static final Rectangle ON_SCREEN_BOX = new Rectangle(20f, 100f, 200f, 150f);

    // The screen every box below is placed against, in UI units.
    private static final Supplier<Rectangle> SCREEN = () -> new Rectangle(0f, 0f, 800f, 600f);

    private static final float TOLERANCE = 0.001f;

    @Nested
    class Advance {

        @Test
        void drawsAParkedMapToNothing() {
            // The whole point: a widget with no part of it on screen goes on rendering a sector map
            // every frame until something writes its opacity down.
            var mapFake = new CoreUiWidgetFake(new PositionFake(OFF_SCREEN_BOX), FULLY_DRAWN);

            buildSuppressorOf(mapFake).advance(ONE_FRAME);

            assertThat(mapFake.getOpacity())
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void leavesAMapOnScreenAlone() {
            var mapFake = new CoreUiWidgetFake(new PositionFake(ON_SCREEN_BOX), FULLY_DRAWN);

            buildSuppressorOf(mapFake).advance(ONE_FRAME);

            assertThat(mapFake.getOpacity())
                .isCloseTo(FULLY_DRAWN, within(TOLERANCE));
        }

        @Test
        void leavesAMapPartlyOnScreenAlone() {
            // The slide: a panel walking on or off screen is partly visible for those frames, and
            // switching it off midway would be a mod's own animation blinking out.
            var mapFake = new CoreUiWidgetFake(
                new PositionFake(new Rectangle(-100f, 100f, 200f, 150f)), FULLY_DRAWN);

            buildSuppressorOf(mapFake).advance(ONE_FRAME);

            assertThat(mapFake.getOpacity())
                .isCloseTo(FULLY_DRAWN, within(TOLERANCE));
        }

        @Test
        void leavesAMapTheLayoutNeverPlacedAlone() {
            // Failing open. With no box there is nothing to say the widget is parked, and the state
            // the caller had before this ran is a map that draws.
            var mapFake = new CoreUiWidgetFake(null, FULLY_DRAWN);

            buildSuppressorOf(mapFake).advance(ONE_FRAME);

            assertThat(mapFake.getOpacity())
                .isCloseTo(FULLY_DRAWN, within(TOLERANCE));
        }

        @Test
        void handsBackTheOpacityTheMapWasFoundAtWhenItReturns() {
            // Not a flat one: the field belongs to whoever built the widget, and a mod fading its
            // own panel would find this script had brightened it.
            var mapFake = new CoreUiWidgetFake(new PositionFake(OFF_SCREEN_BOX), PARTLY_FADED);
            var suppressor = buildSuppressorOf(mapFake);

            suppressor.advance(ONE_FRAME);
            mapFake.moveWidgetTo(new PositionFake(ON_SCREEN_BOX));
            suppressor.advance(ONE_FRAME);

            assertThat(mapFake.getOpacity())
                .isCloseTo(PARTLY_FADED, within(TOLERANCE));
        }

        @Test
        void handsBackTheOpacityWhenTheCallerWithdrawsTheMap() {
            // How a caller switches the whole thing off - the setting behind it turned off, or the
            // screen changing so that suppressing anything would be wrong. The widget comes back on
            // the next frame rather than at its owner's next rebuild.
            var mapFake = new CoreUiWidgetFake(new PositionFake(OFF_SCREEN_BOX), PARTLY_FADED);
            var suppressibleMapFake = new SuppressibleMapFake(mapFake);
            var suppressor = new OffScreenMapSuppressor(suppressibleMapFake, SCREEN);

            suppressor.advance(ONE_FRAME);
            suppressibleMapFake.withdrawMap();
            suppressor.advance(ONE_FRAME);

            assertThat(mapFake.getOpacity())
                .isCloseTo(PARTLY_FADED, within(TOLERANCE));
        }

        @Test
        void holdsAParkedMapAtNothingWhileSomethingElseWritesToIt() {
            // Self-healing rather than a single write. Another mod is free to set the opacity of its
            // own widget at any point, and a suppression that fired once would leave a hidden map
            // rendering for the rest of the session.
            var mapFake = new CoreUiWidgetFake(new PositionFake(OFF_SCREEN_BOX), FULLY_DRAWN);
            var suppressor = buildSuppressorOf(mapFake);

            suppressor.advance(ONE_FRAME);
            mapFake.setOpacity(FULLY_DRAWN);
            suppressor.advance(ONE_FRAME);

            assertThat(mapFake.getOpacity())
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void handsBackWhatItSawFirstRatherThanWhatItWroteItself() {
            // The other half of holding a parked map down: the reading is taken once, so a second
            // one cannot pick up this script's own zero and hand that back as the widget's value.
            var mapFake = new CoreUiWidgetFake(new PositionFake(OFF_SCREEN_BOX), PARTLY_FADED);
            var suppressor = buildSuppressorOf(mapFake);

            suppressor.advance(ONE_FRAME);
            suppressor.advance(ONE_FRAME);
            mapFake.moveWidgetTo(new PositionFake(ON_SCREEN_BOX));
            suppressor.advance(ONE_FRAME);

            assertThat(mapFake.getOpacity())
                .isCloseTo(PARTLY_FADED, within(TOLERANCE));
        }

        @Test
        void carriesNoOpacityAcrossFromAWidgetItsOwnerReplaced() {
            // A rebuilt panel is a different widget, and the value held was the old one's. The new
            // one is read for itself, so a mod that rebuilds at a different opacity gets that one
            // back rather than its predecessor's.
            var replacedMapFake = new CoreUiWidgetFake(
                new PositionFake(OFF_SCREEN_BOX), PARTLY_FADED);
            var rebuiltMapFake = new CoreUiWidgetFake(
                new PositionFake(OFF_SCREEN_BOX), FULLY_DRAWN);

            var suppressibleMapFake = new SuppressibleMapFake(replacedMapFake);
            var suppressor = new OffScreenMapSuppressor(suppressibleMapFake, SCREEN);

            suppressor.advance(ONE_FRAME);
            suppressibleMapFake.replaceMapWith(rebuiltMapFake);
            suppressor.advance(ONE_FRAME);
            suppressibleMapFake.withdrawMap();
            suppressor.advance(ONE_FRAME);

            assertThat(rebuiltMapFake.getOpacity())
                .isCloseTo(FULLY_DRAWN, within(TOLERANCE));
        }

        @Test
        void keepsAFaultOutOfTheCampaignsFrame() {
            // It writes into a widget the game is about to render, from a port that reaches a live
            // widget tree. A fault in either costs the suppression and nothing else.
            var suppressor = new OffScreenMapSuppressor(
                () -> {
                    throw new IllegalStateException("the widget tree could not be read");
                },
                SCREEN);

            assertThatCode(() -> suppressor.advance(ONE_FRAME))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    class RunWhilePaused {

        @Test
        void runsWhilePaused() {
            // The states this exists for are mostly paused ones - a dialog, the menu, or an open
            // screen with a panel parked behind it - so a script standing down while paused would
            // suppress nothing on the frames that matter.
            assertThat(new OffScreenMapSuppressor(() -> null, SCREEN).runWhilePaused())
                .isTrue();
        }
    }

    // The ordinary arrangement: one widget the caller keeps offering, against a fixed screen.
    private static OffScreenMapSuppressor buildSuppressorOf(UIComponentAPI mapFake) {
        return new OffScreenMapSuppressor(() -> mapFake, SCREEN);
    }

    // A caller's answer about which map may be suppressed, which the cases above change between
    // advances - a mod rebuilding its panel, or the caller withdrawing its permission.
    private static final class SuppressibleMapFake implements Supplier<UIComponentAPI> {

        private UIComponentAPI mapFake;

        private SuppressibleMapFake(UIComponentAPI mapFake) {
            this.mapFake = mapFake;
        }

        @Override
        public UIComponentAPI get() {
            return mapFake;
        }

        private void replaceMapWith(UIComponentAPI rebuiltMapFake) {
            mapFake = rebuiltMapFake;
        }

        private void withdrawMap() {
            mapFake = null;
        }
    }
}
