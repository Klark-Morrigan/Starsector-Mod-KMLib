package kmlib.starsector.ui.widgets.tabs;

import kmlib.starsector.ui.controls.BodyInteractionSources;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins what a whole panel's reading offers the pass that paints it: the band button's lone fraction turned
 * into the channel pair a strip is drawn from, and the resting value every part falls back to.
 *
 * <p>The button is carried as one fraction because it is one cell that answers the pointer and holds no
 * lift, while everything drawn as a strip wants a pair - so the composition happens here rather than at
 * each consumer, and this is where "a one-cell control has one channel" is actually stated.
 */
final class TabPanelInteractionSourcesTest {

    private static final float TOLERANCE = 0.001f;

    // The button is a single cell, so which index it is asked about must not matter. Two are asked so a
    // source keyed by index rather than constant would show as a difference between them.
    private static final int FIRST_CELL = 0;
    private static final int ANOTHER_CELL = 3;

    private static final float PART_WAY_LIT = 0.4f;

    private static final TabPanelInteractionSources SOURCES = new TabPanelInteractionSources(
        TabInteractionSources.RESTING,
        PART_WAY_LIT,
        BodyInteractionSources.RESTING);

    @Nested
    class ResolveBandButtonSources {

        @Test
        void resolveBandButtonSourcesReportsTheButtonsHoverOnTheLookChannel() {

            assertThat(SOURCES.resolveBandButtonSources().hoverSource().resolveHoverFractionAt(FIRST_CELL))
                .isCloseTo(PART_WAY_LIT, within(TOLERANCE));
        }

        @Test
        void resolveBandButtonSourcesReportsThatHoverWhicheverCellIsAsked() {
            // One box, so there is no second cell to be asked about - and a source that answered only
            // index zero would leave a chrome numbering its own cells differently drawing a dark button.
            assertThat(SOURCES.resolveBandButtonSources()
                    .hoverSource()
                    .resolveHoverFractionAt(ANOTHER_CELL))
                .isCloseTo(PART_WAY_LIT, within(TOLERANCE));
        }

        @Test
        void resolveBandButtonSourcesLiftsTheButtonForNoPress() {
            // The button acts on the way down and has nothing left to report by the release, so it carries
            // no lift at all - a channel wired live here would show a press the button never holds.
            assertThat(SOURCES.resolveBandButtonSources().pulseSource().resolvePulseFractionAt(FIRST_CELL))
                .isCloseTo(0f, within(TOLERANCE));
        }
    }

    @Nested
    class Resting {

        @Test
        void restingLightsNoBandButton() {
            // What a panel drawn without an animator behind it paints: a button already part-way lit on the
            // first frame would be showing an interaction nobody made.
            assertThat(TabPanelInteractionSources.RESTING.bandButtonHover())
                .isCloseTo(0f, within(TOLERANCE));
        }
    }
}
