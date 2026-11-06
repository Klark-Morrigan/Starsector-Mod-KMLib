package kmlib.starsector.ui.map.presence;

import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.intel.IntelScreenView;
import kmlib.starsector.ui.intel.MapVisorState;
import kmlib.testfixtures.starsector.ui.intel.IntelScreenViewFake;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

/**
 * Pins each read's disjunction over the two hosts that can be showing a map: either alone is enough,
 * both together still read as one, and neither leaves it off. Then the cases that separate the three
 * from one another - a screen with no map at all leaves all three false, so none is another's
 * negation; the look-aware pair each decline the other's screen; and the any-look read takes both
 * screens plus the one neither of them can classify, the map showing with a filter it cannot read.
 *
 * <p>The drawn read is pinned against the showing one over the tabs still fading out: a closing map
 * counts as drawn and never as showing, and a closing tab holding no lit map counts as neither.
 */
class MapPresenceTests {

    private static final Rectangle LIT_VISOR_RECT = new Rectangle(10f, 20f, 300f, 200f);

    private IntelScreenViewFake intelScreenViewFake;

    // The tabs still fading out, and those among them whose visor the intel read finds lit.
    private List<Object> closingTabs;
    private List<Object> litVisorTabs;

    @BeforeEach
    void setUp() {

        intelScreenViewFake = new IntelScreenViewFake();

        closingTabs = new ArrayList<>();
        litVisorTabs = new ArrayList<>();
    }

    @Nested
    class IsAnyVanillaMapShowing {

        @Test
        void isFalseWhenNeitherHostIsShowingAMap() {

            // ARRANGE
            var presence = buildPresence(SectorMapState.NOT_SHOWING);

            // ACT
            var actualShowing = presence.isAnyVanillaMapShowing();

            // ASSERT
            assertThat(actualShowing)
                .isFalse();
        }

        @Test
        void isTrueWhenTheSectorMapAloneIsShowing() {

            // ARRANGE
            var presence = buildPresence(SectorMapState.SHOWING_WITH_STARSCAPE_OFF);

            // ACT
            var actualShowing = presence.isAnyVanillaMapShowing();

            // ASSERT
            assertThat(actualShowing)
                .isTrue();
        }

        @Test
        void isTrueWhenTheLitMapVisorAloneIsShowing() {

            // ARRANGE
            intelScreenViewFake.setMapVisorRect(LIT_VISOR_RECT);

            var presence = buildPresence(SectorMapState.NOT_SHOWING);

            // ACT
            var actualShowing = presence.isAnyVanillaMapShowing();

            // ASSERT
            assertThat(actualShowing)
                .isTrue();
        }

        // Not a contradiction of "one core tab at a time": the two live reads resolve against
        // different core UIs (the sector one follows an interaction dialog's own core UI, the
        // intel one always walks the main core UI), so neither constrains the other and the
        // disjunction has to hold with both true.
        @Test
        void isTrueWhenBothHostsAreShowingAMap() {

            // ARRANGE
            intelScreenViewFake.setMapVisorRect(LIT_VISOR_RECT);

            var presence = buildPresence(SectorMapState.SHOWING_WITH_STARSCAPE_OFF);

            // ACT
            var actualShowing = presence.isAnyVanillaMapShowing();

            // ASSERT
            assertThat(actualShowing)
                .isTrue();
        }

        // What separates this read from the schematic one, which answers false for both of
        // these screens. A map drawing the starfield is still a map on screen.
        @Test
        void isTrueWhileEitherHostIsInStarscapeMode() {

            // ARRANGE
            intelScreenViewFake.setMapVisorRect(LIT_VISOR_RECT);
            intelScreenViewFake.setMapStarscapeModeOn(true);

            var presence = buildPresence(SectorMapState.SHOWING_IN_STARSCAPE_MODE);

            // ACT
            var actualShowing = presence.isAnyVanillaMapShowing();

            // ASSERT
            assertThat(actualShowing)
                .isTrue();
        }

        // The state neither look-aware read accepts. The map is on screen either way, so the
        // read that does not care which look it wears is the one that still answers true - and
        // it is reachable here only because the sector side arrives as a state rather than as
        // the booleans the live port derives from it.
        @Test
        void isTrueWhileTheSectorMapsFilterCannotBeRead() {

            // ARRANGE
            var presence = buildPresence(SectorMapState.SHOWING_WITH_UNREADABLE_FILTER);

            // ACT
            var actualShowing = presence.isAnyVanillaMapShowing();

            // ASSERT
            assertThat(actualShowing)
                .isTrue();
        }

