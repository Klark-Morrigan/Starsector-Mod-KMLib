package kmlib.starsector.ui.input;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins how a panel takes an event for itself, which differs by the kind of event and matters to the
 * screen the panel is drawn over.
 *
 * <p>A move is taken by moving the pointer away rather than by consuming, because a consumed event is
 * invisible to the screen and a vanilla control only lets go of its hover on hearing a move that is not
 * on it. A press is taken by consuming, because the panel acting on it is precisely the reason the
 * screen must not. And a game that no longer offers the setters this depends on has to come back to
 * consuming rather than throwing, which is the case the mock covers - it carries the interface and none
 * of the shape the reach looks for, exactly like an event whose setters have gone.
 */
final class PointerParkingTest {

    // Where the player put the pointer, and near enough the origin that a parked answer cannot coincide.
    private static final int REAL_X = 640;
    private static final int REAL_Y = 480;

    // Far enough out that no widget could contain it, whatever the screen resolution.
    private static final int FAR_OFF_EVERY_WIDGET = 100000;

    @Nested
    class ClaimEvent {

        @Test
        void PointerParking_claimEvent_parksThePointerOfAMoveRatherThanConsumingIt() {
            // The whole point. Left unconsumed, the screen underneath still processes the move; moved away,
            // what it concludes from it is that the pointer is on nothing of its own - so a control lit
            // before the pointer crossed onto the panel lets go, and nothing behind the panel takes its
            // place.
            var eventFake = RelocatableEventFake.createMoveAt(REAL_X, REAL_Y);

            PointerParking.claimEvent(eventFake);

            assertThat(eventFake.isConsumed())
                .isFalse();
            assertThat(eventFake.getX())
                .isGreaterThan(FAR_OFF_EVERY_WIDGET);
            assertThat(eventFake.getY())
                .isGreaterThan(FAR_OFF_EVERY_WIDGET);
        }

        @Test
        void PointerParking_claimEvent_consumesAPressRatherThanParkingIt() {
            // A press is an act, and the panel answering it is the reason the screen must not also answer
            // it. Moving the pointer would let the press through to whatever now sits under it.
            var eventFake = RelocatableEventFake.createLeftPressAt(REAL_X, REAL_Y);

            PointerParking.claimEvent(eventFake);

            assertThat(eventFake.isConsumed())
                .isTrue();
            assertThat(eventFake.getX())
                .isEqualTo(REAL_X);
        }

        @Test
        void PointerParking_claimEvent_consumesAMoveItCannotPark() {
            // A game build that no longer offers the setters leaves the screen with the stale hover it had
            // before any of this existed, which is a worse look and not a crash. Consuming is what keeps a
            // failed reach from handing the screen a live pointer at its real position, so the fallback is
            // the whole of what this case is about.
            var unparkableMoveMock = PointerEventMocks.mockMoveAt(REAL_X, REAL_Y);

            PointerParking.claimEvent(unparkableMoveMock);

            Mockito
                .verify(unparkableMoveMock)
                .consume();
        }
    }
}
