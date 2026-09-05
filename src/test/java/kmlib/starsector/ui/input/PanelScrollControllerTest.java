package kmlib.starsector.ui.input;

import kmlib.starsector.ui.sound.StarsectorUiSound;
import kmlib.starsector.ui.sound.UiSoundCue;
import kmlib.starsector.ui.sound.UiSoundScheme;
import kmlib.starsector.ui.widgets.scroll.ScrollbarThickness;
import kmlib.testfixtures.starsector.ui.sound.UiSoundPlayerFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static kmlib.starsector.ui.input.PanelBodyFixtures.IN_GRAB_COLUMN_X;
import static kmlib.starsector.ui.input.PanelBodyFixtures.IN_GRAB_COLUMN_Y;
import static kmlib.starsector.ui.input.PanelBodyFixtures.ON_LIST_X;
import static kmlib.starsector.ui.input.PanelBodyFixtures.ON_LIST_Y;
import static kmlib.starsector.ui.input.PanelBodyFixtures.ROW;
import static kmlib.starsector.ui.input.PanelBodyFixtures.SHORT_SCROLL_OVERFLOW;
import static kmlib.starsector.ui.input.PanelBodyFixtures.buildGutteredPlacement;
import static kmlib.starsector.ui.input.PanelBodyFixtures.buildGutteredPlacementAtThickness;
import static kmlib.starsector.ui.input.PanelBodyFixtures.buildScrollingPlacement;
import static kmlib.starsector.ui.input.PanelBodyFixtures.buildScrollingPlacementAtThickness;
import static kmlib.starsector.ui.input.PanelBodyFixtures.buildUnscrollablePlacement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Pins how a panel's list moves under the pointer and what each movement is answered with.
 *
 * <p>The wheel cases pin the one moment this end answers audibly, and the rule that decides it: the sound
 * follows the list having moved rather than the wheel having turned, so a notch against the end of a list is
 * as silent as a notch over a list that fits. The drag cases pin the other half of that rule - a drag moves
 * the list and stays silent, being one held act rather than a moment - and the two gates the acts part on: a
 * drag needs a bar to grab, the wheel needs only somewhere to scroll to, so a bar set away takes the drag and
 * leaves the wheel.
 *
 * <p>The latch cases pin what a per-frame pass reads to tell rows carried under a still pointer from a
 * pointer moving over rows. Pinned here rather than only through a panel that acts on it, since the fault it
 * guards against - an arrival announced for a row nobody reached - shows nowhere else.
 */
final class PanelScrollControllerTest {

    // A look whose press and scroll roles are each the other's, so a moment answered from this end's own
    // code rather than from the look records the wrong sound.
    private static final UiSoundScheme SWAPPED_SOUNDS = new UiSoundScheme(
        UiSoundCue.createAtFullVolume(StarsectorUiSound.LIST_SCROLLED),
        StarsectorUiSound.BUTTON_MOUSEOVER,
        UiSoundCue.createAtFullVolume(StarsectorUiSound.BUTTON_PRESSED));

    // The top of the grab column, which maps to the start of the list - the far end from where a press at
    // IN_GRAB_COLUMN_Y takes it, so a drag followed from one to the other moves the whole overflow.
    private static final float AT_GRAB_COLUMN_TOP_Y = ROW.y() + ROW.height();

    // Where the bar stands on the guttered body at offset 0, from its own geometry. Across: the default
    // track is 3 wide and held 3 off the container's right edge, so it covers x 214..217. Down: a 20-tall
    // track over a list overrunning by 20 gives a thumb floored to its grabbable 12, hung from the track
    // top, so it covers y 208..220 with its centre at 214. A press at (215, 219) is therefore on the thumb,
    // 5 above that centre, and the move to 211 is read as a pointer at 206 - the end of the track's travel,
    // and so the whole overflow. Read without the grab offset that same move leaves the list part-way, which
    // is what the case turns on.
    //
    // The x is named rather than taken from the grab column, and that is the point of it: the column is 20
    // wide and the thumb 3, so a press in the gutter is generally NOT on the thumb, and one that misses it
    // grabs the bare track and records no offset at all.
    private static final float ON_THUMB_X = ROW.x() + ROW.width() - 5f;
    private static final float ON_THUMB_ABOVE_CENTRE_Y = ROW.y() + 19f;
    private static final float BELOW_THUMB_CENTRE_Y = ROW.y() + 11f;

