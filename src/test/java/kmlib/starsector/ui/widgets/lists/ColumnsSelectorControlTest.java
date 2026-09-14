package kmlib.starsector.ui.widgets.lists;

import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.controls.SegmentSizing;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the columns selector: a two-segment horizontal radio with one segment per column choice in the
 * choice's own order, lit on the active count, trailed by the caller's caption. Clicking a segment
 * reports that count. The pick is collected through the reporting callback, so this pins the
 * selector's shape and wiring without any store in reach.
 */
final class ColumnsSelectorControlTest {

    // The choice segments in the order the selector lays them out, so a test maps a segment index
    // back to a choice.
    private static final List<ListColumns> CHOICES = List.of(ListColumns.values());

    // The caption the caller resolved and handed over; a literal stands in for whatever a mod's own
    // strings table would return.
    private static final String CAPTION = "Columns";

    // The counts a click reported, in the order they were reported, so a test reads what the caller
    // would have been asked to persist.
    private final List<ListColumns> pickedColumns = new ArrayList<>();

    @Nested
    class BuildSelector {

        @Test
        void buildSelectorBuildsATwoSegmentHorizontalRadio() {
            var selector = buildSelector(ListColumns.ONE);

            // A horizontal radio by type; its even-cell segments read the default UNIFORM sizing.
            assertThat(selector.segmentSizing())
                .isEqualTo(SegmentSizing.UNIFORM);
            assertThat(selector.labels())
                .containsExactly("1", "2");
        }

        @Test
        void buildSelectorTrailsTheSegmentsWithTheCallersCaption() {
            // The caption is the caller's prose, handed over drawn - the segments themselves say
            // only "1" and "2", so without it the row does not say what the counts choose between.
            var selector = buildSelector(ListColumns.ONE);

            assertThat(selector.hasTrailingCaption())
                .isTrue();
            assertThat(selector.trailingLabel())
                .isEqualTo(CAPTION);
        }

        @Test
        void buildSelectorLightsTheActiveChoicesSegment() {
            var selector = buildSelector(ListColumns.TWO);

            assertThat(selector.selectedIndex())
                .isEqualTo(CHOICES.indexOf(ListColumns.TWO));
        }
    }

    @Nested
    class ApplySelection {

        @Test
        void clickingASegmentReportsThatColumnCount() {
            var selector = buildSelector(ListColumns.ONE);

            selector.action().activateCell(CHOICES.indexOf(ListColumns.TWO));

            assertThat(pickedColumns)
                .containsExactly(ListColumns.TWO);
        }

        @Test
        void clickingOutsideTheSegmentsReportsNothing() {
            // A stray hit past the last segment names no choice, so it is ignored rather than
            // reporting a phantom count the caller would persist.
            var selector = buildSelector(ListColumns.ONE);

            selector.action().activateCell(CHOICES.size());

            assertThat(pickedColumns)
                .isEmpty();
        }
    }

    // Builds the selector on the shared caption, collecting whatever a click reports, so each call
    // site reads as the active choice it exercises alone.
    private ControlSpec.HorizontalRadio buildSelector(ListColumns activeColumns) {
        return ColumnsSelectorControl.buildSelector(
            activeColumns,
            CAPTION,
            pickedColumns::add);
    }
}
