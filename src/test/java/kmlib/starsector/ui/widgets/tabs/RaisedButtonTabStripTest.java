package kmlib.starsector.ui.widgets.tabs;

import kmlib.math.geometry.Rectangle;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins {@link RaisedButtonTabStrip}: a button stands inside the tab the row was laid out with, never
 * outside it, and gives up room on its left alone. That is what puts the leading button flush with the row's
 * left edge and one channel between every pair, matching the engine's own button row; containment is the
 * guard beneath it, a button drawn past its tab overlapping the neighbour that would answer a click landing
 * on it. The degenerate tab is pinned beside them so a row squeezed too small collapses its buttons rather
 * than inverting them across each other.
 */
final class RaisedButtonTabStripTest {

    // A laid tab's box: standing numbers with no round relationships between them, so an assertion cannot
    // pass on a coincidence between the channel and either extent.
    private static final Rectangle TAB_BOUNDS = new Rectangle(100f, 50f, 63f, 19f);

    // The row's leading tab and one that follows it, named so a case reads as which of the two rules it is
    // about rather than as a bare number.
    private static final int LEADING_TAB = 0;
    private static final int FOLLOWING_TAB = 1;

    private static final float TOLERANCE = 0.01f;

    @Nested
    class ComputeButtonBox {

        @Test
        void RaisedButtonTabStrip_computeButtonBox_standsTheLeadingButtonFlushWithItsTab() {
            // The first button has no neighbour on its left to be parted from, so it gives up nothing and
            // starts where the row starts - the vanilla row's leftmost button sits flush against its edge.
            var button = RaisedButtonTabStrip.computeButtonBox(TAB_BOUNDS, LEADING_TAB);

            assertThat(button.x())
                .isCloseTo(100f, within(TOLERANCE));
            assertThat(button.width())
                .isCloseTo(63f, within(TOLERANCE));
        }

        @Test
        void RaisedButtonTabStrip_computeButtonBox_takesTheChannelFromTheLeftOfAFollowingButton() {
            // The channel between two buttons comes out of the right-hand one of the pair rather than being
            // added to the row, so a chrome swap moves no tab: the button narrows and its right edge stays
            // where the layout put the tab's.
            var button = RaisedButtonTabStrip.computeButtonBox(TAB_BOUNDS, FOLLOWING_TAB);

            assertThat(button.x())
                .isCloseTo(103f, within(TOLERANCE));
            assertThat(button.width())
                .isCloseTo(60f, within(TOLERANCE));
        }

        @Test
        void RaisedButtonTabStrip_computeButtonBox_fillsTheBandTopToBottom() {
            // A vanilla button is sized to the row it sits in, so the button wears the whole band its host
            // tuned rather than floating clear of the body beneath it.
            var button = RaisedButtonTabStrip.computeButtonBox(TAB_BOUNDS, FOLLOWING_TAB);

            assertThat(button.y())
                .isCloseTo(50f, within(TOLERANCE));
            assertThat(button.height())
                .isCloseTo(19f, within(TOLERANCE));
        }

        @Test
        void RaisedButtonTabStrip_computeButtonBox_collapsesAtItsRightEdgeWhenNarrowerThanItsChannel() {
            // A negative width would draw the button back-to-front across the neighbour the channel parts it
            // from, so a tab with no room for its own channel yields nothing to paint, parked on the far
            // edge of the tab it was laid inside.
            var button = RaisedButtonTabStrip.computeButtonBox(
                new Rectangle(100f, 50f, 2f, 19f),
                FOLLOWING_TAB);

            assertThat(button.width())
                .isCloseTo(0f, within(TOLERANCE));
            assertThat(button.x())
                .isCloseTo(102f, within(TOLERANCE));
        }
    }
}
