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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Pins the by-name reach's one judgement, and the two halves of the contract every caller of it
 * writes its failure handling against.
 *
 * <p>The judgement is that a component exposing no {@code getChildrenCopy} is a leaf rather than a
 * read failure. Every walk built on this class descends through that call, so were it to report a
 * failure instead, each of them would abort at the first leaf it met - and since they all swallow
 * read failures by design, they would abort silently.
 *
 * <p>The contract is the opposite half: a hop that is genuinely absent throws rather than answering
 * null, which is what lets a caller tell a broken reach from an empty screen and apply its own
 * policy to each. Both are reachable here because neither needs a live widget tree - only a
 * component that answers the method name, which is what the fixture is.
 *
 * <p>Its two halves are named by type rather than asserted loosely, because the types are what a
 * guard is written against and neither is what a reader would assume: an unreachable hop raises an
 * unchecked argument failure, and a hop that resolves and then throws arrives wrapped in a *checked*
 * exception from methods that declare none. A guard shaped for either one alone lets the other past.
 *
 * <p>Alongside them, the one thing about the argument-taking invoke a caller cannot see from its
 * signature: a boxed argument resolves the primitive parameter, which is what makes the core UI's
 * {@code (float)} entry points reachable at all from mod code that can only hand over a
 * {@code Float}.
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
    class InvokeWithArgs {

        @Test
        void invokeWithArgsPassesABoxedFloatToAPrimitiveParameter() {
            // The load-bearing case: the core UI's draw and input entry points declare primitive
            // float, and a caller can only hand in the boxed one. Were the argument's own class
            // used as the parameter type, no such method would ever be found.
            var targetFake = new ArgumentTakingTargetFake();

            CoreUiTree.invokeWithArgs(targetFake, "recordAlpha", 0.75f);

            assertThat(targetFake.readRecordedAlpha())
                .isEqualTo(0.75f);
        }

        @Test
        void invokeWithArgsReturnsWhatTheNamedMethodAnswers() {

            assertThat(CoreUiTree
                .invokeWithArgs(new ArgumentTakingTargetFake(), "recordAlpha", 0.75f))
                .isEqualTo("recorded");
        }

        @Test
        void invokeWithArgsThrowsWhenNoMethodTakesThatArgumentShape() {
            // The arguments select the overload, so a name that exists but takes something else is
            // as unreachable as one that does not exist at all. Both are raised rather than
            // answered null, so a caller drawing through this class learns it drew nothing.
            //
            // Named exactly, because the type is what a caller distinguishes on: an unreachable hop
            // and a hop that resolves and then throws (below) arrive as different types, and that
            // is the only thing separating "the reach is broken" from "the target refused".
            assertThatThrownBy(() -> CoreUiTree
                .invokeWithArgs(new ArgumentTakingTargetFake(), "recordAlpha", "not a float"))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void invokeWithArgsSurfacesTheTargetsOwnFailureWrapped() {
            // The other half of the failure contract, and the one a caller sees at runtime rather
            // than during development: a hop that resolves and then throws comes back as the
            // reflection wrapper, not as what the target actually threw. A caller logging the
            // caught throwable has to unwrap to say anything useful about why a draw failed.
            assertThatThrownBy(() -> CoreUiTree
                .invokeWithArgs(new ArgumentTakingTargetFake(), "refuseAlpha", 0.75f))
                .isInstanceOf(InvocationTargetException.class)
                .cause()
                .isInstanceOf(IllegalStateException.class);
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

    // Stands for a core-UI component's argument-taking entry point, of which render(float) is the
    // one this class exists to reach. Primitive float rather than boxed on purpose: resolving it
    // from a Float is the whole behaviour under test, and a Float parameter would pass either way.
    //
    // Local rather than a shipped fixture because nothing outside this test needs it - a consuming
    // mod invoking with arguments does so against a real widget, not against a shape KMLib made up.
    // Public because that is the shape a real core-UI widget presents. The reach makes the resolved
    // member accessible before calling it, so visibility is not what the resolution turns on.
    public static final class ArgumentTakingTargetFake {
        
        private float recordedAlpha = Float.NaN;

        public float readRecordedAlpha() {
            return recordedAlpha;
        }

        // Answers a value so the return path is observable as well as the argument path.
        public String recordAlpha(float alphaMult) {
            recordedAlpha = alphaMult;
            return "recorded";
        }

        // A hop that resolves and then fails, which is how a live component's draw misbehaves -
        // distinct from a hop that was never reachable, and surfacing differently.
        public String refuseAlpha(float alphaMult) {
            throw new IllegalStateException("A target that refuses the call.");
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
