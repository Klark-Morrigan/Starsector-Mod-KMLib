package kmlib.starsector.ui.map;

import kmlib.math.geometry.Rectangle;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the rule an overlay actually suppresses on: whether a point is on the visible map. The boxes
 * are the intel screen's map visor as a 1920x1200 session reported it, because that host is the one
 * whose chrome overlaps its surface - the {@code M} screen's does not, so only this one exercises
 * both halves of the answer.
 */
class MapSurfaceAreaTest {

    private static final Rectangle SURFACE_BOX = new Rectangle(525f, 293f, 890f, 784f);
    private static final Rectangle BAR_BOX = new Rectangle(524f, 1059f, 890f, 19f);

    private static final MapSurfaceArea VISOR_AREA =
        new MapSurfaceArea(SURFACE_BOX, List.of(BAR_BOX));

    @Nested
    class ContainsPoint {

        @Test
        void containsPointIsTrueInsideTheSurfaceAndClearOfTheChrome() {
            assertThat(VISOR_AREA.containsPoint(900f, 600f))
                .isTrue();
        }

        @Test
        void containsPointIsFalseOutsideTheSurface() {
            // The intel list beside the visor. Nothing about it is chrome; it is simply not the map.
            assertThat(VISOR_AREA.containsPoint(200f, 600f))
                .isFalse();
        }

        @Test
        void containsPointIsFalseOverChromeDrawnAcrossTheSurface() {
            // The visor's own control bar, inside the surface box and drawn over it. This is the
            // case a surface-only rule got wrong: the cursor is inside the map's box while the
            // player is looking at a control.
            assertThat(VISOR_AREA.containsPoint(900f, 1070f))
                .isFalse();
        }

        @Test
        void containsPointIsTrueWhenTheTabDrawsNoChromeAtAll() {
            var bareArea = new MapSurfaceArea(SURFACE_BOX, List.of());

            assertThat(bareArea.containsPoint(900f, 1070f))
                .isTrue();
        }
    }

    @Nested
    class Constructor {

        @Test
        void constructorCopiesTheChromeBoxesItWasHandedAtConstruction() {
            // The live read builds the list while walking the tree and a caller holds the result
            // across frames, so a shared list would let a later measure edit an answer already
            // given out.
            var mutableChromeBoxes = new ArrayList<Rectangle>();
            mutableChromeBoxes.add(BAR_BOX);

            var surfaceArea = new MapSurfaceArea(SURFACE_BOX, mutableChromeBoxes);
            mutableChromeBoxes.clear();

            assertThat(surfaceArea.chromeBoxes())
                .containsExactly(BAR_BOX);
        }
    }
}
