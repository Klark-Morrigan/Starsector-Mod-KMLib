package kmlib.starsector.ui.widgets.tabs;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins the resting {@link TabWashSource}: no tab is lifted, whichever index is asked for. The strip
 * paints whatever this hands back without checking it, so a source answering with any depth at all would
 * brighten a tab nothing is happening to.
 */
final class TabWashSourceTest {

    private static final float TOLERANCE = 0.001f;

    // Indices past either end of a row, since a source is asked per tab and must not assume the row it
    // is being walked over.
    private static final int FIRST_INDEX = 0;
    private static final int LATER_INDEX = 4;
    private static final int NO_TAB_INDEX = -1;

    @Nested
    class CreateRestingWashSource {

        @Test
        void createRestingWashSourceLiftsNoTabInTheRow() {
            
            var washes = TabWashSource.createRestingWashSource();

            assertThat(washes.resolveWashAt(FIRST_INDEX).strength())
                .isCloseTo(0f, within(TOLERANCE));
            assertThat(washes.resolveWashAt(LATER_INDEX).strength())
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void createRestingWashSourceAnswersForAnIndexOutsideTheRow() {

            var washes = TabWashSource.createRestingWashSource();

            assertThat(washes.resolveWashAt(NO_TAB_INDEX).strength())
                .isCloseTo(0f, within(TOLERANCE));
        }
    }
}
