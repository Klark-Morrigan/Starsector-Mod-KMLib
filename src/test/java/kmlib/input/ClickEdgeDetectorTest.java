package kmlib.input;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the edge detector's contract: a press fires only on the up-to-down transition, so a
 * held button is one press and a release re-arms the next.
 */
class ClickEdgeDetectorTest {

    @Nested
    class DetectPress {
        @Test
        void reportsPressOnTheRisingEdge() {
            var detector = new ClickEdgeDetector();

            assertThat(detector.detectPress(true)).isTrue();
        }

        @Test
        void reportsNoPressWhileTheButtonStaysDown() {
            var detector = new ClickEdgeDetector();
            detector.detectPress(true);

            assertThat(detector.detectPress(true)).isFalse();
        }

        @Test
        void reportsNoPressWhileTheButtonStaysUp() {
            var detector = new ClickEdgeDetector();

            assertThat(detector.detectPress(false)).isFalse();
        }

        @Test
        void reportsPressAgainAfterAReleaseAndPress() {
            var detector = new ClickEdgeDetector();
            detector.detectPress(true);
            detector.detectPress(false);

            assertThat(detector.detectPress(true)).isTrue();
        }
    }
}
