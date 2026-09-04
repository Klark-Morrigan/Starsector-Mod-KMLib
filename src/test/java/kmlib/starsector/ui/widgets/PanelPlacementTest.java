package kmlib.starsector.ui.widgets;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.widgets.scroll.ScrollbarThickness;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link PanelPlacement#toScrollRegion}: the placement hands its scrolling control to a scrollbar as
 * a {@link kmlib.starsector.ui.widgets.scroll.ScrollRegion} - the body as the container, the flex viewport,
 * and the scroll offset/overflow -
 * so the panel carries no scrollbar geometry of its own. The bar's thickness rides on the placement beside
 * that geometry but stays out of the region, a region describing what is scrolled and not how its bar is
 * drawn.
 */
final class PanelPlacementTest {

    private static final Rectangle BOX = new Rectangle(8f, 18f, 204f, 304f);
    private static final Rectangle BODY = new Rectangle(10f, 20f, 200f, 300f);
    private static final Rectangle VIEWPORT = new Rectangle(18f, 30f, 120f, 100f);

    private static PanelPlacement buildPlacement(ScrollbarThickness thickness) {
        return new PanelPlacement(BOX, BODY, List.of(), VIEWPORT, 15f, 60f, thickness);
    }

    @Nested
    class ToScrollRegion {

        @Test
        void toScrollRegionMapsTheBodyViewportOffsetAndOverflow() {

            var region = buildPlacement(ScrollbarThickness.DEFAULT).toScrollRegion();

            // The body frames the track gutter, the flex viewport is the scroll viewport, and the offset
            // and overflow ride through unchanged.
            assertThat(region.container()).isEqualTo(BODY);
            assertThat(region.viewport()).isEqualTo(VIEWPORT);
            assertThat(region.offset()).isEqualTo(15f);
            assertThat(region.overflow()).isEqualTo(60f);
        }

        @Test
        void toScrollRegionIsTheSameRegionWhateverTheScrollbarThickness() {

            var thin = buildPlacement(ScrollbarThickness.DEFAULT).toScrollRegion();
            var thick = buildPlacement(new ScrollbarThickness(12f)).toScrollRegion();

            // The thickness rides on the placement for the passes that draw and grab the bar; the region
            // says what is scrolled, so widening the bar leaves it untouched.
            assertThat(thick).isEqualTo(thin);
        }
    }
}
