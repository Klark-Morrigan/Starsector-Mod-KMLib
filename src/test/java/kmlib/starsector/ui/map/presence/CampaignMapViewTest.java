package kmlib.starsector.ui.map.presence;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PersistentUIDataAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.campaign.CampaignUIPersistentData;

import org.apache.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Pins the sector-map gates across each way they can fail closed - no sector, no campaign UI, a
 * non-map tab, a star-system sub-view, or UI-data that is not the game's concrete
 * campaign-UI-data class - and how each weighs the Starscape filter: the "is showing" read
 * ignores it, while the two mode reads split the sector sub-view of the map tab between them.
 * The concrete UI-data object is the real
 * class, constructed directly: its field initializers are pure data, the live filter object
 * it owns is mutable, and the location has a real setter. Mocking or subclassing it is not
 * an option - anything that reflects over its method table (Mockito instrumentation, JUnit
 * discovery of a nested subclass) drags in obfuscated-jar types whose dotted member names
 * only pass under the game's non-verifying JVM.
 */
class CampaignMapViewTest {

    private MockedStatic<Global> globalMock;
    private SectorAPI sectorMock;
    private CampaignUIAPI campaignUiMock;

    @BeforeEach
    void setUp() {
        sectorMock = mock(SectorAPI.class);
        campaignUiMock = mock(CampaignUIAPI.class);
        globalMock = mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sectorMock);
        // The class logs a one-shot warning on an unexpected UI-data type; give it a logger
        // so that static field init and that warn path do not dereference null under the mock.
        globalMock.when(() -> Global.getLogger(any(Class.class))).thenReturn(mock(Logger.class));
        when(sectorMock.getCampaignUI()).thenReturn(campaignUiMock);
        // The map tab is the active core tab by default; each test relaxes one condition.
        when(campaignUiMock.getCurrentCoreTab()).thenReturn(CoreUITabId.MAP);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    @Nested
    class IsSectorMapShowing {
        @Test
        void isFalseWhenTheSectorIsMissing() {
            globalMock.when(Global::getSector).thenReturn(null);

            assertThat(CampaignMapView.isSectorMapShowing()).isFalse();
        }

        @Test
        void isFalseWhenTheCampaignUiIsMissing() {
            when(sectorMock.getCampaignUI()).thenReturn(null);

            assertThat(CampaignMapView.isSectorMapShowing()).isFalse();
        }

        @Test
        void isFalseWhenTheActiveTabIsNotTheMap() {
            when(campaignUiMock.getCurrentCoreTab()).thenReturn(CoreUITabId.FLEET);

            assertThat(CampaignMapView.isSectorMapShowing()).isFalse();
        }

        @Test
        void isTrueWithTheStarscapeFilterOff() {
            stubUiDataWithStarscape(false, buildHyperspaceLocation());

            assertThat(CampaignMapView.isSectorMapShowing()).isTrue();
        }

        @Test
        void isTrueWithTheStarscapeFilterOn() {
            // The read deliberately ignores the filter: the map is on screen either way, which is
            // what separates this from the mode reads.
            stubUiDataWithStarscape(true, buildHyperspaceLocation());

            assertThat(CampaignMapView.isSectorMapShowing()).isTrue();
        }

        @Test
        void treatsANullMapLocationAsTheSectorSubView() {
            stubUiDataWithStarscape(false, null);

            assertThat(CampaignMapView.isSectorMapShowing()).isTrue();
        }

        @Test
        void isFalseWhenTheSubViewIsAStarSystem() {
            stubUiDataWithStarscape(false, buildSystemLocation());

            assertThat(CampaignMapView.isSectorMapShowing()).isFalse();
        }

        @Test
        void isFalseWhenTheUiDataIsNotTheGameConcreteType() {
            when(sectorMock.getUIData()).thenReturn(mock(PersistentUIDataAPI.class));

            assertThat(CampaignMapView.isSectorMapShowing()).isFalse();
        }
    }

    @Nested
    class IsSectorMapInStarscapeMode {
        @Test
        void isFalseWhenTheSectorIsMissing() {
            globalMock.when(Global::getSector).thenReturn(null);

            assertThat(CampaignMapView.isSectorMapInStarscapeMode()).isFalse();
        }

        @Test
        void isFalseWhenTheActiveTabIsNotTheMap() {
            when(campaignUiMock.getCurrentCoreTab()).thenReturn(CoreUITabId.FLEET);

            assertThat(CampaignMapView.isSectorMapInStarscapeMode()).isFalse();
        }

        @Test
        void isTrueOnTheSectorSubViewWithStarscapeOn() {
            stubUiDataWithStarscape(true, buildHyperspaceLocation());

            assertThat(CampaignMapView.isSectorMapInStarscapeMode()).isTrue();
        }

        @Test
        void isFalseWhenTheStarscapeFilterIsOff() {
            stubUiDataWithStarscape(false, buildHyperspaceLocation());

            assertThat(CampaignMapView.isSectorMapInStarscapeMode()).isFalse();
        }

        @Test
        void treatsANullMapLocationAsTheSectorSubView() {
            stubUiDataWithStarscape(true, null);

            assertThat(CampaignMapView.isSectorMapInStarscapeMode()).isTrue();
        }

