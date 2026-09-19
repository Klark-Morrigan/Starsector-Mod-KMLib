package kmlib.starsector.ui.layout;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.controls.Control;
import kmlib.starsector.ui.controls.ControlSpecSamples;
import kmlib.starsector.ui.controls.specs.ControlSpec;
import kmlib.starsector.ui.controls.specs.DividerSpec;
import kmlib.starsector.ui.controls.specs.InteractiveSpec;
import kmlib.starsector.ui.font.StripTextMeasurers;
import kmlib.testfixtures.starsector.ui.font.LineWidthMeasurerFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Holds the rules that must be true of every variant of the spec set, rather than of the handful a
 * case was written for.
 *
 * <p>The layout answers each variant through chains of type tests, and every one of those chains
 * ends in a default: an unrecognised variant measures as nothing, splits into no segments, and is
 * placed as a single leaf. None of that fails anything, so a variant added to the set and forgotten
 * in one chain reaches a player as a control that is invisible, or dead to clicks, or a group whose
 * contents never appear. These cases are what fails instead, and they fail without anyone writing a
 * case for the new variant, because they are stated over the catalogue rather than over a list of
 * names.
 *
 * <p>Row height is deliberately absent. One row is the right answer for most variants and the wrong
 * one for those that stack their options, and nothing a spec says today tells the two apart, so no
 * rule here could be stated without guessing which a new variant is.
 */
final class ControlStripLayoutVariantCoverageTest {

    private static final float WIDTH_PER_CHAR = 7f;
    private static final float TAB_WIDTH_PER_CHAR = 9f;

    // A row wide and tall enough for any sample to split within, the split being what is under test
    // rather than the room it is given.
    private static final Rectangle ROW = new Rectangle(0f, 0f, 400f, 200f);

    private final StripTextMeasurers measurersFake = new StripTextMeasurers(
        new LineWidthMeasurerFake(TAB_WIDTH_PER_CHAR),
        new LineWidthMeasurerFake(WIDTH_PER_CHAR));

    private final Map<Class<? extends ControlSpec>, ControlSpec> samples =
        ControlSpecSamples.mapSamplesByVariant();

    @Nested
    class VariantSamples {

        @Test
        void standsForEveryVariantTheSpecSetHolds() {
            // The check every rule below leans on. Without it a variant added to the set would simply
            // be absent from the catalogue, and each rule would pass over a set that no longer
            // describes the code.
            assertThat(List.<Class<?>>copyOf(samples.keySet()))
                .containsExactlyInAnyOrderElementsOf(
                    ControlSpecSamples.readLeafVariants(ControlSpec.class));
        }
    }

    @Nested
    class MeasureStrip {

        @Test
        void measuresEveryVariantButTheRuleAsWiderThanNothing() {
            // A variant the width chain does not recognise measures zero, and the strip sizes its body
            // to the widest row, so such a control is framed out of existence rather than drawn badly.
            samples.forEach((variant, spec) -> {
                if (variant != DividerSpec.class) {
                    assertThat(measureRowWidthOf(spec))
                        .as("%s measures to a width", variant.getSimpleName())
                        .isGreaterThan(0f);
                }
            });
        }

        @Test
        void measuresTheRuleAsNothing() {
            // The one variant that is meant to measure zero: a rule carries no intrinsic width and is
            // stretched to the framed body at placement instead.
            assertThat(measureRowWidthOf(samples.get(DividerSpec.class)))
                .isZero();
        }
    }

    @Nested
    class ToControl {

        @Test
        void splitsAControlIntoSegmentsExactlyWhenItSaysItsCellsAreHitApart() {
            // The two halves of one rule, which nothing else holds together: the control declares
            // whether its cells are separate hit targets, and the layout is what has to produce them.
            // Declared apart and split as one row, every press lands on the first cell; split apart
            // without declaring it, the whole row answers where a cell should.
            samples.forEach((variant, spec) -> {
                var segments = ControlStripLayout.toControl(spec, ROW, measurersFake).segments();

                assertThat(!segments.isEmpty())
                    .as("%s splits into segments", variant.getSimpleName())
                    .isEqualTo(saysItsCellsAreHitApart(spec));
            });
        }
    }

    @Nested
    class LayoutControls {

        @Test
        void expandsEveryGroupIntoTheControlsItHolds() {
            // A group the placement chain does not recognise is laid out as one leaf, so nothing
            // inside it is ever placed and nothing inside it is ever drawn.
            samples.forEach((variant, spec) -> {
                if (ControlSpecSamples.holdsChildSpecs(variant)) {
                    assertThat(layOutAlone(spec))
                        .as("%s expands into what it holds", variant.getSimpleName())
                        .hasSizeGreaterThan(1);
                }
            });
        }

        @Test
        void laysEveryOtherVariantOutAsOneControl() {
            // The complement, so the rule above cannot be satisfied by a layout that expanded
            // everything: a control that is not a group is one control wherever it lands.
            samples.forEach((variant, spec) -> {
                if (!ControlSpecSamples.holdsChildSpecs(variant)) {
                    assertThat(layOutAlone(spec))
                        .as("%s is one control", variant.getSimpleName())
                        .hasSize(1);
                }
            });
        }
    }

    private static boolean saysItsCellsAreHitApart(ControlSpec spec) {

        return spec instanceof InteractiveSpec interactive && interactive.isSegmented();
    }

    // Measures and places one control by itself, which is the shape every rule here asks about: what
    // the layout makes of this variant, with no neighbour to borrow a width or a position from.
    private List<Control> layOutAlone(ControlSpec spec) {

        var measurement = ControlStripLayout.measureStrip(List.of(spec), measurersFake);
        var body = new Rectangle(0f, 0f, measurement.bodyWidth(), measurement.bodyHeight());

        return ControlStripLayout.layoutControls(body, List.of(spec), measurement, measurersFake);
    }

    private float measureRowWidthOf(ControlSpec spec) {

        return ControlStripLayout.measureStrip(List.of(spec), measurersFake).rowWidths().get(0);
    }
}
