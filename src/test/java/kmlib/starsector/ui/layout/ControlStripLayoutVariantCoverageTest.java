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
import static org.assertj.core.api.Assertions.within;

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
 * <p>Row height is read through the geometry it drives rather than asked of the spec, nothing a spec
 * says telling a one-row variant from one that stacks its options. A control the height chain does not
 * recognise is measured one row tall and then split within that row, so its cells come out a fraction
 * of a row each; a group it does not recognise is placed one row tall while the controls it holds
 * stack on past that row. Both of those are shapes the placed geometry shows, and both are stated
 * below.
 *
 * <p>What stays uncovered is a variant that neither splits into cells nor holds other controls and
 * still wants more than one row - a wrapped label would be the first of them. Nothing separates it
 * from a one-row control, so covering it means the spec stating its own height, which is worth adding
 * when such a variant exists and not before.
 */
final class ControlStripLayoutVariantCoverageTest {

    private static final float WIDTH_PER_CHAR = 7f;
    private static final float TAB_WIDTH_PER_CHAR = 9f;

    // A cell stands a control row tall, not a particular bit pattern: the heights are divisions of a
    // measured row, and the rule is about a cell being the size it is drawn and aimed at.
    private static final float CELL_HEIGHT_TOLERANCE = 0.01f;

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

        @Test
        void standsEveryStackedCellAControlRowTall() {
            // Half of what the height chain is for. The split divides whatever row it is handed, so a
            // stacking variant measured as one row still produces all its cells - each a fraction of a
            // row, too short to letter and too short to aim at - rather than failing anything.
            samples.forEach((variant, spec) -> {
                for (var control : layOutAlone(spec)) {
                    if (!stacksItsCells(control)) {
                        continue;
                    }
                    for (var cell : control.segments()) {
                        assertThat(cell.height())
                            .as("%s stands its stacked cells a row tall", variant.getSimpleName())
                            .isCloseTo(ControlStripLayout.CONTROL_ROW_HEIGHT, within(CELL_HEIGHT_TOLERANCE));
                    }
                }
            });
        }

        @Test
        void placesEveryControlWithinTheBodyTheStripSizedForIt() {
            // The other half. A group measured as one row is placed as one row, while the controls it
            // holds are stacked from that row's top through their own heights and run on past the body
            // the measurement sized - off the frame the host drew, where nothing clips or reports them.
            samples.forEach((variant, spec) -> {
                var body = frameBodyFor(spec);

                for (var control : layOutAlone(spec)) {
                    assertThat(liesWithin(control.bounds(), body))
                        .as(
                            "%s places its %s within the body",
                            variant.getSimpleName(),
                            control.spec().getClass().getSimpleName())
                        .isTrue();
                }
            });
        }
    }

    // Whether one rectangle sits wholly inside another, edges included: a control placed exactly on the
    // body's inset edge is framed, where one past it is not.
    private static boolean liesWithin(Rectangle inner, Rectangle outer) {

        return inner.x() >= outer.x()
            && inner.y() >= outer.y()
            && inner.x() + inner.width() <= outer.x() + outer.width()
            && inner.y() + inner.height() <= outer.y() + outer.height();
    }

    private static boolean saysItsCellsAreHitApart(ControlSpec spec) {

        return spec instanceof InteractiveSpec interactive && interactive.isSegmented();
    }

    // Whether a control's cells run down its row rather than across it, read off where the layout put
    // them rather than from which variant it is, so a stacking control added to the set is recognised
    // as one without this being told its name. Cells hanging at one height are a row; cells at
    // differing heights are a stack.
    private static boolean stacksItsCells(Control control) {

        return control.segments().stream()
            .map(Rectangle::y)
            .distinct()
            .count() > 1;
    }

    // The body a strip holding this one control frames for itself, which is the frame the host would
    // draw around it. The SSOT for that rectangle here, so a rule asking whether a control was placed
    // inside the body cannot be asking about a differently-sized one than the layout was handed.
    private Rectangle frameBodyFor(ControlSpec spec) {

        var measurement = ControlStripLayout.measureStrip(List.of(spec), measurersFake);

        return new Rectangle(0f, 0f, measurement.bodyWidth(), measurement.bodyHeight());
    }

    // Measures and places one control by itself, which is the shape every rule here asks about: what
    // the layout makes of this variant, with no neighbour to borrow a width or a position from.
    private List<Control> layOutAlone(ControlSpec spec) {

        return ControlStripLayout.layoutControls(
            frameBodyFor(spec),
            List.of(spec),
            ControlStripLayout.measureStrip(List.of(spec), measurersFake),
            measurersFake);
    }

    private float measureRowWidthOf(ControlSpec spec) {

        return ControlStripLayout.measureStrip(List.of(spec), measurersFake).rowWidths().get(0);
    }
}
