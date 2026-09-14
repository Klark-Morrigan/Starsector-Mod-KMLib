package kmlib.starsector.ui.input;

import kmlib.animation.TraverseDurations;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the pace every motion on a panel takes when nothing states another.
 *
 * <p>Worth pinning because it is the one number here a player actually feels, and because the asymmetry is
 * a decision rather than an accident: read as a single duration, both directions would still travel and
 * both would still arrive, and only the feel would be wrong - which no other test would catch.
 */
final class PanelMotionPacesTest {

    @Nested
    class DefaultDurations {

        @Test
        void defaultDurationsTravelOutTwiceAsQuicklyAsBack() {
            // Pinned as the pair rather than as a ratio, since it is the two values a player sees - and the
            // asymmetry is the point: equal halves make the whole motion read as the slower one.
            assertThat(PanelMotionPaces.DEFAULT_DURATIONS)
                .isEqualTo(new TraverseDurations(0.15f, 0.3f));
        }
    }
}
