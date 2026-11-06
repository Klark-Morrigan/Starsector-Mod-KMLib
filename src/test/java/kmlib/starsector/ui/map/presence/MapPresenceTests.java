package kmlib.starsector.ui.map.presence;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.intel.IntelScreenView;
import kmlib.starsector.ui.intel.MapVisorState;
import kmlib.testfixtures.starsector.StubbedGlobalLogger;
import kmlib.testfixtures.starsector.compatibility.GameReachRecordFixture;
import kmlib.testfixtures.starsector.ui.intel.IntelScreenViewFake;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * Pins each read's disjunction over the two hosts that can be showing a map: either alone is enough,
 * both together still read as one, and neither leaves it off. Then the cases that separate the three
 * from one another - a screen with no map at all leaves all three false, so none is another's
 * negation; the look-aware pair each decline the other's screen; and the any-look read takes both
 * screens plus the one neither of them can classify, the map showing with a filter it cannot read.
 *
 * <p>The drawn reads are pinned against the showing ones over the tabs still fading out: a closing map
 * counts as drawn and never as showing, a closing tab holding no lit map counts as neither, and the
 * Starscape drawn read takes a closing map only in Starscape mode.
 */
class MapPresenceTests {

    private static final Rectangle LIT_VISOR_RECT = new Rectangle(10f, 20f, 300f, 200f);

    private IntelScreenViewFake intelScreenViewFake;

    // The tabs still fading out, what the intel read finds below each, and what the closed sector
    // map's persisted state classifies to.
    private List<Object> closingTabs;
    private Map<Object, MapVisorState> visorStatesBelowTabs;
    private SectorMapState closingSectorMapState;

    @BeforeEach
    void setUp() {

        intelScreenViewFake = new IntelScreenViewFake();

        closingTabs = new ArrayList<>();
        visorStatesBelowTabs = new IdentityHashMap<>();
        closingSectorMapState = SectorMapState.NOT_SHOWING;
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
            closingSectorMapState = SectorMapState.SHOWING_WITH_STARSCAPE_OFF;

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
            visorStatesBelowTabs.put(closingIntelTab, MapVisorState.SHOWING_WITH_STARSCAPE_OFF);

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
            closingSectorMapState = SectorMapState.SHOWING_IN_STARSCAPE_MODE;

            var presence = buildPresence(SectorMapState.NOT_SHOWING);

            // ACT
            var actualDrawn = presence.isAnyVanillaMapDrawn();

            // ASSERT
            assertThat(actualDrawn)
                .isTrue();
        }

        // The closed map was on a star system, which this read declines as the showing one does.
        @Test
        void isFalseForAClosingSectorMapLeftOnAStarSystem() {

            // ARRANGE
            closingTabs.add(createSectorMapTabMock());

            var presence = buildPresence(SectorMapState.NOT_SHOWING);

            // ACT
            var actualDrawn = presence.isAnyVanillaMapDrawn();

            // ASSERT
            assertThat(actualDrawn)
                .isFalse();
        }

        @Test
        void isTrueForAClosingIntelTabWithItsVisorLit() {

            // ARRANGE
            var closingIntelTab = new Object();

            closingTabs.add(closingIntelTab);
            visorStatesBelowTabs.put(closingIntelTab, MapVisorState.SHOWING_IN_STARSCAPE_MODE);

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
                () -> closingSectorMapState,
                intelScreenViewFake,
                () -> {
                    throw new AssertionError(
                        "the closing tabs were read with a map showing");
                },
                MapPresenceTests.this::readVisorStateBelow);

            // ACT
            var actualDrawn = presence.isAnyVanillaMapDrawn();

