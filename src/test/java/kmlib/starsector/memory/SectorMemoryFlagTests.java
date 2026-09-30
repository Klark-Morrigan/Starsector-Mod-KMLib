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
 * Pins the flag's contract against sector memory: a present key reads back its stored value, an
 * absent key falls to the default, and reads/writes before the sector exists resolve to the
 * default / no-op rather than dereferencing a null sector. Each form naming a sector is pinned to
 * act on that sector alone, with the running sector holding a different value beside it.
 */
class SectorMemoryFlagTests {

    private static final String KEY = "$kmlib_test_flag";

    private StoredMemoryFake handedMemoryFake;
    private SectorAPI handedSectorMock;
    private SectorMemoryFake sectorMemoryFake;
    private SectorMemoryFlag flag;

    @BeforeEach
    void openASectorMemory() {

        sectorMemoryFake = new SectorMemoryFake();

        // Default true so the "absent key" and "no sector" branches are distinguishable from a
        // stored false.
        flag = new SectorMemoryFlag(KEY, true);

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
    class IsSet {

        @Test
        void returnsTheStoredValueWhenTheKeyIsPresent() {

            sectorMemoryFake.storeValue(KEY, false);

            assertThat(flag.isSet())
                .isFalse();
        }

        @Test
        void returnsTheDefaultWhenTheKeyIsAbsent() {

            assertThat(flag.isSet())
                .isTrue();
        }

        @Test
        void returnsTheDefaultWhenTheSectorIsMissing() {

            sectorMemoryFake.removeSector();

            assertThat(flag.isSet())
                .isTrue();
        }

        @Test
        void readsTheHandedSectorRatherThanTheRunningOne() {

            sectorMemoryFake.storeValue(KEY, true);
            handedMemoryFake.storeValue(KEY, false);

            assertThat(flag.isSet(handedSectorMock))
                .isFalse();
        }

        @Test
        void returnsTheDefaultWhenTheHandedSectorIsMissing() {

            assertThat(flag.isSet((SectorAPI) null))
                .isTrue();
        }
    }

    @Nested
    class Set {

        @Test
        void writesTheValueToSectorMemoryAndReportsItLanded() {

            assertThat(flag.set(false))
                .isTrue();

            assertThat(sectorMemoryFake.readStoredValue(KEY))
                .isEqualTo(false);
        }

        @Test
        void doesNothingAndReportsNoWriteWhenTheSectorIsMissing() {

            sectorMemoryFake.removeSector();

            assertThat(flag.set(false))
                .isFalse();

            assertThat(sectorMemoryFake.countWritesTo(KEY))
                .isZero();
        }

        @Test
        void writesToTheHandedSectorAndLeavesTheRunningOneAlone() {

            assertThat(flag.set(handedSectorMock, false))
                .isTrue();

            assertThat(handedMemoryFake.readStoredValue(KEY))
                .isEqualTo(false);
            assertThat(sectorMemoryFake.countWritesTo(KEY))
                .isZero();
        }

        @Test
        void reportsNoWriteWhenTheHandedSectorIsMissing() {

            assertThat(flag.set(null, false))
                .isFalse();
        }
    }

    @Nested
    class Toggle {

        @Test
        void writesTheFlippedStoredValue() {

            sectorMemoryFake.storeValue(KEY, true);

            flag.toggle();

            assertThat(sectorMemoryFake.readStoredValue(KEY))
                .isEqualTo(false);
        }

        @Test
        void flipsFromTheDefaultWhenTheKeyIsAbsent() {

            flag.toggle();

            assertThat(sectorMemoryFake.readStoredValue(KEY))
                .isEqualTo(false);
        }

        @Test
        void flipsTheHandedSectorAndLeavesTheRunningOneAlone() {

            handedMemoryFake.storeValue(KEY, false);

            flag.toggle(handedSectorMock);

            assertThat(handedMemoryFake.readStoredValue(KEY))
                .isEqualTo(true);
            assertThat(sectorMemoryFake.countWritesTo(KEY))
                .isZero();
        }
    }
}
