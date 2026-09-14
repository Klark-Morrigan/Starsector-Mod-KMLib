package kmlib.starsector.ui.widgets.lists;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the column choice's surfaces: the frozen persistence keys a consumer's save round-trips
 * through, the column count each choice feeds the list geometry, the default a fresh or unrecognised
 * save falls back to, and the count each segment draws. The keys are pinned as literals so a rename
 * that would silently reset every save's column choice fails here rather than shipping.
 */
final class ListColumnsTest {

    @Nested
    class FromKeyOrDefault {

        @Test
        void fromKeyOrDefaultResolvesAKnownKeyToItsChoice() {
            assertThat(ListColumns.fromKeyOrDefault("2"))
                .isEqualTo(ListColumns.TWO);
        }

        @Test
        void fromKeyOrDefaultFallsBackToOneColumnWhenTheKeyIsNull() {
            // A fresh save has stored no key, which must resolve to the single-column default
            // rather than fail.
            assertThat(ListColumns.fromKeyOrDefault(null))
                .isEqualTo(ListColumns.DEFAULT);
            assertThat(ListColumns.DEFAULT)
                .isEqualTo(ListColumns.ONE);
        }

        @Test
        void fromKeyOrDefaultFallsBackToDefaultWhenTheKeyIsUnrecognised() {
            // A key left by an older or modded build names no choice here, so the list defaults
            // rather than failing on it.
            assertThat(ListColumns.fromKeyOrDefault("no_such_count"))
                .isEqualTo(ListColumns.DEFAULT);
        }
    }

    @Nested
    class PersistenceKey {

        @Test
        void persistenceKeyIsTheFrozenKeyForEachChoice() {
            // Pinned as literals: renaming a key silently resets every save that stored that count
            // back to the default, so a change must break this test before it ships.
            assertThat(ListColumns.ONE.persistenceKey())
                .isEqualTo("1");
            assertThat(ListColumns.TWO.persistenceKey())
                .isEqualTo("2");
        }
    }

    @Nested
    class ResolveLabelText {

        @Test
        void resolveLabelTextIsTheCountItself() {
            // The segments carry the digits directly rather than a string ID a consumer resolves,
            // since a count standing for itself is not prose to translate.
            assertThat(ListColumns.ONE.resolveLabelText())
                .isEqualTo("1");
            assertThat(ListColumns.TWO.resolveLabelText())
                .isEqualTo("2");
        }
    }

    @Nested
    class ColumnCount {

        @Test
        void columnCountIsOneForTheSingleChoiceAndTwoForTheDoubleChoice() {
            assertThat(ListColumns.ONE.columnCount())
                .isEqualTo(1);
            assertThat(ListColumns.TWO.columnCount())
                .isEqualTo(2);
        }
    }
}
