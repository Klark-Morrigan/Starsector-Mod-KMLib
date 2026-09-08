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
 *
 * <p>Its two refinements are pinned on what they leave alone as much as on what they change. Each exists so
 * one thing about a look can be restated without the caller rebuilding eight components by hand, and a
 * refinement that quietly dropped one of the other seven would be found only by looking at the screen.
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

    @Nested
    class WithHeaderBandHeight {

        @Test
        void withHeaderBandHeightStandsTheStyleInTheGivenBand() {

            assertThat(TabStyles.buildAtBandHeight(17f).withHeaderBandHeight(24f).headerBandHeight())
                .isCloseTo(24f, within(TOLERANCE));
        }

        @Test
        void withHeaderBandHeightCarriesEverythingElseOver() {
            // The whole point of a refinement: a caller changing the room a row stands in must not find its
            // chrome, box or lettering changed under it as well.
            var style = TabStyles.buildAtBandHeightInBox(17f, new TabBox(40f, 12f, 3f));
            var restood = style.withHeaderBandHeight(24f);

            assertThat(restood)
                .usingRecursiveComparison()
                .ignoringFields("headerBandHeight")
                .isEqualTo(style);
        }

        @Test
        void withHeaderBandHeightFloorsAtZeroWhenNegative() {
            // The same clamp the constructor applies, since the refinement builds one.
            assertThat(TabStyles.buildAtBandHeight(17f).withHeaderBandHeight(-8f).headerBandHeight())
                .isCloseTo(0f, within(TOLERANCE));
        }
    }

    @Nested
    class WithTabBox {

        @Test
        void withTabBoxStandsTheTabsInTheGivenBox() {

            var box = new TabBox(40f, 12f, 3f);

            assertThat(TabStyles.buildAtBandHeight(17f).withTabBox(box).tabBox())
                .isEqualTo(box);
        }

        @Test
        void withTabBoxCarriesEverythingElseOver() {
            // What a band button relies on: it is drawn in the row's own chrome and colours and differs
            // from the tabs in its width alone, so anything else moving here would part it from the row.
            var style = TabStyles.buildAtBandHeight(17f);
            var reboxed = style.withTabBox(new TabBox(40f, 12f, 3f));

            assertThat(reboxed)
                .usingRecursiveComparison()
                .ignoringFields("tabBox")
                .isEqualTo(style);
        }
    }
}
