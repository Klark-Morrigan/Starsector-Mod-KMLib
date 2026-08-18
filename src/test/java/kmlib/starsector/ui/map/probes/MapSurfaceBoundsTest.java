package kmlib.starsector.ui.map.probes;

import kmlib.math.geometry.Rectangle;
import kmlib.testfixtures.starsector.ui.coreui.CoreUiWidgetFake;
import kmlib.testfixtures.starsector.ui.layout.PositionFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the two rules that tell the map surface from the tab's chrome: which children are candidates
 * at all, and how the candidates split into the surface and the chrome drawn with it. The walk that
 * reaches them is unpublished-API reflection and only observable in a running game, but both rules
 * are plain reads over a box and an opacity - and it is those, not the walk, that decide whether an
 * overlay stands aside over chrome or lights a cell underneath it.
 *
 * <p>The boxes are the ones a 1920x1200 session actually reported, on both hosts the rule has to
 * fit: the {@code M} screen's map tab, whose surface is inset and whose chrome sits beside it, and
 * the intel screen's map visor, whose surface fills the tab and whose control bar is drawn over it.
 * A rule that stops fitting either layout fails here rather than in play.
 */
class MapSurfaceBoundsTest {

    private static final Rectangle TAB_BOX = new Rectangle(10f, 51f, 1900f, 1132f);
    private static final Rectangle SURFACE_BOX = new Rectangle(10f, 79f, 1900f, 1085f);
    private static final Rectangle TAB_STRIP_BOX = new Rectangle(10f, 1168f, 1900f, 15f);
    private static final Rectangle BAR_CONTROL_BOX = new Rectangle(141f, 1165f, 130f, 18f);

    // The intel screen's map visor, the control bar it draws across its own bottom edge, and one of
    // the buttons that bar holds. One box serves as both the visor and its surface because that is
    // the finding: the visor's surface fills it exactly, which is what makes the bar impossible to
    // exclude by complement and is why the chrome half of the answer exists.
    private static final Rectangle VISOR_BOX = new Rectangle(525f, 293f, 890f, 784f);
    private static final Rectangle VISOR_BAR_BOX = new Rectangle(524f, 1059f, 890f, 19f);
    private static final Rectangle VISOR_BAR_BUTTON_BOX = new Rectangle(652f, 1059f, 125f, 19f);
    private static final Rectangle SECOND_VISOR_BAR_BUTTON_BOX =
        new Rectangle(783f, 1059f, 125f, 19f);

    private static final float DRAWN_OPACITY = 1f;
    private static final float FADED_TO_NOTHING_OPACITY = 0f;

    // A child that draws itself and holds nothing, which is every chrome piece on the M map's tab
    // and the state the expansion rule has to leave alone.
    private static DrawnChildBoxes createLeafChild(Rectangle box) {
        return new DrawnChildBoxes(box, List.of());
    }

    // A component the layout placed and drew, so it is a candidate on both counts and any case below
    // turns only on the one thing it changes.
    private static CoreUiWidgetFake createDrawnWidgetFake(Rectangle box, Object... children) {
        return createWidgetFake(box, DRAWN_OPACITY, children);
    }

    private static CoreUiWidgetFake createWidgetFake(
            Rectangle box,
            float opacity,
            Object... children) {

        return new CoreUiWidgetFake(
            box == null ? null : new PositionFake(box),
            opacity,
            children);
    }

    @Nested
    class SelectSurfaceArea {

        @Test
        void selectSurfaceAreaPicksTheSurfaceOverEveryChromePiece() {
            // The order is deliberately not surface-first: the rule has to hold on coverage, not on
            // whichever child the tab happens to list first.
            assertThat(MapSurfaceBounds.selectSurfaceArea(
                    TAB_BOX,
                    List.of(
                        createLeafChild(TAB_STRIP_BOX),
                        createLeafChild(SURFACE_BOX),
                        createLeafChild(BAR_CONTROL_BOX))))
                .isEqualTo(new MapSurfaceArea(
                    SURFACE_BOX,
                    List.of(TAB_STRIP_BOX, BAR_CONTROL_BOX)));
        }

        @Test
        void selectSurfaceAreaKeepsAChromePieceDrawnOverTheSurface() {
            // The intel screen's visor. Its surface is the whole tab, so the bar drawn across the
            // bottom of it is inside the surface rather than beside it - the case that a rule
            // returning the surface alone could not express, and the reason the chrome comes back.
            assertThat(MapSurfaceBounds.selectSurfaceArea(
                    VISOR_BOX,
                    List.of(createLeafChild(VISOR_BOX), createLeafChild(VISOR_BAR_BOX))))
                .isEqualTo(new MapSurfaceArea(VISOR_BOX, List.of(VISOR_BAR_BOX)));
        }

