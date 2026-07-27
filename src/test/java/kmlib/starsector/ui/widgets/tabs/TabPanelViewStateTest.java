package kmlib.starsector.ui.widgets.tabs;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins {@link TabPanelViewState}: the fold arrives bounded so an overshooting animation cannot invert the
 * geometry downstream, while the scroll offset stays raw because how far it may travel is not known until
 * the strip has been measured.
 */
final class TabPanelViewStateTest {
    private static final float TOLERANCE = 0.01f;

    @Nested
    class CollapseFraction {

        @Test
        void collapseFractionKeepsAFractionInsideTheUnitRange() {
            assertThat(new TabPanelViewState(0f, 0.4f).collapseFraction())
                    .isCloseTo(0.4f, within(TOLERANCE));
        }

        @Test
        void collapseFractionClampsAnOvershootToFullyDocked() {
            assertThat(new TabPanelViewState(0f, 2f).collapseFraction()).isCloseTo(1f, within(TOLERANCE));
        }

        @Test
        void collapseFractionClampsAnUndershootToFullyExpanded() {
            assertThat(new TabPanelViewState(0f, -1f).collapseFraction()).isCloseTo(0f, within(TOLERANCE));
        }
    }

    @Nested
    class RawScrollOffset {

        @Test
        void rawScrollOffsetPassesThroughUnclamped() {
            // An offset past the list's end is not wrong yet - the overflow it must fit is only known once
            // the strip is laid out - so it survives to be settled there rather than being cut off here.
            assertThat(new TabPanelViewState(9000f, 0f).rawScrollOffset())
                    .isCloseTo(9000f, within(TOLERANCE));
        }
    }

    @Nested
    class Resting {

        @Test
        void restingIsScrolledToTheTopAndFullyExpanded() {
            assertThat(TabPanelViewState.RESTING.rawScrollOffset()).isCloseTo(0f, within(TOLERANCE));
            assertThat(TabPanelViewState.RESTING.collapseFraction()).isCloseTo(0f, within(TOLERANCE));
        }
    }
}
