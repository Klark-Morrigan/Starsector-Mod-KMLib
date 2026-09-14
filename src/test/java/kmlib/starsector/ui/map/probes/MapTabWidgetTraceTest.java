package kmlib.starsector.ui.map.probes;

import kmlib.math.geometry.Rectangle;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the two rules the trace can be held to off the engine: which components count as being under
 * the cursor, and what each of them contributes to the two halves of the line. The walk that finds
 * them is unpublished-API reflection and only observable in a running game, but what it does with
 * each component it reaches is plain arithmetic over a box and an opacity, and that is where a wrong
 * answer would send a suppression built on it wrong.
 *
 * <p>The second rule is the one that decides whether the trace is readable at all. A widget's box
 * moves whenever anything scrolls beneath a still cursor, so a key carrying boxes reports every
 * frame and the crossings the trace exists for are lost among the copies.
 */
class MapTabWidgetTraceTest {

    private static final Rectangle BOX = new Rectangle(100f, 50f, 200f, 100f);
    private static final Rectangle MOVED_BOX = new Rectangle(100f, 62f, 200f, 100f);

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

    @Nested
    class DescribeIdentity {

        @Test
        void describeIdentityHoldsStillWhileOnlyTheBoxMoves() {
            // The rule the whole split rests on. A list scrolling under a still cursor moves the
            // boxes without changing which widgets contain it, and that is not news.
            assertThat(widgetAt(BOX, DRAWN_OPACITY).describeIdentity())
                .isEqualTo(widgetAt(MOVED_BOX, DRAWN_OPACITY).describeIdentity());
        }

        @Test
        void describeIdentityNamesTheWidgetItsDepthAndItsParent() {
            // What the trace is actually about: which widget, and where in the tree. Two widgets can
            // both contain the cursor by enclosing one another or by merely overlapping, and only
            // the parentage tells those apart.
            assertThat(widgetAt(BOX, DRAWN_OPACITY).describeIdentity())
                .isEqualTo("d2 com.fs.Panel[parent=com.fs.Tab]");
        }

        @Test
        void describeIdentityChangesOnceTheWidgetSitsSomewhereElseInTheTree() {

            var reparented = new MapTabWidgetTrace.UnderCursorWidget(
                2,
                "com.fs.Panel",
                "com.fs.Dialog",
                BOX,
                DRAWN_OPACITY);

            assertThat(widgetAt(BOX, DRAWN_OPACITY).describeIdentity())
                .isNotEqualTo(reparented.describeIdentity());
        }
    }

    @Nested
    class DescribeFully {

        @Test
        void describeFullyKeepsTheBoxAndOpacityTheIdentityLeavesOut() {
            // Keying a line does not trim it. The detail the key omits still has to reach the log,
            // or the split would cost the reading it was meant to make legible.
            assertThat(widgetAt(BOX, DRAWN_OPACITY).describeFully())
                .isEqualTo("d2 com.fs.Panel[x=100 y=50 w=200 h=100 "
                    + "opacity=1.0 parent=com.fs.Tab]");
        }

        @Test
        void describeFullyReflectsABoxTheIdentityIgnores() {

            assertThat(widgetAt(BOX, DRAWN_OPACITY).describeFully())
                .isNotEqualTo(widgetAt(MOVED_BOX, DRAWN_OPACITY).describeFully());
        }
    }

    private static MapTabWidgetTrace.UnderCursorWidget widgetAt(Rectangle box, float opacity) {

        return new MapTabWidgetTrace.UnderCursorWidget(
            2,
            "com.fs.Panel",
            "com.fs.Tab",
            box,
            opacity);
    }
}
