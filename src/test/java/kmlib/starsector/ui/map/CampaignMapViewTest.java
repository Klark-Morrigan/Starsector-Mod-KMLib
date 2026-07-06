package kmlib.starsector.ui.map;

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
 * Pins the sector-map gate across each way it can fail closed - no sector, no campaign UI, a
 * non-map tab, the Starscape filter on, a star-system sub-view, or UI-data that is not the
 * game's concrete campaign-UI-data class - and the one way it opens: the
 * sector sub-view of the map tab with Starscape off. The concrete UI-data object is the real
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
            stubUiData(true, hyperspaceLocation());

            assertThat(CampaignMapView.isSectorMapWithStarscapeOff()).isFalse();
        }

        @Test
        void isTrueOnTheSectorSubViewWithStarscapeOff() {
            stubUiData(false, hyperspaceLocation());

            assertThat(CampaignMapView.isSectorMapWithStarscapeOff()).isTrue();
        }

        @Test
        void treatsANullMapLocationAsTheSectorSubView() {
            stubUiData(false, null);

            assertThat(CampaignMapView.isSectorMapWithStarscapeOff()).isTrue();
        }

        @Test
        void isFalseWhenTheSubViewIsAStarSystem() {
            var systemLocationMock = mock(LocationAPI.class);
            when(systemLocationMock.isHyperspace()).thenReturn(false);
            stubUiData(false, systemLocationMock);

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
        void composesTheThreeSignals() {
            var locationMock = mock(LocationAPI.class);
            when(locationMock.isHyperspace()).thenReturn(true);
            stubUiData(true, locationMock);

            assertThat(CampaignMapView.describeViewState())
                    .isEqualTo("tab=MAP starscape=true mapLocation=hyperspace");
        }

        @Test
        void reportsUnreadableSignalsAndANeverOpenedMap() {
            // A UI-data object of another type makes the filter unreadable; the missing
            // location folds into the same "null" print as a map that was never opened.
            when(sectorMock.getUIData()).thenReturn(mock(PersistentUIDataAPI.class));

            assertThat(CampaignMapView.describeViewState())
                    .isEqualTo("tab=MAP starscape=unreadable mapLocation=null");
        }

        @Test
        void printsASystemLocationById() {
            var locationMock = mock(LocationAPI.class);
            when(locationMock.isHyperspace()).thenReturn(false);
            when(locationMock.getId()).thenReturn("system_corvus");
            stubUiData(false, locationMock);

            assertThat(CampaignMapView.describeViewState())
                    .isEqualTo("tab=MAP starscape=false mapLocation=system_corvus");
        }
    }

    // Shapes a real campaign-UI-data object: the owned filter object is live and mutable
    // (there is no filter setter to stub), and the location goes through the real setter,
    // whose null default doubles as the never-opened-map state.
    private void stubUiData(boolean isStarscapeOn, LocationAPI mapLocation) {
        var uiData = new CampaignUIPersistentData();
        uiData.getMapFilterData().starscape = isStarscapeOn;
        uiData.setCampaignMapLocation(mapLocation);
        when(sectorMock.getUIData()).thenReturn(uiData);
    }

    private LocationAPI hyperspaceLocation() {
        var locationMock = mock(LocationAPI.class);
        when(locationMock.isHyperspace()).thenReturn(true);
        return locationMock;
    }
}
