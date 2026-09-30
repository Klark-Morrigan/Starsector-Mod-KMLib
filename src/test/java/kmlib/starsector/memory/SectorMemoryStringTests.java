package kmlib.starsector.memory;

import com.fs.starfarer.api.campaign.SectorAPI;

import kmlib.testfixtures.starsector.memory.SectorMemoryFake;
import kmlib.testfixtures.starsector.memory.StoredMemoryFake;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the string value's contract against sector memory: a present key reads back its stored value,
 * an absent key reads null, reads and writes before the sector exists resolve to null / no-op rather
 * than dereferencing a null sector, and set/clear report whether they actually touched memory so a
 * caller can gate a side effect on a real change. Each form naming a sector is pinned to act on that
 * sector alone, with the running sector holding a different value beside it.
 */
class SectorMemoryStringTests {

    private static final String KEY = "$kmlib_test_string";
    private static final String OTHER_VALUE = "stored elsewhere";
    private static final String VALUE = "stored";

    private StoredMemoryFake handedMemoryFake;
    private SectorAPI handedSectorMock;
    private SectorMemoryFake sectorMemoryFake;
    private SectorMemoryString value;

    @BeforeEach
    void openASectorMemory() {

        sectorMemoryFake = new SectorMemoryFake();
        value = new SectorMemoryString(KEY);

        // A second sector beside the running one, so a form naming a sector is seen to act on that
        // sector rather than reach the running one through Global.
        handedMemoryFake = new StoredMemoryFake();
        handedSectorMock = mock(SectorAPI.class);

        when(handedSectorMock.getMemoryWithoutUpdate())
            .thenReturn(handedMemoryFake.getMemory());
    }

    @AfterEach
    void closeTheSectorMemory() {
        sectorMemoryFake.close();
    }

    @Nested
    class Get {

        @Test
        void returnsTheStoredValueWhenTheKeyIsPresent() {

            sectorMemoryFake.storeValue(KEY, VALUE);

            assertThat(value.get())
                .isEqualTo(VALUE);
        }

        @Test
        void returnsNullWhenTheKeyIsAbsent() {

            assertThat(value.get())
                .isNull();
        }

        @Test
        void returnsNullWhenTheSectorIsMissing() {

            sectorMemoryFake.removeSector();

            assertThat(value.get())
                .isNull();
        }

        @Test
        void readsTheHandedSectorRatherThanTheRunningOne() {

            sectorMemoryFake.storeValue(KEY, VALUE);
            handedMemoryFake.storeValue(KEY, OTHER_VALUE);

            assertThat(value.get(handedSectorMock))
                .isEqualTo(OTHER_VALUE);
        }

        @Test
        void returnsNullWhenTheHandedSectorIsMissing() {

            assertThat(value.get((SectorAPI) null))
                .isNull();
        }
    }

    @Nested
    class IsSet {

        @Test
        void isTrueWhenTheKeyIsPresent() {

            sectorMemoryFake.storeValue(KEY, VALUE);

            assertThat(value.isSet())
                .isTrue();
        }

        @Test
        void isFalseWhenTheKeyIsAbsent() {

            assertThat(value.isSet())
                .isFalse();
        }

        @Test
        void isFalseWhenTheSectorIsMissing() {

            sectorMemoryFake.removeSector();

            assertThat(value.isSet())
                .isFalse();
        }

        @Test
        void readsTheHandedSectorRatherThanTheRunningOne() {

            sectorMemoryFake.storeValue(KEY, VALUE);

            assertThat(value.isSet(handedSectorMock))
                .isFalse();
        }
    }

    @Nested
    class Set {

        @Test
        void writesTheValueToSectorMemoryAndReportsItLanded() {

            assertThat(value.set(VALUE))
                .isTrue();

            assertThat(sectorMemoryFake.readStoredValue(KEY))
                .isEqualTo(VALUE);
        }

        @Test
        void doesNothingAndReportsNoWriteWhenTheSectorIsMissing() {

            sectorMemoryFake.removeSector();

            assertThat(value.set(VALUE))
                .isFalse();

            assertThat(sectorMemoryFake.countWritesTo(KEY))
                .isZero();
        }

        @Test
        void writesToTheHandedSectorAndLeavesTheRunningOneAlone() {

            assertThat(value.set(handedSectorMock, VALUE))
                .isTrue();

            assertThat(handedMemoryFake.readStoredValue(KEY))
                .isEqualTo(VALUE);
            assertThat(sectorMemoryFake.countWritesTo(KEY))
                .isZero();
        }
    }

    @Nested
    class Clear {

        @Test
        void removesTheStoredValueAndReportsItRemoved() {

            sectorMemoryFake.storeValue(KEY, VALUE);

            assertThat(value.clear())
                .isTrue();

            assertThat(sectorMemoryFake.hasStoredValue(KEY))
                .isFalse();
        }

        @Test
        void reportsNoRemovalAndLeavesMemoryAloneWhenTheKeyIsAbsent() {

            assertThat(value.clear())
                .isFalse();

            assertThat(sectorMemoryFake.countRemovalsOf(KEY))
                .isZero();
        }

        @Test
        void doesNothingAndReportsNoRemovalWhenTheSectorIsMissing() {

            sectorMemoryFake.removeSector();

            assertThat(value.clear())
                .isFalse();

            assertThat(sectorMemoryFake.countRemovalsOf(KEY))
                .isZero();
        }

        @Test
        void removesFromTheHandedSectorAndLeavesTheRunningOneAlone() {

            sectorMemoryFake.storeValue(KEY, VALUE);
            handedMemoryFake.storeValue(KEY, OTHER_VALUE);

            assertThat(value.clear(handedSectorMock))
                .isTrue();

            assertThat(handedMemoryFake.hasStoredValue(KEY))
                .isFalse();
            assertThat(sectorMemoryFake.readStoredValue(KEY))
                .isEqualTo(VALUE);
        }
    }
}
