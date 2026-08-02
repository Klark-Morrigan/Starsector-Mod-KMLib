package kmlib.starsector.ui.map;

import kmlib.math.geometry.Rectangle;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the rule that tells the map surface from the tab's chrome. The walk that gathers the
 * candidates is unpublished-API reflection and only observable in a running game, but the choice
 * made among them is arithmetic over boxes - and it is the choice, not the walk, that decides
 * whether an overlay stands aside over chrome or lights a cell underneath it.
 *
 * <p>The boxes are the ones a 1920x1200 session actually reported, so a rule that stops fitting the
 * game's own layout fails here rather than in play.
 */
class MapSurfaceBoundsTest {

    private static final Rectangle TAB_BOX = new Rectangle(10f, 51f, 1900f, 1132f);
    private static final Rectangle SURFACE_BOX = new Rectangle(10f, 79f, 1900f, 1085f);
    private static final Rectangle TAB_STRIP_BOX = new Rectangle(10f, 1168f, 1900f, 15f);
    private static final Rectangle BAR_CONTROL_BOX = new Rectangle(141f, 1165f, 130f, 18f);

    @Nested
    class SelectSurfaceBox {

        @Test
        void selectSurfaceBoxPicksTheSurfaceOverEveryChromePiece() {
            // The order is deliberately not surface-first: the rule has to hold on coverage, not on
            // whichever child the tab happens to list first.
            assertThat(MapSurfaceBounds.selectSurfaceBox(
                    TAB_BOX,
                    List.of(TAB_STRIP_BOX, SURFACE_BOX, BAR_CONTROL_BOX)))
                .isEqualTo(SURFACE_BOX);
        }

        @Test
        void selectSurfaceBoxFindsNothingWhenOnlyChromeIsPresent() {
            // The loud-absence case. A build that reshapes the tab past this rule reports no
            // surface, so the caller falls back rather than accepting a tab strip as the map.
            assertThat(MapSurfaceBounds.selectSurfaceBox(
                    TAB_BOX,
                    List.of(TAB_STRIP_BOX, BAR_CONTROL_BOX)))
                .isNull();
        }

        @Test
        void selectSurfaceBoxFindsNothingWhenTheTabHasNoChildren() {
            assertThat(MapSurfaceBounds.selectSurfaceBox(TAB_BOX, List.of()))
                .isNull();
        }

        @Test
        void selectSurfaceBoxFindsNothingWhenTheTabWasNeverPositioned() {
            assertThat(MapSurfaceBounds.selectSurfaceBox(null, List.of(SURFACE_BOX)))
                .isNull();
        }

        @Test
        void selectSurfaceBoxConfinesAChildThatOverflowsTheTab() {
            // The map's panned content is larger than the screen and hangs below the surface, so a
            // single-level scan does not reach it. Should a later build promote something that
            // overflows to a direct child, the accepted surface still stops at the tab's edge -
            // suppression can then be too weak, never wider than the tab itself.
            var overflowingChildBox = new Rectangle(-595f, -109f, 3017f, 1950f);

            assertThat(MapSurfaceBounds.selectSurfaceBox(TAB_BOX, List.of(overflowingChildBox)))
                .isEqualTo(TAB_BOX);
        }
    }
}
