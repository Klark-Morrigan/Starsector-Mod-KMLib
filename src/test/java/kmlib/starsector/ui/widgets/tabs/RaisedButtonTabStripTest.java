package kmlib.starsector.ui.widgets.tabs;

import kmlib.math.geometry.Rectangle;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins {@link RaisedButtonTabStrip}: a button gives up room on its left alone, and reaches a hairline past
 * its tab on the two edges that meet a line the panel already draws. Those are what put the leading button's
 * border on the frame beside it, every button's border on the body beneath it, and one channel between each
 * pair - the row the engine's own buttons make. What is guarded underneath is that the reach stops there: a
 * button drawn into its neighbour would overlap the tab that answers a click landing on it, and the
 * degenerate tab is pinned beside them so a row squeezed too small collapses its buttons rather than
 * inverting them across each other.
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
    class ComputeRowFootprint {

        @Test
        void RaisedButtonTabStrip_computeRowFootprint_admitsTheHairlineTheRowReachesPastItsBand() {
            // What a caller clipping the row has to allow: the two borders the buttons lay on the panel's
            // own lines fall outside the band they were laid into, so a clip cut to the band alone would
            // drop them - and a dropped border reads as a button open on that side, not as an error.
            var footprint = RaisedButtonTabStrip.computeRowFootprint(new Rectangle(100f, 50f, 63f, 18f));

            assertThat(footprint)
                .isEqualTo(new Rectangle(99f, 49f, 64f, 19f));
        }
    }

    @Nested
    class ComputeButtonBox {

        @Test
        void RaisedButtonTabStrip_computeButtonBox_reachesTheLeadingButtonOverTheFrameBesideIt() {
            // The first button has no neighbour on its left to be parted from, and the line it does meet
            // there is the panel's own frame - so it gives up no channel and reaches a hairline past the
            // row, laying its border on that frame rather than alongside it.
            var button = RaisedButtonTabStrip.computeButtonBox(TAB_BOUNDS, LEADING_TAB);

            assertThat(button.x())
                .isCloseTo(99f, within(TOLERANCE));
            assertThat(button.width())
                .isCloseTo(64f, within(TOLERANCE));
        }

        @Test
        void RaisedButtonTabStrip_computeButtonBox_takesTheChannelFromTheLeftOfAFollowingButton() {
            // The channel between two buttons comes out of the right-hand one of the pair rather than being
            // added to the row, so a chrome swap moves no tab: the button narrows and its right edge stays
            // where the layout put the tab's. Only the leading button reaches leftward - a following one
            // meets its neighbour there, not a line.
            var button = RaisedButtonTabStrip.computeButtonBox(TAB_BOUNDS, FOLLOWING_TAB);

            assertThat(button.x())
                .isCloseTo(103f, within(TOLERANCE));
            assertThat(button.width())
                .isCloseTo(60f, within(TOLERANCE));
        }

        @Test
        void RaisedButtonTabStrip_computeButtonBox_reachesEveryButtonOverTheBodyBeneathIt() {
            // A vanilla button is sized to the row it sits in, so the button wears the whole band its host
            // tuned - and a hairline more, its bottom border landing on the body's own top border instead of
            // stacking a second rule on top of it.
            var button = RaisedButtonTabStrip.computeButtonBox(TAB_BOUNDS, FOLLOWING_TAB);

            assertThat(button.y())
                .isCloseTo(49f, within(TOLERANCE));
            assertThat(button.height())
                .isCloseTo(20f, within(TOLERANCE));
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

    @Nested
    class ComputeLabelBox {

        @Test
        void RaisedButtonTabStrip_computeLabelBox_raisesTheTextOffTheLineBoxsOwnMiddle() {
            // The face this chrome is lettered in leaves the room for descenders empty at the foot of every
            // glyph, so text centred by that box reads low by about the space it is not using. A whole
            // pixel, never a fraction of one - a face of hard-edged pixels drawn on a half-pixel row is
            // split across two rows of screen.
            var labelBox = RaisedButtonTabStrip.computeLabelBox(new Rectangle(100f, 49f, 60f, 20f));

            assertThat(labelBox.y())
                .isCloseTo(50f, within(TOLERANCE));
        }

        @Test
        void RaisedButtonTabStrip_computeLabelBox_leavesTheButtonsOwnSpanAlone() {
            // Only the text moves. The box keeps the button's width and height so the label still centres
            // across the button it belongs to rather than across a box of its own shape.
            var buttonBox = new Rectangle(100f, 49f, 60f, 20f);
            var labelBox = RaisedButtonTabStrip.computeLabelBox(buttonBox);

            assertThat(labelBox.x())
                .isCloseTo(100f, within(TOLERANCE));
            assertThat(labelBox.width())
                .isCloseTo(60f, within(TOLERANCE));
            assertThat(labelBox.height())
                .isCloseTo(20f, within(TOLERANCE));
        }
    }
}
