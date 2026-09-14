package kmlib.math.geometry;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link RingRegion#toRings}: the flat form hands back the outer ring first and
 * every hole after it, in the order the region holds them, and a region with nothing
 * cut out of it flattens to its outer ring alone.
 *
 * <p>The ordering is the whole contract. Consumers that take "an outer ring plus its
 * holes" read the first entry as the boundary and the rest as holes, so a region that
 * flattened them in any other order would fill as its own inverse rather than fail.
 */
final class RingRegionTest {

    private static final List<double[]> OUTER_RING = List.of(
        new double[] {0, 0},
        new double[] {10, 0},
        new double[] {10, 10},
        new double[] {0, 10});

    private static final List<double[]> FIRST_HOLE = List.of(
        new double[] {2, 2},
        new double[] {2, 4},
        new double[] {4, 4});

    private static final List<double[]> SECOND_HOLE = List.of(
        new double[] {6, 6},
        new double[] {6, 8},
        new double[] {8, 8});

    @Nested
    class ToRings {

        @Test
        void toRingsLeadsWithTheOuterRing() {

            var region = new RingRegion(OUTER_RING, List.of(FIRST_HOLE));

            assertThat(region.toRings())
                .element(0)
                .isSameAs(OUTER_RING);
        }

        @Test
        void toRingsAppendsEveryHoleInTheOrderTheRegionHoldsThem() {

            var region = new RingRegion(OUTER_RING, List.of(FIRST_HOLE, SECOND_HOLE));

            assertThat(region.toRings())
                .containsExactly(OUTER_RING, FIRST_HOLE, SECOND_HOLE);
        }

        @Test
        void toRingsReturnsTheOuterRingAloneWhenNothingIsCutOut() {

            var region = new RingRegion(OUTER_RING, List.of());

            assertThat(region.toRings())
                .containsExactly(OUTER_RING);
        }

        @Test
        void toRingsLeavesTheRegionUntouched() {

            var region = new RingRegion(OUTER_RING, List.of(FIRST_HOLE));

            // The flat form is a fresh list: a consumer that sorts or trims it must not
            // reach back into the region the rings came from.
            region.toRings().clear();

            assertThat(region.outerRing())
                .isSameAs(OUTER_RING);
            assertThat(region.holeRings())
                .containsExactly(FIRST_HOLE);
        }
    }
}
