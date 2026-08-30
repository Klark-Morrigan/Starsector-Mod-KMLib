package kmlib.starsector.ui.coreui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.SectorAPI;

import kmlib.testfixtures.starsector.ui.coreui.CoreHostingDialogFake;
import kmlib.testfixtures.starsector.ui.coreui.CoreUiComponentFake;
import kmlib.testfixtures.starsector.ui.coreui.ModalDialogFake;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Pins what a caller standing an overlay down for a modal depends on, none of it visible from the
 * rule's own shape.
 *
 * <p>That the root is never tested, which is the one that would break silently and constantly: the
 * core UI panel carries the same marker its dialogs do, so a walk including it answers yes on every
 * frame there is a core UI at all - and a caller of this would then never draw on any screen. The
 * fixture standing for that shape is local, a parent carrying the marker being what the core UI is
 * rather than a shape a consuming mod stands up.
 *
 * <p>That a dismissed dialog stops counting while it is still a child. The fade outlives the
 * player's answer, so an overlay gated on presence alone would stay down for those frames after the
 * screen underneath has already gone back to taking input.
 *
 * <p>That every way the live read can fail lands on "no modal", which is what the callers are
 * written against: each of them only ever stands something down on this answer, so an unreadable
 * tree has to leave them drawing rather than take them off a screen the player is looking at.
 *
 * <p>And that a malformed children list does not raise. The list comes back verbatim from whatever
 * the game's own panel answered, and this runs from a render pass every frame, so it is the one
 * thing here that must survive a shape nobody designed.
 */
class CoreUiDialogViewTest {

    @Nested
    class IsModalDialogShowingUnder {

        @Test
        void isModalDialogShowingUnderIsTrueForACoreUiHoldingAModal() {

            var coreUiFake = new CoreUiComponentFake(new ModalDialogFake());

            assertThat(CoreUiDialogView.isModalDialogShowingUnder(coreUiFake))
                .isTrue();
        }

        @Test
        void isModalDialogShowingUnderFindsAModalAmongOrdinarySiblings() {
            // The ordinary shape, and not a contrived one: showing a modal adds two children, the
            // dialog and the event interceptor laid over the screen beneath it, to whatever the
            // core UI was already holding. Only one of the three carries the marker.
            var coreUiFake = new CoreUiComponentFake(
                new CoreUiComponentFake(),
                new ModalDialogFake(),
                new CoreUiComponentFake());

            assertThat(CoreUiDialogView.isModalDialogShowingUnder(coreUiFake))
                .isTrue();
        }

        @Test
        void isModalDialogShowingUnderIsFalseForACoreUiHoldingOnlyOrdinaryComponents() {

            var coreUiFake = new CoreUiComponentFake(
                new CoreUiComponentFake(),
                new CoreUiComponentFake());

            assertThat(CoreUiDialogView.isModalDialogShowingUnder(coreUiFake))
                .isFalse();
        }

        @Test
        void isModalDialogShowingUnderIsFalseForAModalThatHasFadedOut() {
            // A dialog stays a child until its fade finishes, so presence alone would keep an
            // overlay down for frames after the screen underneath has resumed taking input.
            var coreUiFake = new CoreUiComponentFake(ModalDialogFake.createDismissed());

            assertThat(CoreUiDialogView.isModalDialogShowingUnder(coreUiFake))
                .isFalse();
        }

        @Test
        void isModalDialogShowingUnderIsFalseForACoreUiCarryingTheMarkerItself() {
            // The load-bearing case. The core UI panel descends from the same base its dialogs do
            // and so carries the marker, which makes "is this a modal?" true of the root on every
            // frame. Tested against the children alone, a root holding none answers no.
            var coreUiFake = new MarkerCarryingParentFake(new CoreUiComponentFake());

            assertThat(CoreUiDialogView.isModalDialogShowingUnder(coreUiFake))
                .isFalse();
        }

        @Test
        void isModalDialogShowingUnderLooksPastANullChild() {
            // The children list is handed back verbatim from whatever the game's own panel
            // answered, so a null in it takes the read down rather than being ruled out - and a
            // read that runs every frame from a render pass cannot be the thing that raises.
            assertThat(CoreUiDialogView.isModalDialogShowingUnder(new NullHoldingParentFake()))
                .isTrue();
        }

        @Test
        void isModalDialogShowingUnderIsFalseForACoreUiHoldingNothing() {

            assertThat(CoreUiDialogView.isModalDialogShowingUnder(new CoreUiComponentFake()))
                .isFalse();
        }

        @Test
        void isModalDialogShowingUnderIsFalseForAShapeThatListsNoChildren() {
            // A core UI that answers no children at all is a leaf to the reach, which is a shape
            // this must survive rather than a state it can rule out.
            assertThat(CoreUiDialogView.isModalDialogShowingUnder(new Object()))
                .isFalse();
        }

        @Test
        void isModalDialogShowingUnderIsFalseWhenThereIsNoCoreUi() {

            assertThat(CoreUiDialogView.isModalDialogShowingUnder(null))
                .isFalse();
        }
    }

    @Nested
    class IsModalDialogShowing {

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
        void isModalDialogShowingIsTrueForAModalOverTheCoreUiInForce() {

            var campaignUiMock = mock(CampaignUIAPI.class);

            when(campaignUiMock.getCurrentInteractionDialog())
                .thenReturn(CoreHostingDialogFake
                    .createHosting(new CoreUiComponentFake(new ModalDialogFake())));
            when(sectorMock.getCampaignUI())
                .thenReturn(campaignUiMock);

            assertThat(CoreUiDialogView.isModalDialogShowing())
                .isTrue();
        }

        @Test
        void isModalDialogShowingIsFalseBeforeThereIsASector() {

            globalMock
                .when(Global::getSector)
                .thenReturn(null);

            assertThat(CoreUiDialogView.isModalDialogShowing())
                .isFalse();
        }

        @Test
        void isModalDialogShowingIsFalseBeforeThereIsACampaignUi() {

            when(sectorMock.getCampaignUI())
                .thenReturn(null);

            assertThat(CoreUiDialogView.isModalDialogShowing())
                .isFalse();
        }

        @Test
        void isModalDialogShowingIsFalseWhenTheReachIntoTheCoreUiFails() {
            // The reach raises rather than answering null when a hop is absent outright, leaving the
            // policy to each caller. This one's is to report no modal, so an overlay gated on it
            // keeps drawing on a build whose core UI this can no longer walk.
            when(sectorMock.getCampaignUI())
                .thenReturn(mock(CampaignUIAPI.class));

            assertThat(CoreUiDialogView.isModalDialogShowing())
                .isFalse();
        }
    }

    // Stands for the core UI panel itself: a parent that carries the modal marker, which is the one
    // shape the rule has to look past. Local rather than a shipped fixture because a consuming mod
    // stands up dialogs and components, never a second core UI root.
    public static final class MarkerCarryingParentFake {

        private final List<Object> children;

        MarkerCarryingParentFake(Object... children) {
            this.children = List.of(children);
        }

        public float getBackgroundDimAmount() {
            return 0f;
        }

        public List<Object> getChildrenCopy() {
            return children;
        }
    }

    // Stands for a children list holding a null. Built rather than composed from the shipped
    // component fixture, whose list refuses nulls - which is the point: the shape is one only the
    // game can produce, so a test for it has to be assembled deliberately.
    public static final class NullHoldingParentFake {

        public List<Object> getChildrenCopy() {
            return Arrays.asList(null, new ModalDialogFake());
        }
    }
}
