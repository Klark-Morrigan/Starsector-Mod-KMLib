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
import static org.mockito.Mockito.when;

/**
 * Pins the shared guard: sector memory reads back when the sector exists, and resolves to null
 * before it does rather than dereferencing a null sector.
 */
class SectorMemoryAccessTest {

    private MockedStatic<Global> globalMock;
    private SectorAPI sectorMock;
    private MemoryAPI memoryMock;

    @BeforeEach
    void setUp() {
        sectorMock = mock(SectorAPI.class);
        memoryMock = mock(MemoryAPI.class);
        when(sectorMock.getMemoryWithoutUpdate()).thenReturn(memoryMock);
        globalMock = mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sectorMock);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    @Nested
    class ReadSectorMemory {
        @Test
        void returnsTheSectorMemoryWhenTheSectorExists() {
            assertThat(SectorMemoryAccess.readSectorMemory()).isSameAs(memoryMock);
        }

        @Test
        void returnsNullWhenTheSectorIsMissing() {
            globalMock.when(Global::getSector).thenReturn(null);

            assertThat(SectorMemoryAccess.readSectorMemory()).isNull();
        }
    }
}
