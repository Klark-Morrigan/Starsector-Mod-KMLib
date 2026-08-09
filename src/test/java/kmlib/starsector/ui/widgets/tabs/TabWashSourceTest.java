package kmlib.starsector.ui.widgets.tabs;

import kmlib.starsector.ui.widgets.tabs.style.TabHover;
import kmlib.starsector.ui.widgets.tabs.style.TabLook;
import kmlib.starsector.ui.widgets.tabs.style.TabPalette;
import kmlib.starsector.ui.widgets.tabs.style.TabWash;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins the click-pulsed {@link TabWashSource}: the binding where a panel's live pulse fractions meet the
 * palette's click depth. Both halves are what the strip finally paints, so a wrong one shows as a tab lifted
 * to the wrong depth, in the wrong colour, or at the wrong moment - and the per-index reading is what keeps
 * a row and its lifts from falling out of step.
 *
 * <p>The palette is literal rather than {@link TabPalette#createMapTabPalette}, which resolves through a
 * live engine palette a unit test has no sector to supply.
 */
final class TabWashSourceTest {

    private static final float TOLERANCE = 0.001f;

    // A target no look in the palette names, so a source reaching for a look instead of the lift shows as a
    // different colour rather than the same colour at a different depth.
    private static final Color CLICK_TARGET = new Color(80, 80, 80);
    private static final float CLICK_DEPTH = 0.4f;

    private static final Color STAND_IN_SHADE = Color.GRAY;
    private static final TabLook STAND_IN_LOOK = new TabLook(STAND_IN_SHADE, STAND_IN_SHADE);

    private static final TabPalette PALETTE = new TabPalette(
        STAND_IN_SHADE,
        STAND_IN_LOOK,
        STAND_IN_LOOK,
        new TabHover.MeetingShade(STAND_IN_LOOK),
        new TabWash(CLICK_TARGET, CLICK_DEPTH));

    // Indices past either end of a row, since a source is asked per tab and must not assume the row it is
    // being walked over.
    private static final int FIRST_INDEX = 0;
    private static final int LATER_INDEX = 4;
    private static final int NO_TAB_INDEX = -1;

    @Nested
    class CreateClickPulsedWashSource {

        @Test
        void createClickPulsedWashSourceLiftsNoTabWithNoPulseRunning() {

            var washes = TabWashSource.createClickPulsedWashSource(
                PALETTE,
                TabPulseSource.createRestingPulseSource());

            assertThat(washes.resolveWashAt(FIRST_INDEX).strength())
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void createClickPulsedWashSourceLiftsToTheFullClickDepthAtTheLiftsPeak() {

            var washes = TabWashSource.createClickPulsedWashSource(PALETTE, tabIndex -> 1f);

            assertThat(washes.resolveWashAt(FIRST_INDEX).strength())
                .isCloseTo(CLICK_DEPTH, within(TOLERANCE));
        }

        @Test
        void createClickPulsedWashSourceScalesTheClickDepthByTheLiftsProgress() {
            // The whole of what the binding does: the palette says how bright a click reads at its
            // brightest, the fraction says how far through the cycle the tab has got.
            var washes = TabWashSource.createClickPulsedWashSource(PALETTE, tabIndex -> 0.5f);

            assertThat(washes.resolveWashAt(FIRST_INDEX).strength())
                .isCloseTo(0.2f, within(TOLERANCE));
        }

        @Test
        void createClickPulsedWashSourceLiftsTowardTheClickTargetRatherThanAnotherRole() {
            // A palette holds a wash per momentary state; this channel carries the click's, so reaching the
            // hotkey role instead would decay a click through a colour nothing named for it.
            var washes = TabWashSource.createClickPulsedWashSource(PALETTE, tabIndex -> 1f);

            assertThat(washes.resolveWashAt(FIRST_INDEX).target())
                .isEqualTo(CLICK_TARGET);
        }

        @Test
        void createClickPulsedWashSourceAnswersEachTabFromItsOwnPulse() {
            // Asked per index rather than walked as a list, so a click on one tab lifts that tab alone.
            var washes = TabWashSource.createClickPulsedWashSource(
                PALETTE,
                tabIndex -> tabIndex == LATER_INDEX ? 1f : 0f);

            assertThat(washes.resolveWashAt(LATER_INDEX).strength())
                .isCloseTo(CLICK_DEPTH, within(TOLERANCE));
            assertThat(washes.resolveWashAt(FIRST_INDEX).strength())
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void createClickPulsedWashSourceAnswersForAnIndexOutsideTheRow() {

            var washes = TabWashSource.createClickPulsedWashSource(
                PALETTE,
                TabPulseSource.createRestingPulseSource());

            assertThat(washes.resolveWashAt(NO_TAB_INDEX).strength())
                .isCloseTo(0f, within(TOLERANCE));
        }
    }
}
