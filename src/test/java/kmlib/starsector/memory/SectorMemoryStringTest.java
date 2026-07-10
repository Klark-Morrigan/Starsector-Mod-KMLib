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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pins the string value's contract against sector memory: a present key reads back its stored value,
 * an absent key reads null, reads and writes before the sector exists resolve to null / no-op rather
 * than dereferencing a null sector, and set/clear report whether they actually touched memory so a
 * caller can gate a side effect on a real change.
 */
class SectorMemoryStringTest {
    private static final String KEY = "$kmlib_test_string";
    private static final String VALUE = "stored";

    private MockedStatic<Global> globalMock;
    private SectorAPI sectorMock;
    private MemoryAPI memoryMock;
    private SectorMemoryString value;

    @BeforeEach
    void setUp() {
        sectorMock = mock(SectorAPI.class);
        memoryMock = mock(MemoryAPI.class);
        when(sectorMock.getMemoryWithoutUpdate()).thenReturn(memoryMock);
        globalMock = mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sectorMock);
        value = new SectorMemoryString(KEY);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    @Nested
    class Get {
        @Test
        void returnsTheStoredValueWhenTheKeyIsPresent() {
            when(memoryMock.contains(KEY)).thenReturn(true);
            when(memoryMock.getString(KEY)).thenReturn(VALUE);

            assertThat(value.get()).isEqualTo(VALUE);
        }

        @Test
        void returnsNullWhenTheKeyIsAbsent() {
            when(memoryMock.contains(KEY)).thenReturn(false);

            assertThat(value.get()).isNull();
        }

        @Test
        void returnsNullWhenTheSectorIsMissing() {
            globalMock.when(Global::getSector).thenReturn(null);

            assertThat(value.get()).isNull();
        }
    }

    @Nested
    class IsSet {
        @Test
        void isTrueWhenTheKeyIsPresent() {
            when(memoryMock.contains(KEY)).thenReturn(true);

            assertThat(value.isSet()).isTrue();
        }

        @Test
        void isFalseWhenTheKeyIsAbsent() {
            when(memoryMock.contains(KEY)).thenReturn(false);

            assertThat(value.isSet()).isFalse();
        }

        @Test
        void isFalseWhenTheSectorIsMissing() {
            globalMock.when(Global::getSector).thenReturn(null);

            assertThat(value.isSet()).isFalse();
        }
    }

    @Nested
    class Set {
        @Test
        void writesTheValueToSectorMemoryAndReportsItLanded() {
            assertThat(value.set(VALUE)).isTrue();

            verify(memoryMock).set(KEY, VALUE);
        }

        @Test
        void doesNothingAndReportsNoWriteWhenTheSectorIsMissing() {
            globalMock.when(Global::getSector).thenReturn(null);

            assertThat(value.set(VALUE)).isFalse();

            verifyNoInteractions(memoryMock);
        }
    }

    @Nested
    class Clear {
        @Test
        void removesTheStoredValueAndReportsItRemoved() {
            when(memoryMock.contains(KEY)).thenReturn(true);

            assertThat(value.clear()).isTrue();

            verify(memoryMock).unset(KEY);
        }

        @Test
        void reportsNoRemovalAndLeavesMemoryAloneWhenTheKeyIsAbsent() {
            when(memoryMock.contains(KEY)).thenReturn(false);

            assertThat(value.clear()).isFalse();

            verify(memoryMock, never()).unset(KEY);
        }

        @Test
        void doesNothingAndReportsNoRemovalWhenTheSectorIsMissing() {
            globalMock.when(Global::getSector).thenReturn(null);

            assertThat(value.clear()).isFalse();

            verifyNoInteractions(memoryMock);
        }
    }
}
