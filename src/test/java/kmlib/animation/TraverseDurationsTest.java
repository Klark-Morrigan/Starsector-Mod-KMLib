package kmlib.animation;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins {@link TraverseDurations}: which direction reads which duration, and that the two named pairs are the
 * pairs they claim to be. The lookup is one boolean away from being inverted and would still compile and
 * still animate - every motion would simply run the wrong way round, quick to let go and slow to answer -
 * which is exactly the confusion these assertions rule out.
 */
final class TraverseDurationsTest {

    private static final float TOLERANCE = 0.0001f;

    // Two durations far enough apart that a lookup answering with the wrong one cannot be mistaken for a
    // rounding difference.
    private static final float RISE_SECONDS = 0.1f;
    private static final float FALL_SECONDS = 0.4f;

    private static final TraverseDurations DURATIONS =
        new TraverseDurations(RISE_SECONDS, FALL_SECONDS);

    // Named so a lookup reads as a direction rather than as a bare flag.
    private static final boolean RISING = true;
    private static final boolean FALLING = false;

    @Nested
    class ResolveDurationSeconds {

        @Test
        void resolveDurationSecondsReturnsTheRiseWhileHeadingForTheFarEnd() {

            assertThat(DURATIONS.resolveDurationSeconds(RISING))
                .isCloseTo(0.1f, within(TOLERANCE));
        }

        @Test
        void resolveDurationSecondsReturnsTheFallWhileHeadingBackToRest() {

            assertThat(DURATIONS.resolveDurationSeconds(FALLING))
                .isCloseTo(0.4f, within(TOLERANCE));
        }
    }

    @Nested
    class CreateSymmetric {

        @Test
        void createSymmetricGivesBothDirectionsTheSamePace() {
            
            assertThat(TraverseDurations.createSymmetric(0.25f))
                .isEqualTo(new TraverseDurations(0.25f, 0.25f));
        }
    }

    @Nested
    class Snap {

        @Test
        void snapCoversBothDirectionsInOneStep() {
            // A non-positive duration is how an advance is told to skip the travel, so both ends of the
            // named pair have to be one - a snap that only snapped one way would wind the other back down
            // over the frames after an element was already dropped.
            assertThat(TraverseDurations.SNAP)
                .isEqualTo(new TraverseDurations(0f, 0f));
        }
    }
}
