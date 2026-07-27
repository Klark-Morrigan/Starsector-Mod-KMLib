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
 * across each way it fails closed, and {@link VanillaIntelScreenView#isMapVisorLit}, the rule that
 * decides whether the two widget readings amount to a visor worth drawing over.
 *
 * <p>{@link VanillaIntelScreenView#getMapVisorRect} and
 * {@link VanillaIntelScreenView#isMapStarscapeModeOn} are not unit-tested: both cast to the
 * obfuscated intel classes, whose dotted member names fail class-load under a verifying JVM (they
 * only load under the game's non-verifying one), so not even their fail-closed branches can be
 * entered here and the walks are exercised in-game. Only that widget-fetching is out of reach, which
 * is why the rule the visor read applies is reachable on its own.
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

    @Nested
    class IsMapVisorLit {
        // The game snaps both readings, so these are the values the live widgets actually carry.
        private static final float DARK = 0f;
        private static final float LIT = 1f;

        @Test
        void isMapVisorLitIsTrueWhenTheIntelSubtabShowsAndThePreviewIsLit() {
            assertThat(VanillaIntelScreenView.isMapVisorLit(LIT, LIT)).isTrue();
        }

        @Test
        void isMapVisorLitIsFalseWhenASiblingSubtabIsShowing() {
            // The Planets and Factions sub-tabs share the intel tab's container, and switching to one
            // only fades the events panel out - its map widget stays at full opacity behind them. Reading
            // that opacity alone therefore reports a visor that is not on screen, and the sidebar drew
            // over those sub-tabs. The dark panel is what rules them out.
            assertThat(VanillaIntelScreenView.isMapVisorLit(DARK, LIT)).isFalse();
        }

        @Test
        void isMapVisorLitIsFalseWhenThePreviewIsBlanked() {
            // The Intel sub-tab is showing, but a large-description item has blanked the preview, so
            // there is a lit panel with no lit canvas inside it.
            assertThat(VanillaIntelScreenView.isMapVisorLit(LIT, DARK)).isFalse();
        }

        @Test
        void isMapVisorLitIsFalseWhenNeitherReadingIsLit() {
            assertThat(VanillaIntelScreenView.isMapVisorLit(DARK, DARK)).isFalse();
        }

        @Test
        void isMapVisorLitPinsTheThresholdOfEachReading() {
            // Nothing eases these readings today, so a mid value is only reachable if the game changes
            // to fade them. The two thresholds differ - a panel counts as showing from halfway, while
            // the preview has to be all but fully opaque - so each is pinned on its own, keeping a
            // change to either from silently flipping the gate.
            assertThat(VanillaIntelScreenView.isMapVisorLit(0.5f, LIT)).isTrue();
            assertThat(VanillaIntelScreenView.isMapVisorLit(0.49f, LIT)).isFalse();
            assertThat(VanillaIntelScreenView.isMapVisorLit(LIT, 0.9f)).isTrue();
            assertThat(VanillaIntelScreenView.isMapVisorLit(LIT, 0.89f)).isFalse();
        }
    }
}
