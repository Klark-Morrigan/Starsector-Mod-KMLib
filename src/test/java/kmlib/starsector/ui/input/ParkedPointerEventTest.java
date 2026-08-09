package kmlib.starsector.ui.input;

import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.input.InputEventClass;
import com.fs.starfarer.api.input.InputEventType;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the two claims a parked event makes: it is somewhere no widget is, and it is otherwise the event it
 * stands in for. Both matter to the screen it is handed to - the position is what makes every control drop
 * its hover and none take one, and everything else being the truth is what stops a listener reading a
 * modifier or a button off a fabrication.
 */
final class ParkedPointerEventTest {

    // A position on screen the real event carries, so a parked answer cannot coincide with it.
    private static final int REAL_X = 640;
    private static final int REAL_Y = 480;

    // What "off every widget" means here: UI coordinates run up and right from the bottom-left corner, so
    // anything negative is contained by nothing.
    private static final int OFF_EVERY_WIDGET = 0;

    private static InputEventAPI createRealEventMock() {

        var eventMock = Mockito.mock(InputEventAPI.class);

        Mockito
            .when(eventMock.getX())
            .thenReturn(REAL_X);
        Mockito
            .when(eventMock.getY())
            .thenReturn(REAL_Y);

        return eventMock;
    }

    @Nested
    class GetX {

        @Test
        void ParkedPointerEvent_getX_answersOffEveryWidgetRatherThanWhereThePointerIs() {
            // The whole of what this type changes: a screen hit-testing this finds nothing under it, so a
            // control lit a moment ago lets go and whatever sits behind the panel does not light up.
            assertThat(new ParkedPointerEvent(createRealEventMock()).getX())
                .isLessThan(OFF_EVERY_WIDGET);
        }
    }

    @Nested
    class GetY {

        @Test
        void ParkedPointerEvent_getY_answersOffEveryWidgetRatherThanWhereThePointerIs() {
            assertThat(new ParkedPointerEvent(createRealEventMock()).getY())
                .isLessThan(OFF_EVERY_WIDGET);
        }
    }

    @Nested
    class IsMouseMoveEvent {

        @Test
        void ParkedPointerEvent_isMouseMoveEvent_answersFromTheEventItStandsInFor() {
            // A screen decides what to do with an event by its kind, so the kind has to survive the swap:
            // parked as a move, it is read as a move and updates hover, which is the point of handing it on.
            var eventMock = createRealEventMock();

            Mockito
                .when(eventMock.isMouseMoveEvent())
                .thenReturn(true);

            assertThat(new ParkedPointerEvent(eventMock).isMouseMoveEvent())
                .isTrue();
        }
    }

    @Nested
    class GetEventClass {

        @Test
        void ParkedPointerEvent_getEventClass_answersFromTheEventItStandsInFor() {
            var eventMock = createRealEventMock();

            Mockito
                .when(eventMock.getEventClass())
                .thenReturn(InputEventClass.MOUSE_EVENT);
            Mockito
                .when(eventMock.getEventType())
                .thenReturn(InputEventType.MOUSE_MOVE);

            var parked = new ParkedPointerEvent(eventMock);

            assertThat(parked.getEventClass())
                .isEqualTo(InputEventClass.MOUSE_EVENT);
            assertThat(parked.getEventType())
                .isEqualTo(InputEventType.MOUSE_MOVE);
        }
    }

    @Nested
    class IsConsumed {

        @Test
        void ParkedPointerEvent_isConsumed_startsUnclaimedEvenWhereTheRealEventWasClaimed() {
            // It is handed on precisely because the original was claimed, so carrying that claim across
            // would leave the screen with nothing again - which is the state this exists to end.
            var eventMock = createRealEventMock();

            Mockito
                .when(eventMock.isConsumed())
                .thenReturn(true);

            assertThat(new ParkedPointerEvent(eventMock).isConsumed())
                .isFalse();
        }

        @Test
        void ParkedPointerEvent_isConsumed_holdsAClaimMadeOnTheStandInItself() {
            // Whoever takes this one still has to be able to claim it, or two listeners downstream would
            // both act on the same move.
            var parked = new ParkedPointerEvent(createRealEventMock());

            parked.consume();

            assertThat(parked.isConsumed())
                .isTrue();
        }

        @Test
        void ParkedPointerEvent_consume_leavesTheRealEventAlone() {
            // The original is out of the list by now; claiming this one must not reach back into it, so a
            // caller still holding the real event sees the state it left it in.
            var eventMock = createRealEventMock();

            new ParkedPointerEvent(eventMock).consume();

            Mockito
                .verify(eventMock, Mockito.never())
                .consume();
        }
    }
}
