package kmlib.starsector.ui.coreui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.SectorAPI;

import kmlib.testfixtures.starsector.StubbedGlobalLogger;
import kmlib.testfixtures.starsector.ui.coreui.CoreHostingDialogFake;
import kmlib.testfixtures.starsector.ui.coreui.FaderFake;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins which of the core UI's children count as a tab still fading out: one whose wrapper has not
 * finished its fade, never one that has, and never the tab that is up, whose own wrapper holds it too.
 *
 * <p>Then the live read once, over a core UI a dialog stands up, to show it takes its children and its
 * current tab off the same core.
 */
class ClosingCoreTabsTests {

    @Nested
    class ReadClosingTabsAmong {

        // The closing tab's wrapper stays a child of the core UI, drawing it, until its fade runs out.
        @Test
        void keepsTheTabOfAWrapperStillFadingOut() {

            // ARRANGE
            var closingTab = new Object();
            var coreChildren = List.of(TabWrapperFake.createFadingOut(closingTab));

            // ACT
            var actualClosingTabs = ClosingCoreTabs.readClosingTabsAmong(coreChildren, null);

            // ASSERT
            assertThat(actualClosingTabs)
                .containsExactly(closingTab);
        }

        // Faded out is gone: the core UI drops the wrapper on its next advance.
        @Test
        void dropsTheTabOfAWrapperFadedOut() {

            // ARRANGE
            var coreChildren = List.of(TabWrapperFake.createFadedOut(new Object()));

            // ACT
            var actualClosingTabs = ClosingCoreTabs.readClosingTabsAmong(coreChildren, null);

            // ASSERT
            assertThat(actualClosingTabs)
                .isEmpty();
        }

        @Test
        void excludesTheCurrentTab() {

            // ARRANGE
            var currentTab = new Object();
            var coreChildren = List.of(TabWrapperFake.createShowing(currentTab));

            // ACT
            var actualClosingTabs = ClosingCoreTabs.readClosingTabsAmong(coreChildren, currentTab);

            // ASSERT
            assertThat(actualClosingTabs)
                .isEmpty();
        }

        // Most of the core UI's children are bars and overlays, holding no tab at all.
        @Test
        void skipsAChildHoldingNoTab() {

            // ARRANGE
            var coreChildren = List.of(new Object());

            // ACT
            var actualClosingTabs = ClosingCoreTabs.readClosingTabsAmong(coreChildren, null);

            // ASSERT
            assertThat(actualClosingTabs)
                .isEmpty();
        }

        // Switching screens closes one tab and opens the next in the same call, so the old one fades
        // out beside the new one.
        @Test
        void keepsAClosingTabBesideTheCurrentOne() {

            // ARRANGE
            var closingTab = new Object();
            var currentTab = new Object();

            var coreChildren = List.of(
                TabWrapperFake.createFadingOut(closingTab),
                TabWrapperFake.createShowing(currentTab));

            // ACT
            var actualClosingTabs = ClosingCoreTabs.readClosingTabsAmong(coreChildren, currentTab);

            // ASSERT
            assertThat(actualClosingTabs)
                .containsExactly(closingTab);
        }
    }

    @Nested
    class ReadClosingTabs {

        private MockedStatic<Global> globalMock;
        private SectorAPI sectorMock;

        @BeforeEach
        void setUp() {

            sectorMock = mock(SectorAPI.class);

            globalMock = StubbedGlobalLogger.openGlobalAnsweringLoggers();
            globalMock
                .when(Global::getSector)
                .thenReturn(sectorMock);
        }

        @AfterEach
        void tearDown() {

            globalMock.close();
        }

        @Test
        void isEmptyBeforeThereIsACampaignUi() {

            // ARRANGE
            when(sectorMock.getCampaignUI())
                .thenReturn(null);

            // ACT
            var actualClosingTabs = ClosingCoreTabs.readClosingTabs();

            // ASSERT
            assertThat(actualClosingTabs)
                .isEmpty();
        }

        @Test
        void readsTheTabsFadingOutOfTheCoreUiInForce() {

            // ARRANGE
            var closingTab = new Object();
            var currentTab = new Object();

            var coreFake = new CoreFake(
                currentTab,
                TabWrapperFake.createFadingOut(closingTab),
                TabWrapperFake.createShowing(currentTab));

            var campaignUiMock = mock(CampaignUIAPI.class);

            when(campaignUiMock.getCurrentInteractionDialog())
                .thenReturn(CoreHostingDialogFake.createHosting(coreFake));

            when(sectorMock.getCampaignUI())
                .thenReturn(campaignUiMock);

            // ACT
            var actualClosingTabs = ClosingCoreTabs.readClosingTabs();

            // ASSERT
            assertThat(actualClosingTabs)
                .containsExactly(closingTab);
        }
    }

    // Stands for the wrapper the core UI puts each tab in: it hands out the tab, and carries the fader
    // the closing fade runs on.
    public static final class TabWrapperFake {

        private final Object panel;
        private final FaderFake faderFake;

        private TabWrapperFake(Object panel, FaderFake faderFake) {

            this.panel = panel;
            this.faderFake = faderFake;
        }

        static TabWrapperFake createShowing(Object panel) {
            return new TabWrapperFake(panel, new FaderFake(false));
        }

        static TabWrapperFake createFadingOut(Object panel) {
            return new TabWrapperFake(panel, FaderFake.createMidFade(0.5f));
        }

        static TabWrapperFake createFadedOut(Object panel) {
            return new TabWrapperFake(panel, new FaderFake(true));
        }

        public Object getPanel() {
            return panel;
        }

        public FaderFake getFader() {
            return faderFake;
        }
    }

    // Stands for a core UI with its children: the shared core fixture names a tab but holds none.
    // Carries no fader, which the reach reads as still on screen.
    public static final class CoreFake {

        private final Object currentTab;
        private final List<Object> children;

        CoreFake(Object currentTab, Object... children) {

            this.currentTab = currentTab;
            this.children = List.of(children);
        }

        public Object getCurrentTab() {
            return currentTab;
        }

        public List<Object> getChildrenCopy() {
            return children;
        }
    }
}
