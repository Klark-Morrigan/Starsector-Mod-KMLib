package kmlib.math.hashing;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the contract callers depend on when they avalanche hashes before a
 * commutative combine: distinct inputs stay distinct (never a manufactured
 * collision), a one-bit input difference spreads across the whole word, and the
 * fixed point at 0 is documented rather than accidental so callers know to seed.
 */
final class AvalancheTest {

    @Nested
    class MixBits {

        @Test
        void mixBitsKeepsDistinctInputsDistinct() {
            // The finalizer is bijective, so it must never collapse two different
            // inputs onto one output - that is the whole reason it is safe to fold
            // into a combine that must not lose changes.
            assertThat(Avalanche.mixBits(1)).isNotEqualTo(Avalanche.mixBits(2));
        }

        @Test
        void mixBitsSpreadsASingleBitDifferenceAcrossTheWord() {
            // Inputs one bit apart must land far apart, not one bit apart, or a
            // combine of near-identical hashes would barely move. Require many bits
            // to differ between neighbours.
            var differingBits = Integer.bitCount(Avalanche.mixBits(0x1000) ^ Avalanche.mixBits(0x1001));
            assertThat(differingBits).isGreaterThan(8);
        }

        @Test
        void mixBitsMapsZeroToZero() {
            // fmix32's lone fixed point, pinned so the caveat is a tested fact:
            // callers that must keep a 0 input observable have to seed before mixing.
            assertThat(Avalanche.mixBits(0)).isZero();
        }

        @Test
        void mixBitsMovesANonZeroSeededZero() {
            // The seed workaround the class documents: XOR a non-zero seed into a 0
            // input and the fixed point no longer swallows it.
            assertThat(Avalanche.mixBits(0 ^ 0x9e3779b9)).isNotZero();
        }
    }
}
