package kmlib.starsector.memory;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pins the flag's contract against sector memory: a present key reads back its stored value, an
 * absent key falls to the default, and reads/writes before the sector exists resolve to the
 * default / no-op rather than dereferencing a null sector.
 */
class SectorMemoryFlagTest {
    private static final String KEY = "$kmlib_test_flag";

    private MockedStatic<Global> globalMock;
    private SectorAPI sectorMock;
    private MemoryAPI memoryMock;
    private SectorMemoryFlag flag;

    @BeforeEach
    void setUp() {
        sectorMock = mock(SectorAPI.class);
        memoryMock = mock(MemoryAPI.class);
        when(sectorMock.getMemoryWithoutUpdate()).thenReturn(memoryMock);
        globalMock = mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sectorMock);
        // Default true so the "absent key" and "no sector" branches are distinguishable from
        // a stored false.
        flag = new SectorMemoryFlag(KEY, true);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    @Nested
    class IsSet {
        @Test
        void returnsTheStoredValueWhenTheKeyIsPresent() {
            when(memoryMock.contains(KEY)).thenReturn(true);
            when(memoryMock.getBoolean(KEY)).thenReturn(false);

            assertThat(flag.isSet()).isFalse();
        }

        @Test
        void returnsTheDefaultWhenTheKeyIsAbsent() {
            when(memoryMock.contains(KEY)).thenReturn(false);

            assertThat(flag.isSet()).isTrue();
        }

        @Test
        void returnsTheDefaultWhenTheSectorIsMissing() {
            globalMock.when(Global::getSector).thenReturn(null);

            assertThat(flag.isSet()).isTrue();
        }
    }

    @Nested
    class Set {
        @Test
        void writesTheValueToSectorMemory() {
            flag.set(false);

            verify(memoryMock).set(KEY, false);
        }

        @Test
        void doesNothingWhenTheSectorIsMissing() {
            globalMock.when(Global::getSector).thenReturn(null);

            flag.set(false);

            verifyNoInteractions(memoryMock);
        }
    }

    @Nested
    class Toggle {
        @Test
        void writesTheFlippedStoredValue() {
            when(memoryMock.contains(KEY)).thenReturn(true);
            when(memoryMock.getBoolean(KEY)).thenReturn(true);

            flag.toggle();

            verify(memoryMock).set(KEY, false);
        }

        @Test
        void flipsFromTheDefaultWhenTheKeyIsAbsent() {
            when(memoryMock.contains(KEY)).thenReturn(false);

            flag.toggle();

            verify(memoryMock).set(KEY, false);
        }
    }
}
