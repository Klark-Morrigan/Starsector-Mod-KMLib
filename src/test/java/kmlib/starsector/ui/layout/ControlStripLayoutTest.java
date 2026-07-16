package kmlib.starsector.ui.layout;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.controls.ControlAction;
import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.controls.ReselectBehaviour;
import kmlib.starsector.ui.controls.TriangleDirection;
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
            var measurement = ControlStripLayout.measureStrip(List.<ControlSpec>of(), measurerFake);
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
                    List.<ControlSpec>of(ControlSpec.Checkbox.lit("Muted", false, ControlAction.NONE)),
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
                    List.<ControlSpec>of(ControlSpec.Checkbox.lit("A", false, ControlAction.NONE),
                            ControlSpec.Checkbox.lit("B", false, ControlAction.NONE)),
                    measurerFake);
            // Two rows: twice the row height, one gap between them, and the inset top and bottom.
            var expectedHeight = 2f * ControlStripLayout.BODY_PADDING
                    + 2f * ControlStripLayout.CONTROL_ROW_HEIGHT + ControlStripLayout.ROW_GAP;
            assertThat(measurement.bodyHeight()).isCloseTo(expectedHeight, within(TOLERANCE));
        }

        @Test
        void measureStripSizesAHorizontalRadioRowToEqualSegments() {
            var radio = ControlSpec.HorizontalRadio.uniform(List.of("Short", "Full"), "Names",
                    ControlSpec.NO_SELECTION, ControlAction.NONE);
            var measurement = ControlStripLayout.measureStrip(List.<ControlSpec>of(radio), measurerFake);
            // Each segment is the widest option ("Short", 5 chars) plus the segment padding; the row
            // is the two equal segments side by side.
            var segmentWidth = 5 * WIDTH_PER_CHAR + ControlStripLayout.RADIO_SEGMENT_PADDING;
            assertThat(measurement.rowWidths().get(0)).isCloseTo(2 * segmentWidth, within(TOLERANCE));
        }

        @Test
        void measureStripSnapsAHorizontalRadioToPerLabelWidths() {
            // A snapped horizontal radio sizes each cell to its own label plus the padding, so "Short"
            // (5) and "Full" (4) span 9 characters plus two paddings - narrower than the uniform row's
            // two widest-label cells.
            var radio = ControlSpec.HorizontalRadio.snapped(List.of("Short", "Full"), "",
                    ControlSpec.NO_SELECTION, ControlAction.NONE);
            var measurement = ControlStripLayout.measureStrip(List.<ControlSpec>of(radio), measurerFake);
            var expected = 9 * WIDTH_PER_CHAR + 2 * ControlStripLayout.RADIO_SEGMENT_PADDING;
            assertThat(measurement.rowWidths().get(0)).isCloseTo(expected, within(TOLERANCE));
        }

        @Test
        void measureStripStandsAVerticalRadioOneRowTallPerOption() {
            var radio = ControlSpec.VerticalTable.plain(List.of("Factions", "Alliances"),
                    ControlSpec.NO_SELECTION, ControlAction.NONE, ReselectBehaviour.DESELECT);
            var measurement = ControlStripLayout.measureStrip(List.<ControlSpec>of(radio), measurerFake);
            assertThat(measurement.rowHeights().get(0))
                    .isCloseTo(2 * ControlStripLayout.CONTROL_ROW_HEIGHT, within(TOLERANCE));
        }

        @Test
        void measureStripStandsAnIconListOneRowTallPerOption() {
            // The icon list stacks like a vertical radio, so it stands one control-row tall per
            // option regardless of icons.
            var picker = ControlSpec.VerticalTable.iconList(List.of("Hegemony", "Tri-Tachyon"),
                    List.of("crest_heg", "crest_tt"), ControlSpec.NO_SELECTION, ControlAction.NONE);
            var measurement = ControlStripLayout.measureStrip(List.<ControlSpec>of(picker), measurerFake);
            assertThat(measurement.rowHeights().get(0))
                    .isCloseTo(2 * ControlStripLayout.CONTROL_ROW_HEIGHT, within(TOLERANCE));
        }

        @Test
        void measureStripLeavesADividerWithoutIntrinsicWidth() {
            // A divider has no text and no chrome, so it measures zero here - it is stretched to the
            // full framed body only at placement, once a host has framed the body rectangle.
            var specs = List.<ControlSpec>of(new ControlSpec.Divider(),
                    ControlSpec.Checkbox.lit("Muted", false, ControlAction.NONE));
            var measurement = ControlStripLayout.measureStrip(specs, measurerFake);
            assertThat(measurement.rowWidths().get(0)).isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void measureStripDoesNotLetADividerDriveTheBodyWidth() {
            // The divider measures zero and is spanned only at placement, so a strip of only a
            // checkbox measures the same body width whether or not a divider heads it.
            var checkbox = ControlSpec.Checkbox.lit("Muted", false, ControlAction.NONE);
            var withoutDivider = ControlStripLayout.measureStrip(List.<ControlSpec>of(checkbox), measurerFake);
            var withDivider = ControlStripLayout.measureStrip(
                    List.<ControlSpec>of(new ControlSpec.Divider(), checkbox), measurerFake);
            assertThat(withDivider.bodyWidth())
                    .isCloseTo(withoutDivider.bodyWidth(), within(TOLERANCE));
        }

        @Test
        void measureStripSizesAnIconListToItsWidestOptionRow() {
            // "AB" carries a crest, "CDE" does not; the row is the widest of the two, each sized
            // through the shared IconLabelRow geometry so the width tracks whether the option draws an
            // icon. The measurement reads that geometry rather than re-deriving the icon and gap sizes.
            var picker = ControlSpec.VerticalTable.iconList(List.of("AB", "CDE"),
                    Arrays.asList("crest_ab", null), ControlSpec.NO_SELECTION, ControlAction.NONE);
            var measurement = ControlStripLayout.measureStrip(List.<ControlSpec>of(picker), measurerFake);
            var withIcon = IconLabelRow.measureRowWidth(ControlStripLayout.CONTROL_ROW_HEIGHT,
                    2 * WIDTH_PER_CHAR, true);
            var withoutIcon = IconLabelRow.measureRowWidth(ControlStripLayout.CONTROL_ROW_HEIGHT,
                    3 * WIDTH_PER_CHAR, false);
            assertThat(measurement.rowWidths().get(0))
                    .isCloseTo(Math.max(withIcon, withoutIcon), within(TOLERANCE));
        }

        @Test
        void measureStripStandsATwoColumnListOnlyAsTallAsItsLongestColumn() {
            // Three options across two columns wrap into two rows (the first column holds two, the
            // second one), so the list is two rows tall, not three - it wraps rather than stacking one
            // row per option.
            var picker = ControlSpec.VerticalTable.iconList(List.of("A", "B", "C"),
                    Arrays.asList(null, null, null), List.of(), ControlSpec.NO_SELECTION,
                    ControlAction.NONE, 2);
            var measurement = ControlStripLayout.measureStrip(List.<ControlSpec>of(picker), measurerFake);
            assertThat(measurement.rowHeights().get(0))
                    .isCloseTo(2 * ControlStripLayout.CONTROL_ROW_HEIGHT, within(TOLERANCE));
        }

        @Test
        void measureStripSizesATwoColumnListToTwiceOneColumnsWidth() {
            // Two columns sit side by side, each sized to the widest option row, so the list is twice
            // a single column's width - the same width the one-column list of the same options measures.
            var labels = List.of("A", "B", "C");
            var icons = Arrays.asList((String) null, null, null);
            var oneColumn = ControlSpec.VerticalTable.iconList(labels, icons, List.of(),
                    ControlSpec.NO_SELECTION, ControlAction.NONE);
            var twoColumn = ControlSpec.VerticalTable.iconList(labels, icons, List.of(),
                    ControlSpec.NO_SELECTION, ControlAction.NONE, 2);
            var oneWidth = ControlStripLayout.measureStrip(List.<ControlSpec>of(oneColumn), measurerFake)
                    .rowWidths().get(0);
            var twoWidth = ControlStripLayout.measureStrip(List.<ControlSpec>of(twoColumn), measurerFake)
                    .rowWidths().get(0);
            assertThat(twoWidth).isCloseTo(2 * oneWidth, within(TOLERANCE));
        }

        @Test
        void measureStripReservesEachOptionsTrailingValueInTheIconListWidth() {
            // A ranked table row must hold its crest, name, and value; the measurement reads the same
            // IconLabelRow geometry the renderer places the value with, so the column is wide enough
            // that "AB" clears its two-char value "12".
            var picker = ControlSpec.VerticalTable.iconList(List.of("AB"), List.of("crest_ab"),
                    List.of("12"), ControlSpec.NO_SELECTION, ControlAction.NONE);
            var measurement = ControlStripLayout.measureStrip(List.<ControlSpec>of(picker), measurerFake);
            var withValue = IconLabelRow.measureRowWidth(ControlStripLayout.CONTROL_ROW_HEIGHT,
                    2 * WIDTH_PER_CHAR, true, 2 * WIDTH_PER_CHAR);
            assertThat(measurement.rowWidths().get(0)).isCloseTo(withValue, within(TOLERANCE));
        }

        @Test
        void measureStripReservesTheDirectionTriangleSlotInTheSortTableWidth() {
            // A direction table's trailing column is a fixed triangle slot, not measured text, so the
            // row reserves the triangle slot width the renderer sizes the triangle to rather than a
            // letter width it no longer draws.
            var selector = ControlSpec.VerticalTable.directionTable(List.of("AB"),
                    List.of(TriangleDirection.DOWN), ControlSpec.NO_SELECTION, ControlAction.NONE,
                    ReselectBehaviour.REFIRE);
            var measurement = ControlStripLayout.measureStrip(List.<ControlSpec>of(selector), measurerFake);
            var withTriangle = IconLabelRow.measureRowWidth(ControlStripLayout.CONTROL_ROW_HEIGHT,
                    2 * WIDTH_PER_CHAR, false,
                    IconLabelRow.computeDirectionTriangleSlotWidth(ControlStripLayout.CONTROL_ROW_HEIGHT));
            assertThat(measurement.rowWidths().get(0)).isCloseTo(withTriangle, within(TOLERANCE));
        }

        @Test
        void measureStripStandsATabsRowOneTabHeightTall() {
            // A tabs row is drawn in the larger tab face, so it stands one tab-height tall rather than a
            // body-row tall.
            var tabs = new ControlSpec.Tabs(List.of("No Layer", "Political Map"), List.of("N", "P"),
                    0, ControlAction.NONE);
            var measurement = ControlStripLayout.measureStrip(List.<ControlSpec>of(tabs), measurerFake);
            assertThat(measurement.rowHeights().get(0))
                    .isCloseTo(ControlStripLayout.TAB_HEIGHT, within(TOLERANCE));
        }

        @Test
        void measureStripSizesATabsRowToItsTabsSnappedWidths() {
            // "No Layer  [N]" is 13 chars and "Political Map  [P]" is 18; each snaps to its width plus
            // the tab padding (both clear the minimum), and the row is the two tabs side by side.
            var tabs = new ControlSpec.Tabs(List.of("No Layer", "Political Map"), List.of("N", "P"),
                    0, ControlAction.NONE);
            var measurement = ControlStripLayout.measureStrip(List.<ControlSpec>of(tabs), measurerFake);
            var first = 13 * WIDTH_PER_CHAR + ControlStripLayout.TAB_TEXT_PADDING;
            var second = 18 * WIDTH_PER_CHAR + ControlStripLayout.TAB_TEXT_PADDING;
            assertThat(measurement.rowWidths().get(0)).isCloseTo(first + second, within(TOLERANCE));
        }
    }

    @Nested
    class LayoutControls {

        @Test
        void layoutControlsStacksTheFirstRowFromTheBodyTopLeftInset() {
            var specs = List.<ControlSpec>of(ControlSpec.Checkbox.lit("Muted", false, ControlAction.NONE));
            var measurement = ControlStripLayout.measureStrip(specs, measurerFake);
            var controls = ControlStripLayout.layoutControls(frameBody(measurement), specs,
                    measurement.rowHeights(), measurement.rowWidths(), measurerFake);

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
            var specs = List.<ControlSpec>of(ControlSpec.HorizontalRadio.uniform(List.of("Short", "Full"), "Names",
                    ControlSpec.NO_SELECTION, ControlAction.NONE));
            var measurement = ControlStripLayout.measureStrip(specs, measurerFake);
            var radio = ControlStripLayout.layoutControls(frameBody(measurement), specs,
                    measurement.rowHeights(), measurement.rowWidths(), measurerFake).get(0);

            assertThat(radio.segments()).hasSize(2);
            var shortSegment = radio.segments().get(0);
            var fullSegment = radio.segments().get(1);
            assertThat(fullSegment.width()).isCloseTo(shortSegment.width(), within(TOLERANCE));
            assertThat(fullSegment.x())
                    .isCloseTo(shortSegment.x() + shortSegment.width(), within(TOLERANCE));
        }

        @Test
        void layoutControlsSplitsASnappedRadioIntoPerLabelSegments() {
            // A snapped horizontal radio splits into cells sized to each label, so "Short" (5) is wider
            // than "Full" (4) rather than sharing one width - the ragged row the render chrome then rules
            // its seams on.
            var specs = List.<ControlSpec>of(ControlSpec.HorizontalRadio.snapped(List.of("Short", "Full"), "",
                    ControlSpec.NO_SELECTION, ControlAction.NONE));
            var measurement = ControlStripLayout.measureStrip(specs, measurerFake);
            var radio = ControlStripLayout.layoutControls(frameBody(measurement), specs,
                    measurement.rowHeights(), measurement.rowWidths(), measurerFake).get(0);

            assertThat(radio.segments()).hasSize(2);
            var shortSegment = radio.segments().get(0);
            var fullSegment = radio.segments().get(1);
            assertThat(shortSegment.width())
                    .isCloseTo(5 * WIDTH_PER_CHAR + ControlStripLayout.RADIO_SEGMENT_PADDING,
                            within(TOLERANCE));
            assertThat(fullSegment.width())
                    .isCloseTo(4 * WIDTH_PER_CHAR + ControlStripLayout.RADIO_SEGMENT_PADDING,
                            within(TOLERANCE));
            assertThat(fullSegment.x())
                    .isCloseTo(shortSegment.x() + shortSegment.width(), within(TOLERANCE));
        }

        @Test
        void layoutControlsSplitsAnIconListIntoStackedVerticalSegments() {
            var specs = List.<ControlSpec>of(ControlSpec.VerticalTable.iconList(List.of("Hegemony", "Tri-Tachyon"),
                    List.of("crest_heg", "crest_tt"), ControlSpec.NO_SELECTION, ControlAction.NONE));
            var measurement = ControlStripLayout.measureStrip(specs, measurerFake);
            var picker = ControlStripLayout.layoutControls(frameBody(measurement), specs,
                    measurement.rowHeights(), measurement.rowWidths(), measurerFake).get(0);

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
        void layoutControlsSplitsATwoColumnListColumnMajorIntoAGrid() {
            // Three options across two columns: options 0 and 1 fill the left column top to bottom, and
            // option 2 heads the right column - the same column-major wrap the renderer draws against.
            var specs = List.<ControlSpec>of(ControlSpec.VerticalTable.iconList(List.of("A", "B", "C"),
                    Arrays.asList(null, null, null), List.of(), ControlSpec.NO_SELECTION,
                    ControlAction.NONE, 2));
            var measurement = ControlStripLayout.measureStrip(specs, measurerFake);
            var picker = ControlStripLayout.layoutControls(frameBody(measurement), specs,
                    measurement.rowHeights(), measurement.rowWidths(), measurerFake).get(0);

            assertThat(picker.segments()).hasSize(3);
            var topLeft = picker.segments().get(0);
            var bottomLeft = picker.segments().get(1);
            var topRight = picker.segments().get(2);
            // Options 0 and 1 share the left column and stack (0 above 1); option 2 sits in the right
            // column, level with option 0 and one column-width to its right.
            assertThat(topLeft.x()).isCloseTo(bottomLeft.x(), within(TOLERANCE));
            assertThat(topLeft.y()).isGreaterThan(bottomLeft.y());
            assertThat(topRight.y()).isCloseTo(topLeft.y(), within(TOLERANCE));
            assertThat(topRight.x()).isCloseTo(topLeft.x() + topLeft.width(), within(TOLERANCE));
        }

        @Test
        void layoutControlsLeavesACheckboxWithoutSegments() {
            var specs = List.<ControlSpec>of(ControlSpec.Checkbox.lit("Muted", false, ControlAction.NONE));
            var measurement = ControlStripLayout.measureStrip(specs, measurerFake);
            var controls = ControlStripLayout.layoutControls(frameBody(measurement), specs,
                    measurement.rowHeights(), measurement.rowWidths(), measurerFake);
            assertThat(controls.get(0).segments()).isEmpty();
        }

        @Test
        void layoutControlsLeavesALabelWithoutSegments() {
            var specs = List.<ControlSpec>of(new ControlSpec.Label("Non-allied factions are"));
            var measurement = ControlStripLayout.measureStrip(specs, measurerFake);
            var controls = ControlStripLayout.layoutControls(frameBody(measurement), specs,
                    measurement.rowHeights(), measurement.rowWidths(), measurerFake);
            assertThat(controls.get(0).segments()).isEmpty();
        }

        @Test
        void layoutControlsLeavesADividerWithoutSegments() {
            // A divider is a single non-hit row, not a segmented control, so it lays out with no
            // segments like a caption does.
            var specs = List.<ControlSpec>of(new ControlSpec.Divider(),
                    ControlSpec.Checkbox.lit("Muted", false, ControlAction.NONE));
            var measurement = ControlStripLayout.measureStrip(specs, measurerFake);
            var controls = ControlStripLayout.layoutControls(frameBody(measurement), specs,
                    measurement.rowHeights(), measurement.rowWidths(), measurerFake);
            assertThat(controls.get(0).segments()).isEmpty();
        }

        @Test
        void layoutControlsSpansADividerRowAcrossTheFullBodyWidth() {
            // The laid divider row spans the whole framed body - edge to edge inside the border inset,
            // across the padding the other controls sit within - so the rule reaches the frame rather
            // than stopping at the padded content column.
            var specs = List.<ControlSpec>of(new ControlSpec.Divider(),
                    ControlSpec.Checkbox.lit("Muted", false, ControlAction.NONE));
            var measurement = ControlStripLayout.measureStrip(specs, measurerFake);
            var body = frameBody(measurement);
            var divider = ControlStripLayout.layoutControls(body, specs,
                    measurement.rowHeights(), measurement.rowWidths(), measurerFake).get(0);
            assertThat(divider.bounds().x()).isCloseTo(body.x(), within(TOLERANCE));
            assertThat(divider.bounds().width()).isCloseTo(body.width(), within(TOLERANCE));
        }

        @Test
        void layoutControlsReturnsNothingForAnEmptyStrip() {
            var body = new Rectangle(BODY_ORIGIN_X, BODY_ORIGIN_Y, 0f, 0f);
            assertThat(ControlStripLayout.layoutControls(body, List.of(), List.of(), List.of(),
                    measurerFake)).isEmpty();
        }

        @Test
        void layoutControlsSplitsATabsRowIntoLabelSnappedSegmentsSideBySide() {
            // A tabs row splits into one segment per tab, each snapped to its own label-plus-shortcut
            // width (unlike a radio's equal segments), abutting left to right.
            var specs = List.<ControlSpec>of(new ControlSpec.Tabs(List.of("No Layer", "Political Map"),
                    List.of("N", "P"), 0, ControlAction.NONE));
            var measurement = ControlStripLayout.measureStrip(specs, measurerFake);
            var tabs = ControlStripLayout.layoutControls(frameBody(measurement), specs,
                    measurement.rowHeights(), measurement.rowWidths(), measurerFake).get(0);

            assertThat(tabs.segments()).hasSize(2);
            var first = tabs.segments().get(0);
            var second = tabs.segments().get(1);
            var firstWidth = 13 * WIDTH_PER_CHAR + ControlStripLayout.TAB_TEXT_PADDING;
            assertThat(first.width()).isCloseTo(firstWidth, within(TOLERANCE));
            assertThat(second.x())
                    .as("the second tab abuts the first")
                    .isCloseTo(first.x() + first.width(), within(TOLERANCE));
            assertThat(second.height()).isCloseTo(ControlStripLayout.TAB_HEIGHT, within(TOLERANCE));
        }
    }

    // Frames a body rectangle of the measured size at a fixed origin, the way a host sizes its chrome
    // around the strip footprint before handing the body back for placement.
    private static Rectangle frameBody(StripMeasurement measurement) {
        return new Rectangle(BODY_ORIGIN_X, BODY_ORIGIN_Y, measurement.bodyWidth(),
                measurement.bodyHeight());
    }
}