        // The preview panel keeps its filter state while a sibling sub-tab (Planets, Factions)
        // is up, so no filter setting can stand in for a visor being on screen.
        @Test
        void isFalseWhenTheIntelStarscapeFilterIsOnWithNoLitVisor() {

            // ARRANGE
            intelScreenViewFake.setMapStarscapeModeOn(true);

            var presence = buildPresence(SectorMapState.NOT_SHOWING);

            // ACT
            var actualShowing = presence.isAnyVanillaMapShowing();

            // ASSERT
            assertThat(actualShowing)
                .isFalse();
        }

        // A closing map takes no input, so the input-side read must not count it, or hover would
        // resolve against a map the player has already left.
        @Test
        void isFalseForAClosingSectorMap() {

            // ARRANGE
            closingTabs.add(createSectorMapTabMock());

            var presence = buildPresence(SectorMapState.NOT_SHOWING);

            // ACT
            var actualShowing = presence.isAnyVanillaMapShowing();

            // ASSERT
            assertThat(actualShowing)
                .isFalse();
        }

        @Test
        void isFalseForAClosingIntelTabWithItsVisorLit() {

            // ARRANGE
            var closingIntelTab = new Object();

            closingTabs.add(closingIntelTab);
            litVisorTabs.add(closingIntelTab);

            var presence = buildPresence(SectorMapState.NOT_SHOWING);

            // ACT
            var actualShowing = presence.isAnyVanillaMapShowing();

            // ASSERT
            assertThat(actualShowing)
                .isFalse();
        }
    }

    @Nested
    class IsAnyVanillaMapDrawn {

        @Test
        void isTrueWhileAMapIsShowing() {

            // ARRANGE
            var presence = buildPresence(SectorMapState.SHOWING_WITH_STARSCAPE_OFF);

            // ACT
            var actualDrawn = presence.isAnyVanillaMapDrawn();

            // ASSERT
            assertThat(actualDrawn)
                .isTrue();
        }

        // The M screen just closed: the core UI no longer names it, and its wrapper is still
        // drawing it through the fade.
        @Test
        void isTrueForAClosingSectorMap() {

            // ARRANGE
            closingTabs.add(createSectorMapTabMock());

            var presence = buildPresence(SectorMapState.NOT_SHOWING);

            // ACT
            var actualDrawn = presence.isAnyVanillaMapDrawn();

            // ASSERT
            assertThat(actualDrawn)
                .isTrue();
        }

        @Test
        void isTrueForAClosingIntelTabWithItsVisorLit() {

            // ARRANGE
            var closingIntelTab = new Object();

            closingTabs.add(closingIntelTab);
            litVisorTabs.add(closingIntelTab);

            var presence = buildPresence(SectorMapState.NOT_SHOWING);

            // ACT
            var actualDrawn = presence.isAnyVanillaMapDrawn();

            // ASSERT
            assertThat(actualDrawn)
                .isTrue();
        }

        // Leaving the refit screen, say: something is fading out, and none of it is a map.
        @Test
        void isFalseForAClosingTabWithNoMap() {

            // ARRANGE
            closingTabs.add(new Object());

            var presence = buildPresence(SectorMapState.NOT_SHOWING);

            // ACT
            var actualDrawn = presence.isAnyVanillaMapDrawn();

            // ASSERT
            assertThat(actualDrawn)
                .isFalse();
        }

        // The intel screen closed from its Planets or Factions sub-tab. The intel read finds that
        // tab's visor dark, as it does for the tab that is up.
        @Test
        void isFalseForAClosingIntelTabOnAnotherSubTab() {

            // ARRANGE
            var closingIntelTab = new Object();

            closingTabs.add(closingIntelTab);

            var presence = buildPresence(SectorMapState.NOT_SHOWING);

            // ACT
            var actualDrawn = presence.isAnyVanillaMapDrawn();

            // ASSERT
            assertThat(actualDrawn)
                .isFalse();
        }

