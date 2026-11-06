package kmlib.starsector.ui.map.presence;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PersistentUIDataAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.campaign.CampaignUIPersistentData;

import kmlib.testfixtures.starsector.StubbedGlobalLogger;
import kmlib.testfixtures.starsector.compatibility.GameReachRecordFixture;
import kmlib.testfixtures.starsector.ui.coreui.CoreHostingDialogFake;
import kmlib.testfixtures.starsector.ui.coreui.CoreUiComponentFake;
import kmlib.testfixtures.starsector.ui.coreui.CoreUiFake;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the sector-map gates across each way they can fail closed - no sector, no campaign UI, a
 * non-map tab, a map screen a dialog has closed but goes on naming, a star-system sub-view,
 * UI-data that is not the game's concrete campaign-UI-data class, or a concrete class a game
 * build has changed - and how each weighs the Starscape filter: the "is showing" read
 * ignores it, while the two mode reads split the sector sub-view of the map tab between them.
 * A closed map's state is classified off the same signals, as though it were still the tab up.
 * The concrete UI-data object is the real
 * class, constructed directly: its field initializers are pure data, the live filter object
 * it owns is mutable, and the location has a real setter. Mocking or subclassing it is not
 * an option - anything that reflects over its method table (Mockito instrumentation, JUnit
 * discovery of a nested subclass) drags in obfuscated-jar types whose dotted member names
 * only pass under the game's non-verifying JVM.
 */
class CampaignMapViewTests {

    private final GameReachRecordFixture reachRecord = new GameReachRecordFixture();

    private MockedStatic<Global> globalMock;
    private SectorAPI sectorMock;
    private CampaignUIAPI campaignUiMock;

    @BeforeEach
    void setUp() {

        sectorMock = mock(SectorAPI.class);
        campaignUiMock = mock(CampaignUIAPI.class);

        globalMock = StubbedGlobalLogger.openGlobalAnsweringLoggers();
        globalMock
            .when(Global::getSector)
            .thenReturn(sectorMock);

        when(sectorMock.getCampaignUI())
            .thenReturn(campaignUiMock);

        // The map tab is the active core tab by default; each test relaxes one condition.
        when(campaignUiMock.getCurrentCoreTab())
            .thenReturn(CoreUITabId.MAP);
    }

    @AfterEach
    void tearDown() {

        globalMock.close();
    }

    @Nested
    class IsSectorMapShowing {

        @Test
        void isFalseWhenTheSectorIsMissing() {

            // ARRANGE
            globalMock
                .when(Global::getSector)
                .thenReturn(null);

            // ACT
            var actualShowing = CampaignMapView.isSectorMapShowing(reachRecord.getReporter());

            // ASSERT
            assertThat(actualShowing)
                .isFalse();
        }

        @Test
        void isFalseWhenTheCampaignUiIsMissing() {

            // ARRANGE
            when(sectorMock.getCampaignUI())
                .thenReturn(null);

            // ACT
            var actualShowing = CampaignMapView.isSectorMapShowing(reachRecord.getReporter());

            // ASSERT
            assertThat(actualShowing)
                .isFalse();
        }

        @Test
        void isFalseWhenTheActiveTabIsNotTheMap() {

            // ARRANGE
            when(campaignUiMock.getCurrentCoreTab())
                .thenReturn(CoreUITabId.FLEET);

            // ACT
            var actualShowing = CampaignMapView.isSectorMapShowing(reachRecord.getReporter());

            // ASSERT
            assertThat(actualShowing)
                .isFalse();
        }

        // The map screen opened from an interaction and closed again: that dialog's core UI is
        // faded out and taken off screen, and goes on naming the map tab it was showing. Read
        // raw, this reports the map for the rest of the docked visit and every overlay gated on
        // it draws over the dialog.
        @Test
        void isFalseWhileADialogHandsOutAClosedMapScreensCoreUi() {

            // ARRANGE
            when(campaignUiMock.getCurrentInteractionDialog())
                .thenReturn(CoreHostingDialogFake
                    .createHosting(CoreUiFake.createDismissed(new CoreUiComponentFake())));

            stubUiDataWithStarscape(false, buildHyperspaceLocation());

            // ACT
            var actualShowing = CampaignMapView.isSectorMapShowing(reachRecord.getReporter());

            // ASSERT
            assertThat(actualShowing)
                .isFalse();
        }

