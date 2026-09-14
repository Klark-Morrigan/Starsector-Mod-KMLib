package kmlib.math.hashing;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins what a caller spreading like things over one range depends on: every key lands inside the range, a key
 * holds the same share in every run - the values below are what "stable" means, and any change to the
 * derivation would move a share that has already been seen - and keys one character apart land far apart
 * rather than side by side, which is the whole point of spreading them at all.
 */
final class StableFractionsTest {

    private static final float TOLERANCE = 0.000001f;

    // Two keys of the shape a caller actually uses, differing in their last character - the hardest case for
    // a raw string hash, whose values for these sit next to each other.
    private static final String NEIGHBOURING_KEY = "beacon_1";
    private static final String OTHER_NEIGHBOURING_KEY = "beacon_2";

    // The shares those two keys hold, recorded from the derivation itself. Literal rather than recomputed,
    // since recomputing them would only restate the code and pin nothing.
    private static final float NEIGHBOURING_KEY_FRACTION = 0.6839345f;
    private static final float OTHER_NEIGHBOURING_KEY_FRACTION = 0.16206425f;

    // How far apart neighbouring keys must land to count as spread. A tenth of the range is far enough that
    // no reader would take two emitters at these shares for one, and loose enough not to pin the exact values.
    private static final float MINIMUM_SEPARATION = 0.1f;

    // A sweep wide enough that a derivation running off either end of the range would be caught by one of
    // them, without the suite depending on any particular key's share.
    private static final int SWEPT_KEY_COUNT = 1_000;

    @Nested
    class ResolveFraction {

        @Test
        void resolveFractionHoldsAKeyToOneShareInEveryRun() {
            // The stability claim, stated as the values themselves: a caller derives an emitter's place in a
            // cycle from its ID and expects that place to survive a restart, a save load and a new machine.
            assertThat(StableFractions.resolveFraction(NEIGHBOURING_KEY))
                .isCloseTo(NEIGHBOURING_KEY_FRACTION, within(TOLERANCE));

            assertThat(StableFractions.resolveFraction(OTHER_NEIGHBOURING_KEY))
                .isCloseTo(OTHER_NEIGHBOURING_KEY_FRACTION, within(TOLERANCE));
        }

        @Test
        void resolveFractionSeparatesKeysOneCharacterApart() {
            // IDs in a set differ by a digit far more often than they differ wholesale, so this is the case
            // that decides whether the spread works in practice rather than in principle.
            var separation = Math.abs(
                StableFractions.resolveFraction(NEIGHBOURING_KEY)
                    - StableFractions.resolveFraction(OTHER_NEIGHBOURING_KEY));

            assertThat(separation)
                .isGreaterThan(MINIMUM_SEPARATION);
        }

        @Test
        void resolveFractionLandsEveryKeyInsideTheUnitRange() {
            // A share at or past a whole would send whatever it drives a full turn on, so the open upper end
            // is asserted rather than assumed from the bit width it is built out of.
            for (var index = 0; index < SWEPT_KEY_COUNT; index++) {

                var fraction = StableFractions.resolveFraction("emitter_" + index);

                assertThat(fraction)
                    .isGreaterThanOrEqualTo(0f);
                assertThat(fraction)
                    .isLessThan(1f);
            }
        }

        @Test
        void resolveFractionStartsTheRangeForAnEmptyKey() {
            // The avalanche's fixed point at zero, pinned so the documented caveat is a fact: an unnamed
            // subject sits at the start of the range rather than anywhere unpredictable.
            assertThat(StableFractions.resolveFraction(""))
                .isZero();
        }
    }
}
