package kmlib.starsector.ui.coreui;

import com.fs.starfarer.api.ui.UIComponentAPI;

import kmlib.testfixtures.starsector.ui.coreui.CoreUiComponentFake;
import kmlib.testfixtures.starsector.ui.coreui.CoreUiPanelFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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
class CoreUiOverlayPanelsTest {

    @Nested
    class AttachOverlayPanelTo {

        @Test
        void attachOverlayPanelToAddsThePanelToTheCoreUi() {

            var coreUiFake = new CoreUiPanelFake();
            var overlayPanelMock = mock(UIComponentAPI.class);

            CoreUiOverlayPanels.attachOverlayPanelTo(coreUiFake, overlayPanelMock);

            assertThat(coreUiFake.getStandingComponents())
                .containsExactly(overlayPanelMock);
        }

        @Test
        void attachOverlayPanelToRaisesThePanelAboveTheScreensOwnWidgets() {

            var coreUiFake = new CoreUiPanelFake();
            var overlayPanelMock = mock(UIComponentAPI.class);

            CoreUiOverlayPanels.attachOverlayPanelTo(coreUiFake, overlayPanelMock);

            assertThat(coreUiFake.getRaisedComponents())
                .containsExactly(overlayPanelMock);
        }

        @Test
        void attachOverlayPanelToAnswersThePlacementTheCoreUiGaveThePanel() {

            var coreUiFake = new CoreUiPanelFake();
            var overlayPanelMock = mock(UIComponentAPI.class);

            var placement = CoreUiOverlayPanels.attachOverlayPanelTo(coreUiFake, overlayPanelMock);

            assertThat(placement).isNotNull();
        }

        @Test
        void attachOverlayPanelToAnswersNothingWithNoCoreUiInForce() {

            var overlayPanelMock = mock(UIComponentAPI.class);

            assertThat(CoreUiOverlayPanels.attachOverlayPanelTo(null, overlayPanelMock))
                .isNull();
        }

        @Test
        void attachOverlayPanelToAnswersNothingForACoreUiThatTakesNoChildren() {
            // The shape a game build that stopped answering the published panel interface would leave,
            // which has to read as "no room for a dialog" rather than as a cast that throws.
            var coreUiFake = new CoreUiComponentFake();
            var overlayPanelMock = mock(UIComponentAPI.class);

            assertThat(CoreUiOverlayPanels.attachOverlayPanelTo(coreUiFake, overlayPanelMock))
                .isNull();
        }

        @Test
        void attachOverlayPanelToAddsNothingForAPanelTheCallerNeverBuilt() {

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
        void detachOverlayPanelFromTakesThePanelOffTheCoreUi() {

            var coreUiFake = new CoreUiPanelFake();
            var overlayPanelMock = mock(UIComponentAPI.class);
            CoreUiOverlayPanels.attachOverlayPanelTo(coreUiFake, overlayPanelMock);

            CoreUiOverlayPanels.detachOverlayPanelFrom(coreUiFake, overlayPanelMock);

            assertThat(coreUiFake.getStandingComponents())
                .isEmpty();
        }

        @Test
        void detachOverlayPanelFromLeavesACoreUiThatTakesNoChildrenAlone() {

            var coreUiFake = new CoreUiComponentFake();
            var overlayPanelMock = mock(UIComponentAPI.class);

            CoreUiOverlayPanels.detachOverlayPanelFrom(coreUiFake, overlayPanelMock);

            assertThat(coreUiFake.countChildrenReads()).isZero();
        }

        @Test
        void detachOverlayPanelFromRemovesNothingWithNoCoreUiInForce() {
            // The rebuilt-root case: whatever discarded the old core UI discarded the panel with it.
            var overlayPanelMock = mock(UIComponentAPI.class);

            CoreUiOverlayPanels.detachOverlayPanelFrom(null, overlayPanelMock);
        }

        @Test
        void detachOverlayPanelFromRemovesNothingForAPanelTheCallerNeverHeld() {

            var coreUiFake = new CoreUiPanelFake();

            CoreUiOverlayPanels.detachOverlayPanelFrom(coreUiFake, null);

            assertThat(coreUiFake.getRemovedComponents()).isEmpty();
        }
    }
}
