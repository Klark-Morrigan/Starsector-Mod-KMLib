package kmlib.starsector.ui.input;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.controls.Control;
import kmlib.starsector.ui.controls.ControlAction;
import kmlib.starsector.ui.controls.ControlHoverReport;
import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.controls.LabelledControlSpecs;
import kmlib.starsector.ui.controls.VerticalTableSpecs;
import kmlib.starsector.ui.sound.PointerArrivalTarget;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins which kind of thing a resolved body cell is - the one question a hover asks of the control the walk
 * had in hand, and the only part of a body arrival the panel works out for itself rather than taking from
 * the look. Answered wrongly, a control sounds at a level meant for another kind, which nothing on screen
 * shows and no other case would fail on.
 */
final class ResolvedBodyCellTest {

    // A row somewhere away from the origin, since nothing here turns on where the control is laid.
    private static final Rectangle ROW = new Rectangle(100f, 200f, 160f, 20f);

    // The two halves of a segment box, for the controls whose cells are laid side by side.
    private static final Rectangle LEFT_SEGMENT = new Rectangle(100f, 200f, 80f, 20f);
    private static final Rectangle RIGHT_SEGMENT = new Rectangle(180f, 200f, 80f, 20f);

    private static final BodyCellSlot FIRST_SLOT = new BodyCellSlot(0, ControlSpec.SINGLE_CELL);

    @Nested
    class ResolveArrivalTarget {

        @Test
        void resolveArrivalTargetAnswersSingleOptionControlForAWholeRowCheckbox() {
            // Hit anywhere on its row and with one answer to give, so reaching it is the player having
            // aimed at it rather than having swept past it.
            assertThat(buildResolvedCell(buildCheckboxControl()).resolveArrivalTarget())
                .isEqualTo(PointerArrivalTarget.SINGLE_OPTION_CONTROL);
        }

        @Test
        void resolveArrivalTargetAnswersListedItemForARowOfSegments() {
            // A radio's segments abut, so one sweep across the row crosses every one of them - the kind a
            // look quietens.
            assertThat(buildResolvedCell(buildHorizontalRadioControl()).resolveArrivalTarget())
                .isEqualTo(PointerArrivalTarget.LISTED_ITEM);
        }

        @Test
        void resolveArrivalTargetAnswersListedItemForAListsRows() {
            // A list's rows are laid as segments too, and a sweep down a column of them passes many. The
            // axis is what the player is doing rather than which widget drew it, so a table answers as a
            // radio does however differently the two are painted.
            assertThat(buildResolvedCell(buildScrollingListControl()).resolveArrivalTarget())
                .isEqualTo(PointerArrivalTarget.LISTED_ITEM);
        }
    }

    @Nested
    class ResolveHoverReport {

        @Test
        void resolveHoverReportAnswersTheChannelTheControlWasWiredWith() {
            // Read off the control while the walk still holds it, so what a frame's reading carries on is
            // the channel rather than the widget it came from - the spec being gone by the next rebuild.
            var reportedCells = new ArrayList<Integer>();

            var hoverReport = buildResolvedCell(buildScrollingListControl(reportedCells::add))
                .resolveHoverReport();
            hoverReport.reportHoveredCell(1);

            assertThat(reportedCells)
                .containsExactly(1);
        }

        @Test
        void resolveHoverReportAnswersNoneForAControlThatTakesNoReport() {
            // Most controls take none, so the walk answers a channel that drops what it is told rather than
            // a null the frame would have to check before every report.
            assertThat(buildResolvedCell(buildCheckboxControl()).resolveHoverReport())
                .isEqualTo(ControlHoverReport.NONE);
        }
    }

    // The hit as the walk hands it over: a control and the slot it was found at. The slot never decides the
    // kind - a whole-row control's single cell and a row's first segment are both index zero - so every
    // case here names the same one and varies only the control.
    private static ResolvedBodyCell buildResolvedCell(Control control) {
        return new ResolvedBodyCell(control, FIRST_SLOT);
    }

    private static Control buildCheckboxControl() {
        return new Control(
            LabelledControlSpecs.buildCheckbox("X", false, ControlAction.NONE),
            ROW,
            List.of());
    }

    private static Control buildHorizontalRadioControl() {
        return new Control(
            ControlSpec.HorizontalRadio.of(
                List.of("Left", "Right"),
                ControlSpec.NO_SELECTION,
                ControlAction.NONE),
            ROW,
            List.of(LEFT_SEGMENT, RIGHT_SEGMENT));
    }

    private static Control buildScrollingListControl() {
        return buildScrollingListControl(ControlHoverReport.NONE);
    }

    // The same list wired to report the row under the pointer, for the cases about the channel the walk
    // reads off it.
    private static Control buildScrollingListControl(ControlHoverReport hoverReport) {

        var spec = VerticalTableSpecs.buildIconList(
                List.of("First", "Second"),
                Arrays.asList((String) null, null),
                ControlSpec.NO_SELECTION,
                ControlAction.NONE)
            .reportsHoverTo(hoverReport)
            .asScrolling();

        return new Control(spec, ROW, List.of(LEFT_SEGMENT, RIGHT_SEGMENT));
    }
}
