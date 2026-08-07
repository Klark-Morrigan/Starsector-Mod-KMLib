package kmlib.starsector.ui.widgets.tabs;

import kmlib.math.geometry.Rectangle;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins {@link RaisedButtonTabStrip}: a button stands inside the tab the row was laid out with, never
 * outside it. That containment is the whole guard - a button drawn past its tab overlaps the neighbour
 * that would answer a click landing on it, which is the drift the chrome's split from the layout exists to
 * prevent - and the degenerate tab is pinned beside it so a row squeezed too small collapses its buttons
 * rather than inverting them across each other.
 */
final class RaisedButtonTabStripTest {

    // A laid tab's box: standing numbers with no round relationships between them, so an assertion cannot
    // pass on a coincidence between two of the insets.
    private static final Rectangle TAB_BOUNDS = new Rectangle(100f, 50f, 63f, 19f);
    private static final float TOLERANCE = 0.01f;

    @Nested
    class ComputeButtonBox {

        @Test
        void RaisedButtonTabStrip_computeButtonBox_leavesAGutterToEitherSideOfTheTab() {
            // The gap between two buttons comes out of the tabs rather than being added to the row, so a
            // chrome swap moves no tab: the button narrows and its box stays where the layout put it.
            var button = RaisedButtonTabStrip.computeButtonBox(TAB_BOUNDS);

            assertThat(button.x())
                .isCloseTo(102f, within(TOLERANCE));
            assertThat(button.width())
                .isCloseTo(59f, within(TOLERANCE));
        }

        @Test
        void RaisedButtonTabStrip_computeButtonBox_clearsTheBandAboveAndBelowTheButton() {
            // A button reads as standing in the header rather than as filling it, so it gives up a little
            // height at each end while staying centred on the tab's own middle line.
            var button = RaisedButtonTabStrip.computeButtonBox(TAB_BOUNDS);

            assertThat(button.y())
                .isCloseTo(51f, within(TOLERANCE));
            assertThat(button.height())
                .isCloseTo(17f, within(TOLERANCE));
        }

        @Test
        void RaisedButtonTabStrip_computeButtonBox_collapsesToTheTabCentreWhenNarrowerThanItsGutters() {
            // A negative width would draw the button back-to-front across its neighbours, so a tab with no
            // room for its own gutters yields nothing to paint, parked on the tab's centre.
            var button = RaisedButtonTabStrip.computeButtonBox(
                new Rectangle(100f, 50f, 3f, 19f));

            assertThat(button.width())
                .isCloseTo(0f, within(TOLERANCE));
            assertThat(button.x())
                .isCloseTo(101.5f, within(TOLERANCE));
        }

        @Test
        void RaisedButtonTabStrip_computeButtonBox_collapsesToTheTabCentreWhenShorterThanItsClearance() {
            // The same floor on the other axis: a band too short to clear its own button collapses it
            // rather than standing it upside down through the row above.
            var button = RaisedButtonTabStrip.computeButtonBox(
                new Rectangle(100f, 50f, 63f, 1f));

            assertThat(button.height())
                .isCloseTo(0f, within(TOLERANCE));
            assertThat(button.y())
                .isCloseTo(50.5f, within(TOLERANCE));
        }
    }
}
