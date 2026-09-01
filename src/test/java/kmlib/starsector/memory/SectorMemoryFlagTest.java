package kmlib.starsector.memory;

import kmlib.testfixtures.starsector.memory.SectorMemoryFake;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the flag's contract against sector memory: a present key reads back its stored value, an
 * absent key falls to the default, and reads/writes before the sector exists resolve to the
 * default / no-op rather than dereferencing a null sector.
 */
class SectorMemoryFlagTest {

    private static final String KEY = "$kmlib_test_flag";

    private SectorMemoryFake sectorMemoryFake;
    private SectorMemoryFlag flag;

    @BeforeEach
    void openASectorMemory() {

        sectorMemoryFake = new SectorMemoryFake();

        // Default true so the "absent key" and "no sector" branches are distinguishable from a
        // stored false.
        flag = new SectorMemoryFlag(KEY, true);
    }

    @AfterEach
    void closeTheSectorMemory() {
        sectorMemoryFake.close();
    }

    @Nested
    class IsSet {

        @Test
        void isSetReturnsTheStoredValueWhenTheKeyIsPresent() {

            sectorMemoryFake.storeValue(KEY, false);

            assertThat(flag.isSet())
                .isFalse();
        }

        @Test
        void isSetReturnsTheDefaultWhenTheKeyIsAbsent() {

            assertThat(flag.isSet())
                .isTrue();
        }

        @Test
        void isSetReturnsTheDefaultWhenTheSectorIsMissing() {

            sectorMemoryFake.removeSector();

            assertThat(flag.isSet())
                .isTrue();
        }
    }

    @Nested
    class Set {

        @Test
        void setWritesTheValueToSectorMemoryAndReportsItLanded() {

            assertThat(flag.set(false))
                .isTrue();

            assertThat(sectorMemoryFake.readStoredValue(KEY))
                .isEqualTo(false);
        }

        @Test
        void setDoesNothingAndReportsNoWriteWhenTheSectorIsMissing() {

            sectorMemoryFake.removeSector();

            assertThat(flag.set(false))
                .isFalse();

            assertThat(sectorMemoryFake.countWritesTo(KEY))
                .isZero();
        }
    }

    @Nested
    class Toggle {

        @Test
        void toggleWritesTheFlippedStoredValue() {

            sectorMemoryFake.storeValue(KEY, true);

            flag.toggle();

            assertThat(sectorMemoryFake.readStoredValue(KEY))
                .isEqualTo(false);
        }

        @Test
        void toggleFlipsFromTheDefaultWhenTheKeyIsAbsent() {

            flag.toggle();

            assertThat(sectorMemoryFake.readStoredValue(KEY))
                .isEqualTo(false);
        }
    }
}