        // The other side of it, and why the correction is about the core UI being down rather
        // than about a dialog being up: the map is routinely opened from an interaction, and it
        // is as much on screen there as one opened from game space.
        @Test
        void isTrueWhileADialogsMapScreenIsStillShowing() {

            // ARRANGE
            when(campaignUiMock.getCurrentInteractionDialog())
                .thenReturn(CoreHostingDialogFake
                    .createHosting(new CoreUiFake(new CoreUiComponentFake())));

            stubUiDataWithStarscape(false, buildHyperspaceLocation());

            // ACT
            var actualShowing = CampaignMapView.isSectorMapShowing(reachRecord.getReporter());

            // ASSERT
            assertThat(actualShowing)
                .isTrue();
        }

        @Test
        void isTrueWithTheStarscapeFilterOff() {

            // ARRANGE
            stubUiDataWithStarscape(false, buildHyperspaceLocation());

            // ACT
            var actualShowing = CampaignMapView.isSectorMapShowing(reachRecord.getReporter());

            // ASSERT
            assertThat(actualShowing)
                .isTrue();
        }

        // The read deliberately ignores the filter: the map is on screen either way, which is
        // what separates this from the mode reads.
        @Test
        void isTrueWithTheStarscapeFilterOn() {

            // ARRANGE
            stubUiDataWithStarscape(true, buildHyperspaceLocation());

            // ACT
            var actualShowing = CampaignMapView.isSectorMapShowing(reachRecord.getReporter());

            // ASSERT
            assertThat(actualShowing)
                .isTrue();
        }

        @Test
        void treatsANullMapLocationAsTheSectorSubView() {

            // ARRANGE
            stubUiDataWithStarscape(false, null);

            // ACT
            var actualShowing = CampaignMapView.isSectorMapShowing(reachRecord.getReporter());

            // ASSERT
            assertThat(actualShowing)
                .isTrue();
        }

        @Test
        void isFalseWhenTheSubViewIsAStarSystem() {

            // ARRANGE
            stubUiDataWithStarscape(false, buildSystemLocation());

            // ACT
            var actualShowing = CampaignMapView.isSectorMapShowing(reachRecord.getReporter());

            // ASSERT
            assertThat(actualShowing)
                .isFalse();
        }

        @Test
        void isFalseWhenTheUiDataIsNotTheGameConcreteType() {

            // ARRANGE
            when(sectorMock.getUIData())
                .thenReturn(mock(PersistentUIDataAPI.class));

            // ACT
            var actualShowing = CampaignMapView.isSectorMapShowing(reachRecord.getReporter());

            // ASSERT
            assertThat(actualShowing)
                .isFalse();
        }

        // A game build that changed the concrete class fails on the read itself, from render
        // passes with no catch of their own - so it has to come back as the map not showing
        // rather than end the frame.
        @Test
        void isFalseWhenTheUiDataNoLongerLinks() {

            // ARRANGE
            stubUiDataFailingToLink();

            // ACT
            var actualShowing = CampaignMapView.isSectorMapShowing(reachRecord.getReporter());

            // ASSERT
            assertThat(actualShowing)
                .isFalse();
        }

        @Test
        void filesUiDataOfAnotherTypeAsABrokenReach() {

            // ARRANGE
            when(sectorMock.getUIData())
                .thenReturn(mock(PersistentUIDataAPI.class));

            var expectedFailureSite = "reading the sector map's view state";

            // ACT
            CampaignMapView.isSectorMapShowing(reachRecord.getReporter());

            // ASSERT
            assertThat(reachRecord.takeReportedFailure().breakage().failureSite())
                .isEqualTo(expectedFailureSite);
        }

