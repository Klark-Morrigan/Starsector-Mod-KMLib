package kmlib.starsector.ui.map.presence;

import com.fs.starfarer.api.ui.UIComponentAPI;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.intel.IntelScreenView;
import kmlib.starsector.ui.intel.MapVisorState;
import kmlib.testfixtures.starsector.ui.intel.IntelScreenViewFake;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins each read's disjunction over the two hosts that can be showing a map: either alone is enough,
 * both together still read as one, and neither leaves it off. Then the cases that separate the three
 * from one another - a screen with no map at all leaves all three false, so none is another's
 * negation; the look-aware pair each decline the other's screen; and the any-look read takes both
 * screens plus the one neither of them can classify, the map showing with a filter it cannot read.
 */
class MapPresenceTest {

    private static final Rectangle LIT_VISOR_RECT = new Rectangle(10f, 20f, 300f, 200f);

    private IntelScreenViewFake intelScreenViewFake;

    @BeforeEach
    void setUp() {
        intelScreenViewFake = new IntelScreenViewFake();
    }

    @Nested
    class IsAnyMapShowing {

        @Test
        void isFalseWhenNeitherHostIsShowingAMap() {
            assertThat(buildPresence(SectorMapState.NOT_SHOWING).isAnyMapShowing())
                .isFalse();
        }

        @Test
        void isTrueWhenTheSectorMapAloneIsShowing() {
            assertThat(buildPresence(SectorMapState.SHOWING_WITH_STARSCAPE_OFF).isAnyMapShowing())
                .isTrue();
        }

        @Test
        void isTrueWhenTheLitMapVisorAloneIsShowing() {
            intelScreenViewFake.setMapVisorRect(LIT_VISOR_RECT);

            assertThat(buildPresence(SectorMapState.NOT_SHOWING).isAnyMapShowing())
                .isTrue();
        }

        @Test
        void isTrueWhenBothHostsAreShowingAMap() {
            // Not a contradiction of "one core tab at a time": the two live reads resolve against
            // different core UIs (the sector one follows an interaction dialog's own core UI, the
            // intel one always walks the main core UI), so neither constrains the other and the
            // disjunction has to hold with both true.
            intelScreenViewFake.setMapVisorRect(LIT_VISOR_RECT);

            assertThat(buildPresence(SectorMapState.SHOWING_WITH_STARSCAPE_OFF).isAnyMapShowing())
                .isTrue();
        }

        @Test
        void isTrueWhileEitherHostIsInStarscapeMode() {
            // What separates this read from the schematic one, which answers false for both of
            // these screens. A map drawing the starfield is still a map on screen.
            intelScreenViewFake.setMapVisorRect(LIT_VISOR_RECT);
            intelScreenViewFake.setMapStarscapeModeOn(true);

            assertThat(buildPresence(SectorMapState.SHOWING_IN_STARSCAPE_MODE).isAnyMapShowing())
                .isTrue();
        }

        @Test
        void isTrueWhileTheSectorMapsFilterCannotBeRead() {
            // The state neither look-aware read accepts. The map is on screen either way, so the
            // read that does not care which look it wears is the one that still answers true - and
            // it is reachable here only because the sector side arrives as a state rather than as
            // the booleans the live port derives from it.
            assertThat(buildPresence(SectorMapState.SHOWING_WITH_UNREADABLE_FILTER).isAnyMapShowing())
                .isTrue();
        }

        @Test
        void isFalseWhenTheIntelStarscapeFilterIsOnWithNoLitVisor() {
            // The preview panel keeps its filter state while a sibling sub-tab (Planets, Factions)
            // is up, so no filter setting can stand in for a visor being on screen.
            intelScreenViewFake.setMapStarscapeModeOn(true);

            assertThat(buildPresence(SectorMapState.NOT_SHOWING).isAnyMapShowing())
                .isFalse();
        }
    }

    @Nested
    class IsSchematicMapShowing {

        @Test
        void isFalseWhenNeitherHostIsShowingAMap() {
            // The fake opens with no visor, so this is every screen that is neither the map nor the
            // intel tab - and the case that stops this being read as "not starscape".
            assertThat(buildPresence(SectorMapState.NOT_SHOWING).isSchematicMapShowing())
                .isFalse();
        }

        @Test
        void isTrueWhenTheSectorMapAloneIsShowingTheSchematic() {
            assertThat(
                buildPresence(SectorMapState.SHOWING_WITH_STARSCAPE_OFF).isSchematicMapShowing())
                .isTrue();
        }

        @Test
        void isTrueWhenTheLitMapVisorAloneIsShowingTheSchematic() {
            intelScreenViewFake.setMapVisorRect(LIT_VISOR_RECT);

            assertThat(buildPresence(SectorMapState.NOT_SHOWING).isSchematicMapShowing())
                .isTrue();
        }

