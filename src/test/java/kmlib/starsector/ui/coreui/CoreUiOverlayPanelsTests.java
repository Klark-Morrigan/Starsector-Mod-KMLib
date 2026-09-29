package kmlib.starsector.ui.coreui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;

import kmlib.testfixtures.starsector.StubbedGlobalLogger;
import kmlib.testfixtures.starsector.compatibility.GameReachRecordFixture;
import kmlib.testfixtures.starsector.ui.coreui.CoreHostingDialogFake;
import kmlib.testfixtures.starsector.ui.coreui.CoreUiComponentFake;
import kmlib.testfixtures.starsector.ui.coreui.CoreUiFake;
import kmlib.testfixtures.starsector.ui.coreui.CoreUiPanelFake;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins what a caller raising a panel of its own over a core screen depends on, none of it visible from
 * the rule's own shape.
 *
 * <p>That the panel is raised above its siblings and not merely added. The screen's own widgets were
 * added first, so a panel left where the child list put it draws under the screen it was opened over -
 * which looks, from the code, exactly like a panel that attached correctly.
 *
 * <p>That every way the reach can come up empty answers "not attached" rather than raising. The caller
 * is a dialog that either stood up or did not, and a throw out of an attach would leave it holding a
 * panel it believes is on screen.
 *
 * <p>And that removal is quiet about finding nothing. A core UI is rebuilt as the player moves between
 * screens, so a dialog closing after that discards a panel whose parent is already gone - the ordinary
 * case rather than an error.
 */
class CoreUiOverlayPanelsTests {

    @Nested
    class AttachOverlayPanel {

        private final GameReachRecordFixture reachRecord = new GameReachRecordFixture();

        private final UIComponentAPI overlayPanelMock = mock(UIComponentAPI.class);

        private MockedStatic<Global> globalMock;
        private CampaignUIAPI campaignUiMock;

        @BeforeEach
        void setUp() {
            var sectorMock = mock(SectorAPI.class);
            campaignUiMock = mock(CampaignUIAPI.class);

            globalMock = StubbedGlobalLogger.openGlobalAnsweringLoggers();
            globalMock
                .when(Global::getSector)
                .thenReturn(sectorMock);

            when(sectorMock.getCampaignUI())
                .thenReturn(campaignUiMock);
        }

        @AfterEach
        void tearDown() {
            globalMock.close();
        }

        @Test
        void answersNothingAndFilesACoreUiThatIsNotAPanel() {
            // A core UI that no longer answers the published panel interface is a build that
            // reworked it, never an ordinary screen.
            when(campaignUiMock.getCurrentInteractionDialog())
                .thenReturn(CoreHostingDialogFake.createHosting(new CoreUiFake(new CoreUiComponentFake())));

            assertThat(CoreUiOverlayPanels.attachOverlayPanel(overlayPanelMock, reachRecord.getReporter()))
                .isNull();
            assertThat(reachRecord.takeReportedFailure().breakage().brokenDetail())
                .startsWith("the core UI, a ");
        }

        @Test
        void answersNothingAndFilesAReachThatFails() {
            // A campaign UI offering none of the hops to its core, as a renamed accessor leaves it.
            assertThat(CoreUiOverlayPanels.attachOverlayPanel(overlayPanelMock, reachRecord.getReporter()))
                .isNull();
            assertThat(reachRecord.takeReportedFailure().cause())
                .isNotNull();
        }

        @Test
        void filesNothingBeforeThereIsACampaignUi() {
            // Between screens there is no core UI, and a caller asking then has only asked early.
            globalMock
                .when(Global::getSector)
                .thenReturn(null);

            CoreUiOverlayPanels.attachOverlayPanel(overlayPanelMock, reachRecord.getReporter());

            assertThat(reachRecord.hasReported())
                .isFalse();
        }
    }

    @Nested
    class AttachOverlayPanelTo {

        @Test
        void addsThePanelToTheCoreUi() {

            var coreUiFake = new CoreUiPanelFake();
            var overlayPanelMock = mock(UIComponentAPI.class);

            CoreUiOverlayPanels.attachOverlayPanelTo(coreUiFake, overlayPanelMock);

            assertThat(coreUiFake.getStandingComponents())
                .containsExactly(overlayPanelMock);
        }

        @Test
        void raisesThePanelAboveTheScreensOwnWidgets() {

            var coreUiFake = new CoreUiPanelFake();
            var overlayPanelMock = mock(UIComponentAPI.class);

            CoreUiOverlayPanels.attachOverlayPanelTo(coreUiFake, overlayPanelMock);

            assertThat(coreUiFake.getRaisedComponents())
                .containsExactly(overlayPanelMock);
        }

        @Test
        void answersThePlacementTheCoreUiGaveThePanel() {

            var coreUiFake = new CoreUiPanelFake();
            var overlayPanelMock = mock(UIComponentAPI.class);

            var placement = CoreUiOverlayPanels.attachOverlayPanelTo(coreUiFake, overlayPanelMock);

            assertThat(placement).isNotNull();
        }

        @Test
        void answersNothingWithNoCoreUiInForce() {

            var overlayPanelMock = mock(UIComponentAPI.class);

            assertThat(CoreUiOverlayPanels.attachOverlayPanelTo(null, overlayPanelMock))
                .isNull();
        }

        @Test
        void answersNothingForACoreUiThatTakesNoChildren() {
            // The shape a game build that stopped answering the published panel interface would leave,
            // which has to read as "no room for a dialog" rather than as a cast that throws.
            var coreUiFake = new CoreUiComponentFake();
            var overlayPanelMock = mock(UIComponentAPI.class);

            assertThat(CoreUiOverlayPanels.attachOverlayPanelTo(coreUiFake, overlayPanelMock))
                .isNull();
        }

        @Test
        void addsNothingForAPanelTheCallerNeverBuilt() {

            var coreUiFake = new CoreUiPanelFake();

            assertThat(CoreUiOverlayPanels.attachOverlayPanelTo(coreUiFake, null))
                .isNull();
            assertThat(coreUiFake.getAddedComponents())
                .isEmpty();
        }
    }

    @Nested
    class DetachOverlayPanelFrom {

        @Test
        void takesThePanelOffTheCoreUi() {

            var coreUiFake = new CoreUiPanelFake();
            var overlayPanelMock = mock(UIComponentAPI.class);
            CoreUiOverlayPanels.attachOverlayPanelTo(coreUiFake, overlayPanelMock);

            CoreUiOverlayPanels.detachOverlayPanelFrom(coreUiFake, overlayPanelMock);

            assertThat(coreUiFake.getStandingComponents())
                .isEmpty();
        }

        @Test
        void leavesACoreUiThatTakesNoChildrenAlone() {

            var coreUiFake = new CoreUiComponentFake();
            var overlayPanelMock = mock(UIComponentAPI.class);

            CoreUiOverlayPanels.detachOverlayPanelFrom(coreUiFake, overlayPanelMock);

            assertThat(coreUiFake.countChildrenReads()).isZero();
        }

        @Test
        void removesNothingWithNoCoreUiInForce() {
            // The rebuilt-root case: whatever discarded the old core UI discarded the panel with it.
            var overlayPanelMock = mock(UIComponentAPI.class);

            CoreUiOverlayPanels.detachOverlayPanelFrom(null, overlayPanelMock);
        }

        @Test
        void removesNothingForAPanelTheCallerNeverHeld() {

            var coreUiFake = new CoreUiPanelFake();

            CoreUiOverlayPanels.detachOverlayPanelFrom(coreUiFake, null);

            assertThat(coreUiFake.getRemovedComponents()).isEmpty();
        }
    }
}