        @Test
        void filesUiDataThatNoLongerLinksWithWhatWasThrown() {

            // ARRANGE
            stubUiDataFailingToLink();

            // ACT
            CampaignMapView.isSectorMapShowing(reachRecord.getReporter());

            // ASSERT
            assertThat(reachRecord.takeReportedFailure().cause())
                .isInstanceOf(NoSuchFieldError.class);
        }

        // The concrete read is not taken off the map tab at all, so UI data it could not read
        // there is never met.
        @Test
        void filesNothingOffTheMapTab() {

            // ARRANGE
            when(campaignUiMock.getCurrentCoreTab())
                .thenReturn(CoreUITabId.FLEET);
            when(sectorMock.getUIData())
                .thenReturn(mock(PersistentUIDataAPI.class));

            // ACT
            CampaignMapView.isSectorMapShowing(reachRecord.getReporter());

            // ASSERT
            assertThat(reachRecord.hasReported())
                .isFalse();
        }

        // Not showing, and ordinarily so: a map on a star system is a map read correctly.
        @Test
        void filesNothingForASystemSubView() {

            // ARRANGE
            stubUiDataWithStarscape(false, buildSystemLocation());

            // ACT
            CampaignMapView.isSectorMapShowing(reachRecord.getReporter());

            // ASSERT
            assertThat(reachRecord.hasReported())
                .isFalse();
        }
    }

    @Nested
    class ResolveClosingSectorMapState {

        // The tab has already gone from the core UI's current tab, so only the persisted state the
        // closed map left behind says what it was drawing.
        @Test
        void classifiesAClosedMapOffTheMapTabAsTheStateItWasLeftIn() {

            // ARRANGE
            when(campaignUiMock.getCurrentCoreTab())
                .thenReturn(null);

            stubUiDataWithStarscape(true, buildHyperspaceLocation());

            // ACT
            var actualState = CampaignMapView.resolveClosingSectorMapState(reachRecord.getReporter());

            // ASSERT
            assertThat(actualState)
                .isEqualTo(SectorMapState.SHOWING_IN_STARSCAPE_MODE);
        }

        @Test
        void isNotShowingForAClosedMapLeftOnAStarSystem() {

            // ARRANGE
            when(campaignUiMock.getCurrentCoreTab())
                .thenReturn(null);

            stubUiDataWithStarscape(true, buildSystemLocation());

            // ACT
            var actualState = CampaignMapView.resolveClosingSectorMapState(reachRecord.getReporter());

            // ASSERT
            assertThat(actualState)
                .isEqualTo(SectorMapState.NOT_SHOWING);
        }

        @Test
        void isNotShowingWhenTheSectorIsMissing() {

            // ARRANGE
            globalMock
                .when(Global::getSector)
                .thenReturn(null);

            // ACT
            var actualState = CampaignMapView.resolveClosingSectorMapState(reachRecord.getReporter());

            // ASSERT
            assertThat(actualState)
                .isEqualTo(SectorMapState.NOT_SHOWING);
        }

        @Test
        void isNotShowingWhenTheUiDataNoLongerLinks() {

            // ARRANGE
            stubUiDataFailingToLink();

            // ACT
            var actualState = CampaignMapView.resolveClosingSectorMapState(reachRecord.getReporter());

            // ASSERT
            assertThat(actualState)
                .isEqualTo(SectorMapState.NOT_SHOWING);
        }
    }

    @Nested
    class IsSectorMapInStarscapeMode {

        @Test
        void isFalseWhenTheSectorIsMissing() {

            // ARRANGE
            globalMock
                .when(Global::getSector)
                .thenReturn(null);

            // ACT
            var actualStarscape = CampaignMapView.isSectorMapInStarscapeMode(reachRecord.getReporter());

            // ASSERT
            assertThat(actualStarscape)
                .isFalse();
        }

        @Test
        void isFalseWhenTheActiveTabIsNotTheMap() {

            // ARRANGE
            when(campaignUiMock.getCurrentCoreTab())
                .thenReturn(CoreUITabId.FLEET);

            // ACT
            var actualStarscape = CampaignMapView.isSectorMapInStarscapeMode(reachRecord.getReporter());

            // ASSERT
            assertThat(actualStarscape)
                .isFalse();
        }

