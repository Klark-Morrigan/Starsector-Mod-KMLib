package kmlib.math.hashing;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the fold callers depend on to invalidate a cache when any of several live inputs moves: a
 * change in any source shifts the result, the fold is order-sensitive so two sources that swap values
 * do not alias, and an empty combine is a stable constant for a static input.
 */
final class FingerprintsTest {

    @Nested
    class Compute {

        @Test
        void computeShiftsWhenAnySourceAdvances() {
            // The whole point: a consumer folding N counters must see the fold move when any one
            // advances, or a change would be missed and the cache never rebuilt.
            var before = Fingerprints.compute(() -> 3, () -> 7);
            var afterFirstMoves = Fingerprints.compute(() -> 4, () -> 7);
            var afterSecondMoves = Fingerprints.compute(() -> 3, () -> 8);
            assertThat(afterFirstMoves).isNotEqualTo(before);
            assertThat(afterSecondMoves).isNotEqualTo(before);
        }

        @Test
        void computeIsOrderSensitive() {
            // Order matters, so two sources that swap values still fold apart rather than aliasing to
            // the same fingerprint.
            assertThat(Fingerprints.compute(() -> 3, () -> 7))
                .isNotEqualTo(Fingerprints.compute(() -> 7, () -> 3));
        }

        @Test
        void computeReadsEachSourcesCurrentValue() {
            // The fold reads the live value at call time, so the same sources fold identically while
            // unchanged - a rebuild only when a counter actually moved, not on every call.
            var first = Fingerprints.compute(() -> 5, () -> 9);
            var second = Fingerprints.compute(() -> 5, () -> 9);
            assertThat(first).isEqualTo(second);
        }

        @Test
        void computeWithNoSourcesIsAStableConstant() {
            // A static input passes no sources and must get one fixed value, so a view with no live
            // data never triggers a rebuild on its own.
            assertThat(Fingerprints.compute()).isEqualTo(Fingerprints.compute());
        }
    }
}
