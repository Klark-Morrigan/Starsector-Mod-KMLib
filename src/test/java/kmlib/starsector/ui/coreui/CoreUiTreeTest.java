package kmlib.starsector.ui.coreui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.SectorAPI;

import kmlib.testfixtures.starsector.ui.coreui.CoreUiComponentFake;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

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
    }
}