    private final UiSoundPlayerFake soundPlayerFake = new UiSoundPlayerFake();

    @Nested
    class ScrollListUnderPointer {

        @Test
        void scrollListUnderPointerSoundsTheWheelThatMovedTheList() {
            // One act, one sound. The wheel is the panel's answer to the list moving as a whole, which is
            // what lets the rows it carries past the cursor stay quiet.
            var scrolling = buildVanillaSoundingScrolling();

            scrolling.scrollListUnderPointer(
                PointerEventMocks.mockWheelDownAt(ON_LIST_X, ON_LIST_Y),
                buildScrollingPlacement());

            assertThat(soundPlayerFake.getPlayedSounds())
                .containsExactly(StarsectorUiSound.LIST_SCROLLED);
        }

        @Test
        void scrollListUnderPointerSettlesAWheeledOffsetWithinWhatTheListCanScroll() {
            // Settled at the wheel rather than left to the next layout's clamp, which is what makes the
            // silence below real: an unsettled request runs past the end of the list, so every further
            // notch would change a number and read as a list that moved.
            var scrolling = buildVanillaSoundingScrolling();

            scrolling.scrollListUnderPointer(
                PointerEventMocks.mockWheelDownAt(ON_LIST_X, ON_LIST_Y),
                buildScrollingPlacement());

            assertThat(scrolling.getScrollState().getOffset())
                .as("one notch is longer than this list's overflow")
                .isEqualTo(SHORT_SCROLL_OVERFLOW);
        }

        @Test
        void scrollListUnderPointerStaysSilentForAWheelAgainstTheEndOfTheList() {
            // Sounded on the list having moved rather than on the wheel having turned: the panel answers
            // what happened, and at the end of a list nothing did.
            var scrolling = buildVanillaSoundingScrolling();
            var placement = buildScrollingPlacement();

            scrolling.scrollListUnderPointer(PointerEventMocks.mockWheelDownAt(ON_LIST_X, ON_LIST_Y), placement);

            soundPlayerFake.clearPlayedCues();

            scrolling.scrollListUnderPointer(PointerEventMocks.mockWheelDownAt(ON_LIST_X, ON_LIST_Y), placement);

            assertThat(soundPlayerFake.getPlayedSounds())
                .isEmpty();
        }

        @Test
        void scrollListUnderPointerStaysSilentForAWheelOverAListThatFits() {
            // A body with nothing to scroll has nothing to answer: a panel that ticked here would answer
            // every wheel turn the player made over it whether or not it had anything to show for it.
            var scrolling = buildVanillaSoundingScrolling();

            scrolling.scrollListUnderPointer(
                PointerEventMocks.mockWheelDownAt(ON_LIST_X, ON_LIST_Y),
                buildUnscrollablePlacement());

            assertThat(soundPlayerFake.getPlayedSounds())
                .isEmpty();
        }

        @Test
        void scrollListUnderPointerStaysSilentForAWheelOffTheScrollRegion() {
            // The wheel reaches the list only over the list. Off it - over a control pinned above or below
            // the scrolling strip, or over the scrollbar gutter - it moves nothing and has nothing to answer
            // for.
            var scrolling = buildVanillaSoundingScrolling();

            scrolling.scrollListUnderPointer(
                PointerEventMocks.mockWheelDownAt(IN_GRAB_COLUMN_X, IN_GRAB_COLUMN_Y),
                buildGutteredPlacement());

            assertThat(scrolling.getScrollState().getOffset())
                .isZero();
            assertThat(soundPlayerFake.getPlayedSounds())
                .isEmpty();
        }