        @Test
        void isTrueWhenBothHostsAreShowingTheSchematic() {
            intelScreenViewFake.setMapVisorRect(LIT_VISOR_RECT);

            assertThat(
                buildPresence(SectorMapState.SHOWING_WITH_STARSCAPE_OFF).isSchematicMapShowing())
                .isTrue();
        }

        @Test
        void isFalseWhenTheLitMapVisorIsInStarscapeMode() {
            intelScreenViewFake.setMapVisorRect(LIT_VISOR_RECT);
            intelScreenViewFake.setMapStarscapeModeOn(true);

            assertThat(buildPresence(SectorMapState.NOT_SHOWING).isSchematicMapShowing())
                .isFalse();
        }

        @Test
        void isFalseWhenTheSectorMapIsInStarscapeMode() {
            assertThat(
                buildPresence(SectorMapState.SHOWING_IN_STARSCAPE_MODE).isSchematicMapShowing())
                .isFalse();
        }

        @Test
        void isFalseWhileTheSectorMapsFilterCannotBeRead() {
            // Declined rather than assumed: an unknown filter is not evidence of either look.
            assertThat(
                buildPresence(SectorMapState.SHOWING_WITH_UNREADABLE_FILTER).isSchematicMapShowing())
                .isFalse();
        }
    }

    @Nested
    class IsStarscapeMapShowing {

        @Test
        void isFalseWhenNeitherHostIsInStarscapeMode() {
            intelScreenViewFake.setMapVisorRect(LIT_VISOR_RECT);

            assertThat(buildPresence(SectorMapState.NOT_SHOWING).isStarscapeMapShowing())
                .isFalse();
        }

        @Test
        void isTrueWhenTheSectorMapAloneIsInStarscapeMode() {
            assertThat(
                buildPresence(SectorMapState.SHOWING_IN_STARSCAPE_MODE).isStarscapeMapShowing())
                .isTrue();
        }

        @Test
        void isTrueWhenTheLitMapVisorAloneIsInStarscapeMode() {
            intelScreenViewFake.setMapVisorRect(LIT_VISOR_RECT);
            intelScreenViewFake.setMapStarscapeModeOn(true);

            assertThat(buildPresence(SectorMapState.NOT_SHOWING).isStarscapeMapShowing())
                .isTrue();
        }

        @Test
        void isTrueWhenBothHostsAreInStarscapeMode() {
            intelScreenViewFake.setMapVisorRect(LIT_VISOR_RECT);
            intelScreenViewFake.setMapStarscapeModeOn(true);

            assertThat(
                buildPresence(SectorMapState.SHOWING_IN_STARSCAPE_MODE).isStarscapeMapShowing())
                .isTrue();
        }

        @Test
        void isFalseWhenTheIntelStarscapeFilterIsOnWithNoLitVisor() {
            intelScreenViewFake.setMapStarscapeModeOn(true);

            assertThat(buildPresence(SectorMapState.NOT_SHOWING).isStarscapeMapShowing())
                .isFalse();
        }

        @Test
        void isFalseWhileTheSectorMapsFilterCannotBeRead() {
            assertThat(
                buildPresence(SectorMapState.SHOWING_WITH_UNREADABLE_FILTER).isStarscapeMapShowing())
                .isFalse();
        }

        @Test
        void takesOneReadingOfTheVisorRatherThanASignalEachFromIt() {
            // Behind the intel port is a walk down the live widget tree, taken per read. This runs
            // every frame of a campaign, so pairing the visor's presence with its filter out of two
            // reads would take that walk twice to describe one frame.
            var intelScreenFake =
                new SingleReadingIntelScreenFake(MapVisorState.SHOWING_IN_STARSCAPE_MODE);

            assertThat(new MapPresence(() -> SectorMapState.NOT_SHOWING, intelScreenFake)
                .isStarscapeMapShowing())
                .isTrue();
        }
    }

    // The sector read is a plain supplier here rather than the live static, so each case names the
    // state its screen is in and the intel fake carries the rest.
    private MapPresence buildPresence(SectorMapState sectorMapState) {
        return new MapPresence(() -> sectorMapState, intelScreenViewFake);
    }

    // An intel screen that answers the combined reading and faults on either signal asked alone, so
    // a read that went back for the second one says so by failing rather than in a comment. A case
    // that has to pose a whole screen uses the shared fake; this poses one question about how the
    // port is used.
    private record SingleReadingIntelScreenFake(MapVisorState mapVisorState) implements IntelScreenView {

        @Override
        public boolean isIntelTabOpen() {
            return true;
        }

        @Override
        public Rectangle getMapVisorRect() {
            throw new AssertionError("the visor's box must not be read beside its combined state");
        }

        @Override
        public UIComponentAPI getMapVisorWidget() {
            throw new AssertionError("the visor's widget must not be read beside its combined state");
        }

        @Override
        public MapVisorState readMapVisorState() {
            return mapVisorState;
        }

        @Override
        public boolean isMapStarscapeModeOn() {
            throw new AssertionError("the visor's filter must not be read beside its combined state");
        }
    }
}
