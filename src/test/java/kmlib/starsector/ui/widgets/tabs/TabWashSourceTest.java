package kmlib.starsector.ui.widgets.tabs;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins the hover-only {@link TabWashSource}: exactly the tab under the pointer is lifted, and a pointer
 * over no tab lifts none. The strip paints whatever this hands back without checking it, so a source that
 * answered for the wrong index would light a tab the pointer is not on.
 */
final class TabWashSourceTest {

    private static final float TOLERANCE = 0.001f;
    private static final int HOVERED_INDEX = 1;
    private static final TabWash HOVER_WASH = new TabWash(new Color(60, 60, 60), 0.4f);

    // The index a strip reports when the pointer is over the row but between no two tabs of it - the
    // same "outside the row" answer a strip drawn under an idle pointer carries every frame.
    private static final int NO_TAB_INDEX = -1;

    @Nested
    class CreateHoverWashSource {

        @Test
        void createHoverWashSourceLiftsTheHoveredTab() {

            var washes = TabWashSource.createHoverWashSource(HOVERED_INDEX, HOVER_WASH);

            assertThat(washes.resolveWashAt(HOVERED_INDEX).strength())
                .isCloseTo(0.4f, within(TOLERANCE));
        }

        @Test
        void createHoverWashSourceLiftsTheHoveredTabTowardTheWashTarget() {

            var washes = TabWashSource.createHoverWashSource(HOVERED_INDEX, HOVER_WASH);

            assertThat(washes.resolveWashAt(HOVERED_INDEX).target())
                .isEqualTo(new Color(60, 60, 60));
        }

        @Test
        void createHoverWashSourceLeavesEveryOtherTabAtRest() {

            var washes = TabWashSource.createHoverWashSource(HOVERED_INDEX, HOVER_WASH);

            assertThat(washes.resolveWashAt(0).strength())
                .isCloseTo(0f, within(TOLERANCE));
            assertThat(washes.resolveWashAt(2).strength())
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void createHoverWashSourceLiftsNoTabWhenThePointerIsOverNone() {

            var washes = TabWashSource.createHoverWashSource(NO_TAB_INDEX, HOVER_WASH);

            assertThat(washes.resolveWashAt(0).strength())
                .isCloseTo(0f, within(TOLERANCE));
        }
    }
}
