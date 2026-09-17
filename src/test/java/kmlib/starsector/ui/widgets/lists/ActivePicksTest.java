package kmlib.starsector.ui.widgets.lists;

import kmlib.testfixtures.starsector.ui.widgets.lists.Anomaly;
import kmlib.testfixtures.starsector.ui.widgets.lists.AnomalySortMode;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins which of a picker's three live values may be absent: the spotlighted ID, and only that one.
 * A reading assembled without a sort or a column count is rejected where it is built, since both are
 * always in force wherever a list is drawn at all.
 */
final class ActivePicksTest {

    // The fixture vocabulary the bundled sort is chosen from, and the sort itself in its mode's own
    // natural direction - the state a caller that has never flipped the sort reads back.
    private static final ListSortModes<Anomaly> MODES =
        new ListSortModes<>(List.of(AnomalySortMode.values()), AnomalySortMode.ALPHA);

    private static final ListSort<Anomaly> SORT = new ListSort<>(
        AnomalySortMode.ALPHA,
        AnomalySortMode.ALPHA.defaultDirection(),
        MODES);

    @Nested
    class Constructor {

        @Test
        void constructorAcceptsNoSpotlightedItem() {
            // Nothing spotlighted is an ordinary reading, not a half-built one: the list draws every
            // option with no row lit, which is what a cleared spotlight looks like.
            var picks = new ActivePicks<>(null, SORT, ListColumns.ONE);

            assertThat(picks.selectedItemId())
                .isNull();
        }

        @Test
        void constructorRejectsAReadingWithNoSort() {
            // A list is always ordered somehow, so a missing sort is a reading that was never
            // finished - and one that would otherwise fail inside the frame that first ranks a list.
            assertThatThrownBy(() -> new ActivePicks<Anomaly>("storm_1", null, ListColumns.ONE))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("sort");
        }

        @Test
        void constructorRejectsAReadingWithNoColumnCount() {
            // The same for the layout half: a list is always laid across some number of columns, so
            // this fails where it is assembled rather than where the rows are spread.
            assertThatThrownBy(() -> new ActivePicks<>("storm_1", SORT, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("columns");
        }
    }
}
