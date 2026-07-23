package kmlib.starsector.ui.intel;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.SectorAPI;

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
 * Pins {@link VanillaIntelScreenView#isIntelTabOpen} - the one method that reads published API -
 * across each way it fails closed. {@link VanillaIntelScreenView#getVisorRect} is not unit-tested:
 * reaching it casts to the obfuscated intel classes, whose dotted member names fail class-load under
 * a verifying JVM (they only load under the game's non-verifying one), so even its fail-closed
 * branch cannot be entered here and it is exercised in-game.
 */
class VanillaIntelScreenViewTest {

    private MockedStatic<Global> globalMock;
    private SectorAPI sectorMock;
    private CampaignUIAPI campaignUiMock;

    @BeforeEach
    void setUp() {
        sectorMock = mock(SectorAPI.class);
        campaignUiMock = mock(CampaignUIAPI.class);
        globalMock = mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sectorMock);
        // The class logs a one-shot warning on an unexpected campaign-UI type; give it a logger so
        // that static field init and that warn path do not dereference null under the mock.
        globalMock.when(() -> Global.getLogger(any(Class.class))).thenReturn(mock(Logger.class));
        when(sectorMock.getCampaignUI()).thenReturn(campaignUiMock);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    @Nested
    class IsIntelTabOpen {
        @Test
        void isTrueWhenTheIntelTabIsActive() {
            when(campaignUiMock.getCurrentCoreTab()).thenReturn(CoreUITabId.INTEL);

            assertThat(new VanillaIntelScreenView().isIntelTabOpen()).isTrue();
        }

        @Test
        void isFalseWhenAnotherTabIsActive() {
            when(campaignUiMock.getCurrentCoreTab()).thenReturn(CoreUITabId.MAP);

            assertThat(new VanillaIntelScreenView().isIntelTabOpen()).isFalse();
        }

        @Test
        void isFalseWhenTheSectorIsMissing() {
            globalMock.when(Global::getSector).thenReturn(null);

            assertThat(new VanillaIntelScreenView().isIntelTabOpen()).isFalse();
        }

        @Test
        void isFalseWhenTheCampaignUiIsMissing() {
            when(sectorMock.getCampaignUI()).thenReturn(null);

            assertThat(new VanillaIntelScreenView().isIntelTabOpen()).isFalse();
        }
    }
}