        @Test
        void scrollListUnderPointerTakesTheScrollRoleFromTheLookRatherThanNamingOne() {
            // The point of the seam, at the one moment this end answers audibly: which sound a wheel makes
            // is the panel's look talking. A scheme agreeing with a hardcoded role would pass whether or
            // not it was ever read.
            var scrolling = buildScrollingSounding(SWAPPED_SOUNDS);

            scrolling.scrollListUnderPointer(
                PointerEventMocks.mockWheelDownAt(ON_LIST_X, ON_LIST_Y),
                buildScrollingPlacement());

            assertThat(soundPlayerFake.getPlayedSounds())
                .containsExactly(StarsectorUiSound.BUTTON_PRESSED);
        }

        @Test
        void scrollListUnderPointerMovesAndSoundsTheListWhenNoBarIsDrawn() {
            // The wheel is what is left to a player who has set the bar away, so it answers to the list
            // overrunning and not to the bar being drawn - taking it with the bar would strand the rows past
            // the viewport with no way to reach them.
            var scrolling = buildVanillaSoundingScrolling();

            scrolling.scrollListUnderPointer(
                PointerEventMocks.mockWheelDownAt(ON_LIST_X, ON_LIST_Y),
                buildScrollingPlacementAtThickness(ScrollbarThickness.NONE));

            assertThat(scrolling.getScrollState().getOffset())
                .isEqualTo(SHORT_SCROLL_OVERFLOW);
            assertThat(soundPlayerFake.getPlayedSounds())
                .containsExactly(StarsectorUiSound.LIST_SCROLLED);
        }
    }

    @Nested
    class BeginThumbDragIfPressed {

        @Test
        void beginThumbDragIfPressedGrabsAPressInTheGrabColumnAndMovesTheList() {
            // A press on the bare track jumps the thumb to the pointer at once, so the press low in the
            // column carries the list to its end on the same event that grabbed it.
            var scrolling = buildVanillaSoundingScrolling();

            var hasGrabbed = scrolling.beginThumbDragIfPressed(
                PointerEventMocks.mockLeftPressAt(IN_GRAB_COLUMN_X, IN_GRAB_COLUMN_Y),
                buildGutteredPlacement());

            assertThat(hasGrabbed)
                .isTrue();
            assertThat(scrolling.getScrollState().getOffset())
                .isEqualTo(SHORT_SCROLL_OVERFLOW);
        }

        @Test
        void beginThumbDragIfPressedStaysSilentForADragThatMovedTheList() {
            // A drag is one held act carrying the list continuously, with the pointer off on the scrollbar
            // rather than on the rows. Sounded per frame it would be exactly the chatter the wheel's single
            // sound exists to avoid, so the moment belongs to the wheel alone.
            var scrolling = buildVanillaSoundingScrolling();

            scrolling.beginThumbDragIfPressed(
                PointerEventMocks.mockLeftPressAt(IN_GRAB_COLUMN_X, IN_GRAB_COLUMN_Y),
                buildGutteredPlacement());

            assertThat(scrolling.getScrollState().getOffset())
                .as("the drag has to have moved the list for the silence to mean anything")
                .isEqualTo(SHORT_SCROLL_OVERFLOW);
            assertThat(soundPlayerFake.getPlayedSounds())
                .isEmpty();
        }

        @Test
        void beginThumbDragIfPressedLeavesAPressOffTheGrabColumnAlone() {
            // A press over the list is a control's to answer, so nothing here takes it.
            var scrolling = buildVanillaSoundingScrolling();

            var hasGrabbed = scrolling.beginThumbDragIfPressed(
                PointerEventMocks.mockLeftPressAt(ON_LIST_X, ON_LIST_Y),
                buildGutteredPlacement());

            assertThat(hasGrabbed)
                .isFalse();
            assertThat(scrolling.getScrollState().getOffset())
                .isZero();
        }

