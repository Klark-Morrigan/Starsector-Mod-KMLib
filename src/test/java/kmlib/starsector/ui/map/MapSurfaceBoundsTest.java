package kmlib.starsector.ui.map;

import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;

import kmlib.math.geometry.Rectangle;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the two rules that tell the map surface from the tab's chrome: which children are candidates
 * at all, and which of the candidates wins. The walk that reaches them is unpublished-API reflection
 * and only observable in a running game, but both rules are plain reads over a box and an opacity -
 * and it is those, not the walk, that decide whether an overlay stands aside over chrome or lights a
 * cell underneath it.
 *
 * <p>The boxes are the ones a 1920x1200 session actually reported, so a rule that stops fitting the
 * game's own layout fails here rather than in play.
 */
class MapSurfaceBoundsTest {

    private static final Rectangle TAB_BOX = new Rectangle(10f, 51f, 1900f, 1132f);
    private static final Rectangle SURFACE_BOX = new Rectangle(10f, 79f, 1900f, 1085f);
    private static final Rectangle TAB_STRIP_BOX = new Rectangle(10f, 1168f, 1900f, 15f);
    private static final Rectangle BAR_CONTROL_BOX = new Rectangle(141f, 1165f, 130f, 18f);

    private static final float DRAWN_OPACITY = 1f;
    private static final float FADED_TO_NOTHING_OPACITY = 0f;

    // A component the layout placed and drew, so it is a candidate on both counts and any case
    // below turns only on the one thing it changes.
    private static UIComponentAPI createDrawnWidgetMock(Rectangle box) {
        return createWidgetMock(box, DRAWN_OPACITY);
    }

    private static UIComponentAPI createWidgetMock(Rectangle box, float opacity) {
        // Built before the stubbing below rather than inside it: creating a mock while another
        // mock's stubbing is still open is what Mockito reports as unfinished stubbing.
        var positionMock = box == null ? null : createPositionMock(box);

        var widgetMock = mock(UIComponentAPI.class);
        when(widgetMock.getOpacity())
            .thenReturn(opacity);
        when(widgetMock.getPosition())
            .thenReturn(positionMock);

        return widgetMock;
    }

    private static PositionAPI createPositionMock(Rectangle box) {
        var positionMock = mock(PositionAPI.class);

        when(positionMock.getX())
            .thenReturn(box.x());
        when(positionMock.getY())
            .thenReturn(box.y());
        when(positionMock.getWidth())
            .thenReturn(box.width());
        when(positionMock.getHeight())
            .thenReturn(box.height());

        return positionMock;
    }

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

    @Nested
    class CollectDrawnBoxesOf {

        @Test
        void collectDrawnBoxesOfReturnsEveryDrawnChildBoxInTheOrderGiven() {
            var surfaceWidgetMock = createDrawnWidgetMock(SURFACE_BOX);
            var tabStripWidgetMock = createDrawnWidgetMock(TAB_STRIP_BOX);

            assertThat(MapSurfaceBounds.collectDrawnBoxesOf(
                    List.of(surfaceWidgetMock, tabStripWidgetMock)))
                .containsExactly(SURFACE_BOX, TAB_STRIP_BOX);
        }

        @Test
        void collectDrawnBoxesOfLeavesOutAChildFadedToNothing() {
            // A tab the player has switched away from keeps its box and its place in the tree while
            // it fades out. Were it still a candidate, it could out-cover the real surface and hand
            // the rule a box for a screen nobody is looking at.
            var fadedWidgetMock = createWidgetMock(SURFACE_BOX, FADED_TO_NOTHING_OPACITY);
            var tabStripWidgetMock = createDrawnWidgetMock(TAB_STRIP_BOX);

            assertThat(MapSurfaceBounds.collectDrawnBoxesOf(
                    List.of(fadedWidgetMock, tabStripWidgetMock)))
                .containsExactly(TAB_STRIP_BOX);
        }

        @Test
        void collectDrawnBoxesOfLeavesOutAChildTheLayoutNeverPositioned() {
            // No position at all, so it occupies nothing and cannot be measured against the tab.
            var unpositionedWidgetMock = createWidgetMock(null, DRAWN_OPACITY);
            var surfaceWidgetMock = createDrawnWidgetMock(SURFACE_BOX);

            assertThat(MapSurfaceBounds.collectDrawnBoxesOf(
                    List.of(unpositionedWidgetMock, surfaceWidgetMock)))
                .containsExactly(SURFACE_BOX);
        }

        @Test
        void collectDrawnBoxesOfLeavesOutAChildThatIsNotAComponent() {
            // The children come back off an unpublished accessor as a bare list, so nothing
            // guarantees every entry is a component - and one that is not has no box to compare.
            var surfaceWidgetMock = createDrawnWidgetMock(SURFACE_BOX);

            assertThat(MapSurfaceBounds.collectDrawnBoxesOf(
                    List.of(new Object(), surfaceWidgetMock)))
                .containsExactly(SURFACE_BOX);
        }

        @Test
        void collectDrawnBoxesOfReturnsNothingForATabWithNoChildren() {
            assertThat(MapSurfaceBounds.collectDrawnBoxesOf(List.of()))
                .isEmpty();
        }
    }
}
