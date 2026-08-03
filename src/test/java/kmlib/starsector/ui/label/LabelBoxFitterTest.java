package kmlib.starsector.ui.label;

import kmlib.math.geometry.DirectedLine;
import kmlib.math.geometry.RegionChord;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins {@link LabelBoxFitter#fitLargestBox}: given one candidate chord against a
 * region, it caps the font so the whole band stays inside the boundary, stacks text
 * into more lines only when that renders a strictly taller font, honours the line cap
 * and the per-line font clamp, keeps the box out of a keep-out point's clearance, stops
 * the font growth at the tolerance it was given, and returns null when even the minimum
 * font cannot hold the text. The fitter is tested
 * apart from any candidate search so its sizing contract is pinned on rings and a text
 * estimator directly, without the candidate generation a caller wraps it in.
 */
final class LabelBoxFitterTest {

    // A font-height tolerance far finer than any geometry these fixtures assert, so the
    // sizing search is never what a failure is about except where a test names it.
    private static final double FINE_FONT_TOLERANCE = 0.01;

    @Nested
    class FitLargestBox {
        @Test
        void fitLargestBoxCapsTheGirthAtTheRegionSoTheBandStaysInside() {
            // A slab 2000 wide, 700 tall, a fat label (aspect 1) that wants all the girth
            // it can get: the band cannot exceed the 700 the region allows, so the fit caps
            // it just under 700 and the whole band, centred at y=350, stays within y 0..700.
            var box = fitter(1.0, 100.0, 2000.0, 1, 1.0)
                .fitLargestBox(
                    horizontalChord(
                        rectangle(0, 0, 2000, 700), 1000, 350));

            assertThat(box)
                .isNotNull();
            assertThat(box.lineCount())
                .isEqualTo(1);
            assertThat(box.thickness())
                .isGreaterThan(600.0);
            assertThat(box.thickness())
                .isLessThanOrEqualTo(700.0);
            assertThat(box.segment().startY())
                .isCloseTo(350.0, within(1.0));
        }

        @Test
        void fitLargestBoxStacksASquareLabelIntoTwoLines() {
            // In a 1700-square region text six times as long as it is tall runs strictly
            // taller on two lines than one - half the length each line needs, spent against
            // the spare girth - and taller than three, which the girth cannot make taller
            // still.
            var box = fitter(6.0, 100.0, 1700.0, 3, 1.15)
                .fitLargestBox(
                    horizontalChord(
                        rectangle(0, 0, 1700, 1700), 850, 850));

            assertThat(box)
                .isNotNull();
            assertThat(box.lineCount())
                .isEqualTo(2);
        }

        @Test
        void fitLargestBoxKeepsOneLineWhenStackingBuysNoTallerFont() {
            // In the wide slab the single line is not length-limited (aspect 4 at the
            // girth-capped font still fits), so two lines - feasible, but girth-halved to
            // a smaller font - lose to it: a block goes multi-line only when stacking
            // renders a strictly larger font.
            var box = fitter(4.0, 100.0, 2000.0, 3, 1.0)
                .fitLargestBox(
                    horizontalChord(
                        rectangle(0, 0, 2000, 700), 1000, 350));

            assertThat(box)
                .isNotNull();
            assertThat(box.lineCount())
                .isEqualTo(1);
            assertThat(box.fontHeight())
                .isGreaterThan(490.0);
        }

        @Test
        void fitLargestBoxStacksWhenTheSingleLineCannotHoldTheTextAtTheMinimumFont() {
            // At the 340 floor a one-line label (aspect 10) needs 3400 - more than the slab
            // holds - but two lines halve that to 1700 and their 680 girth still fits the
            // 700 the region allows: wrapping rescues text the single line cannot carry
            // at readable size.
            var box = fitter(10.0, 340.0, 2000.0, 2, 1.0)
                .fitLargestBox(
                    horizontalChord(
                        rectangle(0, 0, 2000, 700), 1000, 350));

            assertThat(box)
                .isNotNull();
            assertThat(box.lineCount())
                .isEqualTo(2);
        }

        @Test
        void fitLargestBoxReturnsNullWhenTheMinimumFontFitsNoLineCount() {
            // A 360 floor: one line needs 3600 of length (more than the slab), two lines
            // need 720 of girth (more than the 700 the region allows) - every count fails
            // at the readability floor, so no box at all.
            var box = fitter(10.0, 360.0, 2000.0, 2, 1.0)
                .fitLargestBox(
                    horizontalChord(
                        rectangle(0, 0, 2000, 700), 1000, 350));

            assertThat(box)
                .isNull();
        }

        @Test
        void fitLargestBoxKeepsOneLineWhenTheLineCapIsOne() {
            // The square that would prefer two lines is held to one when the line cap is
            // one, so the text stays a single line at the smaller font the cap forces.
            var box = fitter(6.0, 100.0, 1700.0, 1, 1.15)
                .fitLargestBox(
                    horizontalChord(
                        rectangle(0, 0, 1700, 1700), 850, 850));

            assertThat(box)
                .isNotNull();
            assertThat(box.lineCount())
                .isEqualTo(1);
        }

        @Test
        void fitLargestBoxKeepsTheBoxClearOfAKeepOutPoint() {
            // The slab's chord runs from t=-1000 to t=1000 about the through-point at
            // x=1000. A keep-out at x=1200 with clearance 400 blocks t in [-200, 600],
            // so the roomier survivor is the left stretch [-1000, -200] - x 0 to 800 -
            // and the fitted box must sit in it rather than across the point.
            var box = fitterWithKeepOutClearance(1.0, 100.0, 2000.0, 1, 1.0, 400.0)
                .fitLargestBox(
                    new RegionChord(
                        List.of(rectangle(0, 0, 2000, 700)),
                        List.of(new double[] {1200, 350}),
                        new DirectedLine(1000, 350, 1.0, 0.0)));

            assertThat(box)
                .isNotNull();
            assertThat(box.segment().startX())
                .isCloseTo(0.0, within(1.0));
            assertThat(box.segment().endX())
                .isCloseTo(800.0, within(1.0));
        }

        @Test
        void fitLargestBoxProjectsTheKeepOutsOnceForTheWholeSizing() {
            // The keep-out projection reads the chord's line and the clearance, never a
            // band's thickness, so it is invariant across the many bands one sizing
            // measures. Iterating the keep-outs once - while the band-fit count shows
            // the sizing measured far more bands than that - is what says the
            // projection sits outside the font search rather than inside it; an
            // assertion on the fitted box cannot see the difference.
            var keepOutsFake = new IterationCountingKeepOutsFake(
                List.of(new double[] {1200, 350}));
                
            var fitter = fitterWithKeepOutClearance(1.0, 100.0, 2000.0, 1, 1.0, 400.0);

            fitter.fitLargestBox(
                new RegionChord(
                    List.of(rectangle(0, 0, 2000, 700)),
                    keepOutsFake,
                    new DirectedLine(1000, 350, 1.0, 0.0)));

            assertThat(keepOutsFake.getIterationCount())
                .isEqualTo(1);
            assertThat(fitter.getBandFitCount())
                .isGreaterThan(20);
        }

        @Test
        void fitLargestBoxSpendsOneBandPerHalvingTheFontToleranceCallsFor() {
            // The font clamp spans 1900, so landing within 1 takes 11 halvings and within
            // 100 takes 5 - and every halving is a band measured against the rings and the
            // keep-outs. On top of each sit the three bands the sizing spends regardless:
            // the minimum-font probe, the test of the clamp's top end, and the accepted
            // band's read-back. A coarser tolerance is therefore paid back one-for-one in
            // measurements, which is the whole reason the precision is a caller's knob.
            var chord = horizontalChord(rectangle(0, 0, 2000, 700), 1000, 350);
            var fineFitter = fitterWithFontTolerance(1.0, 100.0, 2000.0, 1, 1.0, 0.0, 1.0);
            var coarseFitter = fitterWithFontTolerance(1.0, 100.0, 2000.0, 1, 1.0, 0.0, 100.0);

            fineFitter.fitLargestBox(chord);
            coarseFitter.fitLargestBox(chord);

            assertThat(fineFitter.getBandFitCount())
                .isEqualTo(14);
            assertThat(coarseFitter.getBandFitCount())
                .isEqualTo(8);
        }

        @Test
        void fitLargestBoxLandsWithinTheFontToleranceOfTheTallestFittingFont() {
            // The slab's 700 girth is what caps this fit, so the tallest font that fits is
            // 700 and a search stopped at a tolerance of 100 has to come back within that
            // of it. Buying fewer measurements costs accepted font height and nothing else:
            // the fit is still a fit, only less finely resolved.
            var box = fitterWithFontTolerance(1.0, 100.0, 2000.0, 1, 1.0, 0.0, 100.0)
                .fitLargestBox(
                    horizontalChord(
                        rectangle(0, 0, 2000, 700), 1000, 350));

            assertThat(box)
                .isNotNull();
            assertThat(box.fontHeight())
                .isCloseTo(700.0, within(100.0));
        }

        @Test
        void fitLargestBoxReturnsNullWhenTheMinimumBandCannotFit() {
            // A minimum font taller than the 1700 the square holds cannot sit anywhere,
            // so the fit finds no box at all.
            var box = fitter(6.0, 3000.0, 4000.0, 1, 1.0)
                .fitLargestBox(
                    horizontalChord(
                        rectangle(0, 0, 1700, 1700), 850, 850));

            assertThat(box)
                .isNull();
        }
    }

    @Nested
    class FitBand {
        @Test
        void fitBandReportsTheStretchOfTheLineTheBandKeepsInsideTheRegion() {
            // A band 200 thick about y=350 clears the slab's 700 girth everywhere, so
            // the whole crossing survives: the chord runs from the left edge at
            // t=-1000 to the right edge at t=1000 about the through-point at x=1000.
            var band = fitter(1.0, 100.0, 2000.0, 1, 1.0)
                .fitBand(
                    horizontalChord(rectangle(0, 0, 2000, 700), 1000, 350),
                    100.0);

            assertThat(band.clearSpan()[0])
                .isCloseTo(-1000.0, within(1.0));
            assertThat(band.clearSpan()[1])
                .isCloseTo(1000.0, within(1.0));
        }

        @Test
        void fitBandReportsNoSpanWhenTheBandIsFatterThanTheRegion() {
            // An 800-thick band cannot sit inside the slab's 700 girth anywhere along
            // the line, so not even the near-miss diagnostic has a span to show.
            var band = fitter(1.0, 100.0, 2000.0, 1, 1.0)
                .fitBand(
                    horizontalChord(rectangle(0, 0, 2000, 700), 1000, 350),
                    400.0);

            assertThat(band.clearSpan())
                .isNull();
            assertThat(band.insetSpan())
                .isNull();
        }

        @Test
        void fitBandReportsNoSpanForALineWithNoDirection() {
            // A degenerate line defines no frame to measure a span in, so the single
            // measurement reports nothing rather than a span in an undefined frame.
            var band = fitter(1.0, 100.0, 2000.0, 1, 1.0)
                .fitBand(
                    new RegionChord(
                        List.of(rectangle(0, 0, 2000, 700)),
                        List.of(),
                        new DirectedLine(1000, 350, 0.0, 0.0)),
                    100.0);

            assertThat(band.clearSpan())
                .isNull();
            assertThat(band.insetSpan())
                .isNull();
        }
    }

    @Nested
    class GetBandFitCount {

        @Test
        void getBandFitCountStartsAtNothingBeforeAnyFit() {
            // A fresh fitter has measured nothing, so a caller summing the count over
            // several fitters starts each from zero rather than from a shared running total.
            assertThat(fitter(1.0, 100.0, 2000.0, 1, 1.0)
                    .getBandFitCount())
                .isEqualTo(0);
        }

        @Test
        void getBandFitCountCountsEveryBandTheSizingMeasured() {
            // One sizing pass is many band fits, not one: the minimum-font probe, a band per
            // font-height search step, and the accepted span's read-back. The count is what
            // the sizing actually spent, which is why it is measured rather than derived
            // from the candidate that asked for it.
            var fitter = fitter(1.0, 100.0, 2000.0, 1, 1.0);

            fitter.fitLargestBox(
                horizontalChord(
                    rectangle(0, 0, 2000, 700), 1000, 350));

            assertThat(fitter.getBandFitCount())
                .isGreaterThan(1);
        }

        @Test
        void getBandFitCountCountsTheProbeOfASizingThatFitsNothing() {
            // A sizing that fails at the readability floor still walked the rings and the
            // keep-outs to find that out, so its probe is counted: a fit that found no box
            // is cheap, never free.
            var fitter = fitter(6.0, 3000.0, 4000.0, 1, 1.0);

            fitter.fitLargestBox(
                horizontalChord(
                    rectangle(0, 0, 1700, 1700), 850, 850));

            assertThat(fitter.getBandFitCount())
                .isEqualTo(1);
        }
    }

    // A fitter with no keep-out clearance and no end inset, so a test isolates the font
    // and line-count sizing from the keep-out and margin trims; the text estimator is the
    // aspect stand-in, whose required length the tests can compute by hand.
    private static LabelBoxFitter fitter(
            double aspect,
            double minFontHeight,
            double maxFontHeight,
            int maxLines,
            double lineSpacing) {
        return fitterWithKeepOutClearance(
            aspect,
            minFontHeight,
            maxFontHeight,
            maxLines,
            lineSpacing,
            0.0);
    }

    // The same fitter with a keep-out radius, for the tests that place a point the box
    // has to steer around rather than only a boundary it has to stay within.
    private static LabelBoxFitter fitterWithKeepOutClearance(
            double aspect,
            double minFontHeight,
            double maxFontHeight,
            int maxLines,
            double lineSpacing,
            double keepOutClearance) {
        return fitterWithFontTolerance(
            aspect,
            minFontHeight,
            maxFontHeight,
            maxLines,
            lineSpacing,
            keepOutClearance,
            FINE_FONT_TOLERANCE);
    }

    // The whole fitter surface, for the tests about how finely the font search runs:
    // everything above fixes the tolerance, since only these read it back.
    private static LabelBoxFitter fitterWithFontTolerance(
            double aspect,
            double minFontHeight,
            double maxFontHeight,
            int maxLines,
            double lineSpacing,
            double keepOutClearance,
            double fontHeightTolerance) {
        return new LabelBoxFitter(
            new NameFitSpecification(minFontHeight, maxFontHeight, maxLines, lineSpacing),
            new BandFitSpecification(keepOutClearance, 0.0, fontHeightTolerance),
            new AspectLabelLengthEstimator(aspect));
    }

    // A horizontal candidate line through the given point against one boundary ring and
    // no keep-outs - the simplest placement the sizing tests need.
    private static RegionChord horizontalChord(
            List<double[]> ring,
            double throughX,
            double throughY) {
        return new RegionChord(
            List.of(ring),
            List.of(),
            new DirectedLine(throughX, throughY, 1.0, 0.0));
    }

    // A counter-clockwise rectangle ring anchored at (minX, minY).
    private static List<double[]> rectangle(
            double minX,
            double minY,
            double width,
            double height) {
        return Arrays.asList(
            new double[] {minX, minY},
            new double[] {minX + width, minY},
            new double[] {minX + width, minY + height},
            new double[] {minX, minY + height});
    }

    // A keep-out collection that records how often it was walked. Whether the fitter
    // projects the obstacles once per chord or once per band is invisible in the fitted
    // box - both give the same box - and shows only in how often the collection is
    // asked for its contents, which is what this counts.
    private static final class IterationCountingKeepOutsFake extends AbstractCollection<double[]> {

        private final List<double[]> points;
        private int iterationCount;

        private IterationCountingKeepOutsFake(List<double[]> points) {
            this.points = points;
        }

        int getIterationCount() {
            return iterationCount;
        }

        @Override
        public Iterator<double[]> iterator() {
            iterationCount++;
            return points.iterator();
        }

        @Override
        public int size() {
            return points.size();
        }
    }
}
