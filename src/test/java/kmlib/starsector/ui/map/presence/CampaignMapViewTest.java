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
 * The concrete UI-data object is the real
 * class, constructed directly: its field initializers are pure data, the live filter object
 * it owns is mutable, and the location has a real setter. Mocking or subclassing it is not
 * an option - anything that reflects over its method table (Mockito instrumentation, JUnit
 * discovery of a nested subclass) drags in obfuscated-jar types whose dotted member names
 * only pass under the game's non-verifying JVM.
 */
class CampaignMapViewTest {

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

            globalMock
                .when(Global::getSector)
                .thenReturn(null);

            assertThat(CampaignMapView.isSectorMapShowing(reachRecord.getReporter()))
                .isFalse();
        }

        @Test
        void isFalseWhenTheCampaignUiIsMissing() {

            when(sectorMock.getCampaignUI())
                .thenReturn(null);

            assertThat(CampaignMapView.isSectorMapShowing(reachRecord.getReporter()))
                .isFalse();
        }

        @Test
        void isFalseWhenTheActiveTabIsNotTheMap() {

            when(campaignUiMock.getCurrentCoreTab())
                .thenReturn(CoreUITabId.FLEET);

            assertThat(CampaignMapView.isSectorMapShowing(reachRecord.getReporter()))
                .isFalse();
        }

        @Test
        void isFalseWhileADialogHandsOutAClosedMapScreensCoreUi() {
            // The map screen opened from an interaction and closed again: that dialog's core UI is
            // faded out and taken off screen, and goes on naming the map tab it was showing. Read
            // raw, this reports the map for the rest of the docked visit and every overlay gated on
            // it draws over the dialog.
            when(campaignUiMock.getCurrentInteractionDialog())
                .thenReturn(CoreHostingDialogFake
                    .createHosting(CoreUiFake.createDismissed(new CoreUiComponentFake())));

            stubUiDataWithStarscape(false, buildHyperspaceLocation());

            assertThat(CampaignMapView.isSectorMapShowing(reachRecord.getReporter()))
                .isFalse();
        }

        @Test
        void isTrueWhileADialogsMapScreenIsStillShowing() {
            // The other side of it, and why the correction is about the core UI being down rather
            // than about a dialog being up: the map is routinely opened from an interaction, and it
            // is as much on screen there as one opened from game space.
            when(campaignUiMock.getCurrentInteractionDialog())
                .thenReturn(CoreHostingDialogFake
                    .createHosting(new CoreUiFake(new CoreUiComponentFake())));

            stubUiDataWithStarscape(false, buildHyperspaceLocation());

            assertThat(CampaignMapView.isSectorMapShowing(reachRecord.getReporter()))
                .isTrue();
        }

        @Test
        void isTrueWithTheStarscapeFilterOff() {

            stubUiDataWithStarscape(false, buildHyperspaceLocation());

            assertThat(CampaignMapView.isSectorMapShowing(reachRecord.getReporter()))
                .isTrue();
        }

        @Test
        void isTrueWithTheStarscapeFilterOn() {
            // The read deliberately ignores the filter: the map is on screen either way, which is
            // what separates this from the mode reads.
            stubUiDataWithStarscape(true, buildHyperspaceLocation());

            assertThat(CampaignMapView.isSectorMapShowing(reachRecord.getReporter()))
                .isTrue();
        }

        @Test
        void treatsANullMapLocationAsTheSectorSubView() {

            stubUiDataWithStarscape(false, null);

            assertThat(CampaignMapView.isSectorMapShowing(reachRecord.getReporter()))
                .isTrue();
        }

        @Test
        void isFalseWhenTheSubViewIsAStarSystem() {

            stubUiDataWithStarscape(false, buildSystemLocation());

            assertThat(CampaignMapView.isSectorMapShowing(reachRecord.getReporter()))
                .isFalse();
        }

        @Test
        void isFalseWhenTheUiDataIsNotTheGameConcreteType() {

            when(sectorMock.getUIData())
                .thenReturn(mock(PersistentUIDataAPI.class));

            assertThat(CampaignMapView.isSectorMapShowing(reachRecord.getReporter()))
                .isFalse();
        }

        @Test
        void isFalseWhenTheUiDataNoLongerLinks() {
            // A game build that changed the concrete class fails on the read itself, from render
            // passes with no catch of their own - so it has to come back as the map not showing
            // rather than end the frame.
            stubUiDataFailingToLink();

            assertThat(CampaignMapView.isSectorMapShowing(reachRecord.getReporter()))
                .isFalse();
        }

        @Test
        void filesUiDataOfAnotherTypeAsABrokenReach() {

            when(sectorMock.getUIData())
                .thenReturn(mock(PersistentUIDataAPI.class));

            CampaignMapView.isSectorMapShowing(reachRecord.getReporter());

            assertThat(reachRecord.takeReportedFailure().breakage().failureSite())
                .isEqualTo("reading the sector map's view state");
        }

        @Test
        void filesUiDataThatNoLongerLinksWithWhatWasThrown() {

            stubUiDataFailingToLink();

            CampaignMapView.isSectorMapShowing(reachRecord.getReporter());

            assertThat(reachRecord.takeReportedFailure().cause())
                .isInstanceOf(NoSuchFieldError.class);
        }

        @Test
        void filesNothingOffTheMapTab() {
            // The concrete read is not taken off the map tab at all, so UI data it could not read
            // there is never met.
            when(campaignUiMock.getCurrentCoreTab())
                .thenReturn(CoreUITabId.FLEET);
            when(sectorMock.getUIData())
                .thenReturn(mock(PersistentUIDataAPI.class));

            CampaignMapView.isSectorMapShowing(reachRecord.getReporter());

            assertThat(reachRecord.hasReported())
                .isFalse();
        }

        @Test
        void filesNothingForASystemSubView() {
            // Not showing, and ordinarily so: a map on a star system is a map read correctly.
            stubUiDataWithStarscape(false, buildSystemLocation());

            CampaignMapView.isSectorMapShowing(reachRecord.getReporter());

            assertThat(reachRecord.hasReported())
                .isFalse();
        }
    }

    @Nested
    class IsSectorMapInStarscapeMode {

        @Test
        void isFalseWhenTheSectorIsMissing() {

            globalMock
                .when(Global::getSector)
                .thenReturn(null);

            assertThat(CampaignMapView.isSectorMapInStarscapeMode(reachRecord.getReporter()))
                .isFalse();
        }

        @Test
        void isFalseWhenTheActiveTabIsNotTheMap() {

            when(campaignUiMock.getCurrentCoreTab())
                .thenReturn(CoreUITabId.FLEET);

            assertThat(CampaignMapView.isSectorMapInStarscapeMode(reachRecord.getReporter()))
                .isFalse();
        }

        @Test
        void isTrueOnTheSectorSubViewWithStarscapeOn() {

            stubUiDataWithStarscape(true, buildHyperspaceLocation());

            assertThat(CampaignMapView.isSectorMapInStarscapeMode(reachRecord.getReporter()))
                .isTrue();
        }

        @Test
        void isFalseWhenTheStarscapeFilterIsOff() {

            stubUiDataWithStarscape(false, buildHyperspaceLocation());

            assertThat(CampaignMapView.isSectorMapInStarscapeMode(reachRecord.getReporter()))
                .isFalse();
        }

        @Test
        void treatsANullMapLocationAsTheSectorSubView() {

            stubUiDataWithStarscape(true, null);

            assertThat(CampaignMapView.isSectorMapInStarscapeMode(reachRecord.getReporter()))
                .isTrue();
        }

        @Test
        void isFalseWhenTheSubViewIsAStarSystem() {
            // A star system drawn with the filter on is not the sector map, so the mode read
            // declines it exactly as the showing read does.
            stubUiDataWithStarscape(true, buildSystemLocation());

            assertThat(CampaignMapView.isSectorMapInStarscapeMode(reachRecord.getReporter()))
                .isFalse();
        }

        @Test
        void isFalseWhenTheUiDataIsNotTheGameConcreteType() {
            // An unreadable filter is neither on nor off, so this read fails closed too.
            when(sectorMock.getUIData()).thenReturn(mock(PersistentUIDataAPI.class));

            assertThat(CampaignMapView.isSectorMapInStarscapeMode(reachRecord.getReporter()))
                .isFalse();
        }

        @Test
        void isFalseWhenTheUiDataNoLongerLinks() {

            stubUiDataFailingToLink();

            assertThat(CampaignMapView.isSectorMapInStarscapeMode(reachRecord.getReporter()))
                .isFalse();
        }
    }

    @Nested
    class IsSectorMapWithStarscapeOff {

        @Test
        void isFalseWhenTheSectorIsMissing() {

            globalMock
                .when(Global::getSector)
                .thenReturn(null);

            assertThat(CampaignMapView.isSectorMapWithStarscapeOff(reachRecord.getReporter()))
                .isFalse();
        }

        @Test
        void isFalseWhenTheCampaignUiIsMissing() {

            when(sectorMock.getCampaignUI())
                .thenReturn(null);

            assertThat(CampaignMapView.isSectorMapWithStarscapeOff(reachRecord.getReporter()))
                .isFalse();
        }

        @Test
        void isFalseWhenTheActiveTabIsNotTheMap() {

            when(campaignUiMock.getCurrentCoreTab())
                .thenReturn(CoreUITabId.FLEET);

            assertThat(CampaignMapView.isSectorMapWithStarscapeOff(reachRecord.getReporter()))
                .isFalse();
        }

        @Test
        void isFalseWhenTheStarscapeFilterIsOn() {

            stubUiDataWithStarscape(true, buildHyperspaceLocation());

            assertThat(CampaignMapView.isSectorMapWithStarscapeOff(reachRecord.getReporter()))
                .isFalse();
        }

        @Test
        void isTrueOnTheSectorSubViewWithStarscapeOff() {

            stubUiDataWithStarscape(false, buildHyperspaceLocation());

            assertThat(CampaignMapView.isSectorMapWithStarscapeOff(reachRecord.getReporter()))
                .isTrue();
        }

        @Test
        void treatsANullMapLocationAsTheSectorSubView() {

            stubUiDataWithStarscape(false, null);

            assertThat(CampaignMapView.isSectorMapWithStarscapeOff(reachRecord.getReporter()))
                .isTrue();
        }

        @Test
        void isFalseWhenTheSubViewIsAStarSystem() {

            stubUiDataWithStarscape(false, buildSystemLocation());

            assertThat(CampaignMapView.isSectorMapWithStarscapeOff(reachRecord.getReporter()))
                .isFalse();
        }

        @Test
        void isFalseWhenTheUiDataIsNotTheGameConcreteType() {
            // A UI-data object of any other type carries no readable sub-view or filter
            // state, so the gate fails closed.
            when(sectorMock.getUIData())
                .thenReturn(mock(PersistentUIDataAPI.class));

            assertThat(CampaignMapView.isSectorMapWithStarscapeOff(reachRecord.getReporter()))
                .isFalse();
        }

        @Test
        void isFalseWhenTheUiDataNoLongerLinks() {

            stubUiDataFailingToLink();

            assertThat(CampaignMapView.isSectorMapWithStarscapeOff(reachRecord.getReporter()))
                .isFalse();
        }
    }

    @Nested
    class ClassifySectorMapState {

        @Test
        void isNotShowingOffTheMapTabWhateverTheSignalsSay() {

            var uiSignals = new CampaignMapView.MapUiSignals(null, true);

            assertThat(CampaignMapView.classifySectorMapState(CoreUITabId.FLEET, uiSignals))
                .isEqualTo(SectorMapState.NOT_SHOWING);
        }

        @Test
        void isNotShowingWhereTheSignalsCannotBeRead() {

            assertThat(CampaignMapView.classifySectorMapState(CoreUITabId.MAP, null))
                .isEqualTo(SectorMapState.NOT_SHOWING);
        }

        @Test
        void isShowingWithAnUnreadableFilterWhereTheUiDataAnswersNoFilter() {
            // The map is on screen and the sub-view is known; only the filter is not, so the
            // showing read holds while neither mode read does.
            var uiSignals = new CampaignMapView.MapUiSignals(null, null);

            assertThat(CampaignMapView.classifySectorMapState(CoreUITabId.MAP, uiSignals))
                .isEqualTo(SectorMapState.SHOWING_WITH_UNREADABLE_FILTER);
        }
    }

    @Nested
    class DescribeViewState {
        @Test
        void reportsAMissingSector() {

            globalMock
                .when(Global::getSector)
                .thenReturn(null);

            assertThat(CampaignMapView.describeViewState())
                .isEqualTo("no sector");
        }

        @Test
        void reportsAMissingCampaignUi() {

            when(sectorMock.getCampaignUI())
                .thenReturn(null);

            assertThat(CampaignMapView.describeViewState())
                .isEqualTo("no campaign UI");
        }

        @Test
        void composesTheSignalsAndTheStateTheyClassifyTo() {

            stubUiDataWithStarscape(true, buildHyperspaceLocation());

            assertThat(CampaignMapView.describeViewState())
                .isEqualTo(
                    "tab=MAP starscape=true mapLocation=hyperspace"
                        + " state=SHOWING_IN_STARSCAPE_MODE");
        }

        @Test
        void reportsUnreadableSignalsAndANeverOpenedMap() {
            // A UI-data object of another type makes the filter unreadable; the missing
            // location folds into the same "null" print as a map that was never opened.
            when(sectorMock.getUIData())
                .thenReturn(mock(PersistentUIDataAPI.class));

            assertThat(CampaignMapView.describeViewState())
                .isEqualTo("tab=MAP starscape=unreadable mapLocation=null state=NOT_SHOWING");
        }

        @Test
        void reportsUnreadableSignalsWhenTheUiDataNoLongerLinks() {

            stubUiDataFailingToLink();

            assertThat(CampaignMapView.describeViewState())
                .isEqualTo("tab=MAP starscape=unreadable mapLocation=null state=NOT_SHOWING");
        }

        @Test
        void printsASystemLocationByIdBesideTheStateItClosed() {
            // The pairing that makes the line diagnostic: the filter reads off, yet the state is
            // not showing, so the sub-view is visibly what closed the gate.
            var locationMock = mock(LocationAPI.class);

            when(locationMock.isHyperspace())
                .thenReturn(false);
            when(locationMock.getId())
                .thenReturn("system_corvus");

            stubUiDataWithStarscape(false, locationMock);

            assertThat(CampaignMapView.describeViewState())
                .isEqualTo(
                    "tab=MAP starscape=false mapLocation=system_corvus state=NOT_SHOWING");
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
