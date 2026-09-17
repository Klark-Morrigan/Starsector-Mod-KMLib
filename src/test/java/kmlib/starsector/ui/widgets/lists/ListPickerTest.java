package kmlib.starsector.ui.widgets.lists;

import kmlib.testfixtures.starsector.ui.widgets.lists.Anomaly;
import kmlib.testfixtures.starsector.ui.widgets.lists.AnomalySortMode;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the bundle's two surfaces: a picker carries the pair it was built from untouched, and the
 * offers-nothing case is a real value rather than a null. The second is the one worth pinning as
 * behaviour - {@code empty()} carries no fallback mode, so a caller that reads its item list first
 * gets an empty list and a caller that resolved a sort against it would find nothing to fall back
 * to, which is the rule every consumer of an empty picker rests on.
 */
final class ListPickerTest {

    private static final ListSortModes<Anomaly> MODES =
        new ListSortModes<>(List.of(AnomalySortMode.values()), AnomalySortMode.ALPHA);

    private static final Anomaly MILD = new Anomaly("Mild", 1, 5);

    @Nested
    class Items {

        @Test
        void itemsAreTheListTheConsumerHandedOver() {
            // The bundle carries rather than reorders: the sort is applied where the list is drawn,
            // so what a consumer put in comes back out in the order it walked its own source.
            assertThat(new ListPicker<>(List.of(MILD), MODES).items())
                .containsExactly(MILD);
        }
    }

    @Nested
    class SortModes {

        @Test
        void sortModesAreTheVocabularyBundledWithTheItems() {
            assertThat(new ListPicker<>(List.of(MILD), MODES).sortModes())
                .isEqualTo(MODES);
        }
    }

    @Nested
    class Empty {

        @Test
        void emptyOffersNoItemsSoAConsumerWithNothingToSpotlightStillAnswersWithAPicker() {
            assertThat(ListPicker.empty().items())
                .isEmpty();
        }

        @Test
        void emptyCarriesNoVocabularyAndNoFallbackMode() {
            // Pinned because it is what makes the empty picker safe only in one reading order: the
            // item list says "nothing to draw" on its own, while the vocabulary would fail a stored
            // sort resolution with no mode to land on.
            assertThat(ListPicker.empty().sortModes().modes())
                .isEmpty();
            assertThat(ListPicker.empty().sortModes().defaultMode())
                .isNull();
        }
    }
}
