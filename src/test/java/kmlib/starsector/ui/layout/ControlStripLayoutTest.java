package kmlib.starsector.ui.layout;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.controls.ControlAction;
import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.controls.LabelledControlSpecs;
import kmlib.starsector.ui.controls.ReselectBehaviour;
import kmlib.starsector.ui.controls.SegmentSizing;
import kmlib.starsector.ui.controls.VerticalTableSpecs;
import kmlib.starsector.ui.font.StripTextMeasurers;
import kmlib.starsector.ui.layout.ControlStripLayout.StripMeasurement;
import kmlib.starsector.ui.text.ImageSpan;
import kmlib.starsector.ui.text.TextSpan;
import kmlib.starsector.ui.widgets.IconLabelRow;
import kmlib.starsector.ui.widgets.RowColumnSpec;
import kmlib.starsector.ui.widgets.RowSlot;
import kmlib.starsector.ui.widgets.TriangleDirection;
import kmlib.starsector.ui.widgets.tabs.style.TabStyle;
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

    // A tab-face width far off the body one, so a row measured through the wrong face lands nowhere near
    // its expected multiple rather than within a rounding of it.
    private static final float TAB_WIDTH_PER_CHAR = 30f;

    private static final float TOLERANCE = 0.01f;
    private static final float BODY_ORIGIN_X = 100f;
    private static final float BODY_ORIGIN_Y = 200f;

    // Both faces measure alike here, so every expectation below stays a plain character count; the
    // faces being told apart is pinned where that is the point under test.
    private final StripTextMeasurers measurersFake = new StripTextMeasurers(
        new LineWidthMeasurerFake(WIDTH_PER_CHAR),
        new LineWidthMeasurerFake(WIDTH_PER_CHAR));

    // The pair with its two faces told apart, for the rows whose point is which face they are charged.
    private final StripTextMeasurers partedFacesMeasurersFake = new StripTextMeasurers(
        new LineWidthMeasurerFake(TAB_WIDTH_PER_CHAR),
        new LineWidthMeasurerFake(WIDTH_PER_CHAR));

    @Nested
    class MeasureStrip {

        @Test
        void measureStripReturnsZeroFootprintAndNoRowsForAnEmptyStrip() {

            var measurement = ControlStripLayout.measureStrip(List.<ControlSpec>of(), measurersFake);

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
                List.<ControlSpec>of(
                    LabelledControlSpecs.buildCheckbox("Muted", false, ControlAction.NONE)),
                measurersFake);

            var expectedRow = ControlStripLayout.CONTROL_ROW_HEIGHT
                + RowColumnSpec.CONTROL_ROW.leadingLabelGap() + 5 * WIDTH_PER_CHAR;

            assertThat(measurement.rowWidths().get(0))
                .isCloseTo(expectedRow, within(TOLERANCE));
            assertThat(measurement.bodyWidth())
                .isCloseTo(expectedRow + 2f * ControlStripLayout.BODY_PADDING, within(TOLERANCE));
        }

        @Test
        void measureStripChargesAContinuedLabelBothRunsAndTheGapBetweenThem() {
            // A control that picks part of its label out in another colour is drawn run by run, so the
            // row it is snapped into has to hold the runs and the word space parting them - "Muted" (5)
            // and "on" (2) at 10 per character, plus the face's own space, which this measurer charges
            // as the one character it is.
            var checkbox = LabelledControlSpecs
                .buildCheckbox("Muted", false, ControlAction.NONE)
                .continuesWith(LabelledControlSpecs.buildLabelSpan("on"));

            var measurement = ControlStripLayout.measureStrip(
                List.<ControlSpec>of(checkbox),
                measurersFake);

            var expectedRow = ControlStripLayout.CONTROL_ROW_HEIGHT
                + RowColumnSpec.CONTROL_ROW.leadingLabelGap()
                + 5 * WIDTH_PER_CHAR
                + WIDTH_PER_CHAR
                + 2 * WIDTH_PER_CHAR;

            assertThat(measurement.rowWidths().get(0))
                .isCloseTo(expectedRow, within(TOLERANCE));
        }

        @Test
        void measureStripChargesAnImageRunTheControlRowHeight() {
            // An image set among a label's words squares off the row it sits on, so the strip reserves
            // that square and the word space in front of it as it would any other run - "Muted" (5) at 10
            // per character, the face's own space, then the 20-unit square. Measured from the runs rather
            // than from the label's text, which holds no image and would size the row too narrow.
            var checkbox = LabelledControlSpecs
                .buildCheckbox("Muted", false, ControlAction.NONE)
                .continuesWith(new ImageSpan("graphics/hegemony_crest.png"));

            var measurement = ControlStripLayout.measureStrip(
                List.<ControlSpec>of(checkbox),
                measurersFake);

            var expectedRow = ControlStripLayout.CONTROL_ROW_HEIGHT
                + RowColumnSpec.CONTROL_ROW.leadingLabelGap()
                + 5 * WIDTH_PER_CHAR
                + WIDTH_PER_CHAR
                + ControlStripLayout.CONTROL_ROW_HEIGHT;

            assertThat(measurement.rowWidths().get(0))
                .isCloseTo(expectedRow, within(TOLERANCE));
        }

        @Test
        void measureStripChargesAContinuedToggleLabelBothRunsAndTheGapBetweenThem() {
            // A toggle sizes its button past its label, so the runs and their gap have to reach the
            // padding rather than only the first run - "Factions" (8) and "3" (1) at 10 per character,
            // plus the face's own space, plus the button's own padding.
            var toggle = LabelledControlSpecs
                .buildToggle("Factions", true, ControlAction.NONE)
                .continuesWith(LabelledControlSpecs.buildLabelSpan("3"));

            var measurement = ControlStripLayout.measureStrip(
                List.<ControlSpec>of(toggle),
                measurersFake);

            var expectedRow = 8 * WIDTH_PER_CHAR
                + WIDTH_PER_CHAR
                + 1 * WIDTH_PER_CHAR
                + ControlStripLayout.TOGGLE_TEXT_PADDING;

            assertThat(measurement.rowWidths().get(0))
                .isCloseTo(expectedRow, within(TOLERANCE));
        }

        @Test
        void measureStripChargesAColumnTableRowBothRunsAndTheGapBetweenThem() {
            // The shape a picker list actually draws: a crest, a name called out part-way through in
            // another colour, and a right-aligned value. The row has to hold both runs and the gap
            // parting them, or the column comes out narrower than the name painted into it - "AB" (2)
            // and "12" (2) at 10 per character with the face's own space between them.
            var table = ControlSpec.VerticalTable.createColumnTable(
                List.of(VerticalTableSpecs
                    .buildRow("AB")
                    .continuesWith(new TextSpan("12", VerticalTableSpecs.ROW_TEXT_COLOUR))
                    .leadsWith(new RowSlot.Image("crest_ab"))
                    .trailsWith(new RowSlot.Text(
                        new TextSpan("7", VerticalTableSpecs.ROW_TEXT_COLOUR)))),
                ControlSpec.NO_SELECTION,
                ControlAction.NONE);

            var measurement = ControlStripLayout.measureStrip(
                List.<ControlSpec>of(table),
                measurersFake);

            var expectedRow = IconLabelRow.measureRowWidth(
                ControlStripLayout.CONTROL_ROW_HEIGHT,
                2 * WIDTH_PER_CHAR + WIDTH_PER_CHAR + 2 * WIDTH_PER_CHAR,
                true,
                1 * WIDTH_PER_CHAR);

            assertThat(measurement.rowWidths().get(0))
                .isCloseTo(expectedRow, within(TOLERANCE));
        }

        @Test
        void measureStripChargesABlankRunNoGapAndNoWidth() {
            // A host assembling a run from parts and coming up empty gets the row it would have had
            // without that run, rather than one widened for glyphs that will never be painted.
            var caption = LabelledControlSpecs
                .buildLabel("Names")
                .continuesWith(LabelledControlSpecs.buildLabelSpan(""));

            var measurement = ControlStripLayout.measureStrip(
                List.<ControlSpec>of(caption),
                measurersFake);

            assertThat(measurement.rowWidths().get(0))
                .isCloseTo(5 * WIDTH_PER_CHAR, within(TOLERANCE));
        }

        @Test
        void measureStripSumsRowHeightsAndGapsPlusInset() {

            var measurement = ControlStripLayout.measureStrip(
                List.<ControlSpec>of(
                    LabelledControlSpecs.buildCheckbox("A", false, ControlAction.NONE),
                    LabelledControlSpecs.buildCheckbox("B", false, ControlAction.NONE)),
                measurersFake);

            // Two rows: twice the row height, one gap between them, and the inset top and bottom.
            var expectedHeight = 2f * ControlStripLayout.BODY_PADDING
                + 2f * ControlStripLayout.CONTROL_ROW_HEIGHT
                + ControlStripLayout.ROW_GAP;

            assertThat(measurement.bodyHeight())
                .isCloseTo(expectedHeight, within(TOLERANCE));
        }

        @Test
        void measureStripSizesAHorizontalRadioRowToEqualSegments() {

            var radio = ControlSpec.HorizontalRadio.of(
                    List.of("Short", "Full"),
                    ControlSpec.NO_SELECTION,
                    ControlAction.NONE)
                .showsCaption("Names");

            var measurement = ControlStripLayout.measureStrip(List.<ControlSpec>of(radio), measurersFake);

            // Each segment is the widest option ("Short", 5 chars) plus the segment padding; the row
            // is the two equal segments side by side.
            var segmentWidth = 5 * WIDTH_PER_CHAR + ControlStripLayout.RADIO_SEGMENT_PADDING;

            assertThat(measurement.rowWidths().get(0))
                .isCloseTo(2 * segmentWidth, within(TOLERANCE));
        }

        @Test
        void measureStripSnapsAHorizontalRadioToPerLabelWidths() {
            // A snapped horizontal radio sizes each cell to its own label plus the padding, so "Short"
            // (5) and "Full" (4) span 9 characters plus two paddings - narrower than the uniform row's
            // two widest-label cells.
            var radio = ControlSpec.HorizontalRadio.of(
                    List.of("Short", "Full"),
                    ControlSpec.NO_SELECTION,
                    ControlAction.NONE)
                .sizesSegments(SegmentSizing.SNAPPED);

            var measurement = ControlStripLayout.measureStrip(List.<ControlSpec>of(radio), measurersFake);
            var expected = 9 * WIDTH_PER_CHAR + 2 * ControlStripLayout.RADIO_SEGMENT_PADDING;

            assertThat(measurement.rowWidths().get(0))
                .isCloseTo(expected, within(TOLERANCE));
        }

        @Test
        void measureStripStandsAVerticalRadioOneRowTallPerOption() {

            var radio = VerticalTableSpecs.buildSegmentedList(
                List.of("Factions", "Alliances"),
                ControlSpec.NO_SELECTION,
                ControlAction.NONE,
                ReselectBehaviour.DESELECT);

            var measurement = ControlStripLayout.measureStrip(List.<ControlSpec>of(radio), measurersFake);

            assertThat(measurement.rowHeights().get(0))
                .isCloseTo(2 * ControlStripLayout.CONTROL_ROW_HEIGHT, within(TOLERANCE));
        }

        @Test
        void measureStripStandsAnIconListOneRowTallPerOption() {
            // The icon list stacks like a vertical radio, so it stands one control-row tall per
            // option regardless of icons.
            var picker = VerticalTableSpecs.buildIconList(
                List.of("Hegemony", "Tri-Tachyon"),
                List.of("crest_heg", "crest_tt"),
                ControlSpec.NO_SELECTION,
                ControlAction.NONE);

            var measurement = ControlStripLayout.measureStrip(List.<ControlSpec>of(picker), measurersFake);

            assertThat(measurement.rowHeights().get(0))
                .isCloseTo(2 * ControlStripLayout.CONTROL_ROW_HEIGHT, within(TOLERANCE));
        }

        @Test
        void measureStripLeavesADividerWithoutIntrinsicWidth() {
            // A divider has no text and no chrome, so it measures zero here - it is stretched to the
            // full framed body only at placement, once a host has framed the body rectangle.
            var specs = List.<ControlSpec>of(
                new ControlSpec.Divider(),
                LabelledControlSpecs.buildCheckbox("Muted", false, ControlAction.NONE));

            var measurement = ControlStripLayout.measureStrip(specs, measurersFake);

            assertThat(measurement.rowWidths().get(0))
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void measureStripDoesNotLetADividerDriveTheBodyWidth() {
            // The divider measures zero and is spanned only at placement, so a strip of only a
            // checkbox measures the same body width whether or not a divider heads it.
            var checkbox = LabelledControlSpecs.buildCheckbox("Muted", false, ControlAction.NONE);
            var withoutDivider = ControlStripLayout.measureStrip(List.<ControlSpec>of(checkbox), measurersFake);
            var withDivider = ControlStripLayout.measureStrip(
                List.<ControlSpec>of(new ControlSpec.Divider(), checkbox),
                measurersFake);

            assertThat(withDivider.bodyWidth())
                .isCloseTo(withoutDivider.bodyWidth(), within(TOLERANCE));
        }

        @Test
        void measureStripSizesAnIconListToItsWidestOptionRow() {
            // "AB" carries a crest, "CDE" does not; the row is the widest of the two, each sized
            // through the shared IconLabelRow geometry so the width tracks whether the option draws an
            // icon. The measurement reads that geometry rather than re-deriving the icon and gap sizes.
            var picker = VerticalTableSpecs.buildIconList(
                List.of("AB", "CDE"),
                Arrays.asList("crest_ab", null),
                ControlSpec.NO_SELECTION,
                ControlAction.NONE);

            var measurement = ControlStripLayout.measureStrip(List.<ControlSpec>of(picker), measurersFake);

            var withIcon = IconLabelRow.measureRowWidth(
                ControlStripLayout.CONTROL_ROW_HEIGHT,
                2 * WIDTH_PER_CHAR,
                true,
                RowSlot.NO_WIDTH);

            var withoutIcon = IconLabelRow.measureRowWidth(
                ControlStripLayout.CONTROL_ROW_HEIGHT,
                3 * WIDTH_PER_CHAR,
                false,
                RowSlot.NO_WIDTH);

            assertThat(measurement.rowWidths().get(0))
                .isCloseTo(Math.max(withIcon, withoutIcon), within(TOLERANCE));
        }

        @Test
        void measureStripStandsATwoColumnListOnlyAsTallAsItsLongestColumn() {
            // Three options across two columns wrap into two rows (the first column holds two, the
            // second one), so the list is two rows tall, not three - it wraps rather than stacking one
            // row per option.
            var picker = VerticalTableSpecs.buildIconList(List.of("A", "B", "C"),
                Arrays.asList(null, null, null),
                List.of(),
                ControlSpec.NO_SELECTION,
                ControlAction.NONE,
                2);

            var measurement = ControlStripLayout.measureStrip(List.<ControlSpec>of(picker), measurersFake);

            assertThat(measurement.rowHeights().get(0))
                .isCloseTo(2 * ControlStripLayout.CONTROL_ROW_HEIGHT, within(TOLERANCE));
        }

        @Test
        void measureStripSizesATwoColumnListToTwiceOneColumnsWidth() {
            // Two columns sit side by side, each sized to the widest option row, so the list is twice
            // a single column's width - the same width the one-column list of the same options measures.
            var labels = List.of("A", "B", "C");
            var icons = Arrays.asList((String) null, null, null);

            var oneColumn = VerticalTableSpecs.buildIconList(
                labels,
                icons,
                List.of(),
                ControlSpec.NO_SELECTION,
                ControlAction.NONE);

            var twoColumn = VerticalTableSpecs.buildIconList(
                labels,
                icons,
                List.of(),
                ControlSpec.NO_SELECTION,
                ControlAction.NONE,
                2);

            var oneWidth = ControlStripLayout
                .measureStrip(List.<ControlSpec>of(oneColumn), measurersFake)
                .rowWidths()
                .get(0);
            var twoWidth = ControlStripLayout
                .measureStrip(List.<ControlSpec>of(twoColumn), measurersFake)
                .rowWidths()
                .get(0);

            assertThat(twoWidth)
                .isCloseTo(2 * oneWidth, within(TOLERANCE));
        }

        @Test
        void measureStripReservesEachOptionsTrailingValueInTheIconListWidth() {
            // A ranked table row must hold its crest, name, and value; the measurement reads the same
            // IconLabelRow geometry the renderer places the value with, so the column is wide enough
            // that "AB" clears its two-char value "12".
            var picker = VerticalTableSpecs.buildIconList(
                List.of("AB"),
                List.of("crest_ab"),
                List.of("12"),
                ControlSpec.NO_SELECTION,
                ControlAction.NONE);

            var measurement = ControlStripLayout.measureStrip(List.<ControlSpec>of(picker), measurersFake);
            var withValue = IconLabelRow.measureRowWidth(
                ControlStripLayout.CONTROL_ROW_HEIGHT,
                2 * WIDTH_PER_CHAR,
                true,
                2 * WIDTH_PER_CHAR);

            assertThat(measurement.rowWidths().get(0))
                .isCloseTo(withValue, within(TOLERANCE));
        }

        @Test
        void measureStripReservesNoValueColumnForABlankTrailingRun() {
            // A ranked list under a mode with no metric fills every row's value with a blank run
            // rather than dropping the column, so the width must come out as though no row trailed
            // anything - otherwise the list widens by a gap in front of glyphs it never draws.
            var blankValued = VerticalTableSpecs.buildIconList(
                List.of("AB"),
                List.of("crest_ab"),
                List.of(""),
                ControlSpec.NO_SELECTION,
                ControlAction.NONE);

            var measurement = ControlStripLayout.measureStrip(
                List.<ControlSpec>of(blankValued),
                measurersFake);
            var withoutValue = IconLabelRow.measureRowWidth(
                ControlStripLayout.CONTROL_ROW_HEIGHT,
                2 * WIDTH_PER_CHAR,
                true,
                RowSlot.NO_WIDTH);

            assertThat(measurement.rowWidths().get(0))
                .isCloseTo(withoutValue, within(TOLERANCE));
        }

        @Test
        void measureStripReservesTheDirectionTriangleSlotInTheSortTableWidth() {
            // A direction table's trailing column is a fixed triangle slot, not measured text, so the
            // row reserves the triangle slot width the renderer sizes the triangle to rather than a
            // letter width it no longer draws.
            var selector = VerticalTableSpecs.buildDirectionTable(
                List.of("AB"),
                List.of(TriangleDirection.DOWN),
                ControlSpec.NO_SELECTION,
                ControlAction.NONE,
                ReselectBehaviour.REFIRE);

            var measurement = ControlStripLayout.measureStrip(List.<ControlSpec>of(selector), measurersFake);
            var withTriangle = IconLabelRow.measureRowWidth(
                ControlStripLayout.CONTROL_ROW_HEIGHT,
                2 * WIDTH_PER_CHAR,
                false,
                IconLabelRow.computeDirectionTriangleSlotWidth(ControlStripLayout.CONTROL_ROW_HEIGHT));

            assertThat(measurement.rowWidths().get(0))
                .isCloseTo(withTriangle, within(TOLERANCE));
        }

        @Test
        void measureStripSizesASideBySideRowToItsTwoColumnsPlusTheGap() {
            // The group lays its two columns across one row, so it is as wide as the left column, the
            // gap parting them, and the right column - not one column's width.
            var pair = new ControlSpec.SideBySide(
                List.of(LabelledControlSpecs.buildCheckbox("L", false, ControlAction.NONE)),
                List.of(LabelledControlSpecs.buildCheckbox("RR", false, ControlAction.NONE)));

            var measurement = ControlStripLayout.measureStrip(List.<ControlSpec>of(pair), measurersFake);

            var leftWidth = ControlStripLayout.CONTROL_ROW_HEIGHT
                + RowColumnSpec.CONTROL_ROW.leadingLabelGap()
                + 1 * WIDTH_PER_CHAR;

            var rightWidth = ControlStripLayout.CONTROL_ROW_HEIGHT
                + RowColumnSpec.CONTROL_ROW.leadingLabelGap()
                + 2 * WIDTH_PER_CHAR;

            assertThat(measurement.rowWidths().get(0))
                .isCloseTo(
                    leftWidth + ControlStripLayout.COLUMN_GAP + rightWidth,
                    within(TOLERANCE));
        }

        @Test
        void measureStripStandsASideBySideRowAsTallAsItsTallerColumn() {
            // The left column holds two stacked controls and the right one, so the group stands as tall
            // as the two-row left column - its taller side - and the right column top-aligns within it.
            var pair = new ControlSpec.SideBySide(
                List.of(
                    LabelledControlSpecs.buildCheckbox("A", false, ControlAction.NONE),
                    LabelledControlSpecs.buildCheckbox("B", false, ControlAction.NONE)),
                List.of(LabelledControlSpecs.buildCheckbox("C", false, ControlAction.NONE)));

            var measurement = ControlStripLayout.measureStrip(List.<ControlSpec>of(pair), measurersFake);

            assertThat(measurement.rowHeights().get(0))
                .isCloseTo(
                    2 * ControlStripLayout.CONTROL_ROW_HEIGHT + ControlStripLayout.ROW_GAP,
                    within(TOLERANCE));
        }

        @Test
        void measureStripStandsATabsRowOneTabHeightTall() {
            // A tabs row is drawn in the larger tab face, so it stands one tab-height tall rather than a
            // body-row tall.
            var tabs = new ControlSpec.Tabs(
                List.of("No Layer", "Political Map"),
                List.of("N", "P"),
                0,
                ControlAction.NONE);

            var measurement = ControlStripLayout.measureStrip(List.<ControlSpec>of(tabs), measurersFake);

            assertThat(measurement.rowHeights().get(0))
                .isCloseTo(TabsControlLayout.TAB_HEIGHT, within(TOLERANCE));
        }

        @Test
        void measureStripSizesATabsRowToItsTabsSnappedWidths() {
            // Neither key stands in its label, so both are spelt out: "No Layer  [Z]" is 13 chars and
            // "Political Map  [Q]" is 18. Each snaps to its width plus the tab padding (both clear the
            // minimum), and the row is the two tabs side by side.
            var tabs = new ControlSpec.Tabs(
                List.of("No Layer", "Political Map"),
                List.of("Z", "Q"),
                0, ControlAction.NONE);

            var measurement = ControlStripLayout.measureStrip(List.<ControlSpec>of(tabs), measurersFake);
            var first = 13 * WIDTH_PER_CHAR + TabsControlLayout.TAB_TEXT_PADDING;
            var second = 18 * WIDTH_PER_CHAR + TabsControlLayout.TAB_TEXT_PADDING;

            assertThat(measurement.rowWidths().get(0))
                .isCloseTo(first + second, within(TOLERANCE));
        }

        @Test
        void measureStripChargesABodyRowTheBodyFaceNotTheTabFace() {
            // A strip letters its body in one face and its tabs in another, so a body row charged the tab
            // face is sized against letters it is never drawn in. The two faces measure far apart here, so
            // reading the wrong one cannot pass as rounding.
            var measurement = ControlStripLayout.measureStrip(
                List.<ControlSpec>of(
                    LabelledControlSpecs.buildCheckbox("Muted", false, ControlAction.NONE)),
                partedFacesMeasurersFake);

            assertThat(measurement.rowWidths().get(0))
                .isCloseTo(
                    ControlStripLayout.CONTROL_ROW_HEIGHT
                        + RowColumnSpec.CONTROL_ROW.leadingLabelGap()
                        + 5 * WIDTH_PER_CHAR,
                    within(TOLERANCE));
        }

        @Test
        void measureStripChargesATabsRowTheTabFaceNotTheBodyFace() {
            // The other half of the same rule: the one row drawn in the tab face is charged that face, so
            // a row of tabs is as wide as the letters the band will actually carry. "No Layer  [Z]" is 13
            // chars and "Political Map  [Q]" is 18, both at the tab face's own width.
            var tabs = new ControlSpec.Tabs(
                List.of("No Layer", "Political Map"),
                List.of("Z", "Q"),
                0,
                ControlAction.NONE);

            var measurement = ControlStripLayout.measureStrip(
                List.<ControlSpec>of(tabs),
                partedFacesMeasurersFake);

            var first = 13 * TAB_WIDTH_PER_CHAR + TabsControlLayout.TAB_TEXT_PADDING;
            var second = 18 * TAB_WIDTH_PER_CHAR + TabsControlLayout.TAB_TEXT_PADDING;

            assertThat(measurement.rowWidths().get(0))
                .isCloseTo(first + second, within(TOLERANCE));
        }
    }

    @Nested
    class LayoutControls {

        @Test
        void layoutControlsStacksTheFirstRowFromTheBodyTopLeftInset() {

            var specs = List.<ControlSpec>of(
                LabelledControlSpecs.buildCheckbox("Muted", false, ControlAction.NONE));
            var measurement = ControlStripLayout.measureStrip(specs, measurersFake);

            var controls = ControlStripLayout.layoutControls(
                buildFrameBody(measurement),
                specs,
                measurement.rowHeights(),
                measurement.rowWidths(),
                measurersFake);

            var row = controls.get(0).bounds();

            assertThat(row.x())
                .isCloseTo(BODY_ORIGIN_X + ControlStripLayout.BODY_PADDING, within(TOLERANCE));
            assertThat(row.y() + row.height())
                .as("the first row hangs one inset below the body top")
                .isCloseTo(
                    BODY_ORIGIN_Y + measurement.bodyHeight() - ControlStripLayout.BODY_PADDING,
                    within(TOLERANCE));
        }

        @Test
        void layoutControlsSplitsARadioIntoAbuttingEqualSegments() {

            var specs = List.<ControlSpec>of(ControlSpec.HorizontalRadio.of(
                    List.of("Short", "Full"),
                    ControlSpec.NO_SELECTION,
                    ControlAction.NONE)
                .showsCaption("Names"));

            var measurement = ControlStripLayout.measureStrip(specs, measurersFake);
            var radio = ControlStripLayout.layoutControls(
                    buildFrameBody(measurement),
                    specs,
                    measurement.rowHeights(),
                    measurement.rowWidths(),
                    measurersFake)
                .get(0);

            assertThat(radio.segments()).hasSize(2);

            var shortSegment = radio.segments().get(0);
            var fullSegment = radio.segments().get(1);

            assertThat(fullSegment.width())
                .isCloseTo(shortSegment.width(), within(TOLERANCE));
            assertThat(fullSegment.x())
                .isCloseTo(shortSegment.x() + shortSegment.width(), within(TOLERANCE));
        }

        @Test
        void layoutControlsSplitsASnappedRadioIntoPerLabelSegments() {
            // A snapped horizontal radio splits into cells sized to each label, so "Short" (5) is wider
            // than "Full" (4) rather than sharing one width - the ragged row the render chrome then rules
            // its seams on.
            var specs = List.<ControlSpec>of(ControlSpec.HorizontalRadio.of(
                    List.of("Short", "Full"),
                    ControlSpec.NO_SELECTION,
                    ControlAction.NONE)
                .sizesSegments(SegmentSizing.SNAPPED));

            var measurement = ControlStripLayout.measureStrip(specs, measurersFake);
            var radio = ControlStripLayout.layoutControls(
                    buildFrameBody(measurement),
                    specs,
                    measurement.rowHeights(),
                    measurement.rowWidths(),
                    measurersFake)
                .get(0);

            assertThat(radio.segments()).hasSize(2);

            var shortSegment = radio.segments().get(0);
            var fullSegment = radio.segments().get(1);

            assertThat(shortSegment.width())
                .isCloseTo(
                    5 * WIDTH_PER_CHAR + ControlStripLayout.RADIO_SEGMENT_PADDING,
                    within(TOLERANCE));

            assertThat(fullSegment.width())
                .isCloseTo(
                    4 * WIDTH_PER_CHAR + ControlStripLayout.RADIO_SEGMENT_PADDING,
                    within(TOLERANCE));

            assertThat(fullSegment.x())
                .isCloseTo(shortSegment.x() + shortSegment.width(), within(TOLERANCE));
        }

        @Test
        void layoutControlsSplitsAnIconListIntoStackedVerticalSegments() {

            var specs = List.<ControlSpec>of(VerticalTableSpecs.buildIconList(
                List.of("Hegemony", "Tri-Tachyon"),
                List.of("crest_heg", "crest_tt"),
                ControlSpec.NO_SELECTION,
                ControlAction.NONE));

            var measurement = ControlStripLayout.measureStrip(specs, measurersFake);
            var picker = ControlStripLayout.layoutControls(
                    buildFrameBody(measurement),
                    specs,
                    measurement.rowHeights(),
                    measurement.rowWidths(),
                    measurersFake)
                .get(0);

            assertThat(picker.segments()).hasSize(2);

            var topSegment = picker.segments().get(0);
            var bottomSegment = picker.segments().get(1);

            // The list stacks top to bottom (element 0 is topmost, UI y grows up), the segments are
            // equal height, and the lower one hangs directly beneath the upper.
            assertThat(topSegment.y())
                .isGreaterThan(bottomSegment.y());
            assertThat(bottomSegment.height())
                .isCloseTo(topSegment.height(), within(TOLERANCE));
            assertThat(topSegment.y())
                .isCloseTo(bottomSegment.y() + bottomSegment.height(), within(TOLERANCE));
        }

        @Test
        void layoutControlsSplitsATwoColumnListColumnMajorIntoAGrid() {
            // Three options across two columns: options 0 and 1 fill the left column top to bottom, and
            // option 2 heads the right column - the same column-major wrap the renderer draws against.
            var specs = List.<ControlSpec>of(VerticalTableSpecs.buildIconList(
                List.of("A", "B", "C"),
                Arrays.asList(null, null, null),
                List.of(),
                ControlSpec.NO_SELECTION,
                ControlAction.NONE,
                2));

            var measurement = ControlStripLayout.measureStrip(specs, measurersFake);
            var picker = ControlStripLayout.layoutControls(
                    buildFrameBody(measurement),
                    specs,
                    measurement.rowHeights(),
                    measurement.rowWidths(),
                    measurersFake)
                .get(0);

            assertThat(picker.segments()).hasSize(3);

            var topLeft = picker.segments().get(0);
            var bottomLeft = picker.segments().get(1);
            var topRight = picker.segments().get(2);

            // Options 0 and 1 share the left column and stack (0 above 1); option 2 sits in the right
            // column, level with option 0 and one column-width to its right.
            assertThat(topLeft.x())
                .isCloseTo(bottomLeft.x(), within(TOLERANCE));
            assertThat(topLeft.y())
                .isGreaterThan(bottomLeft.y());
            assertThat(topRight.y())
                .isCloseTo(topLeft.y(), within(TOLERANCE));
            assertThat(topRight.x())
                .isCloseTo(topLeft.x() + topLeft.width(), within(TOLERANCE));
        }

        @Test
        void layoutControlsLeavesACheckboxWithoutSegments() {

            var specs = List.<ControlSpec>of(
                LabelledControlSpecs.buildCheckbox("Muted", false, ControlAction.NONE));
            var measurement = ControlStripLayout.measureStrip(specs, measurersFake);
            var controls = ControlStripLayout.layoutControls(
                buildFrameBody(measurement),
                specs,
                measurement.rowHeights(),
                measurement.rowWidths(),
                measurersFake);

            assertThat(controls.get(0).segments())
                .isEmpty();
        }

        @Test
        void layoutControlsLeavesALabelWithoutSegments() {

            var specs = List.<ControlSpec>of(LabelledControlSpecs.buildLabel("Non-allied factions are"));
            var measurement = ControlStripLayout.measureStrip(specs, measurersFake);
            var controls = ControlStripLayout.layoutControls(
                buildFrameBody(measurement),
                specs,
                measurement.rowHeights(),
                measurement.rowWidths(),
                measurersFake);

            assertThat(controls.get(0).segments())
                .isEmpty();
        }

        @Test
        void layoutControlsLeavesADividerWithoutSegments() {
            // A divider is a single non-hit row, not a segmented control, so it lays out with no
            // segments like a caption does.
            var specs = List.<ControlSpec>of(
                new ControlSpec.Divider(),
                LabelledControlSpecs.buildCheckbox("Muted", false, ControlAction.NONE));

            var measurement = ControlStripLayout.measureStrip(specs, measurersFake);
            var controls = ControlStripLayout.layoutControls(
                buildFrameBody(measurement),
                specs,
                measurement.rowHeights(),
                measurement.rowWidths(),
                measurersFake);

            assertThat(controls.get(0).segments())
                .isEmpty();
        }

        @Test
        void layoutControlsSpansADividerRowAcrossTheFullBodyWidth() {
            // The laid divider row spans the whole framed body - edge to edge inside the border inset,
            // across the padding the other controls sit within - so the rule reaches the frame rather
            // than stopping at the padded content column.
            var specs = List.<ControlSpec>of(
                new ControlSpec.Divider(),
                LabelledControlSpecs.buildCheckbox("Muted", false, ControlAction.NONE));

            var measurement = ControlStripLayout.measureStrip(specs, measurersFake);
            var body = buildFrameBody(measurement);
            var divider = ControlStripLayout.layoutControls(
                    body,
                    specs,
                    measurement.rowHeights(),
                    measurement.rowWidths(),
                    measurersFake)
                .get(0);

            assertThat(divider.bounds().x())
                .isCloseTo(body.x(), within(TOLERANCE));
            assertThat(divider.bounds().width())
                .isCloseTo(body.width(), within(TOLERANCE));
        }

        @Test
        void layoutControlsReturnsNothingForAnEmptyStrip() {

            var body = new Rectangle(BODY_ORIGIN_X, BODY_ORIGIN_Y, 0f, 0f);

            assertThat(ControlStripLayout.layoutControls(
                    body,
                    List.of(),
                    List.of(),
                    List.of(),
                    measurersFake))
                .isEmpty();
        }

        @Test
        void layoutControlsFlattensASideBySideIntoItsColumnsControlsSideBySide() {
            // The group is not laid out as one control: it expands into its two children, the left at
            // the body inset and the right one left-column-width plus the column gap to its right, both
            // hanging from the group's top.
            var left = LabelledControlSpecs.buildCheckbox("L", false, ControlAction.NONE);
            var right = LabelledControlSpecs.buildCheckbox("RR", false, ControlAction.NONE);
            var specs = List.<ControlSpec>of(new ControlSpec.SideBySide(List.of(left), List.of(right)));
            var measurement = ControlStripLayout.measureStrip(specs, measurersFake);
            var controls = ControlStripLayout.layoutControls(
                buildFrameBody(measurement),
                specs,
                measurement.rowHeights(),
                measurement.rowWidths(),
                measurersFake);

            assertThat(controls).hasSize(2);

            var leftControl = controls.get(0);
            var rightControl = controls.get(1);
            var leftWidth = ControlStripLayout.CONTROL_ROW_HEIGHT
                + RowColumnSpec.CONTROL_ROW.leadingLabelGap()
                + 1 * WIDTH_PER_CHAR;

            assertThat(leftControl.spec())
                .isEqualTo(left);
            assertThat(rightControl.spec())
                .isEqualTo(right);
            assertThat(leftControl.bounds().x())
                .isCloseTo(BODY_ORIGIN_X + ControlStripLayout.BODY_PADDING, within(TOLERANCE));

            assertThat(rightControl.bounds().x())
                .isCloseTo(
                    leftControl.bounds().x() + leftWidth + ControlStripLayout.COLUMN_GAP,
                    within(TOLERANCE));

            assertThat(rightControl.bounds().y() + rightControl.bounds().height())
                .as("both columns hang from the group's top")
                .isCloseTo(
                    leftControl.bounds().y() + leftControl.bounds().height(),
                    within(TOLERANCE));
        }

        @Test
        void layoutControlsStacksASideBySideColumnTopToBottom() {
            // A column is a vertical run like the top-level strip: its two children stack (the first
            // above the second), abutting with one row gap between them.
            var top = LabelledControlSpecs.buildCheckbox("A", false, ControlAction.NONE);
            var bottom = LabelledControlSpecs.buildCheckbox("B", false, ControlAction.NONE);
            var specs = List.<ControlSpec>of(new ControlSpec.SideBySide(
                List.of(top, bottom),
                List.of(LabelledControlSpecs.buildCheckbox("C", false, ControlAction.NONE))));

            var measurement = ControlStripLayout.measureStrip(specs, measurersFake);
            var controls = ControlStripLayout.layoutControls(
                buildFrameBody(measurement),
                specs,
                measurement.rowHeights(),
                measurement.rowWidths(),
                measurersFake);

            // The left column's two children come first (top then bottom), then the right column's one.
            assertThat(controls)
                .hasSize(3);

            var topControl = controls.get(0);
            var bottomControl = controls.get(1);

            assertThat(topControl.bounds().y())
                .isGreaterThan(bottomControl.bounds().y());
            assertThat(topControl.bounds().y())
                .isCloseTo(
                    bottomControl.bounds().y()
                        + bottomControl.bounds().height()
                        + ControlStripLayout.ROW_GAP,
                    within(TOLERANCE));
        }

        @Test
        void layoutControlsSplitsATabsRowIntoLabelSnappedSegmentsSideBySide() {
            // A tabs row splits into one segment per tab, each snapped to the text that tab shows (unlike
            // a radio's equal segments), abutting left to right. Both keys here are spelt out after their
            // label - neither letter stands in it - so each tab carries its bracketed key.
            var specs = List.<ControlSpec>of(new ControlSpec.Tabs(
                List.of("No Layer", "Political Map"),
                List.of("Z", "Q"),
                0,
                ControlAction.NONE));

            var measurement = ControlStripLayout.measureStrip(specs, measurersFake);
            var tabs = ControlStripLayout.layoutControls(
                    buildFrameBody(measurement),
                    specs,
                    measurement.rowHeights(),
                    measurement.rowWidths(),
                    measurersFake)
                .get(0);

            assertThat(tabs.segments())
                .hasSize(2);

            var first = tabs.segments().get(0);
            var second = tabs.segments().get(1);
            var firstWidth = 13 * WIDTH_PER_CHAR + TabsControlLayout.TAB_TEXT_PADDING;

            assertThat(first.width())
                .isCloseTo(firstWidth, within(TOLERANCE));
            assertThat(second.x())
                .as("the second tab abuts the first")
                .isCloseTo(first.x() + first.width(), within(TOLERANCE));
            assertThat(second.height())
                .isCloseTo(TabsControlLayout.TAB_HEIGHT, within(TOLERANCE));
        }

        @Test
        void layoutControlsLeavesABodyTabsRowOnTheUnstyledBandHeight() {
            // A tabs control placed in the BODY takes no style - it sizes itself through the strip's row
            // measurement - so it stands at the baseline band, where a styled header stands at its own.
            // The two paths are deliberately separate; this pins that the body one takes the baseline.
            var specs = List.<ControlSpec>of(new ControlSpec.Tabs(
                List.of("No Layer", "Political Map"),
                List.of("N", "P"),
                0,
                ControlAction.NONE));

            var measurement = ControlStripLayout.measureStrip(specs, measurersFake);
            var bodyTabs = ControlStripLayout.layoutControls(
                    buildFrameBody(measurement),
                    specs,
                    measurement.rowHeights(),
                    measurement.rowWidths(),
                    measurersFake)
                .get(0);

            assertThat(bodyTabs.bounds().height())
                .isCloseTo(TabStyle.DEFAULT_HEADER_BAND_HEIGHT, within(TOLERANCE));
            assertThat(bodyTabs.segments().get(0).height())
                .isCloseTo(TabStyle.DEFAULT_HEADER_BAND_HEIGHT, within(TOLERANCE));
        }
    }

    // Frames a body rectangle of the measured size at a fixed origin, the way a host sizes its chrome
    // around the strip footprint before handing the body back for placement.
    private static Rectangle buildFrameBody(StripMeasurement measurement) {
        return new Rectangle(
            BODY_ORIGIN_X,
            BODY_ORIGIN_Y,
            measurement.bodyWidth(),
            measurement.bodyHeight());
    }
}
