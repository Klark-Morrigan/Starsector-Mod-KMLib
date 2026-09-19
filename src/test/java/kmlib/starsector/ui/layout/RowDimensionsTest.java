package kmlib.starsector.ui.layout;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Covers the rule holding the two lists together: a run states one height and one width per row, so
 * a pair that could not describe a stack is refused where it is stated rather than indexed into
 * later by whoever tried to lay it out.
 */
class RowDimensionsTest {

    private static final List<Float> THREE_WIDTHS = List.of(100f, 60f, 80f);

    @Nested
    class Construction {

        @Test
        void keepsHeightsAndWidthsInTheOrderStated() {

            var rows = new RowDimensions(List.of(20f, 40f), List.of(100f, 60f));

            assertThat(rows.rowHeights()).containsExactly(20f, 40f);
            assertThat(rows.rowWidths()).containsExactly(100f, 60f);
        }

        @Test
        void rejectsARunStatingFewerHeightsThanWidths() {

            assertThatIllegalArgumentException()
                .isThrownBy(() -> new RowDimensions(List.of(20f), THREE_WIDTHS))
                .withMessageContaining("1 heights against 3 widths");
        }

        @Test
        void rejectsARunStatingMoreHeightsThanWidths() {

            // The silent half of the same fault: a longer height list would simply go unread, so a
            // run measured from more rows than it has widths for would lay out as if it matched.
            assertThatIllegalArgumentException()
                .isThrownBy(() -> new RowDimensions(List.of(20f, 40f, 20f, 40f), THREE_WIDTHS));
        }

        @Test
        void holdsACopyRatherThanTheCallersOwnLists() {

            var mutableHeights = new ArrayList<>(List.of(20f, 40f, 20f));
            var rows = new RowDimensions(mutableHeights, THREE_WIDTHS);

            mutableHeights.set(0, 999f);

            assertThat(rows.rowHeights()).containsExactly(20f, 40f, 20f);
        }
    }

    @Nested
    class CountRows {

        @Test
        void countsOneRowPerWidth() {

            assertThat(RowDimensions.createUniform(20f, THREE_WIDTHS).countRows())
                .isEqualTo(3);
        }

        @Test
        void countsNothingForTheEmptyRun() {

            assertThat(RowDimensions.EMPTY.countRows())
                .isZero();
        }
    }

    @Nested
    class CreateUniform {

        @Test
        void givesEveryRowTheSharedHeight() {

            var rows = RowDimensions.createUniform(20f, THREE_WIDTHS);

            assertThat(rows.rowHeights()).containsExactly(20f, 20f, 20f);
        }

        @Test
        void keepsEachRowsOwnWidth() {

            var rows = RowDimensions.createUniform(20f, THREE_WIDTHS);

            assertThat(rows.rowWidths()).containsExactly(100f, 60f, 80f);
        }

        @Test
        void statesNoRowsForNoWidths() {

            var rows = RowDimensions.createUniform(20f, List.of());

            assertThat(rows.rowHeights()).isEmpty();
            assertThat(rows.rowWidths()).isEmpty();
        }
    }
}
