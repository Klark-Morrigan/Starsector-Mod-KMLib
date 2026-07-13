package kmlib.starsector.ui.layout;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.controls.ControlAction;
import kmlib.starsector.ui.controls.ControlKind;
import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.controls.RadioAlignment;
import kmlib.starsector.ui.font.LineWidthMeasurer;
import kmlib.starsector.ui.layout.ControlStripLayout.StripMeasurement;
import kmlib.starsector.ui.widgets.IconLabelRow;
import kmlib.testfixtures.starsector.ui.font.LineWidthMeasurerFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
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

        @Test
        void measureStripStandsAnIconListOneRowTallPerOption() {
            // The icon list stacks like a vertical radio, so it stands one control-row tall per
            // option regardless of icons.
            var picker = ControlSpec.createIconRadioList(List.of("Hegemony", "Tri-Tachyon"),
                    List.of("crest_heg", "crest_tt"), ControlSpec.NO_SELECTION, ControlAction.NONE);
            var measurement = ControlStripLayout.measureStrip(List.of(picker), measurerFake);
            assertThat(measurement.rowHeights().get(0))
                    .isCloseTo(2 * ControlStripLayout.CONTROL_ROW_HEIGHT, within(TOLERANCE));
        }

        @Test
        void measureStripSpansADividerToTheContentWidth() {
            // A divider has no text, so it stretches to the strip's inner width: its row width comes
            // out equal to the widest content row (the checkbox here), not zero.
            var specs = List.of(ControlSpec.createDivider(),
                    ControlSpec.createCheckbox("Muted", false, ControlAction.NONE));
            var measurement = ControlStripLayout.measureStrip(specs, measurerFake);
            assertThat(measurement.rowWidths().get(0))
                    .isCloseTo(measurement.rowWidths().get(1), within(TOLERANCE));
        }

        @Test
        void measureStripDoesNotLetADividerDriveTheBodyWidth() {
            // The divider stretches to the content width but must not set it, so a strip of only a
            // checkbox measures the same body width whether or not a divider heads it.
            var checkbox = ControlSpec.createCheckbox("Muted", false, ControlAction.NONE);
            var withoutDivider = ControlStripLayout.measureStrip(List.of(checkbox), measurerFake);
            var withDivider = ControlStripLayout.measureStrip(
                    List.of(ControlSpec.createDivider(), checkbox), measurerFake);
            assertThat(withDivider.bodyWidth())
                    .isCloseTo(withoutDivider.bodyWidth(), within(TOLERANCE));
        }

        @Test
        void measureStripSizesAnIconListToItsWidestOptionRow() {
            // "AB" carries a crest, "CDE" does not; the row is the widest of the two, each sized
            // through the shared IconLabelRow geometry so the width tracks whether the option draws an
            // icon. The measurement reads that geometry rather than re-deriving the icon and gap sizes.
            var picker = ControlSpec.createIconRadioList(List.of("AB", "CDE"),
                    Arrays.asList("crest_ab", null), ControlSpec.NO_SELECTION, ControlAction.NONE);
            var measurement = ControlStripLayout.measureStrip(List.of(picker), measurerFake);
            var withIcon = IconLabelRow.measureRowWidth(ControlStripLayout.CONTROL_ROW_HEIGHT,
                    2 * WIDTH_PER_CHAR, true);
            var withoutIcon = IconLabelRow.measureRowWidth(ControlStripLayout.CONTROL_ROW_HEIGHT,
                    3 * WIDTH_PER_CHAR, false);
            assertThat(measurement.rowWidths().get(0))
                    .isCloseTo(Math.max(withIcon, withoutIcon), within(TOLERANCE));
        }

        @Test
        void measureStripReservesEachOptionsTrailingValueInTheIconListWidth() {
            // A ranked table row must hold its crest, name, and value; the measurement reads the same
            // IconLabelRow geometry the renderer places the value with, so the column is wide enough
            // that "AB" clears its two-char value "12".
            var picker = ControlSpec.createIconRadioList(List.of("AB"), List.of("crest_ab"),
                    List.of("12"), ControlSpec.NO_SELECTION, ControlAction.NONE);
            var measurement = ControlStripLayout.measureStrip(List.of(picker), measurerFake);
            var withValue = IconLabelRow.measureRowWidth(ControlStripLayout.CONTROL_ROW_HEIGHT,
                    2 * WIDTH_PER_CHAR, true, 2 * WIDTH_PER_CHAR);
            assertThat(measurement.rowWidths().get(0)).isCloseTo(withValue, within(TOLERANCE));
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
        void layoutControlsSplitsAnIconListIntoStackedVerticalSegments() {
            var specs = List.of(ControlSpec.createIconRadioList(List.of("Hegemony", "Tri-Tachyon"),
                    List.of("crest_heg", "crest_tt"), ControlSpec.NO_SELECTION, ControlAction.NONE));
            var measurement = ControlStripLayout.measureStrip(specs, measurerFake);
            var picker = ControlStripLayout.layoutControls(frameBody(measurement), specs,
                    measurement.rowHeights(), measurement.rowWidths()).get(0);

            assertThat(picker.segments()).hasSize(2);
            var topSegment = picker.segments().get(0);
            var bottomSegment = picker.segments().get(1);
            // The list stacks top to bottom (element 0 is topmost, UI y grows up), the segments are
            // equal height, and the lower one hangs directly beneath the upper.
            assertThat(topSegment.y()).isGreaterThan(bottomSegment.y());
            assertThat(bottomSegment.height()).isCloseTo(topSegment.height(), within(TOLERANCE));
            assertThat(topSegment.y())
                    .isCloseTo(bottomSegment.y() + bottomSegment.height(), within(TOLERANCE));
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
        void layoutControlsLeavesADividerWithoutSegments() {
            // A divider is a single non-hit row, not a segmented control, so it lays out with no
            // segments like a caption does.
            var specs = List.of(ControlSpec.createDivider(),
                    ControlSpec.createCheckbox("Muted", false, ControlAction.NONE));
            var measurement = ControlStripLayout.measureStrip(specs, measurerFake);
            var controls = ControlStripLayout.layoutControls(frameBody(measurement), specs,
                    measurement.rowHeights(), measurement.rowWidths());
            assertThat(controls.get(0).segments()).isEmpty();
        }

        @Test
        void layoutControlsSpansADividerRowAcrossTheBodyContentWidth() {
            // The laid divider row is as wide as the widest content row (the checkbox), so the rule
            // crosses the whole body rather than snapping to zero width.
            var specs = List.of(ControlSpec.createDivider(),
                    ControlSpec.createCheckbox("Muted", false, ControlAction.NONE));
            var measurement = ControlStripLayout.measureStrip(specs, measurerFake);
            var controls = ControlStripLayout.layoutControls(frameBody(measurement), specs,
                    measurement.rowHeights(), measurement.rowWidths());
            assertThat(controls.get(0).bounds().width())
                    .isCloseTo(controls.get(1).bounds().width(), within(TOLERANCE));
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