        @Test
        void isTrueOnTheSectorSubViewWithStarscapeOn() {

            // ARRANGE
            stubUiDataWithStarscape(true, buildHyperspaceLocation());

            // ACT
            var actualStarscape = CampaignMapView.isSectorMapInStarscapeMode(reachRecord.getReporter());

            // ASSERT
            assertThat(actualStarscape)
                .isTrue();
        }

        @Test
        void isFalseWhenTheStarscapeFilterIsOff() {

            // ARRANGE
            stubUiDataWithStarscape(false, buildHyperspaceLocation());

            // ACT
            var actualStarscape = CampaignMapView.isSectorMapInStarscapeMode(reachRecord.getReporter());

            // ASSERT
            assertThat(actualStarscape)
                .isFalse();
        }

        @Test
        void treatsANullMapLocationAsTheSectorSubView() {

            // ARRANGE
            stubUiDataWithStarscape(true, null);

            // ACT
            var actualStarscape = CampaignMapView.isSectorMapInStarscapeMode(reachRecord.getReporter());

            // ASSERT
            assertThat(actualStarscape)
                .isTrue();
        }

        // A star system drawn with the filter on is not the sector map, so the mode read
        // declines it exactly as the showing read does.
        @Test
        void isFalseWhenTheSubViewIsAStarSystem() {

            // ARRANGE
            stubUiDataWithStarscape(true, buildSystemLocation());

            // ACT
            var actualStarscape = CampaignMapView.isSectorMapInStarscapeMode(reachRecord.getReporter());

            // ASSERT
            assertThat(actualStarscape)
                .isFalse();
        }

        // An unreadable filter is neither on nor off, so this read fails closed too.
        @Test
        void isFalseWhenTheUiDataIsNotTheGameConcreteType() {

            // ARRANGE
            when(sectorMock.getUIData())
                .thenReturn(mock(PersistentUIDataAPI.class));

            // ACT
            var actualStarscape = CampaignMapView.isSectorMapInStarscapeMode(reachRecord.getReporter());

            // ASSERT
            assertThat(actualStarscape)
                .isFalse();
        }

        @Test
        void isFalseWhenTheUiDataNoLongerLinks() {

            // ARRANGE
            stubUiDataFailingToLink();

            // ACT
            var actualStarscape = CampaignMapView.isSectorMapInStarscapeMode(reachRecord.getReporter());

            // ASSERT
            assertThat(actualStarscape)
                .isFalse();
        }
    }

    @Nested
    class IsSectorMapWithStarscapeOff {

        @Test
        void isFalseWhenTheSectorIsMissing() {

            // ARRANGE
            globalMock
                .when(Global::getSector)
                .thenReturn(null);

            // ACT
            var actualSchematic = CampaignMapView.isSectorMapWithStarscapeOff(reachRecord.getReporter());

            // ASSERT
            assertThat(actualSchematic)
                .isFalse();
        }

        @Test
        void isFalseWhenTheCampaignUiIsMissing() {

            // ARRANGE
            when(sectorMock.getCampaignUI())
                .thenReturn(null);

            // ACT
            var actualSchematic = CampaignMapView.isSectorMapWithStarscapeOff(reachRecord.getReporter());

            // ASSERT
            assertThat(actualSchematic)
                .isFalse();
        }

        @Test
        void isFalseWhenTheActiveTabIsNotTheMap() {

            // ARRANGE
            when(campaignUiMock.getCurrentCoreTab())
                .thenReturn(CoreUITabId.FLEET);

            // ACT
            var actualSchematic = CampaignMapView.isSectorMapWithStarscapeOff(reachRecord.getReporter());

            // ASSERT
            assertThat(actualSchematic)
                .isFalse();
        }

