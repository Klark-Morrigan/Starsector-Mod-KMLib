package kmlib.starsector.ui.widgets.segments;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.controls.SegmentSizing;
import kmlib.starsector.ui.font.LineWidthMeasurer;
import kmlib.testfixtures.starsector.ui.font.LineWidthMeasurerFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link HorizontalSegments}, the width-and-placement SSOT both a horizontal radio and a tab
 * strip size themselves through: uniform sizing gives every segment the widest label plus padding
 * while snapped sizing gives each its own label plus padding (floored at the minimum), the row width
 * is the widths summed, placement lays those widths side by side from an origin, and the dividers land
 * on the real seams of the laid segments whether even or ragged. A consistency check pins that a
 * measured row equals the widths the placement lays, for both sizings.
 */
class HorizontalSegmentsTest {
    // Each character measures 10 wide, so a label's width is a plain multiple of its length.
    private final LineWidthMeasurer measurerFake = new LineWidthMeasurerFake(10d);

    // Uniform and snapped policies over the same 8-padding, 13-size font. The uniform policy takes no
    // floor (0); the snapped policy floors at 40 so a short label still gives a clickable cell.
    private static final SegmentSpec UNIFORM = new SegmentSpec(8f, 0f, 13d, SegmentSizing.UNIFORM);
    private static final SegmentSpec SNAPPED = new SegmentSpec(8f, 40f, 13d, SegmentSizing.SNAPPED);

    @Nested
    class ComputeSegmentWidths {

        @Test
        void givesEverySegmentTheWidestLabelPlusPaddingWhenUniform() {
            // "AB" measures 20, "LONGER" 60; the widest (60) plus 8 padding is 68, and every segment
            // shares that one width so the cells read even.
            assertThat(HorizontalSegments.computeSegmentWidths(List.of("AB", "LONGER"), UNIFORM,
                    measurerFake)).containsExactly(68f, 68f);
        }

        @Test
        void snapsEachSegmentToItsOwnLabelPlusPaddingWhenSnapped() {
            // "AB" measures 20 (+8 = 28) then floors to the 40 minimum, "LONGER" 60 (+8 = 68); each
            // segment takes its own width rather than the widest.
            assertThat(HorizontalSegments.computeSegmentWidths(List.of("AB", "LONGER"), SNAPPED,
                    measurerFake)).containsExactly(40f, 68f);
        }

        @Test
        void snapsWithoutAFloorWhenTheMinimumIsZero() {
            // "AB" measures 20 (+8 = 28) with no floor, "LONGER" 60 (+8 = 68) - the raw snapped widths.
            var noFloorSnapped = new SegmentSpec(8f, 0f, 13d, SegmentSizing.SNAPPED);
            assertThat(HorizontalSegments.computeSegmentWidths(List.of("AB", "LONGER"), noFloorSnapped,
                    measurerFake)).containsExactly(28f, 68f);
        }

        @Test
        void yieldsNoWidthsForNoLabels() {
            assertThat(HorizontalSegments.computeSegmentWidths(List.of(), UNIFORM, measurerFake))
                    .isEmpty();
        }
    }

    @Nested
    class MeasureRowWidth {

        @Test
        void sumsTheUniformSegmentWidths() {
            // Two 68-wide uniform segments span 136.
            assertThat(HorizontalSegments.measureRowWidth(List.of("AB", "LONGER"), UNIFORM,
                    measurerFake)).isEqualTo(136f);
        }

        @Test
        void sumsTheSnappedSegmentWidths() {
            // A 40-floored "AB" plus a 68-wide "LONGER" span 108.
            assertThat(HorizontalSegments.measureRowWidth(List.of("AB", "LONGER"), SNAPPED,
                    measurerFake)).isEqualTo(108f);
        }

        @Test
        void measuresNoWidthForNoLabels() {
            assertThat(HorizontalSegments.measureRowWidth(List.of(), SNAPPED, measurerFake)).isZero();
        }
    }

    @Nested
    class PlaceSegments {

        @Test
        void laysTheWidthsSideBySideFromTheOrigin() {
            assertThat(HorizontalSegments.placeSegments(100f, 0f, 24f, List.of(40f, 68f)))
                    .containsExactly(new Rectangle(100f, 0f, 40f, 24f),
                            new Rectangle(140f, 0f, 68f, 24f));
        }

        @Test
        void yieldsNoSegmentsForNoWidths() {
            assertThat(HorizontalSegments.placeSegments(100f, 0f, 24f, List.of())).isEmpty();
        }
    }

    @Nested
    class ComputeDividers {

        @Test
        void rulesADividerAtEachNonFirstSegmentsLeftEdge() {
            // Even 50-wide cells: the one seam sits at 50, the second segment's left edge.
            var segments = List.of(new Rectangle(0f, 0f, 50f, 20f), new Rectangle(50f, 0f, 50f, 20f));
            assertThat(HorizontalSegments.computeDividers(segments, 1f))
                    .containsExactly(new Rectangle(50f, 0f, 1f, 20f));
        }

        @Test
        void landsDividersOnTheRealSeamsOfRaggedSegments() {
            // Snapped, unequal segments: the seams follow the real left edges (40, then 108), not the
            // even thirds a bounds-and-count re-derivation would place them at.
            var segments = List.of(new Rectangle(0f, 0f, 40f, 20f), new Rectangle(40f, 0f, 68f, 20f),
                    new Rectangle(108f, 0f, 28f, 20f));
            assertThat(HorizontalSegments.computeDividers(segments, 1f))
                    .containsExactly(new Rectangle(40f, 0f, 1f, 20f),
                            new Rectangle(108f, 0f, 1f, 20f));
        }

        @Test
        void yieldsNoDividersForFewerThanTwoSegments() {
            assertThat(HorizontalSegments.computeDividers(
                    List.of(new Rectangle(0f, 0f, 50f, 20f)), 1f)).isEmpty();
            assertThat(HorizontalSegments.computeDividers(List.of(), 1f)).isEmpty();
        }
    }

    @Nested
    class MeasureAndPlaceConsistency {

        @Test
        void measuredRowEqualsTheSummedPlacementWidthsForBothSizings() {
            // The measure and the placement read the same per-segment widths, so a row sized for the
            // strip is exactly as wide as the segments then laid into it - proven for each sizing.
            for (var sizing : SegmentSizing.values()) {
                var labels = List.of("AB", "LONGER", "C");
                var spec = new SegmentSpec(8f, 40f, 13d, sizing);
                var widths = HorizontalSegments.computeSegmentWidths(labels, spec, measurerFake);
                var placed = HorizontalSegments.placeSegments(0f, 0f, 24f, widths);
                var laidOutTotal = 0f;
                for (var segment : placed) {
                    laidOutTotal += segment.width();
                }
                assertThat(HorizontalSegments.measureRowWidth(labels, spec, measurerFake))
                        .isEqualTo(laidOutTotal);
            }
        }
    }
}
