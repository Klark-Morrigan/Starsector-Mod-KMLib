package kmlib.starsector.ui.widgets;

import kmlib.math.geometry.Rectangle;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link PanelPlacement#scrollRegion}: the placement hands its scrolling control to a scrollbar as a
 * {@link ScrollRegion} - the body as the container, the flex viewport, and the scroll offset/overflow -
 * so the panel carries no scrollbar geometry of its own.
 */
final class PanelPlacementTest {

    @Nested
    class ScrollRegion {

        @Test
        void scrollRegionMapsTheBodyViewportOffsetAndOverflow() {
            var body = new Rectangle(10f, 20f, 200f, 300f);
            var viewport = new Rectangle(18f, 30f, 120f, 100f);
            var panel = new TabPanelPlacement(body, List.of(), body);
            var placement = new PanelPlacement(panel, List.of(), viewport, 15f, 60f);

            var region = placement.toScrollRegion();

            // The body frames the track gutter, the flex viewport is the scroll viewport, and the offset
            // and overflow ride through unchanged.
            assertThat(region.container()).isEqualTo(body);
            assertThat(region.viewport()).isEqualTo(viewport);
            assertThat(region.offset()).isEqualTo(15f);
            assertThat(region.overflow()).isEqualTo(60f);
        }
    }
}
