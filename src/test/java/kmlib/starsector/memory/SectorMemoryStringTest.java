package kmlib.starsector.memory;

import kmlib.testfixtures.starsector.memory.SectorMemoryFake;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the string value's contract against sector memory: a present key reads back its stored value,
 * an absent key reads null, reads and writes before the sector exists resolve to null / no-op rather
 * than dereferencing a null sector, and set/clear report whether they actually touched memory so a
 * caller can gate a side effect on a real change.
 */
class SectorMemoryStringTest {

    private static final String KEY = "$kmlib_test_string";
    private static final String VALUE = "stored";

    private SectorMemoryFake sectorMemoryFake;
    private SectorMemoryString value;

    @BeforeEach
    void openASectorMemory() {

        sectorMemoryFake = new SectorMemoryFake();
        value = new SectorMemoryString(KEY);
    }

    @AfterEach
    void closeTheSectorMemory() {
        sectorMemoryFake.close();
    }

    @Nested
    class Get {

        @Test
        void getReturnsTheStoredValueWhenTheKeyIsPresent() {

            sectorMemoryFake.storeValue(KEY, VALUE);

            assertThat(value.get())
                .isEqualTo(VALUE);
        }

        @Test
        void getReturnsNullWhenTheKeyIsAbsent() {

            assertThat(value.get())
                .isNull();
        }

        @Test
        void getReturnsNullWhenTheSectorIsMissing() {

            sectorMemoryFake.removeSector();

            assertThat(value.get())
                .isNull();
        }
    }

    @Nested
    class IsSet {

        @Test
        void isSetIsTrueWhenTheKeyIsPresent() {

            sectorMemoryFake.storeValue(KEY, VALUE);

            assertThat(value.isSet())
                .isTrue();
        }

        @Test
        void isSetIsFalseWhenTheKeyIsAbsent() {

            assertThat(value.isSet())
                .isFalse();
        }

        @Test
        void isSetIsFalseWhenTheSectorIsMissing() {

            sectorMemoryFake.removeSector();

            assertThat(value.isSet())
                .isFalse();
        }
    }

    @Nested
    class Set {

        @Test
        void setWritesTheValueToSectorMemoryAndReportsItLanded() {

            assertThat(value.set(VALUE))
                .isTrue();

            assertThat(sectorMemoryFake.readStoredValue(KEY))
                .isEqualTo(VALUE);
        }

        @Test
        void setDoesNothingAndReportsNoWriteWhenTheSectorIsMissing() {

            sectorMemoryFake.removeSector();

            assertThat(value.set(VALUE))
                .isFalse();

            assertThat(sectorMemoryFake.countWritesTo(KEY))
                .isZero();
        }
    }

    @Nested
    class Clear {

        @Test
        void clearRemovesTheStoredValueAndReportsItRemoved() {

            sectorMemoryFake.storeValue(KEY, VALUE);

            assertThat(value.clear())
                .isTrue();

            assertThat(sectorMemoryFake.hasStoredValue(KEY))
                .isFalse();
        }

        @Test
        void clearReportsNoRemovalAndLeavesMemoryAloneWhenTheKeyIsAbsent() {

            assertThat(value.clear())
                .isFalse();

            assertThat(sectorMemoryFake.countRemovalsOf(KEY))
                .isZero();
        }

        @Test
        void clearDoesNothingAndReportsNoRemovalWhenTheSectorIsMissing() {

            sectorMemoryFake.removeSector();

            assertThat(value.clear())
                .isFalse();

            assertThat(sectorMemoryFake.countRemovalsOf(KEY))
                .isZero();
        }
    }
}
