package kmlib.starsector.ui.widgets.tabs;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins the resting {@link TabHoverSource}: no tab has moved onto the hovered shade, whichever index is asked
 * for. A strip paints whatever this hands back without checking it, so a source answering with any fraction
 * at all would show a hover on a panel nothing is pointing at.
 */
final class TabHoverSourceTest {

    private static final float TOLERANCE = 0.001f;

    // Indices past either end of a row, since a source is asked per tab and must not assume the row it is
    // being walked over.
    private static final int FIRST_INDEX = 0;
    private static final int LATER_INDEX = 4;
    private static final int NO_TAB_INDEX = -1;

    @Nested
    class CreateRestingHoverSource {

        @Test
        void createRestingHoverSourceHoversNoTabInTheRow() {

            var hovers = TabHoverSource.createRestingHoverSource();

            assertThat(hovers.resolveHoverFractionAt(FIRST_INDEX))
                .isCloseTo(0f, within(TOLERANCE));
            assertThat(hovers.resolveHoverFractionAt(LATER_INDEX))
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void createRestingHoverSourceAnswersForAnIndexOutsideTheRow() {

            var hovers = TabHoverSource.createRestingHoverSource();

            assertThat(hovers.resolveHoverFractionAt(NO_TAB_INDEX))
                .isCloseTo(0f, within(TOLERANCE));
        }
    }
}
