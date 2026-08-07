package kmlib.starsector.ui.widgets.tabs.style;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins {@link TabStyle}: the dimensions carry through as given, and a height that would invert the band
 * floors instead, so a caller computing a style from live settings cannot hand the layout a shape it would
 * lay out upside down. The chrome and the paint the style also carries are not clamped and have no rule to
 * pin; they are supplied here through {@link TabStyles} so the height under test is the only value in play.
 */
final class TabStyleTest {
    private static final float TOLERANCE = 0.01f;

    @Nested
    class HeaderBandHeight {

        @Test
        void headerBandHeightCarriesThePositiveValueThrough() {
            assertThat(TabStyles.buildAtBandHeight(17f).headerBandHeight())
                .isCloseTo(17f, within(TOLERANCE));
        }

        @Test
        void headerBandHeightFloorsAtZeroWhenNegative() {
            // A negative band would hang a tab row above the top edge it descends from, so it collapses to
            // nothing instead - a bandless panel, not an inverted one.
            assertThat(TabStyles.buildAtBandHeight(-8f).headerBandHeight())
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void headerBandHeightKeepsZeroAsABandlessStyle() {
            // Zero is a legitimate ask (a panel wanting no tab row at all), so it passes through rather than
            // being nudged up to some minimum.
            assertThat(TabStyles.buildAtBandHeight(0f).headerBandHeight())
                .isCloseTo(0f, within(TOLERANCE));
        }
    }
}