        @Test
        void selectSurfaceAreaNarrowsAChromePieceOverTheSurfaceToWhatItDraws() {
            // The visor's bar again, this time read one level deeper. The bar spans the map's whole
            // width while all it draws is buttons, so excluding its own box parks the hover over map
            // the player can plainly see between them. Two buttons rather than one, since the bar
            // holds several and one would not tell a piece standing for all of its children apart
            // from a piece standing for whichever came first.
            assertThat(MapSurfaceBounds.selectSurfaceArea(
                    VISOR_BOX,
                    List.of(
                        createLeafChild(VISOR_BOX),
                        new DrawnChildBoxes(
                            VISOR_BAR_BOX,
                            List.of(VISOR_BAR_BUTTON_BOX, SECOND_VISOR_BAR_BUTTON_BOX)))))
                .isEqualTo(new MapSurfaceArea(
                    VISOR_BOX,
                    List.of(VISOR_BAR_BUTTON_BOX, SECOND_VISOR_BAR_BUTTON_BOX)));
        }

        @Test
        void selectSurfaceAreaKeepsTheWholeBoxOfAChromePieceMerelyAbuttingTheSurface() {
            // A chrome piece sharing an edge with the surface and no area. Narrowing it would let
            // the one line of pixels the two boxes share read as map, since a box contains its own
            // edges; keeping its own box leaves that line chrome, which is the same side of the
            // trade as every other case that cannot be read confidently.
            var abuttingStripBox = new Rectangle(10f, 1164f, 1900f, 19f);

            assertThat(MapSurfaceBounds.selectSurfaceArea(
                    TAB_BOX,
                    List.of(
                        createLeafChild(SURFACE_BOX),
                        new DrawnChildBoxes(abuttingStripBox, List.of(BAR_CONTROL_BOX)))))
                .isEqualTo(new MapSurfaceArea(SURFACE_BOX, List.of(abuttingStripBox)));
        }

        @Test
        void selectSurfaceAreaKeepsTheWholeBoxOfAChromePieceBesideTheSurface() {
            // The M map's tab strip, which holds controls of its own but is laid out clear of the
            // surface. Narrowing it there would be busywork at best and could only ever widen where
            // the hover reaches, since every point in it is outside the surface already.
            assertThat(MapSurfaceBounds.selectSurfaceArea(
                    TAB_BOX,
                    List.of(
                        createLeafChild(SURFACE_BOX),
                        new DrawnChildBoxes(TAB_STRIP_BOX, List.of(BAR_CONTROL_BOX)))))
                .isEqualTo(new MapSurfaceArea(SURFACE_BOX, List.of(TAB_STRIP_BOX)));
        }

        @Test
        void selectSurfaceAreaFindsNothingWhenOnlyChromeIsPresent() {
            // The loud-absence case. A build that reshapes the tab past this rule reports no
            // surface, so the caller falls back rather than accepting a tab strip as the map.
            assertThat(MapSurfaceBounds.selectSurfaceArea(
                    TAB_BOX,
                    List.of(createLeafChild(TAB_STRIP_BOX), createLeafChild(BAR_CONTROL_BOX))))
                .isNull();
        }

        @Test
        void selectSurfaceAreaFindsNothingWhenTheTabHasNoChildren() {
            assertThat(MapSurfaceBounds.selectSurfaceArea(TAB_BOX, List.of()))
                .isNull();
        }

        @Test
        void selectSurfaceAreaFindsNothingWhenTheTabWasNeverPositioned() {
            assertThat(MapSurfaceBounds.selectSurfaceArea(
                    null,
                    List.of(createLeafChild(SURFACE_BOX))))
                .isNull();
        }

        @Test
        void selectSurfaceAreaConfinesAChildThatOverflowsTheTab() {
            // The map's panned content is larger than the screen and hangs below the surface, so a
            // single-level scan does not reach it. Should a later build promote something that
            // overflows to a direct child, the accepted surface still stops at the tab's edge -
            // suppression can then be too weak, never wider than the tab itself.
            var overflowingChildBox = new Rectangle(-595f, -109f, 3017f, 1950f);

            assertThat(MapSurfaceBounds.selectSurfaceArea(
                    TAB_BOX,
                    List.of(createLeafChild(overflowingChildBox))))
                .isEqualTo(new MapSurfaceArea(TAB_BOX, List.of()));
        }
    }