        @Test
        void isFalseWithNoMapShowingAndNoTabClosing() {

            // ARRANGE
            var presence = buildPresence(SectorMapState.NOT_SHOWING);

            // ACT
            var actualDrawn = presence.isAnyVanillaMapDrawn();

            // ASSERT
            assertThat(actualDrawn)
                .isFalse();
        }

        // Reading the closing tabs walks the core UI's children, and the map's render pass asks
        // this every frame. With a map showing there is nothing that walk could add.
        @Test
        void readsNoClosingTabWhileAMapIsShowing() {

            // ARRANGE
            var presence = new MapPresence(
                () -> SectorMapState.SHOWING_WITH_STARSCAPE_OFF,
                intelScreenViewFake,
                () -> {
                    throw new AssertionError(
                        "the closing tabs were read with a map showing");
                },
                litVisorTabs::contains);

            // ACT
            var actualDrawn = presence.isAnyVanillaMapDrawn();

            // ASSERT
            assertThat(actualDrawn)
                .isTrue();
        }
    }

    @Nested
    class IsSchematicVanillaMapShowing {

        // The fake opens with no visor, so this is every screen that is neither the map nor the
        // intel tab - and the case that stops this being read as "not starscape".
        @Test
        void isFalseWhenNeitherHostIsShowingAMap() {

            // ARRANGE
            var presence = buildPresence(SectorMapState.NOT_SHOWING);

            // ACT
            var actualShowing = presence.isSchematicVanillaMapShowing();

            // ASSERT
            assertThat(actualShowing)
                .isFalse();
        }

        @Test
        void isTrueWhenTheSectorMapAloneIsShowingTheSchematic() {

            // ARRANGE
            var presence = buildPresence(SectorMapState.SHOWING_WITH_STARSCAPE_OFF);

            // ACT
            var actualShowing = presence.isSchematicVanillaMapShowing();

            // ASSERT
            assertThat(actualShowing)
                .isTrue();
        }

        @Test
        void isTrueWhenTheLitMapVisorAloneIsShowingTheSchematic() {

            // ARRANGE
            intelScreenViewFake.setMapVisorRect(LIT_VISOR_RECT);

            var presence = buildPresence(SectorMapState.NOT_SHOWING);

            // ACT
            var actualShowing = presence.isSchematicVanillaMapShowing();

            // ASSERT
            assertThat(actualShowing)
                .isTrue();
        }

        @Test
        void isTrueWhenBothHostsAreShowingTheSchematic() {

            // ARRANGE
            intelScreenViewFake.setMapVisorRect(LIT_VISOR_RECT);

            var presence = buildPresence(SectorMapState.SHOWING_WITH_STARSCAPE_OFF);

            // ACT
            var actualShowing = presence.isSchematicVanillaMapShowing();

            // ASSERT
            assertThat(actualShowing)
                .isTrue();
        }

        @Test
        void isFalseWhenTheLitMapVisorIsInStarscapeMode() {

            // ARRANGE
            intelScreenViewFake.setMapVisorRect(LIT_VISOR_RECT);
            intelScreenViewFake.setMapStarscapeModeOn(true);

            var presence = buildPresence(SectorMapState.NOT_SHOWING);

            // ACT
            var actualShowing = presence.isSchematicVanillaMapShowing();

            // ASSERT
            assertThat(actualShowing)
                .isFalse();
        }

        @Test
        void isFalseWhenTheSectorMapIsInStarscapeMode() {

            // ARRANGE
            var presence = buildPresence(SectorMapState.SHOWING_IN_STARSCAPE_MODE);

            // ACT
            var actualShowing = presence.isSchematicVanillaMapShowing();

            // ASSERT
            assertThat(actualShowing)
                .isFalse();
        }

        // Declined rather than assumed: an unknown filter is not evidence of either look.
        @Test
        void isFalseWhileTheSectorMapsFilterCannotBeRead() {

            // ARRANGE
            var presence = buildPresence(SectorMapState.SHOWING_WITH_UNREADABLE_FILTER);

            // ACT
            var actualShowing = presence.isSchematicVanillaMapShowing();

            // ASSERT
            assertThat(actualShowing)
                .isFalse();
        }
    }

    @Nested
    class IsStarscapeVanillaMapShowing {