        @Test
        void beginThumbDragIfPressedLeavesAPressInTheGrabColumnAloneWhenNoBarIsDrawn() {
            // The same press on the same geometry with the bar set away: with no track and no thumb there
            // is nothing over that column to grab, so it claims nothing. A bar of no width still taking
            // presses would leave a dead strip down the panel that nothing on screen accounts for.
            var scrolling = buildVanillaSoundingScrolling();

            var hasGrabbed = scrolling.beginThumbDragIfPressed(
                PointerEventMocks.mockLeftPressAt(IN_GRAB_COLUMN_X, IN_GRAB_COLUMN_Y),
                buildGutteredPlacementAtThickness(ScrollbarThickness.NONE));

            assertThat(hasGrabbed)
                .isFalse();
            assertThat(scrolling.getScrollState().getOffset())
                .isZero();
        }

        @Test
        void beginThumbDragIfPressedLeavesAPressAloneWhenTheListFits() {
            // No overflow, no bar, nothing to grab - whatever column the press is in.
            var scrolling = buildVanillaSoundingScrolling();

            var hasGrabbed = scrolling.beginThumbDragIfPressed(
                PointerEventMocks.mockLeftPressAt(IN_GRAB_COLUMN_X, IN_GRAB_COLUMN_Y),
                buildUnscrollablePlacement());

            assertThat(hasGrabbed)
                .isFalse();
        }
    }

    @Nested
    class ContinueDragIfHeld {

        @Test
        void continueDragIfHeldReportsNoDragWhenNoneIsHeld() {
            // Nothing to follow, so the event is not this end's and is left untouched for whatever is.
            var scrolling = buildVanillaSoundingScrolling();
            var moveMock = PointerEventMocks.mockMoveAt(ON_LIST_X, ON_LIST_Y);

            var hasFollowed = scrolling.continueDragIfHeld(moveMock, buildGutteredPlacement());

            assertThat(hasFollowed)
                .isFalse();
            verify(moveMock, never())
                .consume();
        }

        @Test
        void continueDragIfHeldFollowsThePointerWhileTheDragIsHeld() {
            // Grabbed low, which took the list to its end; followed to the top of the column, which maps to
            // the start - so a drag that follows carries the whole overflow back, and one that did not would
            // leave the list where the press put it.
            var scrolling = buildVanillaSoundingScrolling();
            var placement = buildGutteredPlacement();

            scrolling.beginThumbDragIfPressed(
                PointerEventMocks.mockLeftPressAt(IN_GRAB_COLUMN_X, IN_GRAB_COLUMN_Y),
                placement);

            var hasFollowed = scrolling.continueDragIfHeld(
                PointerEventMocks.mockMoveAt(IN_GRAB_COLUMN_X, AT_GRAB_COLUMN_TOP_Y),
                placement);

            assertThat(hasFollowed)
                .isTrue();
            assertThat(scrolling.getScrollState().getOffset())
                .isZero();
        }

        @Test
        void continueDragIfHeldKeepsTheThumbUnderAPointerThatGrabbedItOffCentre() {
            // The grab offset: a thumb pressed above its centre stays that far above the pointer as it is
            // dragged, rather than snapping its centre to the cursor on the first move. The two readings
            // named at the top of the class are the ones that part on it.
            var scrolling = buildVanillaSoundingScrolling();
            var placement = buildGutteredPlacement();

            scrolling.beginThumbDragIfPressed(
                PointerEventMocks.mockLeftPressAt(ON_THUMB_X, ON_THUMB_ABOVE_CENTRE_Y),
                placement);

            assertThat(scrolling.getScrollState().getOffset())
                .as("a press on the thumb grabs it where it is rather than jumping it")
                .isZero();

            scrolling.continueDragIfHeld(
                PointerEventMocks.mockMoveAt(ON_THUMB_X, BELOW_THUMB_CENTRE_Y),
                placement);

            assertThat(scrolling.getScrollState().getOffset())
                .isEqualTo(SHORT_SCROLL_OVERFLOW);
        }

