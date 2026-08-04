package kmlib.starsector.ui.layout;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.controls.ControlAction;
import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.controls.LabelledControlSpecs;
import kmlib.starsector.ui.controls.ReselectBehaviour;
import kmlib.starsector.ui.controls.VerticalTableSpecs;
import kmlib.starsector.ui.font.LineWidthMeasurer;
import kmlib.starsector.ui.widgets.PanelPlacement;
import kmlib.starsector.ui.widgets.RowColumnSpec;
import kmlib.testfixtures.starsector.ui.font.LineWidthMeasurerFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins {@link PanelLayout#computePlacement}: the headerless box hangs from the screen's top-left by its
 * padding, the border insets the content, and the body stacks whatever controls the host supplies - so
 * the rectangles a renderer draws are the ones a click hit-test reads. The box is just the bordered body:
 * no tab row (a tab panel adds that through {@link TabPanelLayout}).
 */
final class PanelLayoutTest {

    private static final float SCREEN_HEIGHT = 1080f;
    private static final int PADDING_TOP = 46;
    private static final int PADDING_LEFT = 12;

    // A small bottom margin; the bodies in these cases carry no scrolling control, so the cap never
    // engages and this value only feeds the (never-reached) cap arithmetic. The capped and scrolling
    // behaviour is exercised in the CappedBody tests below with a scrolling body.
    private static final int PADDING_BOTTOM = 12;
    private static final int BORDER_WIDTH = 2;
    private static final float TOLERANCE = 0.01f;

    // A round per-character width makes every snapped rectangle a hand-checkable multiple, so the
    // expected geometry is arithmetic rather than a measured constant.
    private static final float WIDTH_PER_CHAR = 10f;

    private final LineWidthMeasurer measurerFake = new LineWidthMeasurerFake(WIDTH_PER_CHAR);

    // A representative body of generic control specs the way any host would supply: a checkbox, a
    // two-option radio with a trailing caption, and a toggle. The layout snaps and stacks by geometry
    // alone, so each control's lit state is left unset here.
    private static final List<ControlSpec> BODY = List.of(
        LabelledControlSpecs.buildCheckbox("Uninhabited systems", false, ControlAction.NONE),
        ControlSpec.HorizontalRadio.of(
                List.of("Short", "Full"),
                ControlSpec.NO_SELECTION,
                ControlAction.NONE)
            .showsCaption("Names"),
        LabelledControlSpecs.buildToggle("Factions", false, ControlAction.NONE));

    // Content is inset from the box by the border on every edge; with no header the body hangs straight
    // from the inset content top.
    private static final float CONTENT_X = PADDING_LEFT + BORDER_WIDTH;
    private static final float BOX_TOP_Y = SCREEN_HEIGHT - PADDING_TOP;
    private static final float CONTENT_TOP_Y = BOX_TOP_Y - BORDER_WIDTH;

    @Nested
    class ComputePlacement {

        @Test
        void computePlacementHangsTheBoxFromTheTopLeftByItsPadding() {

            var box = place(BODY).box();

            assertThat(box.x())
                .isCloseTo(PADDING_LEFT, within(TOLERANCE));
            assertThat(box.y() + box.height())
                .as("box top edge sits paddingTop below the screen top")
                .isCloseTo(BOX_TOP_Y, within(TOLERANCE));
        }

        @Test
        void computePlacementLeavesAMinimalBorderedBoxWhenNoControlsAreGiven() {

            var placement = place(List.of());

            assertThat(placement.bodyControls())
                .isEmpty();

            // With no body and no header, the box collapses to just the border on every edge.
            assertThat(placement.box().width())
                .isCloseTo(2f * BORDER_WIDTH, within(TOLERANCE));
            assertThat(placement.box().height())
                .isCloseTo(2f * BORDER_WIDTH, within(TOLERANCE));
        }

        @Test
        void computePlacementStacksTheBodyControlsAsAColumnBeneathTheContentTop() {

            var controls = place(BODY).bodyControls();

            assertThat(controls).extracting(control -> control.spec().getClass().getSimpleName())
                .containsExactly("Checkbox", "HorizontalRadio", "Toggle");

            var rowsLeft = CONTENT_X + ControlStripLayout.BODY_PADDING;
            var checkbox = controls.get(0).bounds();

            // "Uninhabited systems" is 19 characters; the row is the tick box, a gap, then the label.
            var expectedCheckboxWidth = ControlStripLayout.CONTROL_ROW_HEIGHT
                + RowColumnSpec.CONTROL_ROW.leadingLabelGap()
                + 19 * WIDTH_PER_CHAR;

            assertThat(checkbox.x())
                .isCloseTo(rowsLeft, within(TOLERANCE));
            assertThat(checkbox.width())
                .isCloseTo(expectedCheckboxWidth, within(TOLERANCE));
            assertThat(checkbox.y() + checkbox.height())
                .as("the first control row hangs one body inset below the content top")
                .isCloseTo(CONTENT_TOP_Y - ControlStripLayout.BODY_PADDING, within(TOLERANCE));
        }

        @Test
        void computePlacementSplitsARadioIntoAbuttingEqualSegments() {

            var radio = place(BODY).bodyControls().get(1);

            assertThat(radio.segments())
                .hasSize(2);

            var shortSegment = radio.segments().get(0);
            var fullSegment = radio.segments().get(1);

            // The two segments are equal width and abut; the exact segment sizing is the control
            // strip's concern, pinned in ControlStripLayoutTest.
            assertThat(fullSegment.width())
                .isCloseTo(shortSegment.width(), within(TOLERANCE));
            assertThat(fullSegment.x())
                .as("the full segment abuts the right edge of the short segment")
                .isCloseTo(shortSegment.x() + shortSegment.width(), within(TOLERANCE));
            assertThat(fullSegment.y())
                .isCloseTo(shortSegment.y(), within(TOLERANCE));
        }

        @Test
        void computePlacementLeavesSingleHitControlsWithoutSegments() {

            var controls = place(BODY).bodyControls();

            assertThat(controls.get(0).segments())
                .as("checkbox has no segments")
                .isEmpty();
            assertThat(controls.get(2).segments())
                .as("toggle has no segments")
                .isEmpty();
        }

        @Test
        void computePlacementEnclosesEveryControlWithinTheBody() {

            var placement = place(BODY);
            var body = placement.body();

            assertThat(body.x())
                .isCloseTo(CONTENT_X, within(TOLERANCE));

            for (var control : placement.bodyControls()) {

                assertRectWithin(control.bounds(), body);

                for (var segment : control.segments()) {
                    assertRectWithin(segment, body);
                }
            }
        }

        @Test
        void computePlacementWrapsTheBodyWithTheBorderOnEveryEdge() {

            var placement = place(BODY);
            var box = placement.box();
            var body = placement.body();

            // No header: the box is exactly the body plus the border on every edge.
            assertThat(box.width())
                .isCloseTo(body.width() + 2f * BORDER_WIDTH, within(TOLERANCE));
            assertThat(box.height())
                .isCloseTo(body.height() + 2f * BORDER_WIDTH, within(TOLERANCE));
        }

        @Test
        void computePlacementGrowsTheBodyDownwardWhenAControlIsAppended() {

            var basePlacement = place(BODY);
            var appended = new ArrayList<>(BODY);

            appended.add(LabelledControlSpecs.buildCheckbox("Muted", false, ControlAction.NONE));

            var grownPlacement = place(List.copyOf(appended));

            // The extra row makes the body taller by exactly one control row plus its leading gap.
            assertThat(grownPlacement.body().height())
                .isCloseTo(
                    basePlacement.body().height()
                        + ControlStripLayout.CONTROL_ROW_HEIGHT
                        + ControlStripLayout.ROW_GAP,
                    within(TOLERANCE));

            // Growth is downward: the body's top edge, and so everything above it, does not move.
            assertThat(grownPlacement.body().y() + grownPlacement.body().height())
                .as("the body top edge is fixed; the extra row extends the bottom")
                .isCloseTo(
                    basePlacement.body().y() + basePlacement.body().height(),
                    within(TOLERANCE));

            var baseFirstRow = basePlacement.bodyControls().get(0).bounds();
            var grownFirstRow = grownPlacement.bodyControls().get(0).bounds();

            assertThat(grownFirstRow.y() + grownFirstRow.height())
                .as("the first control row is unshifted by an appended row below it")
                .isCloseTo(baseFirstRow.y() + baseFirstRow.height(), within(TOLERANCE));
        }

        @Test
        void computePlacementStandsAVerticalRadioOneRowTallPerOption() {

            var radio = place(verticalRadioBody()).bodyControls().get(0);

            // One option-row of height per segment, so a two-option radio is twice a control row. The
            // exact row width is the control strip's concern, pinned in ControlStripLayoutTest.
            assertThat(radio.bounds().height())
                .isCloseTo(2 * ControlStripLayout.CONTROL_ROW_HEIGHT, within(TOLERANCE));
        }

        @Test
        void computePlacementStacksAVerticalRadiosSegmentsTopToBottomInOneColumn() {

            var radio = place(verticalRadioBody()).bodyControls().get(0);

            assertThat(radio.segments())
                .hasSize(2);

            var top = radio.segments().get(0);
            var bottom = radio.segments().get(1);

            // Every segment shares the row's left edge and full width - a single column.
            assertThat(top.x())
                .isCloseTo(radio.bounds().x(), within(TOLERANCE));
            assertThat(bottom.x())
                .isCloseTo(radio.bounds().x(), within(TOLERANCE));
            assertThat(top.width())
                .isCloseTo(radio.bounds().width(), within(TOLERANCE));
            assertThat(bottom.width())
                .isCloseTo(radio.bounds().width(), within(TOLERANCE));

            // Each segment is one control-row tall, the first (Factions) on top of the second.
            assertThat(top.height())
                .isCloseTo(ControlStripLayout.CONTROL_ROW_HEIGHT, within(TOLERANCE));
            assertThat(top.y())
                .as("the top segment abuts the bottom segment's upper edge")
                .isCloseTo(bottom.y() + bottom.height(), within(TOLERANCE));
        }

        @Test
        void computePlacementSnapsALabelRowToItsMeasuredText() {

            var caption = "Non-allied factions are";
            var label = place(List.<ControlSpec>of(LabelledControlSpecs.buildLabel(caption)))
                .bodyControls()
                .get(0);

            // A caption has no widget chrome, so its row is exactly its text width and one row tall.
            assertThat(label.bounds().width())
                .isCloseTo(caption.length() * WIDTH_PER_CHAR, within(TOLERANCE));
            assertThat(label.bounds().height())
                .isCloseTo(ControlStripLayout.CONTROL_ROW_HEIGHT, within(TOLERANCE));
            assertThat(label.segments())
                .as("a caption is never clicked, so it splits into no hit segments")
                .isEmpty();
        }

        @Test
        void computePlacementCapsTheBoxToTheBottomMarginAndOpensAScrollViewport() {

            var placement = placeCapped(0f);

            // The box bottom clears the bottom margin (its top is fixed by the top padding), and the
            // list overruns the room left, so a scroll viewport and overflow are reported.
            assertThat(placement.box().y())
                .isGreaterThanOrEqualTo(TIGHT_PADDING_BOTTOM - TOLERANCE);
            assertThat(placement.scrollOverflow())
                .isGreaterThan(0f);
            assertThat(placement.isScrollbarNeeded())
                .isTrue();
            assertThat(placement.flexViewport().height())
                .isGreaterThan(0f);
        }

        @Test
        void computePlacementPinsTheHeaderAndFooterAroundTheScrollingList() {

            var placement = placeCapped(0f);
            var controls = placement.bodyControls();
            var header = controls.get(0).bounds();
            var footer = controls.get(controls.size() - 1).bounds();

            // The header hangs from the body top and the footer sits at the body bottom, with the list
            // (and its viewport) between them - the pinned-around-a-scroll shape.
            assertThat(header.y())
                .isGreaterThan(placement.flexViewport().y()
                    + placement.flexViewport().height()
                    - TOLERANCE);

            assertThat(footer.y())
                .isLessThan(placement.flexViewport().y() + TOLERANCE);
        }

        @Test
        void computePlacementBakesTheScrollOffsetIntoTheListBounds() {

            var atTop = placeCapped(0f).bodyControls().get(1).bounds();
            var scrolled = placeCapped(20f).bodyControls().get(1).bounds();

            // A larger scroll offset slides the list's content upward (UI y grows up) by that offset,
            // so the offset the placement reports is the shift baked into the list control's bounds.
            assertThat(scrolled.y() - atTop.y())
                .isCloseTo(placeCapped(20f).scrollOffset(), within(TOLERANCE));
            assertThat(placeCapped(20f).scrollOffset())
                .isCloseTo(20f, within(TOLERANCE));
        }

        private void assertRectWithin(Rectangle inner, Rectangle outer) {

            assertThat(inner.x())
                .isGreaterThanOrEqualTo(outer.x() - TOLERANCE);
            assertThat(inner.x() + inner.width())
                .isLessThanOrEqualTo(outer.x() + outer.width() + TOLERANCE);
            assertThat(inner.y())
                .isGreaterThanOrEqualTo(outer.y() - TOLERANCE);
            assertThat(inner.y() + inner.height())
                .isLessThanOrEqualTo(outer.y() + outer.height() + TOLERANCE);
        }

        private PanelPlacement place(List<ControlSpec> bodyControls) {
            return PanelLayout.computePlacement(
                SCREEN_HEIGHT,
                new Padding(PADDING_TOP, 0, PADDING_BOTTOM, PADDING_LEFT),
                BORDER_WIDTH,
                bodyControls,
                measurerFake,
                0f);
        }

        private PanelPlacement placeCapped(float rawScrollOffset) {
            return PanelLayout.computePlacement(
                SCREEN_HEIGHT,
                new Padding(PADDING_TOP, 0, TIGHT_PADDING_BOTTOM, PADDING_LEFT),
                BORDER_WIDTH,
                scrollingBody(),
                measurerFake,
                rawScrollOffset);
        }
    }

    // A two-option vertical selector radio, on its own so the stacked geometry is checked without the
    // other controls' rows in the way. "Alliances" (9 chars) is the wider option.
    private static List<ControlSpec> verticalRadioBody() {
        return List.of(VerticalTableSpecs.buildSegmentedList(
            List.of("Factions", "Alliances"),
            ControlSpec.NO_SELECTION,
            ControlAction.NONE,
            ReselectBehaviour.DESELECT));
    }

    // A bottom margin tight enough that the natural body overruns it, so the cap engages and the list
    // gives up height. Eight options make the list far taller than the room left.
    private static final int TIGHT_PADDING_BOTTOM = 900;

    // A body carrying a scrolling list (a header checkbox, the marked list, a footer checkbox).
    private static List<ControlSpec> scrollingBody() {

        var labels = new ArrayList<String>();
        var icons = new ArrayList<String>();

        for (var index = 0; index < 8; index++) {
            labels.add("Opt" + index);
            icons.add(null);
        }
        var list = VerticalTableSpecs.buildIconList(labels, icons, ControlSpec.NO_SELECTION,
            ControlAction.NONE).asScrolling();
            
        return List.of(
            LabelledControlSpecs.buildCheckbox("Header", false, ControlAction.NONE),
            list,
            LabelledControlSpecs.buildCheckbox("Footer", false, ControlAction.NONE));
    }
}
