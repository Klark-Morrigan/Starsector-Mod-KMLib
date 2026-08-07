package kmlib.starsector.ui.coreui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorAPI;

import kmlib.testfixtures.starsector.ui.coreui.CoreUiComponentFake;
import kmlib.testfixtures.starsector.ui.coreui.CoreUiFake;
import kmlib.testfixtures.starsector.ui.coreui.CoreUiHostFake;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Pins the by-name reach's one judgement and its one contract.
 *
 * <p>The judgement is that a component exposing no {@code getChildrenCopy} is a leaf rather than a
 * read failure. Every walk built on this class descends through that call, so were it to report a
 * failure instead, each of them would abort at the first leaf it met - and since they all swallow
 * read failures by design, they would abort silently.
 *
 * <p>The contract is the opposite half: a hop that is genuinely absent throws rather than answering
 * null, which is what lets a caller tell a broken reach from an empty screen and apply its own
 * policy to each. Both halves are reachable here because neither needs a live widget tree - only a
 * component that answers the method name, which is what the fixture is.
 */
class CoreUiTreeTest {

    @Nested
    class ReadChildrenOf {

        @Test
        void readChildrenOfReturnsWhatAParentHolds() {

            var childFake = new CoreUiComponentFake();
            var parentFake = new CoreUiComponentFake(childFake);

            assertThat(CoreUiTree.readChildrenOf(parentFake))
                .isEqualTo(List.of(childFake));
        }

        @Test
        void readChildrenOfIsEmptyForAComponentThatExposesNoChildren() {
            // The common case: most components are leaves and answer no such method at all. Reading
            // that as a failure would end every walk at the first leaf it reached.
            assertThat(CoreUiTree.readChildrenOf(new Object()))
                .isEmpty();
        }
    }

    @Nested
    class InvokeNoArg {

        @Test
        void invokeNoArgReturnsWhatTheNamedMethodAnswers() {

            var childFake = new CoreUiComponentFake();

            assertThat(CoreUiTree.invokeNoArg(new CoreUiComponentFake(childFake), "getChildrenCopy"))
                .isEqualTo(List.of(childFake));
        }

        @Test
        void invokeNoArgThrowsWhenTheMethodIsAbsent() {
            // Raised rather than swallowed, so a caller taking a hop this class does not name can
            // tell "no such method" apart from "the method answered nothing".
            assertThatThrownBy(() -> CoreUiTree.invokeNoArg(new Object(), "getChildrenCopy"))
                .isInstanceOf(Exception.class);
        }
    }

    @Nested
    class ReadCoreUiOf {

        @Test
        void readCoreUiOfReturnsWhatAHostingDialogStandsUp() {

            var coreFake = new CoreUiComponentFake();

            assertThat(CoreUiTree.readCoreUiOf(new CoreUiHostFake(coreFake)))
                .isSameAs(coreFake);
        }

        @Test
        void readCoreUiOfIsNullForADialogThatHostsNoCoreUi() {
            // A scripted dialog exposes no such method at all. Reading that as a failure would take
            // down the walk for every screen opened while one is up, rather than sending it to the
            // campaign's own core, which is where those screens then live.
            assertThat(CoreUiTree.readCoreUiOf(new Object()))
                .isNull();
        }

        @Test
        void readCoreUiOfIsNullWhenNoDialogIsUp() {
            assertThat(CoreUiTree.readCoreUiOf(null))
                .isNull();
        }
    }

    @Nested
    class ResolveCurrentTab {

        private MockedStatic<Global> globalMock;
        private SectorAPI sectorMock;

        @BeforeEach
        void setUp() {
            sectorMock = mock(SectorAPI.class);
            globalMock = mockStatic(Global.class);
            globalMock
                .when(Global::getSector)
                .thenReturn(sectorMock);
        }

        @AfterEach
        void tearDown() {
            globalMock.close();
        }

        @Test
        void resolveCurrentTabIsNullBeforeThereIsASector() {

            globalMock
                .when(Global::getSector)
                .thenReturn(null);

            assertThat(CoreUiTree.resolveCurrentTab())
                .isNull();
        }

        @Test
        void resolveCurrentTabIsNullBeforeThereIsACampaignUi() {

            when(sectorMock.getCampaignUI())
                .thenReturn(null);

            assertThat(CoreUiTree.resolveCurrentTab())
                .isNull();
        }

        @Test
        void resolveCurrentTabThrowsWhenTheCampaignUiDoesNotAnswerTheCoreHop() {
            // An empty screen answers null; a campaign UI that does not expose the hop at all is a
            // broken reach, and the two are kept apart so a caller can treat them differently.
            when(sectorMock.getCampaignUI())
                .thenReturn(mock(CampaignUIAPI.class));

            assertThatThrownBy(CoreUiTree::resolveCurrentTab)
                .isInstanceOf(Exception.class);
        }

        @Test
        void resolveCurrentTabReadsTheDialogsCoreWhileADialogIsUp() {
            // The screens a dialog opens are drawn from the core it stands up, while the campaign's
            // own goes on holding whatever tab it was left on. Reading the campaign's regardless
            // searches the wrong tree for the whole of a docked visit.
            var tabFake = new CoreUiComponentFake();
            var campaignUiMock = mock(CampaignUIAPI.class);

            when(campaignUiMock.getCurrentInteractionDialog())
                .thenReturn(asCoreHostingDialog(new CoreUiFake(tabFake)));
            when(sectorMock.getCampaignUI())
                .thenReturn(campaignUiMock);

            assertThat(CoreUiTree.resolveCurrentTab())
                .isSameAs(tabFake);
        }

        @Test
        void resolveCurrentTabFallsThroughToTheCampaignsCoreForADialogHostingNone() {
            // A scripted dialog hosts no core UI, so the screens are still the campaign's. Reaching
            // the core hop is what proves the walk carried on rather than stopping at the dialog -
            // the bare campaign UI mock does not expose it, which is the exception raised here.
            var campaignUiMock = mock(CampaignUIAPI.class);

            when(campaignUiMock.getCurrentInteractionDialog())
                .thenReturn(mock(InteractionDialogAPI.class));
            when(sectorMock.getCampaignUI())
                .thenReturn(campaignUiMock);

            assertThatThrownBy(CoreUiTree::resolveCurrentTab)
                .isInstanceOf(Exception.class);
        }
    }

    // Stands for the game's one core-hosting dialog class: an interaction dialog that also answers
    // the accessor the reach takes by name. Public because a proxy is only as visible as the least
    // visible interface it implements, and the reach invokes from its own package.
    public interface CoreHostingDialog {
        Object getCoreUI();
    }

    // The reach receives the dialog as the published type and then hops by name, so exercising it
    // needs an object that is both at once - which no fixture class and no mock can be here, the
    // dialog API being too wide to implement and beyond what the mock maker will extend. A proxy
    // over the two interfaces is the one shape that satisfies both halves.
    private static InteractionDialogAPI asCoreHostingDialog(Object coreUi) {
        return (InteractionDialogAPI) Proxy.newProxyInstance(
            CoreUiTreeTest.class.getClassLoader(),
            new Class<?>[] { InteractionDialogAPI.class, CoreHostingDialog.class },
            (proxy, method, args) ->
                "getCoreUI".equals(method.getName())
                    ? coreUi
                    : null);
    }
}