        @Test
        void continueDragIfHeldConsumesTheEventItFollows() {
            // The surface behind must neither pan nor act while the thumb is held, wherever the pointer has
            // wandered to.
            var scrolling = buildVanillaSoundingScrolling();
            var placement = buildGutteredPlacement();
            var moveMock = PointerEventMocks.mockMoveAt(IN_GRAB_COLUMN_X, AT_GRAB_COLUMN_TOP_Y);

            scrolling.beginThumbDragIfPressed(
                PointerEventMocks.mockLeftPressAt(IN_GRAB_COLUMN_X, IN_GRAB_COLUMN_Y),
                placement);
            scrolling.continueDragIfHeld(moveMock, placement);

            verify(moveMock)
                .consume();
        }

        @Test
        void continueDragIfHeldEndsTheDragOnTheRelease() {
            // The release is the drag's, and the last event it takes: the move after it is nobody's here.
            var scrolling = buildVanillaSoundingScrolling();
            var placement = buildGutteredPlacement();

            scrolling.beginThumbDragIfPressed(
                PointerEventMocks.mockLeftPressAt(IN_GRAB_COLUMN_X, IN_GRAB_COLUMN_Y),
                placement);

            var hasTakenRelease = scrolling.continueDragIfHeld(
                PointerEventMocks.mockLeftReleaseAt(IN_GRAB_COLUMN_X, IN_GRAB_COLUMN_Y),
                placement);
            var hasFollowedAfter = scrolling.continueDragIfHeld(
                PointerEventMocks.mockMoveAt(IN_GRAB_COLUMN_X, AT_GRAB_COLUMN_TOP_Y),
                placement);

            assertThat(hasTakenRelease)
                .isTrue();
            assertThat(hasFollowedAfter)
                .isFalse();
        }

        @Test
        void continueDragIfHeldCarriesTheDragWithoutMovingTheListOnceTheBarHasGone() {
            // A drag reads the same question that started it, so a bar taken away under a held thumb stops
            // carrying the list rather than going on following a pointer with nothing under it. The drag is
            // still carried - the event is taken - so the release still ends it where the player let go.
            var scrolling = buildVanillaSoundingScrolling();

            scrolling.beginThumbDragIfPressed(
                PointerEventMocks.mockLeftPressAt(IN_GRAB_COLUMN_X, IN_GRAB_COLUMN_Y),
                buildGutteredPlacement());

            var hasFollowed = scrolling.continueDragIfHeld(
                PointerEventMocks.mockMoveAt(IN_GRAB_COLUMN_X, AT_GRAB_COLUMN_TOP_Y),
                buildGutteredPlacementAtThickness(ScrollbarThickness.NONE));

            assertThat(hasFollowed)
                .isTrue();
            assertThat(scrolling.getScrollState().getOffset())
                .as("the top of the column would have carried the list back to its start")
                .isEqualTo(SHORT_SCROLL_OVERFLOW);
        }
    }

    @Nested
    class CancelDrag {

        @Test
        void cancelDragLetsGoOfAHeldDrag() {
            // For a panel that stops showing: a drag left dangling would take the next session's first move
            // as its own.
            var scrolling = buildVanillaSoundingScrolling();
            var placement = buildGutteredPlacement();

            scrolling.beginThumbDragIfPressed(
                PointerEventMocks.mockLeftPressAt(IN_GRAB_COLUMN_X, IN_GRAB_COLUMN_Y),
                placement);

            scrolling.cancelDrag();

            assertThat(scrolling.continueDragIfHeld(
                    PointerEventMocks.mockMoveAt(IN_GRAB_COLUMN_X, AT_GRAB_COLUMN_TOP_Y),
                    placement))
                .isFalse();
        }
    }