            // ASSERT
            assertThat(actualDrawn)
                .isTrue();
        }

        // The live binding fails every read closed. A campaign UI offering no core hop breaks both
        // the intel walk and the closing-tab read, and a render pass asking must not end the frame.
        @Test
        void answersNoMapThroughTheLiveBindingWhereTheCoreUiCannotBeReached() {

            // ARRANGE
            var reachRecord = new GameReachRecordFixture();
            var sectorMock = mock(SectorAPI.class);

            when(sectorMock.getCampaignUI())
                .thenReturn(mock(CampaignUIAPI.class));

            try (var globalMock = StubbedGlobalLogger.openGlobalAnsweringLoggers()) {

                globalMock
                    .when(Global::getSector)
                    .thenReturn(sectorMock);

                var presence = new MapPresence(reachRecord.getReporter());

                // ACT
                var actualThrown = catchThrowable(presence::isAnyVanillaMapDrawn);
                var actualDrawn = presence.isAnyVanillaMapDrawn();

                // ASSERT
                assertThat(actualThrown)
                    .isNull();
                assertThat(actualDrawn)
                    .isFalse();
            }
        }
    }

    @Nested
    class IsStarscapeVanillaMapDrawn {

        @Test
        void isTrueWhileAStarscapeMapIsShowing() {

            // ARRANGE
            var presence = buildPresence(SectorMapState.SHOWING_IN_STARSCAPE_MODE);

            // ACT
            var actualDrawn = presence.isStarscapeVanillaMapDrawn();

            // ASSERT
            assertThat(actualDrawn)
                .isTrue();
        }

        // The Starscape surfaces stand aside unless this holds, so a closing Starscape map is what
        // keeps them drawing through its fade.
        @Test
        void isTrueForAClosingSectorMapInStarscapeMode() {

            // ARRANGE
            closingTabs.add(createSectorMapTabMock());
            closingSectorMapState = SectorMapState.SHOWING_IN_STARSCAPE_MODE;

            var presence = buildPresence(SectorMapState.NOT_SHOWING);

            // ACT
            var actualDrawn = presence.isStarscapeVanillaMapDrawn();

            // ASSERT
            assertThat(actualDrawn)
                .isTrue();
        }

        @Test
        void isTrueForAClosingIntelTabWhoseVisorIsInStarscapeMode() {

            // ARRANGE
            var closingIntelTab = new Object();

            closingTabs.add(closingIntelTab);
            visorStatesBelowTabs.put(closingIntelTab, MapVisorState.SHOWING_IN_STARSCAPE_MODE);

            var presence = buildPresence(SectorMapState.NOT_SHOWING);

            // ACT
            var actualDrawn = presence.isStarscapeVanillaMapDrawn();

            // ASSERT
            assertThat(actualDrawn)
                .isTrue();
        }

        @Test
        void isFalseForAClosingSchematicSectorMap() {

            // ARRANGE
            closingTabs.add(createSectorMapTabMock());
            closingSectorMapState = SectorMapState.SHOWING_WITH_STARSCAPE_OFF;

            var presence = buildPresence(SectorMapState.NOT_SHOWING);

            // ACT
            var actualDrawn = presence.isStarscapeVanillaMapDrawn();

            // ASSERT
            assertThat(actualDrawn)
                .isFalse();
        }

        @Test
        void isFalseForAClosingIntelTabOnAnotherSubTab() {

            // ARRANGE
            closingTabs.add(new Object());

            var presence = buildPresence(SectorMapState.NOT_SHOWING);

            // ACT
            var actualDrawn = presence.isStarscapeVanillaMapDrawn();

            // ASSERT
            assertThat(actualDrawn)
                .isFalse();
        }

        @Test
        void isFalseWithNoMapShowingAndNoTabClosing() {

            // ARRANGE
            var presence = buildPresence(SectorMapState.NOT_SHOWING);

            // ACT
            var actualDrawn = presence.isStarscapeVanillaMapDrawn();

            // ASSERT
            assertThat(actualDrawn)
                .isFalse();
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
                () -> closingSectorMapState,
                intelScreenFake,
                () -> closingTabs,
                MapPresenceTests.this::readVisorStateBelow);

            // ACT
            var actualShowing = presence.isStarscapeVanillaMapShowing();

            // ASSERT
            assertThat(actualShowing)
                .isTrue();
        }
    }

    // The sector read is a plain supplier here rather than the live static, so each case names the
    // state its screen is in and the intel fake carries the rest. The closing-tab ports read the
    // fields each case fills.
    private MapPresence buildPresence(SectorMapState sectorMapState) {

        return new MapPresence(
            () -> sectorMapState,
            () -> closingSectorMapState,
            intelScreenViewFake,
            () -> closingTabs,
            this::readVisorStateBelow);
    }

    // A tab no case gave a visor reads as the live read answers a tab holding no events panel.
    private MapVisorState readVisorStateBelow(Object tab) {
        return visorStatesBelowTabs.getOrDefault(tab, MapVisorState.NOT_SHOWING);
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