        @Test
        void isFalseWhenTheStarscapeFilterIsOn() {

            // ARRANGE
            stubUiDataWithStarscape(true, buildHyperspaceLocation());

            // ACT
            var actualSchematic = CampaignMapView.isSectorMapWithStarscapeOff(reachRecord.getReporter());

            // ASSERT
            assertThat(actualSchematic)
                .isFalse();
        }

        @Test
        void isTrueOnTheSectorSubViewWithStarscapeOff() {

            // ARRANGE
            stubUiDataWithStarscape(false, buildHyperspaceLocation());

            // ACT
            var actualSchematic = CampaignMapView.isSectorMapWithStarscapeOff(reachRecord.getReporter());

            // ASSERT
            assertThat(actualSchematic)
                .isTrue();
        }

        @Test
        void treatsANullMapLocationAsTheSectorSubView() {

            // ARRANGE
            stubUiDataWithStarscape(false, null);

            // ACT
            var actualSchematic = CampaignMapView.isSectorMapWithStarscapeOff(reachRecord.getReporter());

            // ASSERT
            assertThat(actualSchematic)
                .isTrue();
        }

        @Test
        void isFalseWhenTheSubViewIsAStarSystem() {

            // ARRANGE
            stubUiDataWithStarscape(false, buildSystemLocation());

            // ACT
            var actualSchematic = CampaignMapView.isSectorMapWithStarscapeOff(reachRecord.getReporter());

            // ASSERT
            assertThat(actualSchematic)
                .isFalse();
        }

        // A UI-data object of any other type carries no readable sub-view or filter
        // state, so the gate fails closed.
        @Test
        void isFalseWhenTheUiDataIsNotTheGameConcreteType() {

            // ARRANGE
            when(sectorMock.getUIData())
                .thenReturn(mock(PersistentUIDataAPI.class));

            // ACT
            var actualSchematic = CampaignMapView.isSectorMapWithStarscapeOff(reachRecord.getReporter());

            // ASSERT
            assertThat(actualSchematic)
                .isFalse();
        }

        @Test
        void isFalseWhenTheUiDataNoLongerLinks() {

            // ARRANGE
            stubUiDataFailingToLink();

            // ACT
            var actualSchematic = CampaignMapView.isSectorMapWithStarscapeOff(reachRecord.getReporter());

            // ASSERT
            assertThat(actualSchematic)
                .isFalse();
        }
    }

    @Nested
    class ClassifySectorMapState {

        @Test
        void isNotShowingOffTheMapTabWhateverTheSignalsSay() {

            // ARRANGE
            var uiSignals = new CampaignMapView.MapUiSignals(null, true);

            // ACT
            var actualState = CampaignMapView.classifySectorMapState(CoreUITabId.FLEET, uiSignals);

            // ASSERT
            assertThat(actualState)
                .isEqualTo(SectorMapState.NOT_SHOWING);
        }

        @Test
        void isNotShowingWhereTheSignalsCannotBeRead() {

            // ARRANGE
            CampaignMapView.MapUiSignals unreadableSignals = null;

            // ACT
            var actualState = CampaignMapView.classifySectorMapState(CoreUITabId.MAP, unreadableSignals);

            // ASSERT
            assertThat(actualState)
                .isEqualTo(SectorMapState.NOT_SHOWING);
        }

        // The map is on screen and the sub-view is known; only the filter is not, so the
        // showing read holds while neither mode read does.
        @Test
        void isShowingWithAnUnreadableFilterWhereTheUiDataAnswersNoFilter() {

            // ARRANGE
            var uiSignals = new CampaignMapView.MapUiSignals(null, null);

            // ACT
            var actualState = CampaignMapView.classifySectorMapState(CoreUITabId.MAP, uiSignals);

            // ASSERT
            assertThat(actualState)
                .isEqualTo(SectorMapState.SHOWING_WITH_UNREADABLE_FILTER);
        }
    }

    @Nested
    class DescribeViewState {

        @Test
        void reportsAMissingSector() {

            // ARRANGE
            globalMock
                .when(Global::getSector)
                .thenReturn(null);

            // ACT
            var actualLine = CampaignMapView.describeViewState();

            // ASSERT
            assertThat(actualLine)
                .isEqualTo("no sector");
        }