    @Nested
    class CollectDrawnBoxesOf {

        @Test
        void collectDrawnBoxesOfReturnsEveryDrawnChildBoxInTheOrderGiven() {
            var surfaceWidgetFake = createDrawnWidgetFake(SURFACE_BOX);
            var tabStripWidgetFake = createDrawnWidgetFake(TAB_STRIP_BOX);

            assertThat(MapSurfaceBounds.collectDrawnBoxesOf(
                    List.of(surfaceWidgetFake, tabStripWidgetFake)))
                .containsExactly(
                    new DrawnChildBoxes(SURFACE_BOX, List.of()),
                    new DrawnChildBoxes(TAB_STRIP_BOX, List.of()));
        }

        @Test
        void collectDrawnBoxesOfReturnsTheBoxesAChildDrawsIn() {
            // The visor's band. Its own box is what a single-level read sees, and the button inside
            // it is what the expansion rule needs handed to it alongside.
            var buttonWidgetFake = createDrawnWidgetFake(VISOR_BAR_BUTTON_BOX);
            var barWidgetFake = createDrawnWidgetFake(VISOR_BAR_BOX, buttonWidgetFake);

            assertThat(MapSurfaceBounds.collectDrawnBoxesOf(List.of(barWidgetFake)))
                .containsExactly(new DrawnChildBoxes(
                    VISOR_BAR_BOX, List.of(VISOR_BAR_BUTTON_BOX)));
        }

        @Test
        void collectDrawnBoxesOfLeavesOutAChildsChildFadedToNothing() {
            // The same sifting one level down. A button faded out is one the player cannot aim at,
            // so leaving it in would suppress the hover over map that is plainly visible.
            var fadedButtonFake = createWidgetFake(VISOR_BAR_BUTTON_BOX, FADED_TO_NOTHING_OPACITY);
            var barWidgetFake = createDrawnWidgetFake(VISOR_BAR_BOX, fadedButtonFake);

            assertThat(MapSurfaceBounds.collectDrawnBoxesOf(List.of(barWidgetFake)))
                .containsExactly(new DrawnChildBoxes(VISOR_BAR_BOX, List.of()));
        }

        @Test
        void collectDrawnBoxesOfLeavesOutAChildFadedToNothing() {
            // A tab the player has switched away from keeps its box and its place in the tree while
            // it fades out. Were it still a candidate, it could out-cover the real surface and hand
            // the rule a box for a screen nobody is looking at.
            var fadedWidgetFake = createWidgetFake(SURFACE_BOX, FADED_TO_NOTHING_OPACITY);
            var tabStripWidgetFake = createDrawnWidgetFake(TAB_STRIP_BOX);

            assertThat(MapSurfaceBounds.collectDrawnBoxesOf(
                    List.of(fadedWidgetFake, tabStripWidgetFake)))
                .containsExactly(new DrawnChildBoxes(TAB_STRIP_BOX, List.of()));
        }

        @Test
        void collectDrawnBoxesOfLeavesOutAChildTheLayoutNeverPositioned() {
            // No position at all, so it occupies nothing and cannot be measured against the tab.
            var unpositionedWidgetFake = createWidgetFake(null, DRAWN_OPACITY);
            var surfaceWidgetFake = createDrawnWidgetFake(SURFACE_BOX);

            assertThat(MapSurfaceBounds.collectDrawnBoxesOf(
                    List.of(unpositionedWidgetFake, surfaceWidgetFake)))
                .containsExactly(new DrawnChildBoxes(SURFACE_BOX, List.of()));
        }

        @Test
        void collectDrawnBoxesOfLeavesOutAChildThatIsNotAComponent() {
            // The children come back off an unpublished accessor as a bare list, so nothing
            // guarantees every entry is a component - and one that is not has no box to compare.
            var surfaceWidgetFake = createDrawnWidgetFake(SURFACE_BOX);

            assertThat(MapSurfaceBounds.collectDrawnBoxesOf(
                    List.of(new Object(), surfaceWidgetFake)))
                .containsExactly(new DrawnChildBoxes(SURFACE_BOX, List.of()));
        }

        @Test
        void collectDrawnBoxesOfReturnsNothingForATabWithNoChildren() {
            assertThat(MapSurfaceBounds.collectDrawnBoxesOf(List.of()))
                .isEmpty();
        }
    }
}
