package kmlib.starsector.ui.layout;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.controls.ControlAction;
import kmlib.starsector.ui.controls.ControlKind;
import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.controls.RadioAlignment;
import kmlib.starsector.ui.font.LineWidthMeasurer;
import kmlib.starsector.ui.layout.ControlStripLayout.StripMeasurement;
import kmlib.testfixtures.starsector.ui.font.LineWidthMeasurerFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins the generic control strip: {@link ControlStripLayout#measureStrip} sizes the body to the
 * widest row and the stacked row heights, and {@link ControlStripLayout#layoutControls} snaps each
 * control into the framed body top to bottom, splitting a radio into abutting segments and leaving a
 * single-hit control or a caption without segments. A round per-character width makes every snapped
 * rectangle a hand-checkable multiple.
 */
final class ControlStripLayoutTest {
    private static final float WIDTH_PER_CHAR = 10f;
    private static final float TOLERANCE = 0.01f;
    private static final float BODY_ORIGIN_X = 100f;
    private static final float BODY_ORIGIN_Y = 200f;

    private final LineWidthMeasurer measurerFake = new LineWidthMeasurerFake(WIDTH_PER_CHAR);

    @Nested
    class MeasureStrip {

        @Test
        void measureStripReturnsZeroFootprintAndNoRowsForAnEmptyStrip() {
            var measurement = ControlStripLayout.measureStrip(List.of(), measurerFake);
            assertThat(measurement.bodyWidth()).isZero();
            assertThat(measurement.bodyHeight()).isZero();
            assertThat(measurement.rowWidths()).isEmpty();
            assertThat(measurement.rowHeights()).isEmpty();
        }

        @Test
        void measureStripSizesTheBodyToTheWidestRowPlusInset() {
            // "Muted" is 5 chars; a checkbox row is the tick box, a gap, then the label, and the body
            // adds the inset on each side.
            var measurement = ControlStripLayout.measureStrip(
                    List.of(ControlSpec.createCheckbox("Muted", false, ControlAction.NONE)),
                    measurerFake);
            var expectedRow = ControlStripLayout.CONTROL_ROW_HEIGHT
                    + ControlStripLayout.CHECKBOX_LABEL_GAP + 5 * WIDTH_PER_CHAR;
            assertThat(measurement.rowWidths().get(0)).isCloseTo(expectedRow, within(TOLERANCE));
            assertThat(measurement.bodyWidth())
                    .isCloseTo(expectedRow + 2f * ControlStripLayout.BODY_PADDING, within(TOLERANCE));
        }

        @Test
        void measureStripSumsRowHeightsAndGapsPlusInset() {
            var measurement = ControlStripLayout.measureStrip(
                    List.of(ControlSpec.createCheckbox("A", false, ControlAction.NONE),
                            ControlSpec.createCheckbox("B", false, ControlAction.NONE)),
                    measurerFake);
            // Two rows: twice the row height, one gap between them, and the inset top and bottom.
            var expectedHeight = 2f * ControlStripLayout.BODY_PADDING
                    + 2f * ControlStripLayout.CONTROL_ROW_HEIGHT + ControlStripLayout.ROW_GAP;
            assertThat(measurement.bodyHeight()).isCloseTo(expectedHeight, within(TOLERANCE));
        }

        @Test
        void measureStripSizesAHorizontalRadioRowToEqualSegments() {
            var radio = new ControlSpec(ControlKind.RADIO, List.of("Short", "Full"), "Names",
                    ControlSpec.NO_SELECTION);
            var measurement = ControlStripLayout.measureStrip(List.of(radio), measurerFake);
            // Each segment is the widest option ("Short", 5 chars) plus the segment padding; the row
            // is the two equal segments side by side.
            var segmentWidth = 5 * WIDTH_PER_CHAR + ControlStripLayout.RADIO_SEGMENT_PADDING;
            assertThat(measurement.rowWidths().get(0)).isCloseTo(2 * segmentWidth, within(TOLERANCE));
        }

        @Test
        void measureStripStandsAVerticalRadioOneRowTallPerOption() {
            var radio = new ControlSpec(ControlKind.RADIO, List.of("Factions", "Alliances"), "",
                    ControlSpec.NO_SELECTION, ControlAction.NONE, RadioAlignment.VERTICAL, true);
            var measurement = ControlStripLayout.measureStrip(List.of(radio), measurerFake);
            assertThat(measurement.rowHeights().get(0))
                    .isCloseTo(2 * ControlStripLayout.CONTROL_ROW_HEIGHT, within(TOLERANCE));
        }
    }

    @Nested
    class LayoutControls {

        @Test
        void layoutControlsStacksTheFirstRowFromTheBodyTopLeftInset() {
            var specs = List.of(ControlSpec.createCheckbox("Muted", false, ControlAction.NONE));
            var measurement = ControlStripLayout.measureStrip(specs, measurerFake);
            var controls = ControlStripLayout.layoutControls(frameBody(measurement), specs,
                    measurement.rowHeights(), measurement.rowWidths());

            var row = controls.get(0).bounds();
            assertThat(row.x())
                    .isCloseTo(BODY_ORIGIN_X + ControlStripLayout.BODY_PADDING, within(TOLERANCE));
            assertThat(row.y() + row.height())
                    .as("the first row hangs one inset below the body top")
                    .isCloseTo(BODY_ORIGIN_Y + measurement.bodyHeight() - ControlStripLayout.BODY_PADDING,
                            within(TOLERANCE));
        }

        @Test
        void layoutControlsSplitsARadioIntoAbuttingEqualSegments() {
            var specs = List.of(new ControlSpec(ControlKind.RADIO, List.of("Short", "Full"), "Names",
                    ControlSpec.NO_SELECTION));
            var measurement = ControlStripLayout.measureStrip(specs, measurerFake);
            var radio = ControlStripLayout.layoutControls(frameBody(measurement), specs,
                    measurement.rowHeights(), measurement.rowWidths()).get(0);

            assertThat(radio.segments()).hasSize(2);
            var shortSegment = radio.segments().get(0);
            var fullSegment = radio.segments().get(1);
            assertThat(fullSegment.width()).isCloseTo(shortSegment.width(), within(TOLERANCE));
            assertThat(fullSegment.x())
                    .isCloseTo(shortSegment.x() + shortSegment.width(), within(TOLERANCE));
        }

        @Test
        void layoutControlsLeavesACheckboxWithoutSegments() {
            var specs = List.of(ControlSpec.createCheckbox("Muted", false, ControlAction.NONE));
            var measurement = ControlStripLayout.measureStrip(specs, measurerFake);
            var controls = ControlStripLayout.layoutControls(frameBody(measurement), specs,
                    measurement.rowHeights(), measurement.rowWidths());
            assertThat(controls.get(0).segments()).isEmpty();
        }

        @Test
        void layoutControlsLeavesALabelWithoutSegments() {
            var specs = List.of(ControlSpec.createLabel("Non-allied factions are"));
            var measurement = ControlStripLayout.measureStrip(specs, measurerFake);
            var controls = ControlStripLayout.layoutControls(frameBody(measurement), specs,
                    measurement.rowHeights(), measurement.rowWidths());
            assertThat(controls.get(0).segments()).isEmpty();
        }

        @Test
        void layoutControlsReturnsNothingForAnEmptyStrip() {
            var body = new Rectangle(BODY_ORIGIN_X, BODY_ORIGIN_Y, 0f, 0f);
            assertThat(ControlStripLayout.layoutControls(body, List.of(), List.of(), List.of()))
                    .isEmpty();
        }
    }

    // Frames a body rectangle of the measured size at a fixed origin, the way a host sizes its chrome
    // around the strip footprint before handing the body back for placement.
    private static Rectangle frameBody(StripMeasurement measurement) {
        return new Rectangle(BODY_ORIGIN_X, BODY_ORIGIN_Y, measurement.bodyWidth(),
                measurement.bodyHeight());
    }
}
