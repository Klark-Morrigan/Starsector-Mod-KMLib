package kmlib.math.geometry;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins what "the same corner" means: within the tolerance, whichever grid cell each report
 * fell in, and standing where the first report put it.
 *
 * <p>The grid is the part worth pinning. It exists only to make the lookup cheap, and the fault
 * it can introduce is silent: two reports a whisker apart but either side of a cell boundary
 * would come back as two corners, and every edge between them would fail to meet with nothing
 * to say why.
 */
final class VertexWelderTest {

    // A round tolerance, so the points below can be stated as literals a known distance
    // inside or outside it.
    private static final double TOLERANCE = 1;

    @Nested
    class Weld {

        @Test
        void twoReportsWithinTheToleranceAreOneCorner() {

            var welder = new VertexWelder(TOLERANCE);

            assertThat(welder.weld(10, 10))
                .isEqualTo(welder.weld(10.5, 10));
        }

        @Test
        void twoReportsBeyondTheToleranceAreTwoCorners() {

            var welder = new VertexWelder(TOLERANCE);

            assertThat(welder.weld(10, 10))
                .isNotEqualTo(welder.weld(12, 10));
        }

        @Test
        void aReportAcrossAGridBoundaryStillFindsItsCorner() {
            // 0.9 and 1.1 fall in different cells of a grid one tolerance across, and are a
            // fifth of a tolerance apart. Scanning the cell alone would miss this; scanning the
            // eight around it is what the welder promises.
            var welder = new VertexWelder(TOLERANCE);

            assertThat(welder.weld(0.9, 0.9))
                .isEqualTo(welder.weld(1.1, 1.1));
        }

        @Test
        void idsCountUpFromZeroInReportOrder() {

            var welder = new VertexWelder(TOLERANCE);

            assertThat(welder.weld(0, 0)).isEqualTo(0);
            assertThat(welder.weld(10, 0)).isEqualTo(1);
            assertThat(welder.weld(20, 0)).isEqualTo(2);
        }
    }

    @Nested
    class CollectPoints {

        @Test
        void aCornerStandsWhereItsFirstReportPutIt() {
            // Not averaged: an edge welded to this corner before the second report arrived
            // ends at the first coordinates, and moving the corner would leave it dangling.
            var welder = new VertexWelder(TOLERANCE);

            welder.weld(10, 10);
            welder.weld(10.5, 10);

            assertThat(welder.collectPoints())
                .hasSize(1);

            assertThat(welder.collectPoints().get(0))
                .containsExactly(10, 10);
        }

        @Test
        void pointsAreListedInIdOrder() {

            var welder = new VertexWelder(TOLERANCE);

            welder.weld(20, 0);
            welder.weld(0, 0);

            assertThat(welder.collectPoints().get(0)).containsExactly(20, 0);
            assertThat(welder.collectPoints().get(1)).containsExactly(0, 0);
        }
    }
}