        @Test
        void isFalseWhenNeitherHostIsInStarscapeMode() {

            // ARRANGE
            intelScreenViewFake.setMapVisorRect(LIT_VISOR_RECT);

            var presence = buildPresence(SectorMapState.NOT_SHOWING);

            // ACT
            var actualShowing = presence.isStarscapeVanillaMapShowing();

            // ASSERT
            assertThat(actualShowing)
                .isFalse();
        }

        @Test
        void isTrueWhenTheSectorMapAloneIsInStarscapeMode() {

            // ARRANGE
            var presence = buildPresence(SectorMapState.SHOWING_IN_STARSCAPE_MODE);

            // ACT
            var actualShowing = presence.isStarscapeVanillaMapShowing();

            // ASSERT
            assertThat(actualShowing)
                .isTrue();
        }

        @Test
        void isTrueWhenTheLitMapVisorAloneIsInStarscapeMode() {

            // ARRANGE
            intelScreenViewFake.setMapVisorRect(LIT_VISOR_RECT);
            intelScreenViewFake.setMapStarscapeModeOn(true);

            var presence = buildPresence(SectorMapState.NOT_SHOWING);

            // ACT
            var actualShowing = presence.isStarscapeVanillaMapShowing();

            // ASSERT
            assertThat(actualShowing)
                .isTrue();
        }

        @Test
        void isTrueWhenBothHostsAreInStarscapeMode() {

            // ARRANGE
            intelScreenViewFake.setMapVisorRect(LIT_VISOR_RECT);
            intelScreenViewFake.setMapStarscapeModeOn(true);

            var presence = buildPresence(SectorMapState.SHOWING_IN_STARSCAPE_MODE);

            // ACT
            var actualShowing = presence.isStarscapeVanillaMapShowing();

            // ASSERT
            assertThat(actualShowing)
                .isTrue();
        }

        @Test
        void isFalseWhenTheIntelStarscapeFilterIsOnWithNoLitVisor() {

            // ARRANGE
            intelScreenViewFake.setMapStarscapeModeOn(true);

            var presence = buildPresence(SectorMapState.NOT_SHOWING);

            // ACT
            var actualShowing = presence.isStarscapeVanillaMapShowing();

            // ASSERT
            assertThat(actualShowing)
                .isFalse();
        }

        @Test
        void isFalseWhileTheSectorMapsFilterCannotBeRead() {

            // ARRANGE
            var presence = buildPresence(SectorMapState.SHOWING_WITH_UNREADABLE_FILTER);

            // ACT
            var actualShowing = presence.isStarscapeVanillaMapShowing();

            // ASSERT
            assertThat(actualShowing)
                .isFalse();
        }

        // Behind the intel port is a walk down the live widget tree, taken per read. This runs
        // every frame of a campaign, so pairing the visor's presence with its filter out of two
        // reads would take that walk twice to describe one frame.
        @Test
        void takesOneReadingOfTheVisorRatherThanASignalEachFromIt() {

            // ARRANGE
            var intelScreenFake =
                new SingleReadingIntelScreenFake(MapVisorState.SHOWING_IN_STARSCAPE_MODE);

            var presence = new MapPresence(
                () -> SectorMapState.NOT_SHOWING,
                intelScreenFake,
                () -> closingTabs,
                litVisorTabs::contains);

            // ACT
            var actualShowing = presence.isStarscapeVanillaMapShowing();

            // ASSERT
            assertThat(actualShowing)
                .isTrue();
        }
    }

    // The sector read is a plain supplier here rather than the live static, so each case names the
    // state its screen is in and the intel fake carries the rest. The closing tabs and the visor test
    // over them read the lists each case fills.
    private MapPresence buildPresence(SectorMapState sectorMapState) {

        return new MapPresence(
            () -> sectorMapState,
            intelScreenViewFake,
            () -> closingTabs,
            litVisorTabs::contains);
    }

    // The M screen's tab is both a map and a laid-out component; a double has to be both for the
    // same reason the map test asks both.
    private static Object createSectorMapTabMock() {

        return mock(
            SectorMapAPI.class,
            withSettings().extraInterfaces(UIComponentAPI.class));
    }

    // An intel screen that answers the combined reading and faults on either signal asked alone, so
    // a read that went back for the second one says so by failing rather than in a comment. A case
    // that has to pose a whole screen uses the shared fake; this poses one question about how the
    // port is used.
    private record SingleReadingIntelScreenFake(MapVisorState mapVisorState)
            implements IntelScreenView {

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