    @Nested
    class TakeHasListScrolledSinceLastFrame {

        private final PanelScrollController scrolling = buildVanillaSoundingScrolling();

        @Test
        void takeHasListScrolledSinceLastFrameReportsAWheelThatMovedTheList() {

            scrolling.scrollListUnderPointer(
                PointerEventMocks.mockWheelDownAt(ON_LIST_X, ON_LIST_Y),
                buildScrollingPlacement());

            assertThat(scrolling.takeHasListScrolledSinceLastFrame())
                .isTrue();
        }

        @Test
        void takeHasListScrolledSinceLastFrameReportsAScrollbarDragThatMovedTheList() {
            // The drag reports through the same latch as the wheel, silent though it is: what the latch
            // answers is that content moved and not what moved it, and rows carried past a cursor parked
            // off on the scrollbar were reached by nobody either way.
            scrolling.beginThumbDragIfPressed(
                PointerEventMocks.mockLeftPressAt(IN_GRAB_COLUMN_X, IN_GRAB_COLUMN_Y),
                buildGutteredPlacement());

            assertThat(scrolling.takeHasListScrolledSinceLastFrame())
                .isTrue();
        }

        @Test
        void takeHasListScrolledSinceLastFrameReportsNothingForAWheelAgainstTheEndOfTheList() {
            // A list already against its stop shows the same rows afterwards, so nothing was carried under
            // the pointer and the frame after it is an ordinary frame.
            var placement = buildScrollingPlacement();

            scrolling.scrollListUnderPointer(PointerEventMocks.mockWheelDownAt(ON_LIST_X, ON_LIST_Y), placement);
            scrolling.takeHasListScrolledSinceLastFrame();
            scrolling.scrollListUnderPointer(PointerEventMocks.mockWheelDownAt(ON_LIST_X, ON_LIST_Y), placement);

            assertThat(scrolling.takeHasListScrolledSinceLastFrame())
                .isFalse();
        }

        @Test
        void takeHasListScrolledSinceLastFrameIsClearedByTheReading() {
            // One movement is answered by the first frame after it and by that frame alone. Left standing,
            // every later frame would adopt whatever is under the cursor and the panel would go permanently
            // deaf to the pointer arriving on anything in its body.
            scrolling.scrollListUnderPointer(
                PointerEventMocks.mockWheelDownAt(ON_LIST_X, ON_LIST_Y),
                buildScrollingPlacement());

            scrolling.takeHasListScrolledSinceLastFrame();

            assertThat(scrolling.takeHasListScrolledSinceLastFrame())
                .isFalse();
        }

        @Test
        void takeHasListScrolledSinceLastFrameReportsNothingOnceTheMovementWasReset() {
            // A movement no frame ever read is a movement the next session must not answer to: the panel
            // stopped showing between the scroll and the frame that would have adopted on it, and the
            // player has been somewhere else since.
            scrolling.scrollListUnderPointer(
                PointerEventMocks.mockWheelDownAt(ON_LIST_X, ON_LIST_Y),
                buildScrollingPlacement());

            scrolling.resetListScrolled();

            assertThat(scrolling.takeHasListScrolledSinceLastFrame())
                .isFalse();
        }
    }

    // A list recording into this class's fake and answering by the engine's own scheme - the look every case
    // not about the scheme itself is written against.
    private PanelScrollController buildVanillaSoundingScrolling() {
        return buildScrollingSounding(UiSoundScheme.createVanillaSoundScheme());
    }

    // The same, by whichever scheme the case is about.
    private PanelScrollController buildScrollingSounding(UiSoundScheme soundScheme) {
        return new PanelScrollController(new PanelSounds(soundPlayerFake, soundScheme));
    }
}
