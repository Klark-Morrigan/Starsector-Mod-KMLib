package kmlib.starsector.economy;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MonthlyReport;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Covers reading a sector's current monthly report off the sector itself, and leaving a sector that holds none as it
 * found it.
 */
final class MonthlyReportsTests {

    private static final String SHARED_DATA_KEY = "core_CEFSSharedDataKey";

    private Map<String, Object> persistentData;
    private SectorAPI sectorMock;

    @BeforeEach
    void setUp() {

        persistentData = new HashMap<>();
        sectorMock = mock(SectorAPI.class);

        when(sectorMock.getPersistentData())
            .thenReturn(persistentData);
    }

    @Nested
    class FindCurrentReport {

        @Test
        void readsTheReportTheSectorsSharedDataHolds() {

            var report = new MonthlyReport();
            var sharedDataMock = mock(SharedData.class);

            when(sharedDataMock.getCurrentReport())
                .thenReturn(report);

            persistentData.put(SHARED_DATA_KEY, sharedDataMock);

            assertThat(MonthlyReports.findCurrentReport(sectorMock))
                .containsSame(report);
        }

        @Test
        void answersEmptyWhereTheSectorHoldsNoSharedData() {

            assertThat(MonthlyReports.findCurrentReport(sectorMock))
                .isEmpty();
        }

        @Test
        void filesNothingInASectorHoldingNoSharedData() {

            MonthlyReports.findCurrentReport(sectorMock);

            assertThat(persistentData)
                .isEmpty();
        }
    }
}
