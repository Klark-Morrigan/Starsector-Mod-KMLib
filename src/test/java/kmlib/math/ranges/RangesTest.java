package kmlib.math.ranges;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins {@link Ranges#clampToUnit}: the SSOT unit-range clamp both the scrollbar and the collapsing tab
 * panel confine their fractions through, so a value past either end squeezes to the nearest bound.
 */
final class RangesTest {
    private static final float TOLERANCE = 0.0001f;

    @Nested
    class ClampToUnit {

        @Test
        void clampToUnitKeepsAValueAlreadyInRange() {
            assertThat(Ranges.clampToUnit(0.5f)).isCloseTo(0.5f, within(TOLERANCE));
        }

        @Test
        void clampToUnitFloorsANegativeValueToZero() {
            assertThat(Ranges.clampToUnit(-0.3f)).isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void clampToUnitCapsAValuePastOneToOne() {
            assertThat(Ranges.clampToUnit(1.7f)).isCloseTo(1f, within(TOLERANCE));
        }

        @Test
        void clampToUnitKeepsTheBoundsThemselves() {
            assertThat(Ranges.clampToUnit(0f)).isCloseTo(0f, within(TOLERANCE));
            assertThat(Ranges.clampToUnit(1f)).isCloseTo(1f, within(TOLERANCE));
        }
    }
}