        @Test
        void isFalseWhenTheSubViewIsAStarSystem() {
            // A star system drawn with the filter on is not the sector map, so the mode read
            // declines it exactly as the showing read does.
            stubUiDataWithStarscape(true, buildSystemLocation());

            assertThat(CampaignMapView.isSectorMapInStarscapeMode()).isFalse();
        }

        @Test
        void isFalseWhenTheUiDataIsNotTheGameConcreteType() {
            // An unreadable filter is neither on nor off, so this read fails closed too.
            when(sectorMock.getUIData()).thenReturn(mock(PersistentUIDataAPI.class));

            assertThat(CampaignMapView.isSectorMapInStarscapeMode()).isFalse();
        }
    }

    @Nested
    class IsSectorMapWithStarscapeOff {
        @Test
        void isFalseWhenTheSectorIsMissing() {
            globalMock.when(Global::getSector).thenReturn(null);

            assertThat(CampaignMapView.isSectorMapWithStarscapeOff()).isFalse();
        }

        @Test
        void isFalseWhenTheCampaignUiIsMissing() {
            when(sectorMock.getCampaignUI()).thenReturn(null);

            assertThat(CampaignMapView.isSectorMapWithStarscapeOff()).isFalse();
        }

        @Test
        void isFalseWhenTheActiveTabIsNotTheMap() {
            when(campaignUiMock.getCurrentCoreTab()).thenReturn(CoreUITabId.FLEET);

            assertThat(CampaignMapView.isSectorMapWithStarscapeOff()).isFalse();
        }

        @Test
        void isFalseWhenTheStarscapeFilterIsOn() {
            stubUiDataWithStarscape(true, buildHyperspaceLocation());

            assertThat(CampaignMapView.isSectorMapWithStarscapeOff()).isFalse();
        }

        @Test
        void isTrueOnTheSectorSubViewWithStarscapeOff() {
            stubUiDataWithStarscape(false, buildHyperspaceLocation());

            assertThat(CampaignMapView.isSectorMapWithStarscapeOff()).isTrue();
        }

        @Test
        void treatsANullMapLocationAsTheSectorSubView() {
            stubUiDataWithStarscape(false, null);

            assertThat(CampaignMapView.isSectorMapWithStarscapeOff()).isTrue();
        }

        @Test
        void isFalseWhenTheSubViewIsAStarSystem() {
            stubUiDataWithStarscape(false, buildSystemLocation());

            assertThat(CampaignMapView.isSectorMapWithStarscapeOff()).isFalse();
        }

        @Test
        void isFalseWhenTheUiDataIsNotTheGameConcreteType() {
            // A UI-data object of any other type carries no readable sub-view or filter
            // state, so the gate fails closed.
            when(sectorMock.getUIData()).thenReturn(mock(PersistentUIDataAPI.class));

            assertThat(CampaignMapView.isSectorMapWithStarscapeOff()).isFalse();
        }
    }

    @Nested
    class DescribeViewState {
        @Test
        void reportsAMissingSector() {
            globalMock.when(Global::getSector).thenReturn(null);

            assertThat(CampaignMapView.describeViewState()).isEqualTo("no sector");
        }

        @Test
        void reportsAMissingCampaignUi() {
            when(sectorMock.getCampaignUI()).thenReturn(null);

            assertThat(CampaignMapView.describeViewState()).isEqualTo("no campaign UI");
        }

        @Test
        void composesTheSignalsAndTheStateTheyClassifyTo() {
            stubUiDataWithStarscape(true, buildHyperspaceLocation());

            assertThat(CampaignMapView.describeViewState()).isEqualTo(
                "tab=MAP starscape=true mapLocation=hyperspace"
                    + " state=SHOWING_IN_STARSCAPE_MODE");
        }

        @Test
        void reportsUnreadableSignalsAndANeverOpenedMap() {
            // A UI-data object of another type makes the filter unreadable; the missing
            // location folds into the same "null" print as a map that was never opened.
            when(sectorMock.getUIData()).thenReturn(mock(PersistentUIDataAPI.class));

            assertThat(CampaignMapView.describeViewState())
                .isEqualTo("tab=MAP starscape=unreadable mapLocation=null state=NOT_SHOWING");
        }

        @Test
        void printsASystemLocationByIdBesideTheStateItClosed() {
            // The pairing that makes the line diagnostic: the filter reads off, yet the state is
            // not showing, so the sub-view is visibly what closed the gate.
            var locationMock = mock(LocationAPI.class);
            when(locationMock.isHyperspace()).thenReturn(false);
            when(locationMock.getId()).thenReturn("system_corvus");
            stubUiDataWithStarscape(false, locationMock);

            assertThat(CampaignMapView.describeViewState()).isEqualTo(
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
        when(sectorMock.getUIData()).thenReturn(uiData);
    }

    private LocationAPI buildHyperspaceLocation() {
        var locationMock = mock(LocationAPI.class);
        when(locationMock.isHyperspace()).thenReturn(true);
        return locationMock;
    }

    private LocationAPI buildSystemLocation() {
        var locationMock = mock(LocationAPI.class);
        when(locationMock.isHyperspace()).thenReturn(false);
        return locationMock;
    }
}
