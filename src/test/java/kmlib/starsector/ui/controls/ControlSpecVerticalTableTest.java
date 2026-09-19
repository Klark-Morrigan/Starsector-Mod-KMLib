package kmlib.starsector.ui.controls;

import kmlib.starsector.ui.controls.specs.ControlAction;
import kmlib.starsector.ui.controls.specs.ControlHoverReport;
import kmlib.starsector.ui.controls.specs.ControlSpec;
import kmlib.starsector.ui.controls.specs.ReselectBehaviour;
import kmlib.starsector.ui.controls.specs.RowGeometry;
import kmlib.starsector.ui.controls.specs.VerticalTableSpec;
import kmlib.starsector.ui.text.TextSpan;
import kmlib.starsector.ui.widgets.RowSlot;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins the stacked table of rows that hold their own parts - a crest, a label, a trailing value -
 * its two geometries, the guards its construction still trips on, and the refinements a host
 * layers onto one after building it through an ordinary factory.
 */
final class ControlSpecVerticalTableTest {

    @Nested
    class VerticalTableConstructor {

        @Test
        void constructorRejectsAColumnCountBelowOne() {
            // A column count is how many columns the rows fold across; a count below one cannot lay
            // out any column, so it fails at construction rather than dividing by a zero column count.
            assertThatThrownBy(() -> new VerticalTableSpec(
                    VerticalTableSpecs.buildRows(List.of("A")),
                    RowGeometry.COLUMNS,
                    ControlSpec.NO_SELECTION,
                    ControlAction.NONE,
                    ControlHoverReport.NONE,
                    ReselectBehaviour.DESELECT,
                    0))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void constructorRejectsAMissingRowGeometry() {
            // How the rows lay out is stated rather than inferred, so a table built without that answer
            // fails where its builder is still on the stack rather than at the first measurement.
            assertThatThrownBy(() -> new VerticalTableSpec(
                    VerticalTableSpecs.buildRows(List.of("A")),
                    null,
                    ControlSpec.NO_SELECTION,
                    ControlAction.NONE,
                    ControlHoverReport.NONE,
                    ReselectBehaviour.DESELECT,
                    ControlSpec.SINGLE_COLUMN))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        void constructorRejectsAMissingHoverReport() {
            // A list reporting nowhere states that with NONE rather than with a null, which would pass
            // every layout and every draw and then throw from inside the first frame that put the pointer
            // on a row - nowhere near the host that built the list.
            assertThatThrownBy(() -> new VerticalTableSpec(
                    VerticalTableSpecs.buildRows(List.of("A")),
                    RowGeometry.COLUMNS,
                    ControlSpec.NO_SELECTION,
                    ControlAction.NONE,
                    null,
                    ReselectBehaviour.DESELECT,
                    ControlSpec.SINGLE_COLUMN))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        void constructorDoesNotAliasTheCallersRowList() {
            // The caller may hand in a mutable list it goes on to reuse; the table must copy it, so a
            // later mutation of the caller's list cannot rewrite the drawn rows.
            var callerRows = new ArrayList<>(VerticalTableSpecs.buildRows(
                List.of("Factions", "Alliances")));

            var table = new VerticalTableSpec(
                callerRows,
                RowGeometry.COLUMNS,
                0,
                ControlAction.NONE,
                ControlHoverReport.NONE,
                ReselectBehaviour.DESELECT,
                ControlSpec.SINGLE_COLUMN);

            callerRows.set(0, VerticalTableSpecs.buildRow("Mutated"));

            assertThat(table.labels())
                .containsExactly("Factions", "Alliances");
        }
    }

    @Nested
    class CreateColumnTable {

        @Test
        void createColumnTableLaysItsRowsInColumns() {
            // The geometry is what the table states about itself, so a picker's rows lay out as a table
            // whatever their slots hold - a stack whose rows all lead with nothing is a table still.
            var picker = VerticalTableSpec.createColumnTable(
                VerticalTableSpecs.buildRows(List.of("Hegemony", "Tri-Tachyon")),
                ControlSpec.NO_SELECTION,
                ControlAction.NONE);

            assertThat(picker.rowGeometry())
                .isEqualTo(RowGeometry.COLUMNS);
        }

        @Test
        void createColumnTableStandsInOneInertUnscrolledColumnUntilRefined() {
            // What a table holds before a host refines it: every row inert on a re-pick, one column, and
            // pinned - so a host states only the refinements its own list wants.
            var picker = VerticalTableSpec.createColumnTable(
                VerticalTableSpecs.buildRows(List.of("Hegemony", "Tri-Tachyon")),
                ControlSpec.NO_SELECTION,
                ControlAction.NONE);

            assertThat(picker.reselect())
                .isEqualTo(ReselectBehaviour.INERT);
            assertThat(picker.columnCount())
                .isEqualTo(1);
            assertThat(picker.hoverReport())
                .isEqualTo(ControlHoverReport.NONE);
        }

        @Test
        void createColumnTableCarriesItsRowsWhateverTheirSlotsHold() {
            // A row states what it leads and trails with itself, so a crested, valued row and a bare one
            // stack in one list rather than in a list per column that must stay index-aligned.
            var picker = VerticalTableSpec.createColumnTable(
                VerticalTableSpecs.buildRows(
                    List.of("Hegemony", "Free Traders"),
                    Arrays.asList("crest_heg", null),
                    List.of("7")),
                0,
                ControlAction.NONE);

            assertThat(picker.labelledRows())
                .hasSize(2);
            assertThat(picker.labelledRows().get(0).leadingRowSlot())
                .isEqualTo(new RowSlot.Image("crest_heg"));
            assertThat(picker.labelledRows().get(0).trailingRowSlot())
                .isEqualTo(new RowSlot.Text(new TextSpan("7", VerticalTableSpecs.ROW_TEXT_COLOUR)));
            assertThat(picker.labelledRows().get(1).leadingRowSlot())
                .isEqualTo(RowSlot.EMPTY);
            assertThat(picker.labelledRows().get(1).trailingRowSlot())
                .isEqualTo(RowSlot.EMPTY);
        }

        @Test
        void createColumnTableLightsTheSelectedRow() {

            var picker = VerticalTableSpecs.buildIconList(
                List.of("Hegemony", "Tri-Tachyon"),
                List.of("crest_heg", "crest_tt"),
                1,
                ControlAction.NONE);

            assertThat(picker.selectedIndex())
                .isEqualTo(1);
        }

        @Test
        void createColumnTableCarriesTheClickActionByRowIndex() {

            var firedCell = new int[] {-99};
            var picker = VerticalTableSpecs.buildIconList(
                List.of("Hegemony", "Tri-Tachyon"),
                List.of("crest_heg", "crest_tt"),
                ControlSpec.NO_SELECTION,
                cell -> firedCell[0] = cell);

            picker.action().activateCell(1);

            assertThat(firedCell[0])
                .isEqualTo(1);
        }
    }

    @Nested
    class CreateSegmentedList {

        @Test
        void createSegmentedListLaysItsRowsAsUniformCells() {
            // The other geometry a stack can read as: equal cells with each name centred, rather than a
            // table of columns - stated by which factory a host reaches for.
            var list = VerticalTableSpec.createSegmentedList(
                VerticalTableSpecs.buildRows(List.of("Factions", "Alliances")),
                0,
                ControlAction.NONE);

            assertThat(list.rowGeometry())
                .isEqualTo(RowGeometry.UNIFORM_SEGMENTS);
        }
    }

    @Nested
    class VerticalTableLabels {

        @Test
        void labelsAreEachRowsRunsReadAsOneLine() {
            // A row's label may be authored in several runs so part of it draws in its own colour; a
            // strip that snaps a control to its text charges the whole line, so the runs read as one -
            // parted by the run vocabulary's own space rather than by one written into a phrase.
            var table = VerticalTableSpec.createColumnTable(
                List.of(VerticalTableSpecs.buildRow("Hegemony")
                    .continuesWith(new TextSpan("(7)", VerticalTableSpecs.ROW_TEXT_COLOUR))),
                0,
                ControlAction.NONE);

            assertThat(table.labels())
                .containsExactly("Hegemony (7)");
        }
    }

    @Nested
    class HandlesReselect {

        @Test
        void handlesReselectCarriesTheChosenBehaviourAndLeavesTheRestAsItWas() {
            // A spotlight list clears on a re-pick of its lit row; the refinement changes that alone, so
            // everything the layout reads stays as the factory built it.
            var picker = VerticalTableSpec
                .createColumnTable(
                    VerticalTableSpecs.buildRows(List.of("Hegemony", "Tri-Tachyon")),
                    1,
                    ControlAction.NONE)
                .handlesReselect(ReselectBehaviour.DESELECT);

            assertThat(picker.reselect())
                .isEqualTo(ReselectBehaviour.DESELECT);
            assertThat(picker.labels())
                .containsExactly("Hegemony", "Tri-Tachyon");
            assertThat(picker.selectedIndex())
                .isEqualTo(1);
            assertThat(picker.rowGeometry())
                .isEqualTo(RowGeometry.COLUMNS);
        }
    }

    @Nested
    class ReportsHoverTo {

        @Test
        void reportsHoverToCarriesTheChannelAndLeavesTheRestAsItWas() {
            // A host answering a hover wires the channel onto a list built through the ordinary factory, so
            // the copy carries it while everything the layout and the press path read stays as it was.
            var reportedCells = new ArrayList<Integer>();

            var picker = VerticalTableSpec
                .createColumnTable(
                    VerticalTableSpecs.buildRows(List.of("Hegemony", "Tri-Tachyon")),
                    1,
                    ControlAction.NONE)
                .handlesReselect(ReselectBehaviour.DESELECT)
                .reportsHoverTo(reportedCells::add);

            picker.hoverReport().reportHoveredCell(1);

            assertThat(reportedCells)
                .containsExactly(1);
            assertThat(picker.labels())
                .containsExactly("Hegemony", "Tri-Tachyon");
            assertThat(picker.selectedIndex())
                .isEqualTo(1);
            assertThat(picker.reselect())
                .isEqualTo(ReselectBehaviour.DESELECT);
        }

        @Test
        void reportsHoverToSurvivesTheRefinementsLayeredAfterIt() {
            // The three refinements share one rebuild rather than each restating every component, which is
            // the fault this pins: a rebuild that dropped the channel would leave a list reporting nothing
            // for no reason its host could see.
            var picker = VerticalTableSpecs.buildIconList(
                    List.of("Hegemony", "Tri-Tachyon"),
                    List.of("crest_heg", "crest_tt"),
                    ControlSpec.NO_SELECTION,
                    ControlAction.NONE)
                .reportsHoverTo(hoveredCell -> {
                })
                .spreadsAcross(2);

            assertThat(picker.hoverReport())
                .isNotEqualTo(ControlHoverReport.NONE);
            assertThat(picker.columnCount())
                .isEqualTo(2);
        }

        @Test
        void reportsHoverToLeavesTheOriginalReportingNowhere() {
            // The copy is a fresh spec, so the source the host still holds is untouched - only the one it
            // wired reports.
            var picker = VerticalTableSpecs.buildIconList(
                List.of("Hegemony"),
                List.of("crest_heg"),
                0,
                ControlAction.NONE);

            picker.reportsHoverTo(hoveredCell -> {
            });

            assertThat(picker.hoverReport())
                .isEqualTo(ControlHoverReport.NONE);
        }
    }

    @Nested
    class SpreadsAcross {

        @Test
        void spreadsAcrossFoldsTheRowsAcrossThatManyColumns() {
            // The count rides on the spec so the layout and the renderer fold the rows the same way.
            var picker = VerticalTableSpec
                .createColumnTable(
                    VerticalTableSpecs.buildRows(List.of("Hegemony", "Tri-Tachyon")),
                    0,
                    ControlAction.NONE)
                .spreadsAcross(2);

            assertThat(picker.columnCount())
                .isEqualTo(2);
            assertThat(picker.labels())
                .containsExactly("Hegemony", "Tri-Tachyon");
        }
    }
}