        @Test
        void reportsAMissingCampaignUi() {

            // ARRANGE
            when(sectorMock.getCampaignUI())
                .thenReturn(null);

            // ACT
            var actualLine = CampaignMapView.describeViewState();

            // ASSERT
            assertThat(actualLine)
                .isEqualTo("no campaign UI");
        }

        @Test
        void composesTheSignalsAndTheStateTheyClassifyTo() {

            // ARRANGE
            stubUiDataWithStarscape(true, buildHyperspaceLocation());

            var expectedLine = "tab=MAP starscape=true mapLocation=hyperspace state=SHOWING_IN_STARSCAPE_MODE";

            // ACT
            var actualLine = CampaignMapView.describeViewState();

            // ASSERT
            assertThat(actualLine)
                .isEqualTo(expectedLine);
        }

        // A UI-data object of another type makes the filter unreadable; the missing
        // location folds into the same "null" print as a map that was never opened.
        @Test
        void reportsUnreadableSignalsAndANeverOpenedMap() {

            // ARRANGE
            when(sectorMock.getUIData())
                .thenReturn(mock(PersistentUIDataAPI.class));

            var expectedLine = "tab=MAP starscape=unreadable mapLocation=null state=NOT_SHOWING";

            // ACT
            var actualLine = CampaignMapView.describeViewState();

            // ASSERT
            assertThat(actualLine)
                .isEqualTo(expectedLine);
        }

        @Test
        void reportsUnreadableSignalsWhenTheUiDataNoLongerLinks() {

            // ARRANGE
            stubUiDataFailingToLink();

            var expectedLine = "tab=MAP starscape=unreadable mapLocation=null state=NOT_SHOWING";

            // ACT
            var actualLine = CampaignMapView.describeViewState();

            // ASSERT
            assertThat(actualLine)
                .isEqualTo(expectedLine);
        }

        // The pairing that makes the line diagnostic: the filter reads off, yet the state is
        // not showing, so the sub-view is visibly what closed the gate.
        @Test
        void printsASystemLocationByIdBesideTheStateItClosed() {

            // ARRANGE
            var locationMock = mock(LocationAPI.class);

            when(locationMock.isHyperspace())
                .thenReturn(false);
            when(locationMock.getId())
                .thenReturn("system_corvus");

            stubUiDataWithStarscape(false, locationMock);

            var expectedLine = "tab=MAP starscape=false mapLocation=system_corvus state=NOT_SHOWING";

            // ACT
            var actualLine = CampaignMapView.describeViewState();

            // ASSERT
            assertThat(actualLine)
                .isEqualTo(expectedLine);
        }
    }

    // Shapes a real campaign-UI-data object: the owned filter object is live and mutable
    // (there is no filter setter to stub), and the location goes through the real setter,
    // whose null default doubles as the never-opened-map state. Named for the flag it carries
    // so each call site says which filter position it is setting up.
    private void stubUiDataWithStarscape(boolean isStarscapeOn, LocationAPI mapLocation) {

        var uiData = new CampaignUIPersistentData();

        uiData.getMapFilterData().starscape = isStarscapeOn;
        uiData.setCampaignMapLocation(mapLocation);

        when(sectorMock.getUIData())
            .thenReturn(uiData);
    }

    // The concrete class cannot be stood in for (see the class doc), so the failure is raised by
    // the UI-data read that opens the same boundary - the first step of the read that a changed
    // class breaks, and inside the catch that has to contain it.
    private void stubUiDataFailingToLink() {

        when(sectorMock.getUIData())
            .thenThrow(new NoSuchFieldError("starscape"));
    }

    private LocationAPI buildHyperspaceLocation() {

        var locationMock = mock(LocationAPI.class);

        when(locationMock.isHyperspace())
            .thenReturn(true);

        return locationMock;
    }

    private LocationAPI buildSystemLocation() {

        var locationMock = mock(LocationAPI.class);

        when(locationMock.isHyperspace())
            .thenReturn(false);

        return locationMock;
    }
}
