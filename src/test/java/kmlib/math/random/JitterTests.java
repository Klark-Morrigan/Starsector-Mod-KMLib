package kmlib.math.random;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the contract of both {@code Jitter.roll} forms:
 *  - output lands in {@code [1 - size, 1 + size)} for a positive
 *    size,
 *  - negative size is normalised to its absolute value,
 *  - a zero size collapses the band to the point {@code 1f},
 *  - the band actually exercises both sides of {@code 1f} across
 *    many rolls (cheap distribution check; not a statistical test),
 *  - the sourced form maps its source's draw onto the same band and
 *    replays from a seed.
 */
final class JitterTests {

    private static final int SAMPLE_COUNT = 10_000;
    private static final long SEED = 42L;

    @Nested
    class Roll {
        @Test
        void positiveSizeLandsInSymmetricBand() {
            var size = 0.15f;

            for (var i = 0; i < SAMPLE_COUNT; i++) {
                var rolled = Jitter.roll(size);
                assertThat(rolled).isBetween(1f - size, 1f + size);
            }
        }

        @Test
        void negativeSizeIsTreatedAsItsAbsoluteValue() {
            // -0.25 must produce the same band as +0.25, not flip
            // the interval or return NaN.
            var size = -0.25f;
            var expectedMin = 0.75f;
            var expectedMax = 1.25f;

            for (var i = 0; i < SAMPLE_COUNT; i++) {
                var rolled = Jitter.roll(size);
                assertThat(rolled).isBetween(expectedMin, expectedMax);
            }
        }

        @Test
        void zeroSizeCollapsesToOne() {
            // size = 0 means min == max == 1, regardless of the
            // underlying PRNG output.
            for (var i = 0; i < 100; i++) {
                assertThat(Jitter.roll(0f)).isEqualTo(1f);
            }
        }

        @Test
        void rollsCoverBothSidesOfOne() {
            // Sanity that the band is actually random, not stuck on
            // one bound. Across 10k samples both halves must appear.
            var sawBelow = false;
            var sawAbove = false;
            for (var i = 0; i < SAMPLE_COUNT; i++) {
                var rolled = Jitter.roll(0.15f);
                if (rolled < 1f)
                    sawBelow = true;
                if (rolled > 1f)
                    sawAbove = true;
                if (sawBelow && sawAbove)
                    break;
            }
            assertThat(sawBelow).isTrue();
            assertThat(sawAbove).isTrue();
        }

        @Test
        void aSourcedRollLandsInTheSameBand() {

            var random = new Random(SEED);

            for (var i = 0; i < SAMPLE_COUNT; i++) {
                assertThat(Jitter.roll(-0.3f, random))
                    .isBetween(0.7f, 1.3f);
            }
        }

        @Test
        void aSourcedRollReplaysFromTheSameSeed() {

            var first = new Random(SEED);
            var second = new Random(SEED);

            for (var i = 0; i < 100; i++) {
                assertThat(Jitter.roll(0.3f, first))
                    .isEqualTo(Jitter.roll(0.3f, second));
            }
        }

        @Test
        void aSourcedRollPlacesTheDrawAcrossTheBand() {

            // A draw of 0.25 sits a quarter of the way up [0.7, 1.3), so the source is mapped rather than ignored.
            var randomMock = mock(Random.class);

            when(randomMock.nextFloat())
                .thenReturn(0.25f);

            assertThat(Jitter.roll(0.3f, randomMock))
                .isCloseTo(0.85f, within(1e-6f));
        }
    }
}
