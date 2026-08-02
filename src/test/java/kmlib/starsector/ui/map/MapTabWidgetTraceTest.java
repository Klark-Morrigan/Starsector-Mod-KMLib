package kmlib.starsector.ui.map;

import kmlib.math.geometry.Rectangle;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the one rule the trace can be held to off the engine: which components count as being under
 * the cursor. The walk that finds them is unpublished-API reflection and only observable in a
 * running game, but what it does with each component it reaches is plain arithmetic over a box and
 * an opacity, and that is where a wrong answer would send a suppression built on it wrong.
 */
class MapTabWidgetTraceTest {

    private static final Rectangle BOX = new Rectangle(100f, 50f, 200f, 100f);

    // Comfortably inside the box, so no case here turns on edge behaviour - that is Rectangle's.
    private static final float CURSOR_X_INSIDE = 200f;
    private static final float CURSOR_Y_INSIDE = 100f;

    private static final float CURSOR_X_OUTSIDE = 10f;
    private static final float CURSOR_Y_OUTSIDE = 10f;

    private static final float DRAWN_OPACITY = 1f;

    @Nested
    class IsWidgetUnderCursor {

        @Test
        void isWidgetUnderCursorIsTrueForADrawnWidgetContainingTheCursor() {
            assertThat(MapTabWidgetTrace.isWidgetUnderCursor(
                    BOX,
                    DRAWN_OPACITY,
                    CURSOR_X_INSIDE,
                    CURSOR_Y_INSIDE))
                .isTrue();
        }

        @Test
        void isWidgetUnderCursorIsFalseWhenTheCursorIsOutsideTheBox() {
            assertThat(MapTabWidgetTrace.isWidgetUnderCursor(
                    BOX,
                    DRAWN_OPACITY,
                    CURSOR_X_OUTSIDE,
                    CURSOR_Y_OUTSIDE))
                .isFalse();
        }

        @Test
        void isWidgetUnderCursorIsFalseForAWidgetFadedToNothing() {
            // A tab the player has switched away from keeps its box and its place in the tree while
            // it fades out, so containment alone would report chrome nobody can see - and an
            // overlay built on that would stand aside for it.
            assertThat(MapTabWidgetTrace.isWidgetUnderCursor(
                    BOX,
                    0f,
                    CURSOR_X_INSIDE,
                    CURSOR_Y_INSIDE))
                .isFalse();
        }

        @Test
        void isWidgetUnderCursorIsFalseForAWidgetWithNoBox() {
            // A component the layout never positioned answers no position at all; it occupies
            // nothing, so the cursor cannot be over it.
            assertThat(MapTabWidgetTrace.isWidgetUnderCursor(
                    null,
                    DRAWN_OPACITY,
                    CURSOR_X_INSIDE,
                    CURSOR_Y_INSIDE))
                .isFalse();
        }
    }
}
